package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 任何走原版结果槽的升级（锻造台模板升级、其它模组的同类配方）完成后，
 * 把输入物品上的锻造等级与嵌入材料搬到结果物品上。
 *
 * <p>用 {@link PlayerEvent.ItemCraftedEvent} 而不是 mixin：原版 {@code ResultSlot#onTake}
 * 统一触发该事件，因此对所有实现模板升级的模组天然兼容，也不会碰到原版逻辑。
 */
public final class ForgeCraftEvents {
    private ForgeCraftEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (result.isEmpty()) return;
        Container matrix = event.getInventory();
        // 只在锻造台升级时保留；工作台等其它结果槽不应误保留
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)
            || !(sp.containerMenu instanceof net.minecraft.world.inventory.SmithingMenu)) {
            return;
        }
        if (matrix == null) return;

        ForgingData carried = null;
        for (int i = 0; i < matrix.getContainerSize(); i++) {
            ItemStack in = matrix.getItem(i);
            if (in.isEmpty()) continue;
            ForgingData data = in.get(ModDataComponents.FORGING.get());
            if (data != null && (!data.embeds().isEmpty() || data.level(
                dev.anvilcraft.thenextforge.forging.ForgingLine.ANVIL) > 0
                || data.level(dev.anvilcraft.thenextforge.forging.ForgingLine.GRINDSTONE) > 0)) {
                carried = data;
                break;
            }
        }
        if (carried == null) return;

        result.set(ModDataComponents.FORGING.get(), carried);
        TheNextForge.LOGGER.info("[TNF] carried forging data onto {} (anvil={} grindstone={} embeds={})",
            result.getItem(), carried.anvilLevel(), carried.grindstoneLevel(), carried.embeds().size());
    }
}
