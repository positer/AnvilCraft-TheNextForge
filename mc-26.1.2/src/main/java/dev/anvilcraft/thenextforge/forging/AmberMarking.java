package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.init.ModAttachments;
import dev.dubhe.anvilcraft.util.ResentmentUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * 琥珀嵌入的怨恨加算。
 *
 * <p>命中一次，把「原版怨恨值 + 20×琥珀数（上限 100）」写回本体数据并打标记；
 * 已有标记则不再叠加，也不参与任何伤害计算。
 */
public final class AmberMarking {
    private AmberMarking() {
    }

    /**
     * 对目标施加一次琥珀加算。
     *
     * @return 施加后的原版怨恨值；重复命中返回当前值（不叠加）
     */
    public static int apply(LivingEntity target, int amberCount) {
        if (amberCount <= 0 || !(target instanceof Mob mob)) return ResentmentUtil.getResentment(target);
        var mark = ModAttachments.AMBER_MARK.get();
        if (target.getData(mark) != 0) return ResentmentUtil.getResentment(target);
        int base = ResentmentUtil.getResentment(target);
        ResentmentUtil.setForcedResentment(mob, Math.min(100, base + 20 * amberCount));
        target.setData(mark, amberCount);
        int after = ResentmentUtil.getResentment(target);
        TheNextForge.LOGGER.info(
            "[TNF] amber embed added {} resentment to {} ({} -> {})",
            20 * amberCount, target.getName().getString(), base, after
        );
        return after;
    }
}
