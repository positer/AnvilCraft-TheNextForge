package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * 武器命中时的嵌入材料效果。
 *
 * <p>从事件里抽出来，既给事件调用，也让 GameTest 可以直接断言。
 */
public final class CombatEffects {
    private CombatEffects() {
    }

    /** 虚空物质的替换比例：武器 30%，护符 10%。 */
    public static float voidRatio(boolean fromWeapon) {
        return fromWeapon ? 0.3F : 0.1F;
    }

    /** 浮霜：武器按附魔数 +5%/个，护符按玩家等级 +1%/级。 */
    public static float frostMultiplier(int enchantCount, int playerLevel) {
        return 1.0F + 0.05F * Math.max(0, enchantCount) + 0.01F * Math.max(0, playerLevel);
    }

    /** 海洋之心：水中造成伤害 +75%。 */
    public static float heartOfTheSeaDamage(float damage, boolean inWater) {
        return inWater ? damage * 1.75F : damage;
    }

    public static void applyOnHit(ServerLevel level, Player attacker, LivingEntity target, ItemStack weapon, float damage) {
        ForgingData data = weapon.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
        if (data == null || data.embeds().isEmpty()) return;

        if (data.hasEmbed("minecraft:echo_shard")) {
            float echoDamage = damage * 0.5F;
            target.hurt(level.damageSources().magic(), echoDamage);
            // 范围伤害：目标周围 2 格内的其他生物同样承受 50%
            for (LivingEntity other : level.getEntitiesOfClass(
                LivingEntity.class, target.getBoundingBox().inflate(2.0),
                entity -> entity != target && entity != attacker
            )) {
                other.hurt(level.damageSources().magic(), echoDamage);
            }
            // 回响粒子：中心音爆 + 环形扩散的幽匿灵魂
            level.sendParticles(ParticleTypes.SONIC_BOOM,
                target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                    target.getX() + Math.cos(angle) * 1.5,
                    target.getY() + 0.8,
                    target.getZ() + Math.sin(angle) * 1.5,
                    2, 0.1, 0.1, 0.1, 0.01);
            }
        }
        if (data.hasEmbed("minecraft:fermented_spider_eye")) {
            int count = (int) data.embeds().stream().filter("minecraft:fermented_spider_eye"::equals).count();
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, Math.max(0, count - 1), false, true));
        }
        if (data.hasEmbed("minecraft:slime_ball")) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
        }
        if (data.hasEmbed("anvilcraft:negative_matter_nugget")) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, true));
            var vulnerable = dev.anvilcraft.thenextforge.init.ModAttachments.VULNERABLE_UNTIL.get();
            target.setData(vulnerable, level.getGameTime() + 100L);
        }
        if (data.hasEmbed("minecraft:ender_eye")) {
            double dx = (target.getRandom().nextDouble() - 0.5) * 26;
            double dz = (target.getRandom().nextDouble() - 0.5) * 26;
            target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
            level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(), 24, 0.4, 0.4, 0.4, 0.3);
        }
        if (data.hasEmbed("anvilcraft:ember_metal_ingot")) {
            target.setRemainingFireTicks(200);
        }
        // 设计：凋零玫瑰 —— 命中目标获得 5s 凋零（工具/武器）
        if (data.hasEmbed("minecraft:wither_rose")) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, true));
        }
        if (data.hasEmbed("minecraft:glow_ink_sac")) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
        }
        if (data.hasEmbed("minecraft:heart_of_the_sea") && attacker.isInWater()) {
            target.hurt(level.damageSources().magic(), damage * 0.75F);
        }
        // 纯函数入口见 heartOfTheSeaDamage，供测试断言
        
    }
}
