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
import net.minecraft.world.entity.npc.Villager;
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

    /** 猫护符的一次结算结果：额外等级与新的连续成功计数。 */
    public record CatOutcome(int levelBonus, int streak) {
    }

    /** 猫护符：连续成功两次后，下一次成功额外 +1 级并重置；失败必须断开连续。 */
    public static CatOutcome catOutcome(boolean success, int streak) {
        if (!success) return new CatOutcome(0, 0);
        if (streak >= 2) return new CatOutcome(1, 0);
        return new CatOutcome(0, streak + 1);
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
        // 设计（新增）：砂轮上副手拿海绵芽锤击 → 不判定成功与否，消耗 1 个海绵芽，逆序取出最近嵌入的材料。
        // 取出与「能否砂轮升级」无关，所以放在砂轮限制之前。
        if (line == ForgingLine.GRINDSTONE && EmbedRemoval.isSpongeGemmule(player.getOffhandItem())) {
            return spongeRemove(player, level, pos, display, target);
        }

        return settle(new StrikeAccess() {
            public ItemStack material() { return player.getOffhandItem(); }
            public RandomSource random() { return player.getRandom(); }
            public boolean pay(int cost) {
                if (player.isCreative()) return true;
                if (player.totalExperience < cost) return false;
                player.giveExperiencePoints(-cost);
                return true;
            }
            public void refund(int xp) { player.giveExperiencePoints(xp); }
            public boolean onFire() { return player.isOnFire(); }
            public void rubyCost() { player.hurt(level.damageSources().magic(), 1.0F); }
            public void save(ItemStack stack) { ForgeDisplay.setStack(display, stack); }
            public void destroy() { display.discard(); }
            public void message(Component message) {
                ForgeFeedback.deliver(target.getHoverName(), message, player.isCreative(),
                    value -> player.displayClientMessage(value, true), player::sendSystemMessage);
            }
        }, target, level, pos, line, baseMaterial, hammerMaterial);
    }

    /** 手动与自动仅提供输入/支付/输出；皇家、材质和护符规则只有一个结算入口。 */
    public interface StrikeAccess {
        ItemStack material();
        RandomSource random();
        boolean pay(int cost);
        void refund(int xp);
        boolean onFire();
        void rubyCost();
        void save(ItemStack stack);
        void destroy();
        void message(Component message);
    }

    public static Result settle(StrikeAccess access, ItemStack target, ServerLevel level, BlockPos pos,
                                ForgingLine line, ForgingMaterial baseMaterial, ForgingMaterial hammerMaterial) {
        TargetKind kind = TargetKind.of(target);
        if (kind == null) {
            String message = "无法锻造｜此物品不支持强化";
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }
        ForgingData data = target.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);

        // 设计 L23/L36：工具与护符不能用砂轮升级 —— 放置是允许的（便于海绵芽取出），锤击时才拒绝
        if (!dev.anvilcraft.thenextforge.api.TnfApi.canUseLine(target, line)) {
            String message = line == ForgingLine.ANVIL ? "该物品未开放铁砧升级" : kind == TargetKind.TOOL ? "工具无法通过砂轮升级" : "护符无法通过砂轮升级";
            TheNextForge.LOGGER.info("[TNF] grindstone refused {}: {}", kind, message);
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }

        if (data.locked()) {
            String message = "无法锻造｜物品已被黄玉封印";
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }
        try { dev.anvilcraft.thenextforge.api.TnfApi.validateUpgrades(target, line); }
        catch (RuntimeException invalidRules) {
            String message = "无法锻造｜附属升级配置不可用（未消耗经验）";
            TheNextForge.LOGGER.error("[TNF] invalid upgrade configuration", invalidRules);
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }
        var mappedBase = dev.anvilcraft.thenextforge.api.TnfApi.base(level.getBlockState(pos));
        if (mappedBase != null && (!mappedBase.enabled() || mappedBase.line() != line)) mappedBase = null;
        Amulets.Snapshot amuletSnapshot = Amulets.scan(level, pos);
        Set<String> amulets = amuletSnapshot.effects();
        int currentLevel = data.level(line);
        int cost = ForgingRules.experienceCost(currentLevel);
        double frostFactor = 1.0;
        if (baseMaterial == ForgingMaterial.FROST) frostFactor *= 0.5;
        if (hammerMaterial == ForgingMaterial.FROST) frostFactor *= 0.5;
        cost = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(cost * frostFactor
            * (mappedBase == null ? 1.0 : mappedBase.experienceMultiplier()))));
        if (!access.pay(cost)) {
            String message = "经验不足｜本次需要 " + cost + " 点经验（非经验等级）";
            access.message(Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED));
            return new Result(false, false, message);
        }
        data = data.withXpSpent((int) Math.min(Integer.MAX_VALUE, (long) data.xpSpent() + cost));

        ForgingRules.write(target, data);
        access.save(target);

        // 设计（用户裁定）：犰狳的「保护下一次锤击」在这一锤开始时就被消耗，不论成败
        boolean protectedStrike = data.nextHitProtected();
        if (protectedStrike) {
            data = data.withNextHitProtected(false);
            ForgingRules.write(target, data);
            access.save(target);
            TheNextForge.LOGGER.info("[TNF] armadillo next-strike protection consumed at strike start");
        }
        RandomSource random = access.random();
        int amuletLevels = amuletSnapshot.totalLevel();
        double rate = Math.min(1.0, ForgingRules.successRate(currentLevel)
            + amuletLevels * ForgingRules.AMULET_LEVEL_BONUS
            + (mappedBase == null ? 0 : mappedBase.successBonus()));
        rate = Math.max(0, rate);
        if (amuletLevels > 0) {
            TheNextForge.LOGGER.info("[TNF] amulet levels contribute {} bonus, rate now {}", amuletLevels, rate);
        }
        if (amulets.contains("ruby") && access.onFire()) {
            rate = Math.min(1.0, rate + 0.10);
            access.rubyCost();
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
            java.util.List<String> upgrades = new java.util.ArrayList<>();
            java.util.List<String> sources = new java.util.ArrayList<>();
            String embedMessage = "";
            if (mappedBase != null && mappedBase.extraLevels() > 0) {
                newLevel += mappedBase.extraLevels();
                sources.add("基底追加 +" + mappedBase.extraLevels() + " 级");
            }
            if (baseRoyal) sources.add("皇家基底必成");
            else if (hammerRoyal) sources.add("皇家锤必成");
            if (amulets.contains("cat")) {
                CatOutcome cat = catOutcome(true, data.catStreak());
                if (cat.levelBonus() > 0) {
                    newLevel += cat.levelBonus();
                    sources.add("猫护符 +1 级");
                }
                data = data.withCatStreak(cat.streak());
            }
            if (amulets.contains("sapphire") && consumeLava(level, pos)) {
                newLevel++;
                sources.add("蓝宝石护符 +1 级");
            }
            data = data.withLevel(line, newLevel);
            double multiplier = amulets.contains("anvil") ? 1.1 : 1.0;
            for (int gained = currentLevel + 1; gained <= newLevel; gained++) {
                String upgrade = applyRoleUpgrades(target, line, gained, random, multiplier);
                if (upgrade != null && !upgrade.isBlank()) upgrades.add(upgrade);
            }

            int extra = 0;
            if (baseMaterial == ForgingMaterial.EMBER && random.nextDouble() < 0.5) extra++;
            if (hammerMaterial == ForgingMaterial.EMBER && random.nextDouble() < 0.5) extra++;
            for (int i = 0; i < extra; i++) {
                newLevel++;
                data = data.withLevel(line, newLevel);
                String extraUpgrade = applyRoleUpgrades(target, line, newLevel, random, multiplier);
                if (extraUpgrade != null && !extraUpgrade.isBlank()) upgrades.add(extraUpgrade);
            }

            if (extra > 0) sources.add("余烬 +" + extra + " 级");

            if (line == ForgingLine.ANVIL) {
                ItemStack offhand = access.material();
                EmbedMaterials.Definition definition = EmbedMaterials.of(offhand);
                boolean uniqueBlocked = definition != null && definition.unique() && data.hasEmbed(definition.id());
                boolean allowed = definition != null && EmbedMaterials.allowsFor(definition, target);
                if (definition != null && !allowed) {
                    embedMessage = "未嵌入：材料不适用于此物品（未消耗）";
                } else if (definition != null && uniqueBlocked) {
                    embedMessage = "未嵌入：已有同种唯一材料（未消耗）";
                } else if (definition != null
                    && data.embeds().size() >= EmbedMaterials.slotCount(kind, data)) {
                    // 槽位已满是**静默拒绝**过：玩家会以为材料不能用，所以必须给一句提示
                    embedMessage = "未嵌入：嵌入槽位已满（" + data.embeds().size() + "/"
                        + EmbedMaterials.slotCount(kind, data) + "，材料未消耗）";
                } else if (definition != null) {
                    try {
                        ItemStack embedded = target.copy();
                        EmbedMaterials.applyForRoles(embedded, definition, newLevel, level);
                        dev.anvilcraft.thenextforge.api.TnfApi.copyComponents(target, embedded);
                        offhand.shrink(1);
                        data = data.withEmbed(definition.id());
                    } catch (RuntimeException invalidEffect) {
                        TheNextForge.LOGGER.error("[TNF] embed effect rejected {}", definition.id(), invalidEffect);
                        embedMessage = "未嵌入：材料效果不可用（未消耗）";
                    }
                    if (embedMessage.isBlank()) embedMessage = "嵌入成功：" + definition.label() + "（槽位 " + data.embeds().size()
                        + "/" + EmbedMaterials.slotCount(kind, data) + "）";
                }
            }

            ForgeQuality.renameOnUpgrade(target, data, random);
            ForgingRules.write(target, data);
            access.save(target);
            if (!amulets.contains("silence")) {
                int tierLevel = amulets.contains("abnormal") ? 4 + random.nextInt(7) : newLevel;
                playSuccessEffect(level, pos, tierLevel);
            }
            String text = ForgeFeedback.success(line, currentLevel, newLevel, cost, upgrades, sources, embedMessage);
            TheNextForge.LOGGER.info("[TNF] success: {}", text);
            access.message(Component.literal(text.toString()));
            return new Result(true, false, text.toString());
        }

        // 失败即断开猫护符的「连续成功」
        if (data.catStreak() != 0) {
            data = data.withCatStreak(catOutcome(false, data.catStreak()).streak());
            ForgingRules.write(target, data);
            access.save(target);
        }
        boolean baseSafe = baseMaterial.isSafe();
        boolean hammerSafe = hammerMaterial.isSafe();
        boolean overlimit = baseSafe || hammerSafe;
        double damageChance = amulets.contains("feather") ? 0.3 : 0.5;
        if (random.nextDouble() >= damageChance) {
            String message = "升级失败｜Lv." + currentLevel + " 不变";
            TheNextForge.LOGGER.info("[TNF] failure without damage, level stays {}", currentLevel);
            if (!amulets.contains("silence")) {
                level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }

        if (protectedStrike) {
            String message = "犰狳后续保护已生效｜Lv." + currentLevel + " 不变";
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }
        String protector = pickProtector(level, pos, amulets, data);
        if (protector != null) {
            data = data.withProtectionUsed(protector);
            if ("armadillo".equals(protector)) {
                data = data.withNextHitProtected(true);
            }
            if ("emerald".equals(protector)) {
                consumeVillager(level, pos);
            }
            if (overlimit) {
                String message = amuletName(protector) + "已生效｜免于降级，Lv." + currentLevel + " 不变";
                ForgingRules.write(target, data);
                access.save(target);
                TheNextForge.LOGGER.info("[TNF] damage blocked by {} (overlimit)", protector);
                access.message(Component.literal(message));
                return new Result(false, false, message);
            }
            String message = amuletName(protector) + "已生效｜免于损毁，Lv." + currentLevel + " 不变";
            ForgingRules.write(target, data);
            access.save(target);
            TheNextForge.LOGGER.info("[TNF] damage blocked by {}", protector);
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }


        ForgingRules.FailureOutcome outcome = ForgingRules.failureOutcome(
            baseSafe, hammerSafe, data.hasActiveEmbed(ForgingRules.TRANSCENDIUM_EMBED));
        if (outcome == ForgingRules.FailureOutcome.RESET_TO_ZERO) {
            if (amulets.contains("dog")) {
                int refund = data.xpSpent() / 2;
                if (refund > 0) {
                    access.refund(refund);
                    data = data.withXpSpent(Math.max(0, data.xpSpent() - refund));
                    TheNextForge.LOGGER.info("[TNF] dog refunded {} xp on transcendium reset", refund);
                }
            }
            data = ForgingRules.resetToZero(data, line);
            ForgingRules.write(target, data);
            access.save(target);
            String message = "超限合金粒保住装备｜Lv." + currentLevel + " → 0";
            TheNextForge.LOGGER.info("[TNF] transcendium embed prevented destruction, reset to 0");
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }

        if (outcome == ForgingRules.FailureOutcome.DESTROY && amulets.contains("topaz") && !data.protectionUsed("topaz")) {
            data = data.withProtectionUsed("topaz").withLocked();
            data = data.withLevel(line, currentLevel + 1);
            applyRoleUpgrades(target, line, currentLevel + 1, random, amulets.contains("anvil") ? 1.1 : 1.0);
            ForgeQuality.renameOnUpgrade(target, data, random);
            ForgingRules.write(target, data);
            access.save(target);
            if (effectsEnabled(amulets)) strikeLightning(level, pos);
            String message = "黄玉护符保住装备｜Lv." + currentLevel + " → " + (currentLevel + 1) + "｜已封印";
            TheNextForge.LOGGER.info("[TNF] topaz blocked damage, item locked");
            access.message(Component.literal(message));
            return new Result(false, false, message);
        }


        if (outcome == ForgingRules.FailureOutcome.DESTROY) {
            access.destroy();
            if (amulets.contains("dog") && data.xpSpent() > 0) {
                int refund = data.xpSpent() / 2;
                access.refund(refund);
                data = data.withXpSpent(Math.max(0, data.xpSpent() - refund));
                TheNextForge.LOGGER.info("[TNF] dog amulet refunded {} xp", refund);
            }
            if (effectsEnabled(amulets)) {
                level.playSound(null, pos, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    24, 0.4, 0.3, 0.4, 0.02);
            }
            String message = "锻造失败｜装备已损毁";
            TheNextForge.LOGGER.info("[TNF] destroyed");
            access.message(Component.literal(message));
            return new Result(false, true, message);
        }

        int newLevel = ForgingRules.degradedLevel(currentLevel, baseSafe, hammerSafe);
        if (amulets.contains("dog")) {
            int refund = data.xpSpent() / 2;
            if (refund > 0) {
                access.refund(refund);
                data = data.withXpSpent(Math.max(0, data.xpSpent() - refund));
                TheNextForge.LOGGER.info("[TNF] dog refunded {} xp on overlimit degrade", refund);
            }
        }
        data = ForgingRules.applyFailure(data, line, currentLevel, baseSafe, hammerSafe);
        if (newLevel == 0) {
            TheNextForge.LOGGER.info("[TNF] level reset to 0, royal token and protections unlocked");
        }
        ForgingRules.write(target, data);
        access.save(target);
        if (effectsEnabled(amulets)) {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
        String message = "锻造失败｜Lv." + currentLevel + " → " + newLevel + "（-" + (currentLevel - newLevel) + "）";
        TheNextForge.LOGGER.info("[TNF] degraded to {}", newLevel);
        access.message(Component.literal(message));
        return new Result(false, false, message);
    }

    private static String applyRoleUpgrades(ItemStack target, ForgingLine line, int newLevel,
                                            RandomSource random, double multiplier) {
        java.util.List<String> texts = new java.util.ArrayList<>();
        for (TargetKind role : TargetKind.rolesOf(target)) {
            if (line == ForgingLine.GRINDSTONE && !role.canUseGrindstone() && dev.anvilcraft.thenextforge.api.TnfApi.target(target) == null) continue;
            String text = ForgingRules.applyUpgrade(target, role, line, newLevel, random, multiplier);
            if (text != null && !texts.contains(text)) texts.add(text);
        }
        return String.join("；", texts);
    }

    /** 海绵芽取出：不判定成功与否、不降级，只把最近嵌入的材料还回去。 */
    private static Result spongeRemove(Player player, ServerLevel level, BlockPos pos,
                                       Display.ItemDisplay display, ItemStack target) {
        ItemStack sponge = player.getOffhandItem();
        ItemStack removed;
        try { removed = EmbedRemoval.removeLatest(target, level); }
        catch (RuntimeException rejected) {
            String message = "无法取出：材料撤销接口不可用，物品与海绵芽均保留";
            TheNextForge.LOGGER.error("[TNF] embed removal rejected", rejected);
            player.displayClientMessage(Component.literal(message), true);
            return new Result(false, false, message);
        }
        if (removed.isEmpty()) {
            String message = "没有可取的嵌入材料";
            TheNextForge.LOGGER.info("[TNF] sponge gemmule: nothing to remove");
            player.displayClientMessage(Component.literal(message), true);
            return new Result(false, false, message);
        }
        sponge.shrink(1);
        ForgeDisplay.setStack(display, target);
        player.getInventory().placeItemBackInInventory(removed);
        level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 0.8F, 1.0F);
        String message = "取出 " + removed.getHoverName().getString();
        TheNextForge.LOGGER.info("[TNF] sponge gemmule returned {}", removed.getItem());
        player.displayClientMessage(Component.literal(message), true);
        return new Result(true, false, message);
    }

    private static String pickProtector(ServerLevel level, BlockPos pos, Set<String> amulets, ForgingData data) {
        if (amulets.contains("armadillo") && !data.protectionUsed("armadillo")) return "armadillo";
        if (amulets.contains("emerald") && !data.protectionUsed("emerald") && hasVillagerNear(level, pos)) return "emerald";
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

    /** 设计：绿宝石护符要求「基底周围 3*3 有村民」——判定盒 3×3×3。 */
    public static boolean hasVillagerNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(Villager.class, villagerBox(pos)).isEmpty();
    }

    /** 绿宝石护符的村民判定盒（3×3×3，以基底为中心）。 */
    public static net.minecraft.world.phys.AABB villagerBox(BlockPos pos) {
        return new net.minecraft.world.phys.AABB(
            pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
            pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }

    private static void consumeVillager(ServerLevel level, BlockPos pos) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, villagerBox(pos));
        if (villagers.isEmpty()) return;
        Villager villager = villagers.getFirst();
        net.minecraft.world.entity.npc.VillagerData data = villager.getVillagerData();
        int nextLevel = Math.min(5, data.getLevel() + 1);
        villager.setVillagerData(data.setLevel(nextLevel));
        villager.setVillagerXp(net.minecraft.world.entity.npc.VillagerData.getMinXpPerLevel(nextLevel));
        TheNextForge.LOGGER.info("[TNF] emerald amulet raised villager to level {}", nextLevel);
        villager.kill();
    }

    /**
     * 蓝宝石护符：基底周围 3×1×3 有岩浆源时，随机把其中一块换成黑曜石。
     *
     * @return 是否真的消耗了一块岩浆源（决定要不要额外 +1 级）
     */
    public static boolean consumeLava(ServerLevel level, BlockPos pos) {
        List<BlockPos> lava = new java.util.ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos check = pos.offset(dx, 0, dz);
                if (level.getBlockState(check).is(Blocks.LAVA)
                    && level.getFluidState(check).isSource()) lava.add(check);
            }
        }
        if (lava.isEmpty()) return false;
        BlockPos picked = lava.get(level.getRandom().nextInt(lava.size()));
        level.setBlockAndUpdate(picked, Blocks.OBSIDIAN.defaultBlockState());
        TheNextForge.LOGGER.info("[TNF] sapphire amulet consumed lava at {}", picked);
        return true;
    }

    /** 设计：寂静护符屏蔽升级/失败/无变化的所有特效与声音，只保留文字。 */
    public static boolean effectsEnabled(java.util.Set<String> amulets) {
        return amulets == null || !amulets.contains("silence");
    }

    private static void strikeLightning(ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos.above()));
        level.addFreshEntity(bolt);
    }

    /**
     * 成功时按强化等级播放分级特效：等级越高，粒子越多、音效层次越丰富。
     */
    public static void playSuccessEffect(ServerLevel serverLevel, BlockPos pos, int level) {
        int tier = ForgeQuality.tier(level);
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
