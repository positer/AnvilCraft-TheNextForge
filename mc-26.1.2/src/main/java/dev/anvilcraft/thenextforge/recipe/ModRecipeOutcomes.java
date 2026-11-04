package dev.anvilcraft.thenextforge.recipe;

import dev.anvilcraft.lib.v2.recipe.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的配方结果类型。
 */
public final class ModRecipeOutcomes {
    public static final DeferredRegister<IRecipeOutcome.Type<?>> OUTCOME_TYPES =
        DeferredRegister.create(LibRegistries.OUTCOME_TYPE_REGISTRY, TheNextForge.MOD_ID);

    public static final DeferredHolder<IRecipeOutcome.Type<?>, ForgeUpgradeOutcome.Type> FORGE_UPGRADE =
        OUTCOME_TYPES.register("forge_upgrade", ForgeUpgradeOutcome.Type::new);

    private ModRecipeOutcomes() {
    }

    public static void register(IEventBus modEventBus) {
        OUTCOME_TYPES.register(modEventBus);
    }
}
