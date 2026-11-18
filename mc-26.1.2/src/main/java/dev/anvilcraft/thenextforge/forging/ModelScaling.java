package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.world.item.ItemStack;

/**
 * 被锻造物品的**真实渲染**放大倍率。
 *
 * <p>统一计算 GUI、手持、第三人称与掉落物倍率；两代在各自完整渲染入口使用，
 * 展示物需抵消已经保存到实体变换中的 TNF 倍率。
 *
 * <p>受两层开关控制：
 * <ol>
 *   <li>配置项 {@code experimentalModelScaling}（模组配置页，默认关闭）——关闭时一律返回 1.0；</li>
 *   <li>数据驱动的 {@code model_scale}（{@link ForgeRulesData}）——可按材料/物品关闭或改每级倍率。</li>
 * </ol>
 */
public final class ModelScaling {
    /** 旧锻造台展示变换已存一次倍率；绘制时抵消，避免平方放大。 */
    public static float displayCorrection(com.mojang.math.Transformation transform) {
        var translation = transform.translation();
        var left = transform.leftRotation();
        var right = transform.rightRotation();
        var scale = transform.scale();
        float epsilon = 0.0001F;
        float half = (float) Math.sqrt(0.5);
        boolean forgePose = Math.abs(translation.x()) < epsilon && Math.abs(translation.y() - 0.12F) < epsilon
            && Math.abs(translation.z()) < epsilon && Math.abs(left.x()) < epsilon
            && Math.abs(left.y()) < epsilon && Math.abs(left.z()) < epsilon && Math.abs(Math.abs(left.w()) - 1) < epsilon
            && Math.abs(Math.abs(right.x() * half + right.w() * half) - 1) < epsilon;
        if (!forgePose || !Float.isFinite(scale.x()) || scale.x() <= 0
            || Math.abs(scale.x() - scale.y()) > epsilon || Math.abs(scale.x() - scale.z()) > epsilon) return 1.0F;
        return 1.0F / scale.x();
    }

    private ModelScaling() {
    }

    /** 实验性开关是否开启。 */
    public static boolean enabled() {
        return TheNextForge.CONFIG != null && TheNextForge.CONFIG.experimentalModelScaling;
    }

    /** 该物品的真实渲染倍率；不放大时返回 1.0。 */
    public static float scaleOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !enabled()) return 1.0F;
        return ForgeDisplay.rawScaleOf(stack);
    }
}
