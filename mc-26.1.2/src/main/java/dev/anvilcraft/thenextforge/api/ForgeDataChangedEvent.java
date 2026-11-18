package dev.anvilcraft.thenextforge.api;

import dev.anvilcraft.thenextforge.forging.ForgingData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/** 等级/嵌入状态写回并完成 TNF 属性回退后的通知，不可取消；附属可据此协调自己的额外状态。 */
public final class ForgeDataChangedEvent extends Event {
    private final ItemStack target;
    private final ForgingData before;
    private final ForgingData after;
    public ForgeDataChangedEvent(ItemStack target, ForgingData before, ForgingData after) {
        this.target = target; this.before = before; this.after = after;
    }
    public ItemStack target() { return target; }
    public ForgingData before() { return before; }
    public ForgingData after() { return after; }
}
