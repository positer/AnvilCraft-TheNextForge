package dev.anvilcraft.thenextforge.api;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.*;

/** 数据定义的写入快照：移除只使用当时的定义，不依赖 /reload 后的定义或当前对象分类。 */
public final class ManagedEmbeddings {
    private ManagedEmbeddings() { }
    public static TnfDefinitions.Embedding definition(String id) {
        var api = TnfApi.embedding(id);
        if (api != null) return api;
        var legacy = EmbedData.spec(id);
        if (legacy == null) return null;
        Map<TargetKind, List<TnfDefinitions.AttributeGain>> attributes = new EnumMap<>(TargetKind.class);
        legacy.attributes().forEach((kind, values) -> attributes.put(kind, values.stream().map(v ->
            new TnfDefinitions.AttributeGain(v.attribute(), v.operation(), v.amount())).toList()));
        return new TnfDefinitions.Embedding(true, legacy.label(), legacy.unique(), legacy.kinds(), legacy.color(),
            attributes, legacy.durabilityFactor(), legacy.fortune(), legacy.unbreakable(), legacy.extraSlots(), true, List.of());
    }
    public static EmbedMaterials.Definition materialDefinition(String id, TnfDefinitions.Embedding spec) {
        if (!spec.enabled()) return null;
        return new EmbedMaterials.Definition(id, spec.label().isBlank() ? id : spec.label(), spec.unique(), spec.kinds(), spec.color());
    }
    public static String describe(TnfDefinitions.Embedding spec, TargetKind kind) {
        List<String> text = new ArrayList<>();
        for (var role : spec.kinds()) {
            if (kind != null && kind != role) continue;
            for (var gain : spec.attributes().getOrDefault(role, List.of())) {
                var attribute = EmbedData.attribute(gain.attribute());
                String name = attribute == null ? gain.attribute() : net.minecraft.network.chat.Component.translatable(attribute.value().getDescriptionId()).getString();
                String amount = gain.operation().equals("add_value") ? String.format(Locale.ROOT, "%+.2f", gain.amount())
                    : String.format(Locale.ROOT, "%+.1f%%", gain.amount() * 100);
                text.add(name + " " + amount + "（" + gain.operation() + "）");
            }
        }
        if (spec.durabilityFactor() != 1) text.add("耐久 ×" + spec.durabilityFactor());
        if (spec.fortune() > 0) text.add("时运 +" + spec.fortune());
        if (spec.unbreakable()) text.add("无法破坏");
        if (spec.extraSlots() > 0) text.add("嵌入槽位 +" + spec.extraSlots());
        for (var effect : spec.effects()) {
            try { text.add(TnfApi.embedEffect(effect.type()).describe(effect.config())); }
            catch (RuntimeException absentOnClient) { text.add(effect.type().toString()); }
        }
        return String.join("，", new LinkedHashSet<>(text));
    }
    /** 客户端语言视图；持久化定义、旧 describe 合同与回滚数值不变。 */
    public static String describeLocalized(TnfDefinitions.Embedding spec, TargetKind kind) {
        return describeLocalizedForRoles(spec, kind == null ? spec.kinds() : Set.of(kind));
    }
    private static String describeLocalizedForRoles(TnfDefinitions.Embedding spec, Set<TargetKind> roles) {
        var text = new LinkedHashSet<String>();
        Map<String, Double> amounts = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        Map<String, String> operations = new LinkedHashMap<>();
        for (var role : roles) {
            for (var gain : spec.attributes().getOrDefault(role, List.of())) {
                var attribute = EmbedData.attribute(gain.attribute());
                String key = (attribute == null ? gain.attribute() : attribute.getRegisteredName()) + "/" + gain.operation();
                names.put(key, attribute == null ? gain.attribute() : net.minecraft.network.chat.Component.translatable(attribute.value().getDescriptionId()).getString());
                operations.put(key, gain.operation());
                if (gain.operation().equals("add_multiplied_total")) amounts.merge(key, gain.amount(), (a,b) -> (1+a)*(1+b)-1);
                else amounts.merge(key, gain.amount(), Double::sum);
            }
        }
        amounts.forEach((key, value) -> {
            String operation = operations.get(key);
            String amount = operation.equals("add_value") ? String.format(Locale.ROOT, "%+.2f", value) : String.format(Locale.ROOT, "%+.1f%%", value * 100);
            text.add(names.get(key) + " " + amount + " (" + net.minecraft.network.chat.Component.translatable("thenextforge.tooltip." + operation).getString() + ")");
        });
        if (spec.durabilityFactor() != 1) text.add(net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.durability", spec.durabilityFactor()).getString());
        if (spec.fortune() > 0 && (roles.contains(TargetKind.TOOL) || roles.contains(TargetKind.AMULET))) text.add(net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.fortune", spec.fortune()).getString());
        if (spec.unbreakable()) text.add(net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.unbreakable").getString());
        if (spec.extraSlots() > 0) text.add(net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.slots", spec.extraSlots()).getString());
        for (var effect : spec.effects()) {
            try { text.add(TnfApi.embedEffect(effect.type()).describe(effect.config())); }
            catch (RuntimeException absentOnClient) { text.add(effect.type().toString()); }
        }
        return String.join(net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.separator").getString(), text);
    }
    /** 重算只读嵌入当时的定义，不用重载后的倍率改写旧装备。 */
    public static Map<Integer, Double> durabilityFactors(ItemStack stack) {
        Map<Integer, Double> result = new LinkedHashMap<>();
        for (var entry : journal(stack)) {
            var spec = TnfDefinitions.Embedding.CODEC.parse(JsonOps.INSTANCE, entry.get("definition")).getOrThrow();
            result.put(entry.get("position").getAsInt(), spec.durabilityFactor());
        }
        return result;
    }
    public static String describeAppliedLocalized(ItemStack target, String materialId) {
        var text = new LinkedHashSet<String>();
        for (var entry : journal(target)) if (entry.get("material").getAsString().equals(materialId)) {
            var spec = TnfDefinitions.Embedding.CODEC.parse(JsonOps.INSTANCE, entry.get("definition")).getOrThrow();
            Set<TargetKind> roles = new LinkedHashSet<>();
            entry.getAsJsonArray("roles").forEach(v -> roles.add(TargetKind.valueOf(v.getAsString())));
            String value = describeLocalizedForRoles(spec, roles);
            text.add(spec.unique() ? net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.unique", value).getString() : value);
        }
        return String.join(net.minecraft.network.chat.Component.translatable("thenextforge.tooltip.separator").getString(), text);
    }
    private static List<JsonObject> journal(ItemStack target) {
        List<String> entries = target.getOrDefault(ModDataComponents.EMBED_JOURNAL.get(), List.of());
        return entries.stream()
            .map(s -> JsonParser.parseString(s).getAsJsonObject()).toList();
    }
    public static String describeApplied(ItemStack target, String materialId) {
        Set<String> descriptions = new LinkedHashSet<>();
        for (var entry : journal(target)) if (entry.get("material").getAsString().equals(materialId)) {
            var spec = TnfDefinitions.Embedding.CODEC.parse(JsonOps.INSTANCE, entry.get("definition")).getOrThrow();
            descriptions.add(describe(spec, null));
        }
        return String.join("；", descriptions);
    }
    private static void save(ItemStack target, List<JsonObject> journal) {
        List<String> encoded = journal.stream().map(JsonObject::toString).toList();
        if (encoded.stream().anyMatch(s -> s.length() > 30000)) throw new IllegalArgumentException("Embedding snapshot exceeds 30000 characters");
        if (encoded.isEmpty()) target.remove(ModDataComponents.EMBED_JOURNAL.get());
        else target.set(ModDataComponents.EMBED_JOURNAL.get(), encoded);
    }
    public static boolean apply(ItemStack target, String materialId, TnfDefinitions.Embedding spec,
                                 List<TargetKind> requestedRoles, ServerLevel level) {
        if (!spec.enabled()) throw new IllegalArgumentException("Embedding disabled: " + materialId);
        Set<TargetKind> roles = new LinkedHashSet<>(requestedRoles); roles.retainAll(spec.kinds());
        if (roles.isEmpty()) return false;
        ItemStack copy = target.copy();
        List<JsonObject> history = new ArrayList<>(journal(copy));
        int token = history.stream().mapToInt(v -> v.get("token").getAsInt()).max().orElse(0) + 1;
        int position = copy.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).embeds().size();
        var entry = new JsonObject(); entry.addProperty("version", 1); entry.addProperty("material", materialId);
        entry.addProperty("position", position); entry.addProperty("token", token);
        entry.add("definition", TnfDefinitions.Embedding.CODEC.encodeStart(JsonOps.INSTANCE, spec).getOrThrow());
        JsonArray appliedRoles = new JsonArray(); roles.forEach(v -> appliedRoles.add(v.name())); entry.add("roles", appliedRoles);
        var durability = copy.get(ModDataComponents.DURABILITY_HISTORY.get());
        entry.addProperty("baseline_before", durability == null ? copy.getMaxDamage() : durability.baseline());
        entry.add("unbreakable_before", encodeComponent(copy, DataComponents.UNBREAKABLE));
        var context = new TnfApi.EmbedContext(copy, materialId, token, roles, level, "attributes", new JsonObject());
        for (var role : roles) {
            int index = 0;
            for (var gain : spec.attributes().getOrDefault(role, List.of())) {
                context.addAttribute("definition/" + role + "/" + index++, TnfApi.requireAttribute(gain.attribute()),
                    gain.amount(), TnfApi.operation(gain.operation()), slot(role));
            }
        }
        if (spec.durabilityFactor() != 1) EmbedMaterials.scaleDurability(copy, spec.durabilityFactor());
        int fortuneAdded = 0;
        if (spec.fortune() > 0 && (roles.contains(TargetKind.TOOL) || roles.contains(TargetKind.AMULET))) {
            var fortune = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,
                    Identifier.withDefaultNamespace("fortune")));
            var enchantments = copy.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            int before = enchantments.getLevel(fortune);
            fortuneAdded = Math.min(255 - before, spec.fortune());
            var changed = new ItemEnchantments.Mutable(enchantments); changed.set(fortune, before + fortuneAdded);
            copy.set(DataComponents.ENCHANTMENTS, changed.toImmutable());
        }
        entry.addProperty("fortune_added", fortuneAdded);
        if (spec.unbreakable()) copy.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
        JsonArray states = new JsonArray();
        for (int i = 0; i < spec.effects().size(); i++) {
            var effect = spec.effects().get(i); JsonObject state = new JsonObject();
            TnfApi.embedEffect(effect.type()).apply(new TnfApi.EmbedContext(copy, materialId, token, roles, level,
                effect.type() + "/" + i, state), effect.config());
            states.add(state);
        }
        entry.add("states", states);
        history.add(entry); save(copy, history);
        TnfApi.copyComponents(target, copy);
        return true;
    }
    public static EquipmentSlotGroup slot(TargetKind kind) {
        return kind == TargetKind.ARMOR ? EquipmentSlotGroup.ARMOR : kind == TargetKind.AMULET ? EquipmentSlotGroup.ANY : EquipmentSlotGroup.MAINHAND;
    }
    private static <T> JsonElement encodeComponent(ItemStack stack, DataComponentType<T> type) {
        T value = stack.get(type);
        return value == null ? JsonNull.INSTANCE : Objects.requireNonNull(type.codec()).encodeStart(JsonOps.INSTANCE, value).getOrThrow();
    }
    private static <T> void restoreComponent(ItemStack stack, DataComponentType<T> type, JsonElement value) {
        if (value.isJsonNull()) stack.remove(type);
        else stack.set(type, Objects.requireNonNull(type.codec()).parse(JsonOps.INSTANCE, value).getOrThrow());
    }
    /** 移除前调用；失败不修改原物品，调用方也不得弹出材料记录。 */
    public static boolean remove(ItemStack target, String materialId, int position, ServerLevel level) {
        List<JsonObject> history = new ArrayList<>(journal(target));
        int found = -1;
        for (int i = history.size() - 1; i >= 0; i--) if (history.get(i).get("position").getAsInt() == position
            && history.get(i).get("material").getAsString().equals(materialId)) { found = i; break; }
        if (found < 0) return false;
        JsonObject entry = history.get(found);
        if (entry.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported embedding journal version");
        var spec = TnfDefinitions.Embedding.CODEC.parse(JsonOps.INSTANCE, entry.get("definition")).getOrThrow();
        ItemStack copy = target.copy();
        int token = entry.get("token").getAsInt();
        Set<TargetKind> roles = new LinkedHashSet<>();
        entry.getAsJsonArray("roles").forEach(v -> roles.add(TargetKind.valueOf(v.getAsString())));
        for (int i = spec.effects().size() - 1; i >= 0; i--) {
            var effect = spec.effects().get(i);
            TnfApi.embedEffect(effect.type()).remove(new TnfApi.EmbedContext(copy, materialId, token, roles, level,
                effect.type() + "/" + i, entry.getAsJsonArray("states").get(i).getAsJsonObject().deepCopy()), effect.config());
        }
        var modifiers = copy.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        String prefix = "embed_api/" + token + "/";
        var kept = modifiers.modifiers().stream().filter(v -> !v.modifier().id().getNamespace().equals("thenextforge")
            || !v.modifier().id().getPath().startsWith(prefix)).toList();
        copy.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(kept));
        if (spec.durabilityFactor() != 1) ForgeDurability.restoreBaseline(copy, entry.get("baseline_before").getAsInt());
        int fortuneAdded = entry.get("fortune_added").getAsInt();
        if (fortuneAdded > 0) {
            var enchantments = copy.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            var changed = new ItemEnchantments.Mutable(enchantments);
            var fortuneKey = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, Identifier.withDefaultNamespace("fortune"));
            var holder = enchantments.keySet().stream().filter(v -> v.unwrapKey().map(k -> k.equals(fortuneKey)).orElse(false)).findFirst();
            holder.ifPresent(v -> changed.set(v, Math.max(0, enchantments.getLevel(v) - fortuneAdded)));
            copy.set(DataComponents.ENCHANTMENTS, changed.toImmutable());
        }
        if (spec.unbreakable()) restoreComponent(copy, DataComponents.UNBREAKABLE, entry.get("unbreakable_before"));
        history.remove(found); save(copy, history); TnfApi.copyComponents(target, copy);
        return true;
    }
}
