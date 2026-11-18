package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.forging.ForgeQuality;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** 只保存品质整数，不持有物品、实体或世界；嵌套渲染必须恢复上下文。 */
public final class QualityGlint {
    private static final ThreadLocal<Integer> CURRENT = ThreadLocal.withInitial(() -> 0);
    private QualityGlint() { }
    public static int tier(ItemStack stack) {
        var data = stack.get(ModDataComponents.FORGING.get());
        return data == null ? 0 : ForgeQuality.tier(ForgeQuality.level(data));
    }
    public static int current() { return CURRENT.get(); }
    public static int enter(int tier) { int before = current(); CURRENT.set(tier); return before; }
    public static void restore(int tier) { if (tier == 0) CURRENT.remove(); else CURRENT.set(tier); }
    public static String texture(int tier) {
        return "textures/misc/quality_" + switch (tier) {
            case 1 -> "blue";
            case 2 -> "purple";
            case 3 -> "gold";
            default -> throw new IllegalArgumentException("Not a quality glint: " + tier);
        } + ".png";
    }
}
