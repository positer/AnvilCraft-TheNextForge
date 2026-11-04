package dev.anvilcraft.thenextforge.recipe;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的配方类型与序列化器。
 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, TheNextForge.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TheNextForge.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ForgeUpgradeRecipe>> FORGE_UPGRADE_TYPE =
        RECIPE_TYPES.register("forge_upgrade", () -> new RecipeType<>() {
            @Override
            public String toString() {
                return TheNextForge.MOD_ID + ":forge_upgrade";
            }
        });

    public static final DeferredHolder<RecipeSerializer<?>, ForgeUpgradeRecipe.Serializer> FORGE_UPGRADE_SERIALIZER =
        RECIPE_SERIALIZERS.register("forge_upgrade", ForgeUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
