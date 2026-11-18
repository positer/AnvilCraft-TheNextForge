package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 挖掘速度相关的嵌入效果：海洋之心抵消水下惩罚，超限合金粒抵消飞行惩罚。
 */
@EventBusSubscriber(modid = TheNextForge.MOD_ID)
public final class ForgeMiningEvents {
    private ForgeMiningEvents() {
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack tool = player.getMainHandItem();
        ForgingData toolData = tool.get(ModDataComponents.FORGING.get());

        if (player.isUnderWater() && toolData != null && toolData.hasActiveEmbed("minecraft:heart_of_the_sea")) {
            event.setNewSpeed(event.getNewSpeed() * 5.0F);
        }
        if (player.getAbilities().flying && !player.onGround()
            && dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "anvilcraft:transcendium_nugget")) {
            event.setNewSpeed(event.getNewSpeed() * 5.0F);
        }
    }

    private static boolean carriesAmuletEmbed(ServerPlayer player, String material) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && data.hasActiveEmbed(material)) return true;
        }
        return false;
    }
}
