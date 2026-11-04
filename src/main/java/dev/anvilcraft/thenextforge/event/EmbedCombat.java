package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 2026-11-03 新增材料里「造成伤害时追加」这一类效果的纯逻辑：
 * 魔法伤害追加、虚空段结算、风暴风弹、不死图腾触发。
 *
 * <p>全部抽成静态方法，GameTest 可以直接调用，不需要构造真实的伤害事件。
 */
public final class EmbedCombat {
    /** 本模组的虚空伤害类型（`data/thenextforge/damage_type/void.json`）。 */
    public static final ResourceKey<DamageType> VOID = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TheNextForge.MOD_ID, "void"));

    /** 附带的魔法伤害类型。 */
    public static final ResourceKey<DamageType> MAGIC_EXTRA = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TheNextForge.MOD_ID, "magic_extra"));

    private EmbedCombat() {
    }

    /** 武器上的龙蛋：额外 25% 魔法伤害。 */
    public static float dragonWeaponBonus(ForgingData weapon) {
        return weapon != null && weapon.hasEmbed("minecraft:dragon_egg") ? 0.25F : 0.0F;
    }

    /** 护符/装甲上的龙蛋：额外 10% 魔法伤害。 */
    public static float dragonArmourBonus(ForgingData armour) {
        return armour != null && armour.hasEmbed("minecraft:dragon_egg") ? 0.10F : 0.0F;
    }

    /** 武器上的不死图腾：额外造成目标最大生命 5% 的魔法伤害。 */
    public static float totemMaxHealthBonus(ForgingData weapon, LivingEntity target) {
        if (weapon == null || !weapon.hasEmbed("minecraft:totem_of_undying") || target == null) return 0.0F;
        return target.getMaxHealth() * 0.05F;
    }

    /**
     * 命中时按武器/护符追加魔法伤害。
     *
     * @return 实际追加的伤害量（供日志与测试）
     */
    public static float applyMagicDamage(LivingEntity target, DamageSource source, float damage,
                                         ForgingData weapon, ForgingData armour) {
        if (target == null || target.level().isClientSide()) return 0.0F;
        float extra = damage * (dragonWeaponBonus(weapon) + dragonArmourBonus(armour))
            + totemMaxHealthBonus(weapon, target);
        if (extra <= 0.0F) return 0.0F;
        DamageSource magic = magicSource(target);
        target.hurt(magic, extra);
        TheNextForge.LOGGER.info("[TNF] magic extra {} applied to {}", String.format("%.2f", extra), target.getName().getString());
        return extra;
    }

    /** 把这一段伤害作为「虚空」打出去（设计：武器 30% + 护符 10% 加算）。 */
    public static float applyVoidDamage(LivingEntity target, float amount) {
        if (target == null || amount <= 0.0F || target.level().isClientSide()) return 0.0F;
        target.hurt(voidSource(target), amount);
        TheNextForge.LOGGER.info("[TNF] void extra {} applied to {}", String.format("%.2f", amount), target.getName().getString());
        return amount;
    }

    /**
     * 是否属于「虚空伤害」：原版掉入虚空（`minecraft:out_of_world`）或本模组追加的 `thenextforge:void`。
     *
     * <p>只有这两类才该被虚空护符免疫；不能把所有伤害都当虚空，否则护符等于无敌。
     */
    public static boolean isVoidDamage(DamageSource source) {
        if (source == null) return false;
        var key = source.typeHolder().unwrapKey();
        if (key.isEmpty()) return false;
        var id = key.get().location();
        if ("thenextforge".equals(id.getNamespace())) return "void".equals(id.getPath());
        return "minecraft".equals(id.getNamespace()) && "out_of_world".equals(id.getPath());
    }

    /**
     * 是否属于「魔法伤害」：瞬间伤害/治疗药水（`minecraft:magic`）、守卫者（`indirect_magic`）、
     * 龙息（`dragon_breath`）、音爆（`sonic_boom`），以及本模组追加的 `thenextforge:magic_extra`。
     */
    public static boolean isMagicDamage(DamageSource source) {
        if (source == null) return false;
        var key = source.typeHolder().unwrapKey();
        if (key.isEmpty()) return false;
        var id = key.get().location();
        if ("thenextforge".equals(id.getNamespace())) return "magic_extra".equals(id.getPath());
        if (!"minecraft".equals(id.getNamespace())) return false;
        return switch (id.getPath()) {
            case "magic", "indirect_magic", "dragon_breath", "sonic_boom" -> true;
            default -> false;
        };
    }

    /** 是否携带能让自身免疫虚空伤害的护符（设计：虚空物质护符）。 */
    public static boolean immuneToVoid(Player player) {
        return carries(player, "anvilcraft:void_matter");
    }

    /** 是否携带能让自身免疫魔法伤害的护符/装甲（设计：龙蛋）。 */
    public static boolean immuneToMagic(Player player) {
        return carries(player, "minecraft:dragon_egg");
    }

    /** 玩家是否携带（装甲或护符）带某材料嵌入的物品。 */
    public static boolean carries(Player player, String material) {
        if (player == null) return false;
        for (ItemStack stack : player.getInventory().armor) {
            ForgingData data = stack.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
            if (data != null && data.hasEmbed(material)) return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!path.endsWith("_amulet")) continue;
            ForgingData data = stack.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
            if (data != null && data.hasEmbed(material)) return true;
        }
        return false;
    }

    /**
     * 旋风棒：命中后原地生成一次风弹爆炸（工具/武器）。
     *
     * @return 是否真的炸了
     */
    public static boolean windBurst(LivingEntity target, ForgingData weapon) {
        if (target == null || weapon == null) return false;
        if (!weapon.hasEmbed("minecraft:breeze_rod")) return false;
        if (target.level().isClientSide()) return false;
        Vec3 pos = target.position();
        target.level().explode(target, null, null, pos.x, pos.y + 0.5, pos.z, 1.5F, false,
            net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        TheNextForge.LOGGER.info("[TNF] wind charge burst at {} {}", pos.x, pos.z);
        return true;
    }

    /**
     * 不死图腾（护符）：受到致命伤害时触发一次，冷却 240s。
     *
     * @param now 当前游戏时间（tick）
     * @return 是否因此免死
     */
    public static boolean totemSaves(ServerLevel level, Player player, long now) {
        if (player == null || !carries(player, "minecraft:totem_of_undying")) return false;
        Long last = TOTEM_READY.get(player.getUUID());
        if (last != null && now - last < 240L * 20L) return false;
        TOTEM_READY.put(player.getUUID(), now);
        player.setHealth(1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0, true, false));
        TheNextForge.LOGGER.info("[TNF] totem embed saved {} at {}", player.getName().getString(), now);
        return true;
    }

    private static final java.util.Map<java.util.UUID, Long> TOTEM_READY = new java.util.HashMap<>();

    /** 测试用：清空图腾冷却。 */
    public static void resetTotemCooldown() {
        TOTEM_READY.clear();
    }

    private static DamageSource magicSource(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return target.damageSources().magic();
        return level.damageSources().source(MAGIC_EXTRA);
    }

    private static DamageSource voidSource(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return target.damageSources().magic();
        return level.damageSources().source(VOID);
    }
}
