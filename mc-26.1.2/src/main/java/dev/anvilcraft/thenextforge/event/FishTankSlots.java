package dev.anvilcraft.thenextforge.event;

import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 26.1.2 的鱼缸槽位适配层。
 *
 * <p>AnvilCraft 26 的 {@code getItemHandler()} 返回 NeoForge **Transfer API** 的
 * {@code ItemStacksResourceHandler}（没有 {@code getStackInSlot}）。本类把「按槽位读一个 ItemStack / 写一个 ItemStack」
 * 包装回来，让上层逻辑（右键拦截、自动化锤击）尽量保持原样。
 */
public final class FishTankSlots {
    private FishTankSlots() {
    }

    /** 读第 slot 个槽位；越界或空返回空栈。 */
    public static ItemStack get(FishTankBlockEntity tank, int slot) {
        var handler = tank.getItemHandler();
        if (handler == null || slot < 0 || slot >= handler.size()) return ItemStack.EMPTY;
        ItemResource resource = handler.getResource(slot);
        if (resource == null || resource.isEmpty()) return ItemStack.EMPTY;
        int amount = handler.getAmountAsInt(slot);
        return amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount);
    }

    /** 写第 slot 个槽位；空栈表示清空。 */
    public static void set(FishTankBlockEntity tank, int slot, ItemStack stack) {
        var handler = tank.getItemHandler();
        if (handler == null || slot < 0 || slot >= handler.size()) return;
        if (stack == null || stack.isEmpty()) {
            handler.set(slot, ItemResource.EMPTY, 0);
        } else {
            handler.set(slot, ItemResource.of(stack), stack.getCount());
        }
    }

    /** 槽位数量。 */
    public static int size(FishTankBlockEntity tank) {
        var handler = tank.getItemHandler();
        return handler == null ? 0 : handler.size();
    }

    /** 往输出槽插入；返回插入数量。 */
    /** 往输出槽插入；返回**剩余的**物品（旧语义的 leftover）。 */
    public static ItemStack insertOutput(FishTankBlockEntity tank, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        var output = tank.getOutput();
        if (output == null) return stack;
        int inserted;
        try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            inserted = output.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
        }
        if (inserted <= 0) return stack;
        ItemStack leftover = stack.copy();
        leftover.shrink(Math.min(inserted, leftover.getCount()));
        return leftover;
    }

    /** 对任意 Transfer 物品 handler 读一个槽位。 */
    public static ItemStack get(net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler handler, int slot) {
        if (handler == null || slot < 0 || slot >= handler.size()) return ItemStack.EMPTY;
        ItemResource resource = handler.getResource(slot);
        if (resource == null || resource.isEmpty()) return ItemStack.EMPTY;
        int amount = handler.getAmountAsInt(slot);
        return amount <= 0 ? ItemStack.EMPTY : resource.toStack(amount);
    }

    /** 对任意 Transfer 物品 handler 写一个槽位。 */
    public static void set(net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler handler, int slot, ItemStack stack) {
        if (handler == null || slot < 0 || slot >= handler.size()) return;
        if (stack == null || stack.isEmpty()) {
            handler.set(slot, ItemResource.EMPTY, 0);
        } else {
            handler.set(slot, ItemResource.of(stack), stack.getCount());
        }
    }

    /** 取出并清空一个槽位（对应旧的 extractItem）。 */
    public static ItemStack extract(net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler handler, int slot) {
        ItemStack stack = get(handler, slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        set(handler, slot, ItemStack.EMPTY);
        return stack;
    }
}
