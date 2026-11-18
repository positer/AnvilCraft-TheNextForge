package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;

import net.minecraft.core.Holder;
import java.util.List;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 锻造判定的数值规则。
 *
 * <p>成功率按品质分段下降，10 级后逐级下降并保底 5%；每次成功按对象类别随机提升一项属性。
 */
public final class ForgingRules {
    public static final double BASE_RATE = 0.9;
    public static final double TIER_DECAY = 0.85;
    public static final double MIN_RATE = 0.3;

    /** 展示框里每级升级过的护符提供的成功率加成。 */
    public static final double AMULET_LEVEL_BONUS = 0.005;

    /** 自动锤结构允许的最高等级；经验可以无限供应，因此用等级硬上限约束。 */
    public static final int MAX_AUTOMATION_LEVEL = 50;

    private ForgingRules() {
    }

    /**
     * 成功率：品质内恒定，跨品质时下降 20%；10 级之后每级再乘 0.8，最低 5%。
     *
     * <p>普通 0-3 90%、优秀 4-6 76.5%、稀有 7-9 65.03%、传说 10 级 55.27%，
     * 11 级起逐级乘 0.85，14 级起保底 30%。
     */
    public static double successRate(int level) {
        int tier = level <= 3 ? 0 : level <= 6 ? 1 : level <= 9 ? 2 : 3;
        double rate = BASE_RATE * Math.pow(TIER_DECAY, tier);
        if (level >= 10) {
            rate *= Math.pow(TIER_DECAY, level - 10);
        }
        return Math.max(MIN_RATE, rate);
    }

    /** 失败时是否真的造成损毁：50% 概率损毁，50% 不变。 */
    public static boolean destructionRoll(net.minecraft.util.RandomSource random) {
        return random.nextDouble() < 0.5;
    }

    /**
     * 失败时是否摧毁目标：基底与锤都不是超限。
     */
    public static boolean destroysOnFailure(boolean baseSafe, boolean hammerSafe) {
        return !(baseSafe || hammerSafe);
    }

    /**
     * 超限失败的单次降级上限：双超限 1 级，单超限 3 级。
     *
     * <p>连续失败会连续降级，不额外限制次数。
     */
    public static int degradeLimit(boolean baseSafe, boolean hammerSafe) {
        return baseSafe && hammerSafe ? 1 : 3;
    }

    /**
     * 超限失败后的等级，下限 0。
     */
    public static int degradedLevel(int currentLevel, boolean baseSafe, boolean hammerSafe) {
        return Math.max(0, currentLevel - degradeLimit(baseSafe, hammerSafe));
    }

    /**
     * 失败后的等级与状态结算。
     *
     * <p>超限失败按上限降级，等级归 0 时把必定成功次数重置为「未使用」（-1），
     * 让皇家材质重新生效；损毁保护状态在未来接入护符时一并清除。
     */
    public static ForgingData applyFailure(ForgingData data, ForgingLine line, int currentLevel,
                                           boolean baseSafe, boolean hammerSafe) {
        int newLevel = degradedLevel(currentLevel, baseSafe, hammerSafe);
        ForgingData next = data.withLevel(line, newLevel);
        if (newLevel == 0) {
            next = next.withTokens(line, -1);
            next = next.withProtections(java.util.List.of());
            next = next.withNextHitProtected(false);
        }
        return next;
    }

    /**
     * 单次锤击的经验消耗：6 + 4×等级，20 级起每 5 级再乘 1.5。
     */
    /** 归零并清空该线的一切状态（保护、必定成功、下一锤保护）。 */
    public static ForgingData resetToZero(ForgingData data, ForgingLine line) {
        ForgingData next = data.withLevel(line, 0);
        next = next.withTokens(line, -1);
        next = next.withProtections(java.util.List.of());
        next = next.withNextHitProtected(false);
        return next;
    }

    /** 降级返还：把被降掉的每一级消耗相加，返还其 50%。 */
    public static int refundForDegrade(int fromLevel, int toLevel) {
        if (fromLevel <= toLevel) return 0;
        int sum = 0;
        for (int level = Math.max(0, toLevel); level < fromLevel; level++) {
            sum += experienceCost(level);
        }
        return sum / 2;
    }

