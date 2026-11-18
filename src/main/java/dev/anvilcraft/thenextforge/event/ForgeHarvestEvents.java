package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 经验与挖掘相关的嵌入材料效果。
 */
public final class ForgeHarvestEvents {
    private ForgeHarvestEvents() {
    }

    /** 青金石：拾取经验时 +20%；绿宝石：拾取经验时按经验修补的兑换率修复嵌入物品。 */
    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // 设计：获得经验 +20%（装甲/护符）——按「携带」判定，手持也算
        if (dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "minecraft:lapis_lazuli")) {
            int bonus = (int) Math.round(event.getOrb().getValue() * 0.2);
            if (bonus > 0) player.giveExperiencePoints(bonus);
        }
        repairEmbeddedFromXp(player, event.getOrb().getValue());
    }

    /**
     * 绿宝石「独立经验修补」：与经验修补附魔同兑换率——**1 点经验修复 2 点耐久**，
     * 剩余经验按未修复比例继续作用于下一件，经验本身照常进入经验条（与原版一致）。
     *
     * @return 本次一共修复的耐久点
     */
    public static int repairEmbeddedFromXp(net.minecraft.world.entity.player.Player player, int xp) {
        int remaining = xp;
        int repairedTotal = 0;
        for (ItemStack stack : equipped(player)) {
            if (remaining <= 0) break;
            if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) continue;
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data == null || !data.hasActiveEmbed("minecraft:emerald")) continue;
            XpRepair repair = repairForXp(remaining, stack.getDamageValue());
            if (repair.repaired() <= 0) continue;
            stack.setDamageValue(stack.getDamageValue() - repair.repaired());
            repairedTotal += repair.repaired();
            remaining = repair.leftoverXp();
        }
        return repairedTotal;
    }

    /** 手持与穿着槽位（工具/武器/装甲）。 */
    private static java.util.List<ItemStack> equipped(net.minecraft.world.entity.player.Player player) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.add(player.getMainHandItem());
        stacks.add(player.getOffhandItem());
        for (ItemStack armour : player.getInventory().armor) stacks.add(armour);
        return stacks;
    }

    /** 一次经验修补的结果：修复了多少耐久、还剩多少经验可用。 */
    public record XpRepair(int repaired, int leftoverXp) {
    }

    /**
     * 按原版经验修补的算法换算：可用耐久 = 经验 × 2，修不完的部分按比例退回经验。
     *
     * <p>原版 {@code ExperienceOrb#repairPlayerItems}：{@code i = xp*2}、{@code j = min(i, damage)}、
     * {@code k = xp - j*xp/i}，这里逐字复刻。
     */
    public static XpRepair repairForXp(int xp, int damage) {
        if (xp <= 0 || damage <= 0) return new XpRepair(0, Math.max(0, xp));
        int budget = xp * 2;
        int repaired = Math.min(budget, damage);
        int leftover = xp - repaired * xp / budget;
        return new XpRepair(repaired, Math.max(0, leftover));
    }

    /** 青金石：手持带嵌入的武器/工具击杀时，掉落经验 +20%。 */
    @SubscribeEvent
    public static void onExperienceDrop(net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack weapon = player.getMainHandItem();
        ForgingData data = weapon.get(ModDataComponents.FORGING.get());
        if (data == null || !data.hasActiveEmbed("minecraft:lapis_lazuli")) return;
        event.setDroppedExperience(xpBonus(event.getDroppedExperience()));
    }

    /** 青金石的击杀经验加成：+20%。 */
    public static int xpBonus(int dropped) {
        return dropped <= 0 ? dropped : dropped + (int) Math.round(dropped * 0.2);
    }

    /** 青金石 · 装甲/护符 的「获得经验 +20%」：玩家任何来源的经验变化，穿着/携带即可。 */
    @SubscribeEvent
    public static void onXpChange(net.neoforged.neoforge.event.entity.player.PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAmount() <= 0) return;
        if (!carries(player, "minecraft:lapis_lazuli")) return;
        event.setAmount(xpBonus(event.getAmount()));
    }

    /** 黄玉：玩家被雷劈时，携带的黄玉嵌入物品恢复 10 点耐久。 */
    @SubscribeEvent
    public static void onLightning(net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int repaired = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (repairIfTopaz(stack)) repaired++;
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (repairIfTopaz(stack)) repaired++;
        }
        if (repairIfTopaz(player.getOffhandItem())) repaired++;
        if (repaired > 0) {
            TheNextForge.LOGGER.info("[TNF] topaz repaired {} items after lightning", repaired);
        }
    }

    public static boolean repairIfTopaz(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        if (data == null || !data.hasActiveEmbed("anvilcraft:topaz")) return false;
        if (stack.getDamageValue() <= 0) return false;
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - 10));
        return true;
    }

    /** 浮霜：挖掘方块时把掉落转化为经验（简化：每方块 1 点）。 */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack tool = player.getMainHandItem();
        ForgingData data = tool.get(ModDataComponents.FORGING.get());
        if (data == null) return;
        if (data.hasActiveEmbed("anvilcraft:frost_metal_ingot")) {
            player.giveExperiencePoints(1);
        }
    }

    private static boolean carries(ServerPlayer player, String material) {
        for (ItemStack stack : player.getInventory().items) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && data.hasActiveEmbed(material)) return true;
        }
        for (ItemStack stack : player.getInventory().armor) {
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && data.hasActiveEmbed(material)) return true;
        }
        ForgingData offhand = player.getOffhandItem().get(ModDataComponents.FORGING.get());
        return offhand != null && offhand.hasActiveEmbed(material);
    }
}
