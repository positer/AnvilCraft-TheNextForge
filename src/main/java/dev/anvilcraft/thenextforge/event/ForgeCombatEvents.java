package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 武器嵌入材料在命中时触发的附加效果。
 *
 * <p>只处理主手武器带嵌入的玩家攻击；受击反制在 {@link ForgeDefenceEvents}。
 */
public final class ForgeCombatEvents {
    private static final java.util.Map<LivingEntity, Float> VOID_PENDING = new java.util.WeakHashMap<>();

    private ForgeCombatEvents() {
    }

    /** 伤害结算前：诅咒金按诅咒数加成、虚空物质把 30% 伤害换成虚空段、琥珀按怨恨值加成。 */
    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        // 自身免疫与攻击者无关：掉进虚空、被药水砸这类伤害根本没有玩家攻击者，
        // 所以必须放在最前面判定，否则护符永远不生效。
        if (event.getEntity() instanceof Player victim) {
            if (dev.anvilcraft.thenextforge.event.EmbedCombat.immuneToVoid(victim)
                && dev.anvilcraft.thenextforge.event.EmbedCombat.isVoidDamage(event.getSource())) {
                TheNextForge.LOGGER.info("[TNF] void immunity blocked {} for {}",
                    event.getSource().type().msgId(), victim.getName().getString());
                event.setNewDamage(0.0F);
                return;
            }
            if (dev.anvilcraft.thenextforge.event.EmbedCombat.immuneToMagic(victim)
                && dev.anvilcraft.thenextforge.event.EmbedCombat.isMagicDamage(event.getSource())) {
                TheNextForge.LOGGER.info("[TNF] magic immunity blocked {} for {}",
                    event.getSource().type().msgId(), victim.getName().getString());
                event.setNewDamage(0.0F);
                return;
            }
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) return;
        ItemStack weapon = player.getMainHandItem();
        ForgingData data = weapon.get(ModDataComponents.FORGING.get());

        // 设计：武器 30% + 护符 10%，不同来源的换算百分比**加算**（同时存在 = 40%）
        float weaponVoid = data != null && data.hasEmbed("anvilcraft:void_matter") ? 0.3F : 0.0F;
        float amuletVoid = dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(
            player, "anvilcraft:void_matter") ? 0.1F : 0.0F;