    public static int experienceCost(int level) {
        double base = 6 + level * 4;
        if (level >= 20) {
            int tier = (level - 20) / 5 + 1;
            base *= Math.pow(1.5, tier);
        }
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(base)));
    }

    /**
     * 按对象类别在区间内随机取一个提升值并写入属性修饰符。
     *
     * @return 本次提升的说明文本，护符没有可写的属性时返回 null
     */
    /** 不带倍率的版本（倍率 1.0）。 */
    public static String applyUpgrade(ItemStack stack, TargetKind kind, ForgingLine line, int newLevel,
                                      RandomSource random) {
        return applyUpgrade(stack, kind, line, newLevel, random, 1.0);
    }

    public static String applyUpgrade(ItemStack stack, TargetKind kind, ForgingLine line, int newLevel,
                                      RandomSource random, double multiplier) {
        var pool = dev.anvilcraft.thenextforge.api.TnfApi.upgrades(stack);
        var custom = pool == null ? null : pool.rule(kind, line);
        if (custom != null) return dev.anvilcraft.thenextforge.api.TnfApi.applyUpgradeRule(stack, kind, line, newLevel, random, multiplier, custom);
        // 数值区间与权重来自 ForgeRulesData（设计 L19：数据驱动），这里只负责把结果落到物品上
        List<ForgeRulesData.Outcome> eligible = ForgeRulesData.eligible(kind, line, stack);
        ForgeRulesData.Outcome outcome = ForgeRulesData.pick(eligible, random);
        if (outcome == null) return null;
        double raw = outcome.roll(random);
        return switch (outcome.effect()) {
            case "attack" -> {
                double amount = raw * multiplier;
                addModifier(stack, Attributes.ATTACK_DAMAGE, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "weapon_attack", newLevel);
                yield String.format("攻击力 +%.1f%%", amount * 100.0);
            }
            case "reach" -> {
                double amount = raw * multiplier;
                // 设计：提高 10% 实体触及距离 —— 按基础触及的百分比，不是绝对值
                addModifier(stack, Attributes.ENTITY_INTERACTION_RANGE, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "weapon_reach", newLevel);
                yield "实体触及 +10%";
            }
            case "cooldown" -> {
                double amount = raw * multiplier;
                addModifier(stack, Attributes.ATTACK_SPEED, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "weapon_cooldown", newLevel);
                yield String.format("攻击冷却回复 +%.1f%%", amount * 100.0);
            }
            case "sweep" -> {
                double amount = raw * multiplier;
                addModifier(stack, Attributes.SWEEPING_DAMAGE_RATIO, amount,
                    AttributeModifier.Operation.ADD_VALUE, "weapon_sweep", newLevel);
                yield "横扫伤害 +10%";
            }
            case "mining" -> {
                double amount = raw * multiplier;
                addModifier(stack, Attributes.BLOCK_BREAK_SPEED, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "tool_mining", newLevel);
                yield String.format("挖掘速度 +%.1f%%", amount * 100.0);
            }
            case "durability" -> {
                ForgeDurability.upgrade(stack, line, newLevel, 1.0 + raw);
                yield String.format("耐久 +%.1f%%", raw * 100.0);
            }
            case "armor_flat" -> {
                addModifier(stack, Attributes.ARMOR, raw,
                    AttributeModifier.Operation.ADD_VALUE, "armor_value", newLevel);
                yield "护甲 +" + (long) raw;
            }
            case "move_speed" -> {
                addModifier(stack, Attributes.MOVEMENT_SPEED, raw,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "armor_speed", newLevel);
                // 设计：砂轮升级降低 5% 耐久
                ForgeDurability.upgrade(stack, line, newLevel, 0.95);
                yield String.format("移速 +%.1f%%（耐久 -5%%）", raw * 100.0);
            }
            case "amulet_damage" -> {
                addModifier(stack, Attributes.ATTACK_DAMAGE, raw,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "amulet_damage", newLevel);
                yield String.format("伤害 +%.1f%%", raw * 100.0);
            }
            default -> {
                TheNextForge.LOGGER.warn("[TNF] unknown upgrade effect '{}' in forge rules", outcome.effect());
                yield null;
            }
        };
    }
    /** 失败后的处理方式。 */
    public enum FailureOutcome {
        /** 超限基底或超限锤：只降级（1 级或 3 级），绝不摧毁、也不触发超限合金粒的归零。 */
        DEGRADE,
        /** 没有超限保护，但嵌入了超限合金粒：把「摧毁」换成「等级归零」。 */
        RESET_TO_ZERO,
        /** 其余情况：摧毁对象。 */
        DESTROY
    }

    /**
     * 判定失败后果。
     *
     * <p>设计：「超限：升级失败后最多下降 3 级……不摧毁升级对象，同时超限基底和锤则为最多下降 1 级」；
     * 「超限金属粒：……阻挡最终损毁替换为等级降至 0」。所以**超限保护优先**——只有在对象本来会被摧毁时，
     * 超限合金粒才把摧毁换成归零。两条升级路径（手动锤击与自动锤）都必须走这个判定，避免顺序分叉。
     */
    public static FailureOutcome failureOutcome(boolean baseSafe, boolean hammerSafe, boolean hasTranscendiumEmbed) {
        if (baseSafe || hammerSafe) return FailureOutcome.DEGRADE;
        if (hasTranscendiumEmbed) return FailureOutcome.RESET_TO_ZERO;
        return FailureOutcome.DESTROY;
    }

    /** 超限合金粒的 id，失败判定与槽位计算共用。 */
    public static final String TRANSCENDIUM_EMBED = "anvilcraft:transcendium_nugget";

    /** 砂轮独有的属性键；其余锻造键都属于铁砧线。 */
    private static final java.util.Set<String> GRINDSTONE_KEYS =
        java.util.Set.of("weapon_cooldown", "weapon_sweep", "armor_speed");

    /**
     * 把锻造状态写回物品，并按当前等级清掉已经作废的修饰符。
     *
     * <p>每成功一级写入一条 {@code forging/<key>/<level>}，所以降级与归零必须以等级为界删除，
     * 否则被降掉的等级会继续加成。所有写回都必须走这里，不要再直接 set 组件。
     */
    public static void write(ItemStack stack, ForgingData data) {
        ForgingData before = stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        ForgeQuality.refreshAppearance(stack, data);
        ForgeDurability.prune(stack, data);
        ItemAttributeModifiers modifiers = dev.anvilcraft.thenextforge.forging.ForgeAttributes.current(stack);
        java.util.List<ItemAttributeModifiers.Entry> kept =
            new java.util.ArrayList<>(modifiers.modifiers().size());
        boolean pruned = false;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (isObsolete(entry.modifier().id(), data)) {
                pruned = true;
                continue;
            }
            if (ForgingSummary.isForgeModifier(entry.modifier().id())) {
                String key = ForgingSummary.keyOf(entry.modifier().id());
                if ("tool_mining".equals(key) && entry.attribute().equals(Attributes.MINING_EFFICIENCY)) {
                    entry = new ItemAttributeModifiers.Entry(Attributes.BLOCK_BREAK_SPEED, entry.modifier(), entry.slot());
                    pruned = true;
                } else if ("weapon_cooldown".equals(key)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                    entry = new ItemAttributeModifiers.Entry(entry.attribute(),
                        new AttributeModifier(entry.modifier().id(), entry.modifier().amount(),
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), entry.slot());
                    pruned = true;
                }
            }
            kept.add(entry);
        }
        if (pruned) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS,
                new ItemAttributeModifiers(java.util.List.copyOf(kept), modifiers.showInTooltip()));
        }
        stack.set(ModDataComponents.FORGING.get(), data);
        dev.anvilcraft.thenextforge.api.TnfApi.publishDataChange(stack, before, data);
    }

    /** 该条锻造修饰符的等级是否已经高于物品当前的对应等级。 */
    private static boolean isObsolete(ResourceLocation id, ForgingData data) {
        if (!ForgingSummary.isForgeModifier(id)) return false;
        int level = ForgingSummary.levelOf(id);
        int cap = data.level((GRINDSTONE_KEYS.contains(ForgingSummary.keyOf(id)) || ForgingSummary.keyOf(id).startsWith("api_g_"))
            ? ForgingLine.GRINDSTONE : ForgingLine.ANVIL);
        // 旧格式条目没有等级段，按「一次成功」计：只有整条线归零时才清除
        return level >= 0 ? level > cap : cap <= 0;
    }


    /** API 维护的修饰符沿用等级寻址，降级自动清理；slot 明确指定，避免多角色误落装备槽。 */
    public static void addApiModifier(ItemStack stack, Holder<Attribute> attribute, double amount,
                                      AttributeModifier.Operation operation, String key, int level, EquipmentSlotGroup slot) {
        if (!key.startsWith("api_a_") && !key.startsWith("api_g_")) throw new IllegalArgumentException("Reserved API modifier key");
        if (!Double.isFinite(amount) || level < 1) throw new IllegalArgumentException("Invalid API modifier");
        var current = dev.anvilcraft.thenextforge.forging.ForgeAttributes.current(stack);
        var id = ResourceLocation.fromNamespaceAndPath("thenextforge", "forging/" + key + "/" + level);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, current.withModifierAdded(attribute, new AttributeModifier(id, amount, operation), slot));
    }

    private static void addModifier(
        ItemStack stack,
        Holder<Attribute> attribute,
        double amount,
        AttributeModifier.Operation operation,
        String key,
        int level
    ) {
        ItemAttributeModifiers current = dev.anvilcraft.thenextforge.forging.ForgeAttributes.current(stack);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            "thenextforge", "forging/" + key + "/" + level
        );
        ItemAttributeModifiers updated = current.withModifierAdded(
            attribute,
            new AttributeModifier(id, amount, operation),
            (TargetKind.of(stack) == TargetKind.ARMOR ? EquipmentSlotGroup.ARMOR
                : TargetKind.of(stack) == TargetKind.AMULET ? EquipmentSlotGroup.ANY : EquipmentSlotGroup.MAINHAND)
        );
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, updated);
    }
}
