package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.forging.ModelScaling;
import net.minecraft.world.item.ItemStack;

/** 仅抵消当前展示物自身已有的 TNF 变换，不影响嵌套渲染的其他物品。 */
public final class DisplayScaleContext {
    public record State(ItemStack item, float correction) { }
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();
    private DisplayScaleContext() { }
    public static State enter(ItemStack item, float correction) {
        State previous = CURRENT.get();
        CURRENT.set(new State(item, correction));
        return previous;
    }
    public static void restore(State previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }
    public static float scaleFor(ItemStack item) {
        State state = CURRENT.get();
        return ModelScaling.scaleOf(item) * (state != null && state.item() == item ? state.correction() : 1.0F);
    }
}
