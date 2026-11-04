package dev.anvilcraft.thenextforge.gametest;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

/**
 * 26.1.2 的 {@link GameTestInstance}：直接持有一个测试方法引用。
 *
 * <p>原版只剩「数据驱动」测试（函数要在 {@code Registries.TEST_FUNCTION} 里注册），
 * 那对 107 个手写方法太重。本类让 {@link TestRegistry} 在代码里注册方法即可，
 * 与 1.21.1 的 {@code @GameTest} 注解用法等价。
 */
public final class MethodTestInstance extends GameTestInstance {
    /** 代码注册的实例不落盘，codec 只是接口要求。 */
    public static final MapCodec<MethodTestInstance> CODEC = MapCodec.unit(() -> null);

    private final Consumer<GameTestHelper> body;

    public MethodTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info, Consumer<GameTestHelper> body) {
        super(info);
        this.body = body;
    }

    @Override
    public void run(GameTestHelper helper) {
        this.body.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("thenextforge method test");
    }
}
