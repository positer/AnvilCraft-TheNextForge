package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;

/**
 * 一次锤击的完整结算。
 *
 * <p>成功与失败都受展示框护符影响，护符效果见设计合同第 6 节。
 */
public final class ForgingService {
    public record Result(boolean success, boolean destroyed, String message) {
    }

    private ForgingService() {
    }

    public static Result hammer(
        Player player,
        ServerLevel level,
        BlockPos pos,
        ForgingLine line,
        ForgingMaterial baseMaterial,
        ForgingMaterial hammerMaterial
    ) {
        Display.ItemDisplay display = ForgeDisplay.find(level, pos);
        TheNextForge.LOGGER.info(
            "[TNF] hammer player={} pos={} line={} base={} hammer={} display={}",
            player.getName().getString(), pos, line, baseMaterial, hammerMaterial, display != null
        );
        if (display == null) return new Result(false, false, "这里没有可锻造的装备");
        ItemStack target = ForgeDisplay.stackOf(display).copy();
        TargetKind kind = TargetKind.of(target);
        if (kind == null) return new Result(false, false, "该物品不能锻造");
        ForgingData data = target.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);

        // 设计（新增）：砂轮上副手拿海绵芽锤击 → 不判定成功与否，消耗 1 个海绵芽，逆序取出最近嵌入的材料。
        // 取出与「能否砂轮升级」无关，所以放在砂轮限制之前。
        if (line == ForgingLine.GRINDSTONE && EmbedRemoval.isSpongeGemmule(player.getOffhandItem())) {
            return spongeRemove(player, level, pos, display, target);
        }

        // 设计 L23/L36：工具与护符不能用砂轮升级 —— 放置是允许的（便于海绵芽取出），锤击时才拒绝
        if (line == ForgingLine.GRINDSTONE && !kind.canUseGrindstone()) {
            String message = kind == TargetKind.TOOL ? "工具无法通过砂轮升级" : "护符无法通过砂轮升级";
            TheNextForge.LOGGER.info("[TNF] grindstone refused {}: {}", kind, message);
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }

