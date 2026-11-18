package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * 海绵芽：按嵌入顺序**逆序取出**最近一次嵌入的材料（砂轮专用）。
 *
 * <p>取出是「撤销一次嵌入」：材料本身还给玩家，它写下的属性修饰符、耐久倍率、时运与不毁标志一并撤回，
 * 不判定成功与否、不消耗经验、不改变等级。
 */
public final class EmbedRemoval {
    /** 海绵芽的物品 id（AnvilCraft）。 */
    public static final String SPONGE_GEMMULE = "anvilcraft:sponge_gemmule";

    private EmbedRemoval() {
    }

    public static boolean isSpongeGemmule(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return SPONGE_GEMMULE.equals(id.toString());
    }

    /**
     * 取出最近嵌入的材料。
     *
     * @return 被取出的材料物品（没有嵌入时返回空栈，调用方据此决定是否消耗海绵芽）
     */
    public static ItemStack removeLatest(ItemStack stack, ServerLevel level) {
        ForgingData data = stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        if (data.embeds().isEmpty()) return ItemStack.EMPTY;

        List<String> remaining = new ArrayList<>(data.embeds());
        String removedId = remaining.remove(remaining.size() - 1);
        boolean managed = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.remove(stack, removedId, remaining.size(), level);
        stack.set(ModDataComponents.FORGING.get(), data.withEmbeds(remaining));
        if (managed) {
            ForgeDurability.rebaseDynamic(stack);
            dev.anvilcraft.thenextforge.api.TnfApi.publishDataChange(stack, data, data.withEmbeds(remaining));
            return materialStack(removedId);
        }

        stripModifiers(stack, removedId);
        revertDurability(stack, removedId);
        revertFortune(stack, removedId);
        revertUnbreakable(stack, removedId);
        if ("minecraft:diamond".equals(removedId) && !remaining.contains(removedId)) {
            var original = stack.remove(ModDataComponents.PRE_EMBED_TOOL.get());
            if (original != null) stack.set(DataComponents.TOOL, original);
        }

        ForgeDurability.rebaseDynamic(stack);
        ItemStack removed = materialStack(removedId);
        TheNextForge.LOGGER.info("[TNF] sponge gemmule removed embed {} ({} left)", removedId, remaining.size());
        dev.anvilcraft.thenextforge.api.TnfApi.publishDataChange(stack, data, data.withEmbeds(remaining));
        return removed;
    }

    /** 撤回该材料写下的属性修饰符（id 前缀 `embed/<材料>/...`）。 */
    private static void stripModifiers(ItemStack stack, String materialId) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        String prefix = "embed/" + materialId.substring(materialId.indexOf(':') + 1) + "/";
        List<ItemAttributeModifiers.Entry> kept = new ArrayList<>(modifiers.modifiers().size());
        int latest = modifiers.modifiers().stream()
            .filter(entry -> TheNextForge.MOD_ID.equals(entry.modifier().id().getNamespace())
                && entry.modifier().id().getPath().startsWith(prefix))
            .mapToInt(entry -> {
                String path = entry.modifier().id().getPath();
                try { return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1)); }
                catch (NumberFormatException ignored) { return -1; }
            }).max().orElse(-1);
        boolean changed = false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            Identifier id = entry.modifier().id();
            if (TheNextForge.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith(prefix)
                && (latest < 0 || id.getPath().endsWith("/" + latest))) {
                changed = true;
                continue;
            }
            kept.add(entry);
        }
        if (changed) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                new ItemAttributeModifiers(List.copyOf(kept)));
        }
    }

    /** 耐久倍率（委托 {@link EmbedMaterials} 的单一数据源）。 */
    public static double durabilityFactor(String materialId) {
        return EmbedMaterials.durabilityFactor(materialId, null);
    }

    /** 时运等级（委托单一数据源）。 */
    public static int fortuneLevel(String materialId) {
        return EmbedMaterials.fortuneLevel(materialId);
    }

    private static void revertDurability(ItemStack stack, String materialId) {
        // 只在该材料当初真的乘过倍率的对象类别下反算
        TargetKind kind = TargetKind.of(stack);
        if (!EmbedMaterials.durabilityApplies(materialId, kind)) return;
        double factor = EmbedMaterials.durabilityFactor(materialId, kind);
        if (factor != 1.0D && ForgeDurability.scaleBaseline(stack, 1.0D / factor)) return;
        Integer max = stack.get(DataComponents.MAX_DAMAGE);
        if (factor == 1.0D || max == null || max <= 1) return;
        int restored = Math.max(1, (int) Math.round(max / factor));
        stack.set(DataComponents.MAX_DAMAGE, restored);
        stack.set(DataComponents.DAMAGE, Math.min(stack.getOrDefault(DataComponents.DAMAGE, 0), restored - 1));
    }

    private static void revertFortune(ItemStack stack, String materialId) {
        int level = fortuneLevel(materialId);
        if (level <= 0) return;
        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var key = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.ENCHANTMENT,
            Identifier.withDefaultNamespace("fortune"));
        var holder = current.keySet().stream().filter(h -> h.unwrapKey().map(k -> k.equals(key)).orElse(false)).findFirst();
        if (holder.isEmpty()) return;
        int now = current.getLevel(holder.get());
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        int next = now - level;
        if (next <= 0) {
            mutable.removeIf(h -> h.unwrapKey().map(k -> k.equals(key)).orElse(false));
        } else {
            mutable.set(holder.get(), next);
        }
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    private static void revertUnbreakable(ItemStack stack, String materialId) {
        if (!EmbedMaterials.grantsUnbreakable(materialId)) return;
        ForgingData remaining = stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        if (remaining.embeds().stream().anyMatch(EmbedMaterials::grantsUnbreakable)) return;
        stack.remove(DataComponents.UNBREAKABLE);
    }

    /** 材料 id → 物品（原木标签取橡木原木）。 */
    public static ItemStack materialStack(String materialId) {
        if ("#minecraft:logs".equals(materialId)) return new ItemStack(Items.OAK_LOG);
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(materialId))
            .map(ItemStack::new)
            .orElse(ItemStack.EMPTY);
    }
}
