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
        DeferredRegister.createDataComponents(TheNextForge.MOD_ID);

    public static final Supplier<DataComponentType<ForgingData>> FORGING = REGISTER.registerComponentType(
        "forging",
        builder -> builder
            .persistent(ForgingData.CODEC)
            .networkSynchronized(ForgingData.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<dev.anvilcraft.thenextforge.forging.ForgeDurability>> DURABILITY_HISTORY = REGISTER.registerComponentType(
        "durability_history", builder -> builder
            .persistent(dev.anvilcraft.thenextforge.forging.ForgeDurability.CODEC)
            .networkSynchronized(dev.anvilcraft.thenextforge.forging.ForgeDurability.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<net.minecraft.world.item.component.Tool>> PRE_EMBED_TOOL = REGISTER.registerComponentType(
        "pre_embed_tool", builder -> builder
            .persistent(net.minecraft.world.item.component.Tool.CODEC)
            .networkSynchronized(net.minecraft.world.item.component.Tool.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<net.minecraft.network.chat.Component>> AFFIX_BASE_NAME = REGISTER.registerComponentType(
        "affix_base_name", builder -> builder
            .persistent(net.minecraft.network.chat.ComponentSerialization.CODEC)
            .networkSynchronized(net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC)
    );

    public static final Supplier<DataComponentType<Integer>> ORIGINAL_GLINT = REGISTER.registerComponentType(
        "original_glint", builder -> builder.persistent(com.mojang.serialization.Codec.INT)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
    );

    public static final Supplier<DataComponentType<java.util.List<String>>> EMBED_JOURNAL = REGISTER.registerComponentType(
        "embed_journal", builder -> builder.persistent(com.mojang.serialization.Codec.STRING.listOf())
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list()))
    );

    public static final Supplier<DataComponentType<Boolean>> DURABILITY_MANAGED = REGISTER.registerComponentType(
        "durability_managed", builder -> builder.persistent(com.mojang.serialization.Codec.BOOL)
            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL)
    );

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
