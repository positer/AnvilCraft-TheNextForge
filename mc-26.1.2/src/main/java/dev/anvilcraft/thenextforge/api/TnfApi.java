package dev.anvilcraft.thenextforge.api;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.thenextforge.forging.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.state.BlockState;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/** TNF 公共扩展接口 v1。应在附属模组 common setup 的 enqueueWork 中注册，数据包优先于代码默认映射。 */
public final class TnfApi {
    public static final int API_VERSION = 1;
    private TnfApi() { }
    public interface Registration extends AutoCloseable { @Override void close(); }
    @FunctionalInterface public interface UpgradeEffect { String apply(UpgradeContext context, JsonObject config); }
    public interface EmbedEffect {
        void apply(EmbedContext context, JsonObject config);
        void remove(EmbedContext context, JsonObject frozenConfig);
        String describe(JsonObject config);
    }
    private static final Map<String, TnfDefinitions.Target> TARGETS = new ConcurrentSkipListMap<>();
    private static final Map<String, TnfDefinitions.Upgrades> UPGRADES = new ConcurrentSkipListMap<>();
    private static final Map<String, TnfDefinitions.Base> BASES = new ConcurrentSkipListMap<>();
    private static final Map<String, ForgingMaterial> HAMMERS = new ConcurrentSkipListMap<>();
    private static final Map<String, TnfDefinitions.Embedding> EMBEDDINGS = new ConcurrentSkipListMap<>();
    private static final Map<Identifier, UpgradeEffect> UPGRADE_EFFECTS = new ConcurrentSkipListMap<>();
    private static final Map<Identifier, EmbedEffect> EMBED_EFFECTS = new ConcurrentSkipListMap<>();

    static {
        UPGRADE_EFFECTS.put(Identifier.parse("thenextforge:attribute"), (context, config) -> {
            String name = config.get("attribute").getAsString();
            var attribute = requireAttribute(name);
            var operation = operation(config.has("operation") ? config.get("operation").getAsString() : "add_value");
            context.addAttribute(name, attribute, context.amount(), operation);
            return net.minecraft.network.chat.Component.translatable(attribute.value().getDescriptionId()).getString() + " " + (context.amount() >= 0 ? "+" : "") + context.amount();
        });
        UPGRADE_EFFECTS.put(Identifier.parse("thenextforge:durability"), (context, config) -> {
            if (1 + context.amount() <= 0) throw new IllegalArgumentException("Durability factor must be positive");
            ForgeDurability.upgrade(context.target(), context.line(), context.level(), 1 + context.amount());
            return "耐久 " + String.format(java.util.Locale.ROOT, "%+.1f%%", context.amount() * 100);
        });
    }