        if (data.locked()) {
            return new Result(false, false, "该物品已被黄玉封印，无法继续锤击");
        }
        Set<String> amulets = Amulets.collect(level, pos);
        int currentLevel = data.level(line);
        int cost = ForgingRules.experienceCost(currentLevel);
        double frostFactor = 1.0;
        if (baseMaterial == ForgingMaterial.FROST) frostFactor *= 0.5;
        if (hammerMaterial == ForgingMaterial.FROST) frostFactor *= 0.5;
        cost = Math.max(1, (int) Math.round(cost * frostFactor));
        TheNextForge.LOGGER.info("[TNF] level={} cost={} totalXp={} amulets={}", currentLevel, cost, player.totalExperience, amulets);
        if (!player.isCreative() && player.totalExperience < cost) {
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
            String message = "经验不足，需要 " + cost + " 点经验";
            player.sendOverlayMessage(Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED));
            return new Result(false, false, message);
        }
        if (!player.isCreative()) player.giveExperiencePoints(-cost);
        data = data.withXpSpent(data.xpSpent() + cost);

        RandomSource random = player.getRandom();
        double rate = ForgingRules.successRate(currentLevel)
            + Amulets.totalLevel(level, pos) * ForgingRules.AMULET_LEVEL_BONUS;
        rate = Math.min(1.0, rate);
        int amuletLevels = Amulets.totalLevel(level, pos);
        if (amuletLevels > 0) {
            TheNextForge.LOGGER.info("[TNF] amulet levels contribute {} bonus, rate now {}", amuletLevels, rate);
        }
        if (amulets.contains("ruby") && player.isOnFire()) {
            rate = Math.min(1.0, rate + 0.10);
            player.hurt(level.damageSources().magic(), 1.0F);
        }
        String baseRoyalKey = "royal_base_" + line.name().toLowerCase();
        String hammerRoyalKey = "royal_hammer_" + line.name().toLowerCase();
        boolean baseRoyal = baseMaterial == ForgingMaterial.ROYAL && !data.protectionUsed(baseRoyalKey);
        boolean hammerRoyal = hammerMaterial == ForgingMaterial.ROYAL && !data.protectionUsed(hammerRoyalKey);
        boolean success;
        if (baseRoyal) {
            success = true;
            data = data.withProtectionUsed(baseRoyalKey);
        } else if (hammerRoyal) {
            success = true;
            data = data.withProtectionUsed(hammerRoyalKey);
        } else {
            success = random.nextDouble() < rate;
        }
        TheNextForge.LOGGER.info(
            "[TNF] roll success={} baseRoyal={} hammerRoyal={} rate={}", success, baseRoyal, hammerRoyal, rate
        );

        if (success) {
            int newLevel = currentLevel + 1;
            StringBuilder text = new StringBuilder("锻造成功 Lv.");
            int streak = data.catStreak();
            if (amulets.contains("cat")) {
                if (streak >= 2) {
                    newLevel++;
                    streak = 0;
                    text.append("（猫护符追加一级）");
                } else {
                    streak++;
                }
                data = data.withCatStreak(streak);
            }
            if (amulets.contains("sapphire") && consumeLava(level, pos)) {
                newLevel++;
                text.append("（蓝宝石追加一级）");
            }
            data = data.withLevel(line, newLevel);
            double multiplier = amulets.contains("anvil") ? 1.1 : 1.0;
            String upgrade = ForgingRules.applyUpgrade(target, kind, line, newLevel, random, multiplier);
            text.append(newLevel).append("（").append(quality(newLevel)).append("）");
            if (upgrade != null) text.append(" ").append(upgrade);

            int extra = 0;
            if (baseMaterial == ForgingMaterial.EMBER && random.nextDouble() < 0.5) extra++;
            if (hammerMaterial == ForgingMaterial.EMBER && random.nextDouble() < 0.5) extra++;
            for (int i = 0; i < extra; i++) {
                newLevel++;
                data = data.withLevel(line, newLevel);
                String extraUpgrade = ForgingRules.applyUpgrade(target, kind, line, newLevel, random, multiplier);
                text.append(" 余烬 +1");
                if (extraUpgrade != null) text.append("（").append(extraUpgrade).append("）");
            }

            if (line == ForgingLine.ANVIL) {
                ItemStack offhand = player.getOffhandItem();
                EmbedMaterials.Definition definition = EmbedMaterials.of(offhand);
                boolean uniqueBlocked = definition != null && definition.unique() && data.hasEmbed(definition.id());
                boolean allowed = definition != null && EmbedMaterials.allowsFor(definition, target);
                if (definition != null && !allowed) {
                    text.append(" 该材料不能嵌入此类对象");
                } else if (definition != null && uniqueBlocked) {
                    text.append(" 这枚材料已经有了");
                } else if (definition != null
                    && data.embeds().size() >= EmbedMaterials.slotCount(kind, data)) {
                    // 槽位已满是**静默拒绝**过：玩家会以为材料不能用，所以必须给一句提示
                    text.append(" 嵌入槽位已满（").append(data.embeds().size()).append(" / ")
                        .append(EmbedMaterials.slotCount(kind, data)).append("）");
                } else if (definition != null) {
                    offhand.shrink(1);
                    data = data.withEmbed(definition.id());
                    EmbedMaterials.applyForRoles(target, definition, newLevel, level);
                    text.append(" 嵌入 ").append(definition.label());
                }
            }

            applyRarity(target, newLevel);
            ForgingRules.write(target, data);
            ForgeDisplay.setStack(display, target);
            if (!amulets.contains("silence")) {
                int tierLevel = amulets.contains("abnormal") ? 4 + random.nextInt(7) : newLevel;
                playSuccessEffect(level, pos, tierLevel);
            }
            TheNextForge.LOGGER.info("[TNF] success: {}", text);
            player.sendOverlayMessage(Component.literal(text.toString()));
            return new Result(true, false, text.toString());
        }

        boolean baseSafe = baseMaterial.isSafe();
        boolean hammerSafe = hammerMaterial.isSafe();
        boolean overlimit = baseSafe || hammerSafe;
        if (data.nextHitProtected()) {
            data = data.withNextHitProtected(false);
            ForgingRules.write(target, data);
            ForgeDisplay.setStack(display, target);
            String message = "损毁被 犰狳护符 的后续保护 抵挡，等级不变";
            TheNextForge.LOGGER.info("[TNF] next-hit protection consumed");
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }
        double damageChance = amulets.contains("feather") ? 0.3 : 0.5;
        if (random.nextDouble() >= damageChance) {
            String message = "升级失败，等级不变";
            TheNextForge.LOGGER.info("[TNF] failure without damage, level stays {}", currentLevel);
            if (!amulets.contains("silence")) {
                level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }

        String protector = pickProtector(level, pos, amulets, data);
        if (protector != null) {
            data = data.withProtectionUsed(protector);
            if ("armadillo".equals(protector)) {
                data = data.withNextHitProtected(true);
            }
            if ("emerald".equals(protector)) {
                consumeVillager(level, pos, player);
            }
            if (overlimit) {
                String message = "损毁被 " + amuletName(protector) + " 抵挡，等级不变（超限保护降级）";
                ForgingRules.write(target, data);
                ForgeDisplay.setStack(display, target);
                TheNextForge.LOGGER.info("[TNF] damage blocked by {} (overlimit)", protector);
                player.sendOverlayMessage(Component.literal(message));
                return new Result(false, false, message);
            }
            String message = "损毁被 " + amuletName(protector) + " 抵挡";
            ForgingRules.write(target, data);
            ForgeDisplay.setStack(display, target);
            TheNextForge.LOGGER.info("[TNF] damage blocked by {}", protector);
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }


        ForgingRules.FailureOutcome outcome = ForgingRules.failureOutcome(
            baseSafe, hammerSafe, data.hasEmbed(ForgingRules.TRANSCENDIUM_EMBED));
        if (outcome == ForgingRules.FailureOutcome.RESET_TO_ZERO) {
            if (amulets.contains("dog")) {
                int refund = ForgingRules.refundForDegrade(currentLevel, 0);
                if (refund > 0) {
                    player.giveExperiencePoints(refund);
                    TheNextForge.LOGGER.info("[TNF] dog refunded {} xp on transcendium reset", refund);
                }
            }
            data = ForgingRules.resetToZero(data, line);
            ForgingRules.write(target, data);
            ForgeDisplay.setStack(display, target);
            String message = "损毁被 超限合金粒 抵消，等级归 0";
            TheNextForge.LOGGER.info("[TNF] transcendium embed prevented destruction, reset to 0");
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }

        if (outcome == ForgingRules.FailureOutcome.DESTROY && amulets.contains("topaz") && !data.protectionUsed("topaz")) {
            data = data.withProtectionUsed("topaz").withLocked();
            data = data.withLevel(line, currentLevel + 1);
            ForgingRules.write(target, data);
            ForgeDisplay.setStack(display, target);
            strikeLightning(level, pos);
            String message = "损毁被 黄玉 抵挡，等级 +1，物品被封印";
            TheNextForge.LOGGER.info("[TNF] topaz blocked damage, item locked");
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }


        if (outcome == ForgingRules.FailureOutcome.DESTROY) {
            display.discard();
            if (amulets.contains("dog") && data.xpSpent() > 0) {
                int refund = data.xpSpent() / 2;
                player.giveExperiencePoints(refund);
                TheNextForge.LOGGER.info("[TNF] dog amulet refunded {} xp", refund);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                24, 0.4, 0.3, 0.4, 0.02);
            String message = "锻造失败，装备已损毁";
            TheNextForge.LOGGER.info("[TNF] destroyed");
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, true, message);
        }

        int newLevel = ForgingRules.degradedLevel(currentLevel, baseSafe, hammerSafe);
        if (amulets.contains("dog")) {
            int refund = ForgingRules.refundForDegrade(currentLevel, newLevel);
            if (refund > 0) {
                player.giveExperiencePoints(refund);
                TheNextForge.LOGGER.info("[TNF] dog refunded {} xp on overlimit degrade", refund);
            }
        }
        data = ForgingRules.applyFailure(data, line, currentLevel, baseSafe, hammerSafe);
        if (newLevel == 0) {
            TheNextForge.LOGGER.info("[TNF] level reset to 0, royal token and protections unlocked");
        }
        ForgingRules.write(target, data);
        ForgeDisplay.setStack(display, target);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 0.6F);
        String message = "锻造失败，等级降至 Lv." + newLevel;
        TheNextForge.LOGGER.info("[TNF] degraded to {}", newLevel);
        player.sendOverlayMessage(Component.literal(message));
        return new Result(false, false, message);
    }

    /** 海绵芽取出：不判定成功与否、不降级，只把最近嵌入的材料还回去。 */
    private static Result spongeRemove(Player player, ServerLevel level, BlockPos pos,
                                       Display.ItemDisplay display, ItemStack target) {
        ItemStack sponge = player.getOffhandItem();
        ItemStack removed = EmbedRemoval.removeLatest(target, level);
        if (removed.isEmpty()) {
            String message = "没有可取的嵌入材料";
            TheNextForge.LOGGER.info("[TNF] sponge gemmule: nothing to remove");
            player.sendOverlayMessage(Component.literal(message));
            return new Result(false, false, message);
        }
        sponge.shrink(1);
        ForgeDisplay.setStack(display, target);
        player.getInventory().placeItemBackInInventory(removed);
        level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 0.8F, 1.0F);
        String message = "取出 " + removed.getHoverName().getString();
        TheNextForge.LOGGER.info("[TNF] sponge gemmule returned {}", removed.getItem());
        player.sendOverlayMessage(Component.literal(message));
        return new Result(true, false, message);
    }

    private static String pickProtector(ServerLevel level, BlockPos pos, Set<String> amulets, ForgingData data) {
        if (amulets.contains("armadillo") && !data.protectionUsed("armadillo")) return "armadillo";
        if (amulets.contains("emerald") && !data.protectionUsed("emerald") && hasVillager(level, pos)) return "emerald";
        return null;
    }

    private static String amuletName(String id) {
        return switch (id) {
            case "armadillo" -> "犰狳护符";
            case "emerald" -> "绿宝石护符";
            case "topaz" -> "黄玉护符";
            default -> id;
        };
    }

    private static boolean hasVillager(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5)).isEmpty();
    }

    private static void consumeVillager(ServerLevel level, BlockPos pos, Player player) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, new net.minecraft.world.phys.AABB(pos).inflate(1.5));
        if (villagers.isEmpty()) return;
        Villager villager = villagers.getFirst();
        // 26.1.2 的 VillagerData 是 record：升级用 withLevel(...)，读级用 level()
        var data = villager.getVillagerData();
        int nextLevel = Math.min(
            net.minecraft.world.entity.npc.villager.VillagerData.MAX_VILLAGER_LEVEL,
            data.level() + 1
        );
        villager.setVillagerData(data.withLevel(nextLevel));
        villager.setVillagerXp(
            net.minecraft.world.entity.npc.villager.VillagerData.getMinXpPerLevel(nextLevel)
        );
        TheNextForge.LOGGER.info("[TNF] emerald amulet raised villager to level {} then consumed it", nextLevel);
        villager.kill(level);
    }


    private static boolean consumeLava(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos check = pos.offset(dx, 0, dz);
                if (level.getBlockState(check).is(Blocks.LAVA)) {
                    level.setBlockAndUpdate(check, Blocks.OBSIDIAN.defaultBlockState());
                    return true;
                }
            }
        }
        return false;
    }

    private static void strikeLightning(ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if (bolt == null) return;
        bolt.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0F, 0.0F);
        level.addFreshEntity(bolt);
    }

    private static void applyRarity(ItemStack stack, int level) {
        net.minecraft.world.item.Rarity rarity = switch (quality(level)) {
            case "传说" -> net.minecraft.world.item.Rarity.EPIC;
            case "稀有" -> net.minecraft.world.item.Rarity.RARE;
            case "优秀" -> net.minecraft.world.item.Rarity.UNCOMMON;
            default -> net.minecraft.world.item.Rarity.COMMON;
        };
        stack.set(net.minecraft.core.component.DataComponents.RARITY, rarity);
        if (level >= 7) {
            stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }

    /**
     * 成功时按强化等级播放分级特效：等级越高，粒子越多、音效层次越丰富。
     */
    public static void playSuccessEffect(ServerLevel serverLevel, BlockPos pos, int level) {
        int tier = level >= 10 ? 3 : level >= 7 ? 2 : level >= 4 ? 1 : 0;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.15;
        double z = pos.getZ() + 0.5;
        switch (tier) {
            case 0 -> {
                serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.25, 0.2, 0.25, 0.04);
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.9F, 1.2F);
            }
            case 1 -> {
                serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 20, 0.35, 0.25, 0.35, 0.05);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 12, 0.45, 0.35, 0.45, 0.05);
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            case 2 -> {
                serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 32, 0.45, 0.3, 0.45, 0.06);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 24, 0.6, 0.45, 0.6, 0.06);
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 12, 0.35, 0.6, 0.35, 0.02);
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.6F);
                serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.9F, 1.2F);
                serverLevel.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.4F);
            }
            default -> {
                serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 50, 0.6, 0.4, 0.6, 0.08);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y, z, 36, 0.8, 0.6, 0.8, 0.08);
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 24, 0.5, 0.9, 0.5, 0.02);
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 16, 0.5, 0.5, 0.5, 0.15);
                serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.8F);
                serverLevel.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
                serverLevel.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
            }
        }
    }

    public static String quality(int level) {
        if (level >= 10) return "传说";
        if (level >= 7) return "稀有";
        if (level >= 4) return "优秀";
        return "普通";
    }
}
