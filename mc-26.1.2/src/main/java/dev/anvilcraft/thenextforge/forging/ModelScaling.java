package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.world.item.ItemStack;

/**
 * 被锻造物品的**真实渲染**放大倍率。
 *
 * <p>同一件物品在手持、物品栏、第三人称（实体手持）与掉落物形态下都走原版的
 * {@code ItemRenderer#render}，所以只要在那里按本类给出的倍率缩放，四种形态就是一致的真实放大。
 *
 * <p>受两层开关控制：
 * <ol>
 *   <li>配置项 {@code experimentalModelScaling}（模组配置页，默认关闭）——关闭时一律返回 1.0；</li>
 *   <li>数据驱动的 {@code model_scale}（{@link ForgeRulesData}）——可按材料/物品关闭或改每级倍率。</li>
 * </ol>
 */
public final class ModelScaling {
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
