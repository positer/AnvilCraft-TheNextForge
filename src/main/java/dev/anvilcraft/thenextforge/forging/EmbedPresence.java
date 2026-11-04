package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 嵌入材料是否「正在生效」的判定。
 *
 * <p>不同对象类别按各自的使用方式判定：工具/武器必须手持，装甲必须穿戴，
 * 护符只按背包携带判定。
 */
public final class EmbedPresence {
    private EmbedPresence() {
    }

    /**
     * 该材料是否因为玩家持有对应类别、且已嵌入该材料的物品而生效。
     */
    public static boolean active(Player player, String material) {
        // 工具/武器：主手或副手
        if (hasEmbed(player.getMainHandItem(), material)) return true;
        if (hasEmbed(player.getOffhandItem(), material)) return true;
        // 装甲：四个装备槽
        for (ItemStack stack : player.getInventory().armor) {
            if (hasEmbed(stack, material)) return true;
        }
        return false;
    }

    /** 装甲槽（已穿戴）里是否有该材料。 */
    public static boolean worn(Player player, String material) {
        for (ItemStack stack : player.getInventory().armor) {
            if (hasEmbed(stack, material)) return true;
        }
        return false;
    }

    /** 手持（主手/副手）是否有该材料。 */
    public static boolean held(Player player, String material) {
        return hasEmbed(player.getMainHandItem(), material)
            || hasEmbed(player.getOffhandItem(), material);
    }

    /** 背包（含护符）里是否有该材料——护符走这条。 */
    public static boolean carried(Player player, String material) {
        for (ItemStack stack : player.getInventory().items) {
            if (hasEmbed(stack, material)) return true;
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (hasEmbed(stack, material)) return true;
        }
        return hasEmbed(player.getOffhandItem(), material);
    }

    private static boolean hasEmbed(ItemStack stack, String material) {
        if (stack == null || stack.isEmpty()) return false;
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        return data != null && data.hasEmbed(material);
    }
}
