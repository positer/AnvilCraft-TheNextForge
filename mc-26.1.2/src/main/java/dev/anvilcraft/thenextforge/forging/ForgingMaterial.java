package dev.anvilcraft.thenextforge.forging;

import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.ember.EmberAnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.frost.FrostAnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.royal.RoyalAnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.trascendence.TranscendenceAnvilHammerItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
        return BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath("anvilcraft", path)).orElse(null);
    }

    /**
     * 判定铁砧方块的材质，非铁砧返回 null。
     */
    public static ForgingMaterial ofAnvilBlock(BlockState state) {
        var mapped = dev.anvilcraft.thenextforge.api.TnfApi.base(state);
        if (mapped != null) return mapped.enabled() && mapped.line() == ForgingLine.ANVIL ? mapped.material() : null;
        return ofBlock(state, "_anvil", Blocks.ANVIL);
    }

    /**
     * 判定砂轮方块的材质，非砂轮返回 null。
     */
    public static ForgingMaterial ofGrindstoneBlock(BlockState state) {
        var mapped = dev.anvilcraft.thenextforge.api.TnfApi.base(state);
        if (mapped != null) return mapped.enabled() && mapped.line() == ForgingLine.GRINDSTONE ? mapped.material() : null;
        return ofBlock(state, "_grindstone", Blocks.GRINDSTONE);
    }

    private static ForgingMaterial ofBlock(BlockState state, String suffix, Block vanilla) {
        Block block = state.getBlock();
        if (block == vanilla
            || block == Blocks.CHIPPED_ANVIL
            || block == Blocks.DAMAGED_ANVIL) {
            return BASE;
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
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
     * 判定**手持**铁砧锤的材质，非铁砧锤返回 null。
     *
     * <p>只认铁砧锤的物品形态。铁砧物品本身**不能**手持当锤——那是自动化路径的行为：
     * 落下的铁砧由 {@link #ofAnvilBlock} 判定并直接作用于台面，不经过这里。
     */
    public static ForgingMaterial ofHammer(ItemStack stack) {
        var mapped = dev.anvilcraft.thenextforge.api.TnfApi.hammer(stack);
        if (mapped != null) return mapped;
        if (stack.getItem() instanceof TranscendenceAnvilHammerItem) return TRANSCENDENCE;
        if (stack.getItem() instanceof RoyalAnvilHammerItem) return ROYAL;
        if (stack.getItem() instanceof FrostAnvilHammerItem) return FROST;
        if (stack.getItem() instanceof EmberAnvilHammerItem) return EMBER;
        if (stack.getItem() instanceof AnvilHammerItem) return BASE;
        return null;
    }

    /**
     * 该材质是否不摧毁目标。
     */
    public boolean isSafe() {
        return this == TRANSCENDENCE;
    }
}
