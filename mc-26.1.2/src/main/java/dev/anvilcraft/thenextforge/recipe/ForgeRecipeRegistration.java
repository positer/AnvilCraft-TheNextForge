package dev.anvilcraft.thenextforge.recipe;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeManagerEvent;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlock;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 把「自动锤升级」注册进原版的 WorldRecipe 列表。
 *
 * <p>用 {@code ON_ANVIL_FALL_ON} 触发器与「落点下方鱼缸、鱼缸下方砧/砂轮」的谓词，
 * 优先级高于鱼缸本体配方，因此不需要 mixin 就能接管落砧。
 */
public final class ForgeRecipeRegistration {
    /**
     * 配方优先级。
     *
     * <p>注意 AnvilLib 的实现与它的文档注释相反：`IPrioritized.compareTo` 返回 `-Integer.compare(...)`，
     * 所以**数值越大越先执行**。AnvilCraft 自己的落砧配方在 201 一带，这里取 1000 才能排到它们前面，
     * 否则鱼缸会先把经验流体做成经验颗粒，我们的自动锤轮不到执行。
     */
    private static final int PRIORITY = 1000;

    private ForgeRecipeRegistration() {
    }

    @SubscribeEvent
    public static void onInit(InWorldRecipeManagerEvent.Init event) {
        var fishTank = BuiltInRegistries.BLOCK.getOptional(
            Identifier.fromNamespaceAndPath("anvilcraft", "fish_tank")
        ).orElse(null);
        if (fishTank == null) return;

        var manager = event.getRecipeManager();
        List<RecipeHolder<InWorldRecipe>> recipes = new ArrayList<>();

        // Registry 本身实现了 HolderGetter，可直接传给 of(HolderGetter, TagKey)
        var tankPredicate = HasBlock.builder().below().of(fishTank).build();
        var anvilBase = HasBlock.builder().below(2).of(net.minecraft.core.registries.BuiltInRegistries.BLOCK, BlockTags.ANVIL).build();
        var grindstoneBase = HasBlock.builder().below(2).of(grindstoneBlocks()).build();

        recipes.add(new RecipeHolder<>(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                Identifier.fromNamespaceAndPath(TheNextForge.MOD_ID, "forge_upgrade")),
            new InWorldRecipe(
                new net.minecraft.world.item.ItemStackTemplate(Items.ANVIL),
                dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers.ON_ANVIL_FALL_ON.get(),
                List.of(),
                List.of(tankPredicate, anvilBase),
                List.of(ForgeUpgradeOutcome.INSTANCE),
                PRIORITY, true, 1
            )
        ));
        recipes.add(new RecipeHolder<>(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                Identifier.fromNamespaceAndPath(TheNextForge.MOD_ID, "forge_upgrade_grindstone")),
            new InWorldRecipe(
                new net.minecraft.world.item.ItemStackTemplate(Items.GRINDSTONE),
                dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers.ON_ANVIL_FALL_ON.get(),
                List.of(),
                List.of(tankPredicate, grindstoneBase),
                List.of(ForgeUpgradeOutcome.INSTANCE),
                PRIORITY, true, 1
            )
        ));
        manager.anvillib$addRecipes(recipes);
        TheNextForge.LOGGER.info("[TNF] registered {} forge upgrade recipe(s)", recipes.size());
    }

    /**
     * 所有算「砂轮基底」的方块：原版砂轮 + 铁砧工艺的各材质砂轮（`anvilcraft:*_grindstone`）。
     */
    public static List<net.minecraft.world.level.block.Block> grindstoneBlocks() {
        List<net.minecraft.world.level.block.Block> blocks = new ArrayList<>();
        blocks.add(net.minecraft.world.level.block.Blocks.GRINDSTONE);
        for (net.minecraft.world.level.block.Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if ("anvilcraft".equals(id.getNamespace()) && id.getPath().endsWith("_grindstone")) {
                blocks.add(block);
            }
        }
        return List.copyOf(blocks);
    }
}
