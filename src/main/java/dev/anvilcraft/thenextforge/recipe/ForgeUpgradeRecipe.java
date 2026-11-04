package dev.anvilcraft.thenextforge.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

/**
 * 自动锤升级配方。
 *
 * <p>使用原版的 {@code ON_ANVIL_FALL_ON} 触发器，谓词匹配「鱼缸 + 下方砧/砂轮」，
 * 结果 {@link ForgeUpgradeOutcome} 执行一次自动锤结算。这样无需 mixin 就能接管落砧。
 */
public class ForgeUpgradeRecipe extends InWorldRecipe {
    public ForgeUpgradeRecipe(
        ItemStack icon,
        List<IRecipePredicate<?>> conflicting,
        List<IRecipePredicate<?>> nonConflicting,
        List<IRecipeOutcome<?>> outcomes,
        int priority
    ) {
        super(
            icon,
            dev.dubhe.anvilcraft.init.recipe.ModRecipeTriggers.ON_ANVIL_FALL_ON.get(),
            conflicting,
            nonConflicting,
            outcomes,
            priority,
            true,
            // 一次落砧必须只结算一次锤击：世界内配方管理器会把同一次触发重复执行到 maxEfficiency
            // （AnvilLib 默认 64），那会让一次落锤静默变成几十次锤击，等级与看到的锤击数对不上。
            1
        );
    }

    @Override
    public RecipeSerializer<? extends InWorldRecipe> getSerializer() {
        return ModRecipes.FORGE_UPGRADE_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends InWorldRecipe> getType() {
        return ModRecipes.FORGE_UPGRADE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<ForgeUpgradeRecipe> {
        public static final MapCodec<ForgeUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.CODEC.fieldOf("icon").forGetter(InWorldRecipe::icon),
            IRecipePredicate.CODEC.listOf().optionalFieldOf("conflicting", List.of()).forGetter(InWorldRecipe::conflicting),
            IRecipePredicate.CODEC.listOf().optionalFieldOf("non_conflicting", List.of()).forGetter(InWorldRecipe::nonConflicting),
            IRecipeOutcome.CODEC.listOf().optionalFieldOf("outcomes", List.of()).forGetter(InWorldRecipe::outcomes),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(InWorldRecipe::priority)
        ).apply(instance, (icon, conflicting, nonConflicting, outcomes, priority) ->
            new ForgeUpgradeRecipe(icon, conflicting, nonConflicting, outcomes, priority)));

        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeUpgradeRecipe> STREAM_CODEC =
            StreamCodec.of(
                (buf, recipe) -> {},
                buf -> new ForgeUpgradeRecipe(
                    ItemStack.EMPTY, List.of(), List.of(), List.of(ForgeUpgradeOutcome.INSTANCE), 0
                )
            );

        @Override
        public MapCodec<ForgeUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ForgeUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
