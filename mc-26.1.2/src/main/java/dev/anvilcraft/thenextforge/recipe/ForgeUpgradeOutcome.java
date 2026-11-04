package dev.anvilcraft.thenextforge.recipe;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.event.AutomationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * 自动锤升级配方结果。
 *
 * <p>配方匹配到「鱼缸 + 下方砧/砂轮」后，由本结果执行一次自动锤结算。
 */
public record ForgeUpgradeOutcome() implements IRecipeOutcome<ForgeUpgradeOutcome> {
    public static final ForgeUpgradeOutcome INSTANCE = new ForgeUpgradeOutcome();

    @Override
    public Type getType() {
        return ModRecipeOutcomes.FORGE_UPGRADE.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        var level = context.getLevel();
        BlockPos landing = BlockPos.containing(context.getPos());
        BlockPos tankPos = landing.below();
        boolean handled = AutomationHandler.applyAutomatedHit(level, tankPos, level.getBlockState(landing));
        if (handled) {
            TheNextForge.LOGGER.info("[TNF] forge upgrade recipe handled at {}", tankPos);
        }
    }

    public static class Type implements IRecipeOutcome.Type<ForgeUpgradeOutcome> {
        public static final MapCodec<ForgeUpgradeOutcome> CODEC = MapCodec.unit(ForgeUpgradeOutcome::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, ForgeUpgradeOutcome> STREAM_CODEC =
            StreamCodec.unit(new ForgeUpgradeOutcome());

        @Override
        public MapCodec<ForgeUpgradeOutcome> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ForgeUpgradeOutcome> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
