package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 26.1.2 移植的冒烟测试：先证明测试框架本身在这套环境里能跑。 */
public final class PortSmokeTests {
    private PortSmokeTests() {
    }

    public static void registerAll(RegisterGameTestsEvent registerEvent) {
        ForgingGameTests.registerAll(registerEvent);
        PortAdapterTests.registerAll(registerEvent);
        TestRegistry.register(registerEvent, "mod_loads_and_tests_run", helper -> {
            helper.assertTrue(TheNextForge.MOD_ID.equals("thenextforge"), "mod id 应保持为 thenextforge");
            helper.succeed();
        });
    }
}
