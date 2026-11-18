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
@EventBusSubscriber(modid = TheNextForge.MOD_ID)
public final class ForgeItemTicker {
    private static final java.util.Map<net.minecraft.world.entity.player.Player, Boolean> FLIGHT_GRANTED = new java.util.WeakHashMap<>();
    private static final int EFFECT_INTERVAL = 40;
    private static final int REPAIR_INTERVAL = 100;
    private static final EquipmentSlot[] ACTIVE_SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
        EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ForgeItemTicker() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean repairDue = player.tickCount % REPAIR_INTERVAL == 0;
        boolean effectsDue = player.tickCount % EFFECT_INTERVAL == 0;
        if (!repairDue && !effectsDue) return;
        // 电网只在损坏且嵌黄玉的装备上查询，同一玩家本轮最多一次。
        if (repairDue) repairEquipped(player, () ->
            dev.dubhe.anvilcraft.api.power.PowerGrid.findPowerGridContains(
                player.level(), player.position()).isPresent());
        boolean saturation = false;
        boolean heart = false;
        boolean heavy = false;
        boolean flight = false;
        java.util.List<MobEffectInstance> effects = new java.util.ArrayList<>();
        for (EquipmentSlot slot : ACTIVE_SLOTS) {
            if (!slot.isArmor()) continue;
            ItemStack armour = player.getItemBySlot(slot);
            if (effectsDue) effects.addAll(armourEffects(armour));
            saturation |= grantsSaturation(armour);
            heart |= hasEmbed(armour, "minecraft:heart_of_the_sea");
            heavy |= heavyCoreDrownsSwimming(armour, player.isInWater());
        }
        ItemStack mainHand = player.getMainHandItem();
        if (effectsDue && hasRedstoneHaste(mainHand.get(ModDataComponents.FORGING.get()))
            && TargetKind.rolesOf(mainHand).contains(TargetKind.TOOL)) {
            effects.add(new MobEffectInstance(MobEffects.HASTE, 120, 0, true, false));
        }
        for (ItemStack amulet : carriedAmulets(player)) {
            if (effectsDue) effects.addAll(amuletEffects(amulet));
            ForgingData data = amulet.get(ModDataComponents.FORGING.get());
            saturation |= grantsSaturation(amulet);
            heart |= data.hasActiveEmbed("minecraft:heart_of_the_sea");
            heavy |= heavyCoreDrownsSwimming(amulet, player.isInWater());
            if (repairDue && data.hasActiveEmbed("#minecraft:logs")) {
                repair(mainHand, java.util.Collections.frequency(data.activeEmbeds(), "#minecraft:logs"));
            }
            flight |= data.hasActiveEmbed("anvilcraft:transcendium_nugget");
        }
        if (player.tickCount % 400 == 0 && saturation) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 0, true, false));
        }
        if (!effectsDue) return;
        if (flight && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            FLIGHT_GRANTED.put(player, true);
            player.onUpdateAbilities();
        } else if (!flight && FLIGHT_GRANTED.remove(player) != null && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        if (grantsConduitPower(player.isInWater(), heart, false)) {
            effects.add(new MobEffectInstance(MobEffects.CONDUIT_POWER, 120, 0, true, false));
        }
        if (heavy) {
            player.setSwimming(false);
            effects.add(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1, true, false));
        }
        for (MobEffectInstance effect : combineEffects(effects)) player.addEffect(effect);
    }

    /** 不持有玩家或世界的跨 tick 缓存，避免断开后滞留。 */
    public static java.util.List<ItemStack> carriedAmulets(net.minecraft.world.entity.player.Player player) {
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (isEmbeddedAmulet(stack)) result.add(stack);
        }
        if (isEmbeddedAmulet(player.getOffhandItem())) result.add(player.getOffhandItem());
        return result;
    }

    private static boolean isEmbeddedAmulet(ItemStack stack) {
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_amulet")) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && !data.activeEmbeds().isEmpty();
    }

    private static boolean hasEmbed(ItemStack stack, String material) {
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && data.hasActiveEmbed(material);
    }

    /** 每个装备槽只修一次；查询器可注入以验证电网开销上界。 */
    public static void repairEquipped(net.minecraft.world.entity.player.Player player,
                                      java.util.function.BooleanSupplier gridQuery) {
        Boolean inGrid = null;
        for (EquipmentSlot slot : ACTIVE_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) continue;
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data == null || data.activeEmbeds().isEmpty()) continue;
            if (data.hasActiveEmbed("anvilcraft:topaz") && inGrid == null) inGrid = gridQuery.getAsBoolean();
            repair(stack, repairAmount(data, player.isInWater(), player.isOnFire(), Boolean.TRUE.equals(inGrid)));
        }
    }

    /**
     * 海洋庇佑是否生效：必须在水中，且装甲或护符上嵌了海洋之心。
     *
     * <p>抽成纯函数，矩阵测试可以直接断言「出水就没了」。
     */
    public static boolean grantsConduitPower(boolean inWater, boolean wornHeart, boolean carriedHeart) {
        return inWater && (wornHeart || carriedHeart);
    }

    /**
     * 该装甲是否因为嵌入发光墨囊而应当提供夜视。
     *
     * <p>抽成纯函数，GameTest 可以直接断言。
     */
    public static boolean grantsNightVision(ItemStack armour) {
        if (armour == null || armour.isEmpty()) return false;
        ForgingData data = armour.get(ModDataComponents.FORGING.get());
        return data != null && data.hasActiveEmbed("minecraft:glow_ink_sac");
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
        return materialEffects(armour.get(ModDataComponents.FORGING.get()), false);
    }

    /** 红石：主手工具或护符带嵌入时提供急迫。 */
    public static boolean hasRedstoneHaste(ForgingData mainHandData) {
        return mainHandData != null && mainHandData.hasActiveEmbed("minecraft:redstone");
    }

    public static int repairAmount(ForgingData data, net.minecraft.world.entity.player.Player player) {
        return repairAmount(
            data,
            player.isInWater(),
            player.isOnFire(),
            data.hasActiveEmbed("anvilcraft:topaz") && dev.dubhe.anvilcraft.api.power.PowerGrid.findPowerGridContains(
                player.level(), player.position()).isPresent()
        );
    }

    /**
     * 带状态的恢复量计算：水中、着火、电网内各自独立判断。
     */
    public static int repairAmount(ForgingData data, boolean inWater, boolean onFire, boolean inGrid) {
        int restore = 0;
        // 绿宝石改走「独立经验修补」（ForgeHarvestEvents），不再计入每 5 秒回复
        if (inWater) restore += 2 * java.util.Collections.frequency(data.activeEmbeds(), "anvilcraft:sapphire");
        if (onFire) restore += 2 * java.util.Collections.frequency(data.activeEmbeds(), "anvilcraft:ruby");
        if (inGrid) restore += 2 * java.util.Collections.frequency(data.activeEmbeds(), "anvilcraft:topaz");
        if (true) restore += 1 * java.util.Collections.frequency(data.activeEmbeds(), "#minecraft:logs");
        return restore;
    }

    /**
     * 护符类嵌入提供的持续效果（设计第 45-66 行）。
     *
     * <p>只返回药水效果；「修复手持物品」「创造飞行」这类副作用仍在 tick 里做。
     * 抽成纯函数是为了让矩阵测试能逐条断言「哪个材料给几级什么效果」。
     */
    public static java.util.List<MobEffectInstance> amuletEffects(ItemStack amulet) {
        return materialEffects(amulet.get(ModDataComponents.FORGING.get()), true);
    }

    /** 每个嵌入贡献一份效果，之后按效果类别合并；重复材料不会丢失。 */
    private static java.util.List<MobEffectInstance> materialEffects(ForgingData data, boolean amulet) {
        java.util.List<MobEffectInstance> effects = new java.util.ArrayList<>();
        if (data == null) return effects;
        for (String material : data.activeEmbeds()) {
            switch (material) {
                case "minecraft:redstone" -> {
                    effects.add(new MobEffectInstance(MobEffects.SPEED, 120, 0, true, false));
                    if (amulet) effects.add(new MobEffectInstance(MobEffects.HASTE, 120, 0, true, false));
                }
                case "minecraft:glow_ink_sac" -> effects.add(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0, true, false));
                case "minecraft:nether_star" -> effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, 2, true, false));
                case "anvilcraft:neutronium_ingot" -> effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false));
                case "minecraft:netherite_ingot" -> {
                    if (amulet) effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false));
                }
                case "anvilcraft:sapphire" -> {
                    if (amulet) effects.add(new MobEffectInstance(MobEffects.WATER_BREATHING, 120, 0, true, false));
                }
                case "anvilcraft:ruby" -> {
                    if (amulet) effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
                }
                case "minecraft:feather" -> {
                    if (amulet) effects.add(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));
                }
                case "minecraft:enchanted_golden_apple" -> {
                    effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
                    effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, true, false));
                    effects.add(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
                }
                case "minecraft:heavy_core" -> effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, 1, true, false));
                case "minecraft:totem_of_undying" -> effects.add(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, true, false));
                default -> { }
            }
        }
        return effects;
    }

    public static java.util.List<MobEffectInstance> combineEffects(java.util.List<MobEffectInstance> effects) {
        java.util.Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, MobEffectInstance> combined = new java.util.LinkedHashMap<>();
        for (MobEffectInstance effect : effects) {
            MobEffectInstance old = combined.get(effect.getEffect());
            int amplifier = effect.getAmplifier() + (old == null ? 0 : old.getAmplifier() + 1);
            if (effect.getEffect().equals(MobEffects.RESISTANCE)) amplifier = cappedResistance(amplifier);
            combined.put(effect.getEffect(), new MobEffectInstance(effect.getEffect(),
                Math.max(effect.getDuration(), old == null ? 0 : old.getDuration()), amplifier, true, false));
        }
        return java.util.List.copyOf(combined.values());
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
        if (data.hasActiveEmbed("minecraft:feather")) {
            effects.add(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false));
        }
        // 设计：附魔金苹果(神之力) 装甲/护符 → 抗火、抗性提升 +1、生命恢复 +1
        if (data.hasActiveEmbed("minecraft:enchanted_golden_apple")) {
            effects.add(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0, true, false));
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(0), true, false));
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
        }
        // 设计：沉重核心(沉重) 装甲/护符 → 抗性 +2（最高 4）
        if (data.hasActiveEmbed("minecraft:heavy_core")) {
            effects.add(new MobEffectInstance(MobEffects.RESISTANCE, 120, cappedResistance(1), true, false));
        }
        // 设计：不死图腾(不朽生命) 装甲/护符 → 生命恢复 +2（最大生命由属性修饰符给）
        if (data.hasActiveEmbed("minecraft:totem_of_undying")) {
            effects.add(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, true, false));
        }
    }

    /** 沉重核心：水中无法游泳（设计「抗性 +2、移速 -20% 且水中无法游泳」的第三段）。 */
    public static boolean heavyCoreDrownsSwimming(ItemStack stack, boolean inWater) {
        if (!inWater || stack == null || stack.isEmpty()) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && data.hasActiveEmbed("minecraft:heavy_core");
    }

    /** 金胡萝卜：每 20 秒给 5 秒饱和（唯一嵌入）。 */
    public static boolean grantsSaturation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && data.hasActiveEmbed("minecraft:golden_carrot");
    }

    private static void repair(ItemStack stack, int amount) {
        int damage = stack.getDamageValue();
        if (!stack.isDamageableItem() || damage <= 0 || amount <= 0) return;
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