    private static <T> Registration put(Map<String, T> map, String selector, T value, Codec<T> codec) {
        Objects.requireNonNull(value); validateSelector(selector);
        T checked = codec.parse(JsonOps.INSTANCE, codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow()).getOrThrow();
        if (map.putIfAbsent(selector, checked) != null) throw new IllegalStateException("Duplicate TNF registration: " + selector);
        return () -> map.remove(selector, checked);
    }
    private static void validateSelector(String selector) {
        Identifier.parse(selector.startsWith("#") ? selector.substring(1) : selector);
    }
    public static Registration registerTarget(String itemOrTag, TnfDefinitions.Target target) { return put(TARGETS, itemOrTag, target, TnfDefinitions.Target.CODEC); }
    public static Registration registerUpgrades(String itemOrTag, TnfDefinitions.Upgrades upgrades) { return put(UPGRADES, itemOrTag, upgrades, TnfDefinitions.Upgrades.CODEC); }
    public static Registration registerBase(String blockOrTag, TnfDefinitions.Base base) { return put(BASES, blockOrTag, base, TnfDefinitions.Base.CODEC); }
    public static Registration registerHammer(String itemOrTag, ForgingMaterial material) { return put(HAMMERS, itemOrTag, material, TnfDefinitions.MATERIAL); }
    public static Registration registerEmbedding(String itemOrTag, TnfDefinitions.Embedding embedding) { return put(EMBEDDINGS, itemOrTag, embedding, TnfDefinitions.Embedding.CODEC); }
    public static Registration registerUpgradeEffect(Identifier id, UpgradeEffect effect) {
        Objects.requireNonNull(id); Objects.requireNonNull(effect);
        if (UPGRADE_EFFECTS.putIfAbsent(id, effect) != null) throw new IllegalStateException("Duplicate upgrade effect: " + id);
        return () -> UPGRADE_EFFECTS.remove(id, effect);
    }
    public static Registration registerEmbedEffect(Identifier id, EmbedEffect effect) {
        Objects.requireNonNull(id); Objects.requireNonNull(effect);
        if (EMBED_EFFECTS.putIfAbsent(id, effect) != null) throw new IllegalStateException("Duplicate embed effect: " + id);
        return () -> EMBED_EFFECTS.remove(id, effect);
    }
    private static <T> T itemDefault(Map<String,T> map, ItemStack item) {
        if (map.isEmpty()) return null;
        T exact = map.get(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
        if (exact != null) return exact;
        for (var entry : map.entrySet()) if (entry.getKey().startsWith("#")
            && item.is(TagKey.create(Registries.ITEM, Identifier.parse(entry.getKey().substring(1))))) return entry.getValue();
        return null;
    }
    public static TnfDefinitions.Target target(ItemStack item) {
        if (item.isEmpty()) return null;
        var data = item.getItem().builtInRegistryHolder().getData(TnfDataMaps.TARGETS);
        return data != null ? data : itemDefault(TARGETS, item);
    }
    public static TnfDefinitions.Upgrades upgrades(ItemStack item) {
        if (item.isEmpty()) return null;
        var data = item.getItem().builtInRegistryHolder().getData(TnfDataMaps.UPGRADES);
        return data != null ? data : itemDefault(UPGRADES, item);
    }
    public static TnfDefinitions.Base base(BlockState state) {
        var data = state.getBlock().builtInRegistryHolder().getData(TnfDataMaps.BASES);
        if (data != null) return data;
        var exact = BASES.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (exact != null) return exact;
        for (var entry : BASES.entrySet()) if (entry.getKey().startsWith("#")
            && state.is(TagKey.create(Registries.BLOCK, Identifier.parse(entry.getKey().substring(1))))) return entry.getValue();
        return null;
    }
    public static ForgingMaterial hammer(ItemStack item) {
        if (item.isEmpty()) return null;
        var data = item.getItem().builtInRegistryHolder().getData(TnfDataMaps.HAMMERS);
        return data != null ? data : itemDefault(HAMMERS, item);
    }
    public static TnfDefinitions.Embedding embedding(ItemStack item) {
        if (item.isEmpty()) return null;
        var data = item.getItem().builtInRegistryHolder().getData(TnfDataMaps.EMBEDDINGS);
        return data != null ? data : itemDefault(EMBEDDINGS, item);
    }
    public static TnfDefinitions.Embedding embedding(String materialId) {
        if (materialId.startsWith("#") || !hasEmbeddingOverrides()) return null;
        var key = Identifier.parse(materialId);
        var holder = BuiltInRegistries.ITEM.get(key).orElse(null);
        if (holder == null) return null;
        var mapped = holder.getData(TnfDataMaps.EMBEDDINGS);
        if (mapped != null) return mapped;
        return EMBEDDINGS.isEmpty() ? null : itemDefault(EMBEDDINGS, new ItemStack(holder));
    }
    public static boolean builtinEffectsEnabled(String materialId) {
        var spec = embedding(materialId);
        return spec == null || (spec.enabled() && spec.inheritBuiltinEffects());
    }
    public static boolean hasEmbeddingOverrides() {
        return !EMBEDDINGS.isEmpty() || !BuiltInRegistries.ITEM.getDataMap(TnfDataMaps.EMBEDDINGS).isEmpty();
    }
    public static boolean canUseLine(ItemStack target, ForgingLine line) {
        var rule = target(target);
        if (rule != null) return rule.enabled() && rule.lines().contains(line);
        return line == ForgingLine.ANVIL || TargetKind.rolesOf(target).stream().anyMatch(TargetKind::canUseGrindstone);
    }
    public static int embedCount(ItemStack target, String materialId) {
        return (int) target.getOrDefault(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get(), ForgingData.EMPTY).embeds().stream().filter(materialId::equals).count();
    }
    /** 验证将参与抽取的扩展效果；错误在支付经验之前向玩家报告。 */
    public static void validateUpgrades(ItemStack target, ForgingLine line) {
        var pool = upgrades(target);
        if (pool == null) return;
        for (var role : TargetKind.rolesOf(target)) {
            var rule = pool.rule(role, line);
            if (rule == null) continue;
            for (var outcome : rule.outcomes()) {
                if (!UPGRADE_EFFECTS.containsKey(outcome.effect().type())) throw new IllegalArgumentException("Missing upgrade effect: " + outcome.effect().type());
                if (outcome.effect().type().toString().equals("thenextforge:attribute")) {
                    var config = outcome.effect().config(); requireAttribute(config.get("attribute").getAsString());
                    if (config.has("operation")) operation(config.get("operation").getAsString());
                }
            }
        }
    }
    public record EmbedResult(boolean applied, String message) { }
    /** 独立嵌入入口：与锤击共用定义和效果执行器，成功才消耗一份材料。 */
    public static EmbedResult tryEmbed(ItemStack target, ItemStack material, net.minecraft.server.level.ServerLevel level) {
        if (target == material || target.isEmpty() || material.isEmpty()) return new EmbedResult(false, "对象和材料必须是不同的非空栈");
        var kind = TargetKind.of(target);
        var definition = EmbedMaterials.of(material);
        var data = target.getOrDefault(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        if (kind == null || !EmbedMaterials.allowsFor(definition, target)) return new EmbedResult(false, "材料不适用于对象");
        if (data.locked()) return new EmbedResult(false, "物品已封印");
        if (definition.unique() && data.hasEmbed(definition.id())) return new EmbedResult(false, "唯一材料已存在");
        if (data.embeds().size() >= EmbedMaterials.slotCount(kind, data)) return new EmbedResult(false, "嵌入槽位已满");
        try {
            ItemStack copy = target.copy();
            EmbedMaterials.applyForRoles(copy, definition, data.anvilLevel(), level);
            copy.set(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get(), data.withEmbed(definition.id()));
            copyComponents(target, copy); material.shrink(1);
            publishDataChange(target, data, data.withEmbed(definition.id()));
            return new EmbedResult(true, "嵌入成功");
        } catch (RuntimeException failure) {
            dev.anvilcraft.thenextforge.TheNextForge.LOGGER.error("[TNF] API embedding rejected {}", definition.id(), failure);
            return new EmbedResult(false, "效果不可用，物品与材料均保留");
        }
    }
    /** 提交后的观察通知。观察者异常不能把已经完成的交易误报为失败。 */
    public static void publishDataChange(ItemStack target, ForgingData before, ForgingData after) {
        if (before.anvilLevel() == after.anvilLevel() && before.grindstoneLevel() == after.grindstoneLevel()
            && before.embeds().equals(after.embeds())) return;
        try { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new ForgeDataChangedEvent(target, before, after)); }
        catch (RuntimeException failure) {
            dev.anvilcraft.thenextforge.TheNextForge.LOGGER.error("[TNF] state observer failed after successful commit", failure);
        }
    }
    public static ItemStack removeLatestEmbedding(ItemStack target, net.minecraft.server.level.ServerLevel level) {
        return EmbedRemoval.removeLatest(target, level);
    }
    /** 提交副本的完整组件差异；单纯合并 patch 无法删除“不在物品原型中”的旧组件。 */
    public static void copyComponents(ItemStack target, ItemStack source) {
        if (target.getItem() != source.getItem()) throw new IllegalArgumentException("Component transaction cannot change item type");
        for (var type : new HashSet<>(target.getComponents().keySet())) if (!source.has(type)) target.remove(type);
        target.applyComponents(source.getComponents());
    }
    public static Holder<Attribute> requireAttribute(String id) {
        var attribute = EmbedData.attribute(id);
        if (attribute == null) throw new IllegalArgumentException("Unknown attribute: " + id);
        return attribute;
    }
    public static AttributeModifier.Operation operation(String id) {
        for (var value : AttributeModifier.Operation.values()) if (value.getSerializedName().equals(id)) return value;
        throw new IllegalArgumentException("Invalid attribute operation: " + id);
    }
    public record UpgradeContext(ItemStack target, TargetKind kind, ForgingLine line, int level,
                                 double amount, Identifier effectId) {
        public void addAttribute(String key, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite amount");
            String encoded = HexFormat.of().formatHex((effectId + "/" + kind + "/" + key).getBytes(StandardCharsets.UTF_8));
            ForgingRules.addApiModifier(target, attribute, amount, operation,
                "api_" + (line == ForgingLine.ANVIL ? "a_" : "g_") + encoded, level,
                kind == TargetKind.ARMOR ? EquipmentSlotGroup.ARMOR : kind == TargetKind.AMULET ? EquipmentSlotGroup.ANY : EquipmentSlotGroup.MAINHAND);
        }
    }
    public record EmbedContext(ItemStack target, String materialId, int token, Set<TargetKind> roles,
                               net.minecraft.server.level.ServerLevel level, String scope, JsonObject state) {
        public EmbedContext { roles = Collections.unmodifiableSet(roles.isEmpty() ? EnumSet.noneOf(TargetKind.class) : EnumSet.copyOf(roles)); }
        public void addAttribute(String key, Holder<Attribute> attribute, double amount,
                                 AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite amount");
            String encoded = HexFormat.of().formatHex((scope + "/" + key).getBytes(StandardCharsets.UTF_8));
            Identifier id = Identifier.fromNamespaceAndPath("thenextforge", "embed_api/" + token + "/" + encoded);
            var old = dev.anvilcraft.thenextforge.forging.ForgeAttributes.current(target);
            target.set(DataComponents.ATTRIBUTE_MODIFIERS, old.withModifierAdded(attribute, new AttributeModifier(id, amount, operation), slot));
        }
    }
    public static String applyUpgrade(UpgradeContext context, TnfDefinitions.Effect effect) {
        var handler = UPGRADE_EFFECTS.get(effect.type());
        if (handler == null) throw new IllegalArgumentException("Missing upgrade effect handler: " + effect.type());
        return handler.apply(context, effect.config());
    }
    public static EmbedEffect embedEffect(Identifier id) {
        var handler = EMBED_EFFECTS.get(id);
        if (handler == null) throw new IllegalArgumentException("Missing embed effect handler: " + id);
        return handler;
    }
    public static String applyUpgradeRule(ItemStack target, TargetKind kind, ForgingLine line, int level,
                                          RandomSource random, double multiplier, TnfDefinitions.UpgradeRule rule) {
        List<TnfDefinitions.Upgrade> choices = rule.outcomes().stream().filter(entry -> !entry.swordsOnly() || target.is(net.minecraft.tags.ItemTags.SWORDS)).toList();
        int total = choices.stream().mapToInt(TnfDefinitions.Upgrade::weight).sum();
        if (total == 0) return null;
        int roll = random.nextInt(total);
        for (var choice : choices) {
            roll -= choice.weight();
            if (roll >= 0) continue;
            double raw = choice.min() == choice.max() ? choice.min() : choice.min() + random.nextDouble() * (choice.max() - choice.min());
            // 先在副本上执行，未知效果或回调失败时不留下半条属性。
            ItemStack copy = target.copy();
            String description;
            try { description = applyUpgrade(new UpgradeContext(copy, kind, line, level, raw * multiplier, choice.effect().type()), choice.effect()); }
            catch (RuntimeException failure) {
                dev.anvilcraft.thenextforge.TheNextForge.LOGGER.error("[TNF] upgrade effect failed {}", choice.effect().type(), failure);
                return "附属加成失败（未写入）：" + choice.effect().type();
            }
            copyComponents(target, copy);
            return description;
        }
        return null;
    }
}
