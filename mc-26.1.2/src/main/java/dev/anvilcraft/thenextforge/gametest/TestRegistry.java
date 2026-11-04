package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

/**
 * 把测试方法注册进 26.1.2 的 GameTest 注册表。
 *
 * <p>每次 {@code register} 的默认参数与 1.21.1 的 {@code @GameTest(template = "empty")} 对齐：
 * 结构 {@code minecraft:empty}、1 个 tick 上限 100、required。
 */
public final class TestRegistry {
    private TestRegistry() {
    }

    @SubscribeEvent
    public static void onRegisterTests(RegisterGameTestsEvent event) {
        TheNextForge.LOGGER.info("[TNF] registering game tests");
        PortSmokeTests.registerAll(event);
    }

    /** 注册一条测试。 */
    private static Holder<TestEnvironmentDefinition<?>> ENVIRONMENT;

    public static void register(RegisterGameTestsEvent event, String name, Consumer<GameTestHelper> body) {
        // 空环境：不额外设游戏规则/天气，等价于 1.21.1 的默认 GameTest 环境
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(TheNextForge.MOD_ID, name + "_env"),
            new net.minecraft.gametest.framework.TestEnvironmentDefinition.AllOf(java.util.List.of())
        );
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("empty"),
            100,
            0,
            true,
            Rotation.NONE,
            false,
            1,
            1,
            false,
            0
        );
        event.registerTest(
            Identifier.fromNamespaceAndPath(TheNextForge.MOD_ID, name),
            testData -> new MethodTestInstance(testData, body),
            data
        );
    }
}
