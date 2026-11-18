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

    /** 粘液球减速的修饰符 id。 */
    public static final net.minecraft.resources.ResourceLocation SLIME_SLOW_ID =
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("thenextforge", "embed/slime_ball/slow");

    private static final java.util.Map<LivingEntity, Long> SLIME_SLOW_UNTIL = new java.util.WeakHashMap<>();

    /** 粘液球：移速 ×0.5，持续 ticks（缓慢效果凑不出 50%）。 */
    public static void applySlimeSlow(LivingEntity target, int ticks) {
        if (target == null || target.level().isClientSide()) return;
        var speed = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (speed.getModifier(SLIME_SLOW_ID) == null) {
            speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                SLIME_SLOW_ID, -0.5D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        SLIME_SLOW_UNTIL.put(target, target.level().getGameTime() + ticks);
    }

    /** 每 tick 由事件钩子调用：到期摘除。 */
    public static void expireSlimeSlow(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof LivingEntity target)) return;
        Long until = SLIME_SLOW_UNTIL.get(target);
        if (until == null) return;
        if (target.level().getGameTime() < until) return;
        SLIME_SLOW_UNTIL.remove(target);
        var speed = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SLIME_SLOW_ID);
    }

    /** 一件物品上某材料的嵌入数量。 */
    public static int countEmbeds(ForgingData data, String materialId) {
        if (data == null) return 0;
        return (int) data.activeEmbeds().stream().filter(materialId::equals).count();
    }

    /** 玩家随身护符（*_amulet）上某材料的总嵌入数。 */
    public static int carriedAmuletEmbeds(Player player, String materialId) {
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!path.endsWith("_amulet")) continue;
            count += countEmbeds(stack.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get()), materialId);
        }
        return count;
    }

    /** 海洋之心：水中造成伤害 +75%。 */
    public static float heartOfTheSeaDamage(float damage, boolean inWater) {
        return inWater ? damage * 1.75F : damage;
    }

    public static void applyOnHit(ServerLevel level, Player attacker, LivingEntity target, ItemStack weapon, float damage) {
        ForgingData data = weapon.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
        if (data == null) data = ForgingData.EMPTY;
        // 武器上没嵌入也要继续：设计里「中毒」这类效果护符单独也能给
        if (data.activeEmbeds().isEmpty() && carriedAmuletEmbeds(attacker, "minecraft:fermented_spider_eye") == 0) return;

        if (data.hasActiveEmbed("minecraft:echo_shard")) {
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
        // 设计：中毒「武器/护符」都算 —— 武器嵌入 + 随身护符嵌入一起数，每个 +1 级
        int spiderEyes = countEmbeds(data, "minecraft:fermented_spider_eye")
            + carriedAmuletEmbeds(attacker, "minecraft:fermented_spider_eye");
        if (spiderEyes > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, spiderEyes - 1, false, true));
        }
        if (data.hasActiveEmbed("minecraft:slime_ball")) {
            // 设计：命中降低 50% 移速（缓慢 II 只有 -30%），用瞬态修饰符精确 -50%
            applySlimeSlow(target, 100);
        }
        if (data.hasActiveEmbed("anvilcraft:negative_matter_nugget")) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 0, false, true));
            var vulnerable = dev.anvilcraft.thenextforge.init.ModAttachments.VULNERABLE_UNTIL.get();
            target.setData(vulnerable, level.getGameTime() + 100L);
        }
        if (data.hasActiveEmbed("minecraft:ender_pearl")) {
            double dx = (target.getRandom().nextDouble() - 0.5) * 26;
            double dz = (target.getRandom().nextDouble() - 0.5) * 26;
            target.teleportTo(target.getX() + dx, target.getY(), target.getZ() + dz);
            level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(), 24, 0.4, 0.4, 0.4, 0.3);
        }
        if (data.hasActiveEmbed("anvilcraft:ember_metal_ingot")) {
            target.setRemainingFireTicks(200);
        }
        // 设计：凋零玫瑰 —— 命中目标获得 5s 凋零（工具/武器）
        if (data.hasActiveEmbed("minecraft:wither_rose")) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, true));
        }
        if (data.hasActiveEmbed("minecraft:glow_ink_sac")) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
        }
        if (data.hasActiveEmbed("minecraft:heart_of_the_sea") && attacker.isInWater()) {
            target.hurt(level.damageSources().magic(), damage * 0.75F);
        }
        // 纯函数入口见 heartOfTheSeaDamage，供测试断言
        
    }
}
