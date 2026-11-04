package dev.anvilcraft.thenextforge.forging;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.EmberAnvilHammerItem;
import dev.dubhe.anvilcraft.item.FrostAnvilHammerItem;
import dev.dubhe.anvilcraft.item.RoyalAnvilHammerItem;
import dev.dubhe.anvilcraft.item.TranscendenceAnvilHammerItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 锻造台与铁砧锤的材质档位。
 *
 * <p>五档材质各自独立参与判定：基底决定失败后果，锤提供成功率之外的额外效果。
 */
public enum ForgingMaterial {
    BASE,
    ROYAL,
    FROST,
    EMBER,
    TRANSCENDENCE;

    private static Block anvilcraft(String path) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", path)).orElse(null);
    }

    /**
     * 判定铁砧方块的材质，非铁砧返回 null。
     */
    public static ForgingMaterial ofAnvilBlock(BlockState state) {
        return ofBlock(state, "_anvil", Blocks.ANVIL);
    }

    /**
     * 判定砂轮方块的材质，非砂轮返回 null。
     */
    public static ForgingMaterial ofGrindstoneBlock(BlockState state) {
        return ofBlock(state, "_grindstone", Blocks.GRINDSTONE);
    }

    private static ForgingMaterial ofBlock(BlockState state, String suffix, Block vanilla) {
        Block block = state.getBlock();
        if (block == vanilla
            || block == Blocks.CHIPPED_ANVIL
            || block == Blocks.DAMAGED_ANVIL) {
            return BASE;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (!"anvilcraft".equals(id.getNamespace())) return null;
        String path = id.getPath();
        // 幻灵铁砧的落砧配方等同于普通锤
        if ("spectral_anvil".equals(path)) return BASE;
        if (!path.endsWith(suffix)) return null;
        return switch (path.substring(0, path.length() - suffix.length())) {
            case "royal" -> ROYAL;
            case "frost" -> FROST;
            case "ember" -> EMBER;
            case "transcendence" -> TRANSCENDENCE;
            default -> BASE;
        };
    }

    /**
     * 判定铁砧锤的材质，非铁砧锤返回 null。
     */
    public static ForgingMaterial ofHammer(ItemStack stack) {

        if (stack.getItem() instanceof TranscendenceAnvilHammerItem) return TRANSCENDENCE;
        if (stack.getItem() instanceof RoyalAnvilHammerItem) return ROYAL;
        if (stack.getItem() instanceof FrostAnvilHammerItem) return FROST;
        if (stack.getItem() instanceof EmberAnvilHammerItem) return EMBER;
        if (stack.getItem() instanceof AnvilHammerItem) return BASE;
        // 铁砧物品本身也可以当锤用
        if (stack.is(net.minecraft.world.item.Items.ANVIL)
            || stack.is(net.minecraft.world.item.Items.CHIPPED_ANVIL)
            || stack.is(net.minecraft.world.item.Items.DAMAGED_ANVIL)) {
            return BASE;
        }
        return null;
    }

    /**
     * 该材质是否不摧毁目标。
     */
    public boolean isSafe() {
        return this == TRANSCENDENCE;
    }
}
