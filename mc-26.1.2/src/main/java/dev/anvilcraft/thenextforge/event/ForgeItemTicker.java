package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 嵌入材料的持续性效果。
 *
 * <p>每 5 秒结算一次：按嵌入材料恢复耐久；携带带有对应嵌入的护符时给玩家效果。
 */
public final class ForgeItemTicker {
    private static final int INTERVAL = 40;

    private ForgeItemTicker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % INTERVAL != 0) return;
        int armourWithGlow = 0;
        for (ItemStack a : armourSlots(player)) {
            if (grantsNightVision(a)) armourWithGlow++;
        }
        if (armourWithGlow > 0) {
            TheNextForge.LOGGER.info("[TNF] ticker: {} armour piece(s) grant night vision (player {})", armourWithGlow, player.getName().getString());
        }

        ItemStack mainHand = player.getMainHandItem();
        ForgingData mainData = mainHand.get(ModDataComponents.FORGING.get());
        if (mainData != null && mainData.hasEmbed("minecraft:redstone")
            && TargetKind.of(mainHand) == TargetKind.TOOL) {
            // 急迫只给工具；武器上的红石改为降低武器冷却（见 EmbedMaterials.apply）
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, 0, true, false));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            tickStack(player, player.getItemBySlot(slot));
        }
        tickStack(player, player.getMainHandItem());
        tickStack(player, player.getOffhandItem());

        // 设计：沉重核心 装甲/护符 → 水中无法游泳
        if (player.isInWater()) {
            for (ItemStack armourPiece : armourSlots(player)) {
                if (heavyCoreDrownsSwimming(armourPiece, true)) {
                    player.setSwimming(false);
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, true, false));
                    break;
                }
            }
        }
        boolean saturationSource = false;
        for (ItemStack armour : armourSlots(player)) {
            if (grantsSaturation(armour)) saturationSource = true;
        }
        for (ItemStack carried : player.getInventory().getNonEquipmentItems()) {
            if (!carried.isEmpty() && carried.getItem() instanceof net.minecraft.world.item.Item
                && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(carried.getItem()).getPath().endsWith("_amulet")
                && grantsSaturation(carried)) {
                saturationSource = true;
            }
        }
        if (saturationSource && player.tickCount % 400 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0, true, false));
        }
        for (ItemStack armour : armourSlots(player)) {
            for (net.minecraft.world.effect.MobEffectInstance effect : armourEffects(armour)) {
                player.addEffect(effect);
            }
            if (grantsNightVision(armour)) {
                TheNextForge.LOGGER.info("[TNF] night vision from {} (player {})", armour.getItem(), player.getName().getString());
            }
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) continue;
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_amulet")) {
                tickAmulet(player, stack);
            }
        }
    }

    /**
     * 该装甲是否因为嵌入发光墨囊而应当提供夜视。
     *
     * <p>抽成纯函数，GameTest 可以直接断言。
     */
    public static boolean grantsNightVision(ItemStack armour) {
        if (armour == null || armour.isEmpty()) return false;
        ForgingData data = armour.get(ModDataComponents.FORGING.get());
        return data != null && data.hasEmbed("minecraft:glow_ink_sac");
    }

    private static void tickStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        if (data == null || data.embeds().isEmpty()) return;
        int restore = repairAmount(data, player);
        if (restore > 0) repair(stack, restore);
    }

    /**
     * 每 5 秒按嵌入材料计算的耐久恢复量。
     *
     * <p>抽出成纯函数，GameTest 可以直接断言各材料的恢复数值。
     */
    /** 抗性提升的全局上限：设计注明「最高 4」，即 amplifier ≤ 3。 */
    public static int cappedResistance(int amplifier) {
        return Math.max(0, Math.min(3, amplifier));
    }

    /**
     * 装甲类嵌入提供的持续效果（设计：下界之星抗性+3、中子锭抗性+1、红石速度 I、发光墨囊夜视）。
     *
     * <p>抽成纯函数，矩阵测试可直接断言「哪件装甲给哪个效果」，不必跑整条 tick。
     */
    public static java.util.List<MobEffectInstance> armourEffects(ItemStack armour) {
        java.util.List<MobEffectInstance> effects = new java.util.ArrayList<>();
        ForgingData data = armour.get(ModDataComponents.FORGING.get());
        if (data == null) return effects;
        if (data.hasEmbed("minecraft:nether_star")) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(2), true, false));
        }
        // 设计：中子锭 抗性提升 +1（最高 4）(装甲/护符)
        if (data.hasEmbed("anvilcraft:neutronium_ingot")) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(0), true, false));
        }
        // 设计：红石 提供 1 级速度（装甲/护符）
        if (data.hasEmbed("minecraft:redstone")) {
            effects.add(new MobEffectInstance(MobEffects.SPEED, 120, 0, true, false));
        }
        if (grantsNightVision(armour)) {
            effects.add(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, true, false));
        }
        addNewEmbedEffects(data, effects);
        return effects;
    }

    /** 红石：主手工具或护符带嵌入时提供急迫。 */
    public static boolean hasRedstoneHaste(ForgingData mainHandData) {
        return mainHandData != null && mainHandData.hasEmbed("minecraft:redstone");
    }

    public static int repairAmount(ForgingData data, net.minecraft.world.entity.player.Player player) {
        return repairAmount(
            data,
            player.isInWater(),
            player.isOnFire(),
            dev.dubhe.anvilcraft.api.power.PowerGrid.findPowerGridContains(
                player.level(), player.position()).isPresent()
        );
    }

    /**
     * 带状态的恢复量计算：水中、着火、电网内各自独立判断。
     */
    public static int repairAmount(ForgingData data, boolean inWater, boolean onFire, boolean inGrid) {
        int restore = 0;
        // 绿宝石改走「独立经验修补」（ForgeHarvestEvents），不再计入每 5 秒回复
        if (data.hasEmbed("anvilcraft:sapphire") && inWater) restore += 2;
        if (data.hasEmbed("anvilcraft:ruby") && onFire) restore += 2;
        if (data.hasEmbed("anvilcraft:topaz") && inGrid) restore += 2;
        if (data.hasEmbed("#minecraft:logs")) restore += 1;
        return restore;
    }

    /**
     * 护符类嵌入提供的持续效果（设计第 45-66 行）。
     *
     * <p>只返回药水效果；「修复手持物品」「创造飞行」这类副作用仍在 tick 里做。
     * 抽成纯函数是为了让矩阵测试能逐条断言「哪个材料给几级什么效果」。
     */
    public static java.util.List<MobEffectInstance> amuletEffects(ItemStack amulet) {
        java.util.List<MobEffectInstance> effects = new java.util.ArrayList<>();
        ForgingData data = amulet.get(ModDataComponents.FORGING.get());
        if (data == null || data.embeds().isEmpty()) return effects;
        if (data.hasEmbed("anvilcraft:sapphire")) {
            effects.add(new MobEffectInstance(MobEffects.WATER_BREATHING, 120, 0, true, false));
        }
        if (data.hasEmbed("anvilcraft:ruby")) {
            effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
        }
        if (data.hasEmbed("minecraft:redstone")) {
            effects.add(new MobEffectInstance(MobEffects.HASTE, 120, 0, true, false));
            effects.add(new MobEffectInstance(MobEffects.SPEED, 120, 0, true, false));
        }
        if (data.hasEmbed("minecraft:glow_ink_sac")) {
            effects.add(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
        }
        if (data.hasEmbed("minecraft:heart_of_the_sea")) {
            effects.add(new MobEffectInstance(MobEffects.CONDUIT_POWER, 120, 0, true, false));
        }
        // 设计：下界之星 抗性提升 +3（最高 4）
        if (data.hasEmbed("minecraft:nether_star")) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(2), true, false));
        }
        // 设计：下界合金锭 / 中子锭 抗性提升 +1（最高 4）
        if (data.hasEmbed("anvilcraft:netherite_ingot") || data.hasEmbed("anvilcraft:neutronium_ingot")) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(0), true, false));
        }
        addNewEmbedEffects(data, effects);
        return effects;
    }

    /**
     * 2026-11-03 新增材料的持续效果（装甲与护符共用）：
     * 羽毛缓降、附魔金苹果/沉重核心的抗性与恢复、不死图腾的生命恢复。
     *
     * <p>抽成纯函数，矩阵测试可以直接断言「哪个材料给几级什么效果」。
     */
    public static void addNewEmbedEffects(ForgingData data, java.util.List<MobEffectInstance> effects) {
        if (data == null) return;
        // 设计：羽毛(轻柔) 护符 → 缓降
        if (data.hasEmbed("minecraft:feather")) {
            effects.add(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));
        }
        // 设计：附魔金苹果(神之力) 装甲/护符 → 抗火、抗性提升 +1、生命恢复 +1
        if (data.hasEmbed("minecraft:enchanted_golden_apple")) {
            effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(0), true, false));
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
        }
        // 设计：沉重核心(沉重) 装甲/护符 → 抗性 +2（最高 4）
        if (data.hasEmbed("minecraft:heavy_core")) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(1), true, false));
        }
        // 设计：不死图腾(不朽生命) 装甲/护符 → 生命恢复 +2（最大生命由属性修饰符给）
        if (data.hasEmbed("minecraft:totem_of_undying")) {
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, true, false));
        }
    }

    /** 沉重核心：水中无法游泳（设计「抗性 +2、移速 -20% 且水中无法游泳」的第三段）。 */
    public static boolean heavyCoreDrownsSwimming(ItemStack stack, boolean inWater) {
        if (!inWater || stack == null || stack.isEmpty()) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && data.hasEmbed("minecraft:heavy_core");
    }

    /** 金胡萝卜：每 20 秒给 5 秒饱和（唯一嵌入）。 */
    public static boolean grantsSaturation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && data.hasEmbed("minecraft:golden_carrot");
    }

    private static void tickAmulet(ServerPlayer player, ItemStack amulet) {
        ForgingData data = amulet.get(ModDataComponents.FORGING.get());
        if (data == null || data.embeds().isEmpty()) return;
        for (MobEffectInstance effect : amuletEffects(amulet)) {
            player.addEffect(effect);
        }
        if (data.hasEmbed("#minecraft:logs")) {
            ItemStack held = player.getMainHandItem();
            if (held.isDamageableItem() && held.getDamageValue() > 0) {
                repair(held, 1);
            }
        }
        if (data.hasEmbed("anvilcraft:transcendium_nugget")) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }
    private static void repair(ItemStack stack, int amount) {
        int damage = stack.getDamageValue();
        if (damage <= 0) return;
        stack.setDamageValue(Math.max(0, damage - amount));
    }

    /** 26.1.2：Inventory.armor 已移除，按装备槽取。 */
    private static java.util.List<ItemStack> armourSlots(net.minecraft.world.entity.player.Player player) {
        return java.util.List.of(
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
            player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)
        );
    }
}
