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
        // 设计：余烬与负物质粒的「被攻击」效果是「装甲/护符」，蜘蛛眼与末影珍珠只认装甲
        boolean ember = (armour != null && armour.hasActiveEmbed("anvilcraft:ember_metal_ingot"))
            || dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "anvilcraft:ember_metal_ingot");
        boolean negative = (armour != null && armour.hasActiveEmbed("anvilcraft:negative_matter_nugget"))
            || dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "anvilcraft:negative_matter_nugget");
        if (armour == null && !ember && !negative) return;
        if (ember) {
            living.setRemainingFireTicks(200);
        }
        if (armour != null && armour.hasActiveEmbed("minecraft:fermented_spider_eye")) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, true));
        }
        if (negative) {
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, true));
            // 设计「漂浮且受到伤害提高 30% 持续 5s」：攻击者也要挂易伤
            living.setData(dev.anvilcraft.thenextforge.init.ModAttachments.VULNERABLE_UNTIL.get(),
                level.getGameTime() + 100L);
        }
        if (armour != null && armour.hasActiveEmbed("minecraft:ender_pearl")) {
            double dx = (living.getRandom().nextDouble() - 0.5) * 26;
            double dz = (living.getRandom().nextDouble() - 0.5) * 26;
            living.teleportTo(living.getX() + dx, living.getY(), living.getZ() + dz);
        }
    }

    private static ForgingData embeddedArmour(Player player) {
        for (ItemStack stack : player.getInventory().armor) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && !data.activeEmbeds().isEmpty()) return data;
        }
        return null;
    }
}
