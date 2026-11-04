package dev.anvilcraft.thenextforge.event;

import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * 26.1.2 的鱼缸流体适配层。
 *
 * <p>{@code getFluidHandler()} 返回 AnvilCraft 的 {@code FluidStackResourceHandler}（Transfer API），
 * 没有 {@code getFluidInTank/fill/drain}。本类把它包回「读一个 FluidStack / 灌入 / 抽出」。
 */
public final class FishTankFluids {
    private FishTankFluids() {
    }

    /** 当前内容。 */
    public static FluidStack get(FishTankBlockEntity tank) {
        var handler = tank.getFluidHandler();
        if (handler == null) return FluidStack.EMPTY;
        FluidStack stack = handler.getStack();
        return stack == null ? FluidStack.EMPTY : stack;
    }

    /** 当前数量（mB）。 */
    public static int amount(FishTankBlockEntity tank) {
        return get(tank).getAmount();
    }

    /** 是否装的是经验流体。 */
    public static boolean isExperience(FishTankBlockEntity tank) {
        FluidStack stack = get(tank);
        return !stack.isEmpty() && stack.getFluidType().equals(ModFluids.EXP_FLUID_TYPE.get());
    }

    /** 灌入流体，返回实际灌入量。 */
    public static int insert(FishTankBlockEntity tank, FluidStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        var handler = tank.getFluidHandler();
        if (handler == null) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(0, FluidResource.of(stack), stack.getAmount(), tx);
            tx.commit();
            return inserted;
        }
    }

    /** 抽出经验流体，返回实际抽出量。 */
    public static int extract(FishTankBlockEntity tank, int amount) {
        if (amount <= 0) return 0;
        var handler = tank.getFluidHandler();
        if (handler == null) return 0;
        FluidStack current = get(tank);
        if (current.isEmpty()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int drained = handler.extract(0, FluidResource.of(current), amount, tx);
            tx.commit();
            return drained;
        }
    }

    /** 抽出全部经验流体。 */
    public static int drainAll(FishTankBlockEntity tank) {
        return extract(tank, amount(tank));
    }

    /** 直接设定内容（绕过插入上限，测试与创造模式用途）。 */
    public static void set(FishTankBlockEntity tank, FluidStack stack) {
        var handler = tank.getFluidHandler();
        if (handler == null || stack == null || stack.isEmpty()) return;
        handler.set(FluidResource.of(stack), stack.getAmount());
    }
}
