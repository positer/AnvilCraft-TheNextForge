package dev.anvilcraft.thenextforge.init;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 本模组注册的数据组件。
 */
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
        DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, TheNextForge.MOD_ID);

    public static final Supplier<DataComponentType<ForgingData>> FORGING = REGISTER.registerComponentType(
        "forging",
        builder -> builder
            .persistent(ForgingData.CODEC)
            .networkSynchronized(ForgingData.STREAM_CODEC)
    );

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