        if (weaponVoid + amuletVoid > 0.0F) {
            float ratio = Math.min(1.0F, weaponVoid + amuletVoid);
            float original = event.getNewDamage();
            event.setNewDamage(original * (1.0F - ratio));
            if (event.getEntity() instanceof LivingEntity living) {
                VOID_PENDING.put(living, original * ratio);
            }
        }
        if (data != null && data.hasEmbed("anvilcraft:frost_metal_ingot")) {
            int enchantCount = weapon.getOrDefault(
                net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
            ).size();
            if (enchantCount > 0) {
                event.setNewDamage(event.getNewDamage() * (1.0F + 0.05F * enchantCount));
            }
        }
        if (data != null && data.hasEmbed("anvilcraft:void_matter") == false
            && dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "anvilcraft:frost_metal_ingot")) {
            int playerLevel = Math.max(0, player.experienceLevel);
            if (playerLevel > 0) {
                event.setNewDamage(event.getNewDamage() * (1.0F + 0.01F * playerLevel));
            }
        }
        if (event.getEntity() instanceof LivingEntity hurt) {
            var vulnerable = dev.anvilcraft.thenextforge.init.ModAttachments.VULNERABLE_UNTIL.get();
            long until = hurt.getData(vulnerable);
            if (until > 0 && hurt.level().getGameTime() <= until) {
                event.setNewDamage(event.getNewDamage() * 1.3F);
            }
        }
        // 魔法/虚空免疫已在方法开头按伤害类型判定，这里不再重复检查
        if (carriesCursedAmulet(player)) {
            int curses = countCurses(player);
            if (curses > 0) {
                event.setNewDamage(event.getNewDamage() * (1.0F + 0.05F * curses));
            }
        }
        // 设计 L73：琥珀是**武器**嵌入 —— 命中后给目标加基础怨恨值 +20%（最高 100%）。
        // 之前这段缩进在「诅咒金护符」块里，导致只有带诅咒金护符时才生效。
        if (event.getEntity() instanceof LivingEntity amberTarget) {
            applyAmberMark(data, amberTarget);
        }
    }

    /**
     * 琥珀标记：按武器上的琥珀数量给目标加怨恨（每种武器只加一次）。
     *
     * <p>抽成公开方法便于测试「接线」本身，而不只是 {@code AmberMarking.apply} 的值计算。
     *
     * @return 施加后的怨恨值；没有琥珀嵌入或目标不是生物时返回 -1
     */
    public static int applyAmberMark(ForgingData data, LivingEntity target) {
        if (data == null || target == null) return -1;
        int amberCount = (int) data.embeds().stream().filter("anvilcraft:amber"::equals).count();
        if (amberCount <= 0) return -1;
        return dev.anvilcraft.thenextforge.forging.AmberMarking.apply(target, amberCount);
    }

    private static boolean carriesAmuletEmbed(Player player, String material) {
        for (ItemStack stack : player.getInventory().items) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && data.hasEmbed(material)) return true;
        }
        return false;
    }

    private static boolean carriesCursedAmulet(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && data.hasEmbed("anvilcraft:cursed_gold_ingot")) return true;
        }
        return false;
    }

    public static int countCurses(Player player) {
        int curses = 0;
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.add(player.getMainHandItem());
        stacks.add(player.getOffhandItem());
        stacks.addAll(player.getInventory().armor);
        stacks.addAll(player.getInventory().items);
        for (ItemStack stack : stacks) {
            var enchants = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.ENCHANTMENTS,
                net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY
            );
            for (var entry : enchants.entrySet()) {
                if (entry.getKey().is(net.minecraft.tags.EnchantmentTags.CURSE)) {
                    curses += entry.getIntValue();
                }
            }
        }
        return curses;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        if (!(target.level() instanceof ServerLevel level)) return;
        ItemStack weapon = player.getMainHandItem();
        ForgingData data = weapon.get(ModDataComponents.FORGING.get());
        TheNextForge.LOGGER.info(
            "[TNF] combat {} hit {} with {} anvil={} embeds={} damage={}",
            player.getName().getString(), target.getType(), weapon.getItem(),
            data == null ? 0 : data.anvilLevel(),
            data == null ? java.util.List.of() : data.embeds(),
            String.format("%.2f", event.getNewDamage())
        );
        // 设计：龙蛋 / 不死图腾 → 命中追加魔法伤害；旋风棒 → 风弹爆炸；虚空 → 结算预扣段落
        if (data != null) {
            ForgingData armourData = dev.anvilcraft.thenextforge.event.EmbedCombat
                .carries(player, "minecraft:dragon_egg")
                    ? ForgingData.EMPTY.withEmbed("minecraft:dragon_egg")
                    : null;
            dev.anvilcraft.thenextforge.event.EmbedCombat.applyMagicDamage(
                target, event.getSource(), event.getNewDamage(), data, armourData);
            dev.anvilcraft.thenextforge.event.EmbedCombat.windBurst(target, data);
            Float voidPending = VOID_PENDING.remove(target);
            if (voidPending != null && voidPending > 0.0F) {
                dev.anvilcraft.thenextforge.event.EmbedCombat.applyVoidDamage(target, voidPending);
            }
        }
        if (data == null || data.embeds().isEmpty()) return;
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(
            level, player, target, weapon, event.getNewDamage()
        );
    }

    private static int count(ForgingData data, String id) {
        return (int) data.embeds().stream().filter(id::equals).count();
    }
}
