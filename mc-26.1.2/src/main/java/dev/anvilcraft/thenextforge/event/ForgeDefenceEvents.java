package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 护甲/护符嵌入材料在玩家被攻击时触发的反制效果。
 */
public final class ForgeDefenceEvents {
    private ForgeDefenceEvents() {
    }

    /** 设计：不死图腾(护符) 受到致命伤害时触发一次，冷却 240s。 */
    @SubscribeEvent
    public static void onLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (event.getNewDamage() < player.getHealth()) return;
        long now = level.getGameTime();
        if (dev.anvilcraft.thenextforge.event.EmbedCombat.totemSaves(level, player, now)) {
            event.setNewDamage(0.0F);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity living)) return;

        ForgingData armour = embeddedArmour(player);
        if (armour == null) return;
        if (armour.hasEmbed("anvilcraft:ember_metal_ingot")) {
            living.setRemainingFireTicks(200);
        }
        if (armour.hasEmbed("minecraft:fermented_spider_eye")) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
        }
        if (armour.hasEmbed("anvilcraft:negative_matter_nugget")) {
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, true));
        }
        if (armour.hasEmbed("minecraft:ender_pearl")) {
            double dx = (living.getRandom().nextDouble() - 0.5) * 26;
            double dz = (living.getRandom().nextDouble() - 0.5) * 26;
            living.teleportTo(living.getX() + dx, living.getY(), living.getZ() + dz);
        }
    }

    private static ForgingData embeddedArmour(Player player) {
        for (ItemStack stack : armourSlots(player)) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && !data.embeds().isEmpty()) return data;
        }
        return null;
    }

    /** 26.1.2：Inventory.armor 已移除。 */
    private static java.util.List<ItemStack> armourSlots(net.minecraft.world.entity.player.Player player) {
        return java.util.List.of(
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)
        );
    }
}
