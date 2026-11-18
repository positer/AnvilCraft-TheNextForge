package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;

import java.lang.annotation.ElementType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 接线审计：声明了 {@code @SubscribeEvent} 方法的类必须真的被订阅。
 *
 * <p>NeoForge 只有在类带 {@code @EventBusSubscriber} 或被显式 {@code register} 时才会调用这些方法。
 * 两类都没做的类，编译、启动、其余测试全都正常，只有玩家进游戏才发现整片玩法是死的——
 * 26.1.2 移植就是这样丢掉 11 个类的接线。这条测试直接用 FML 自己的扫描结果
 * （{@code ModFileScanData}，正是自动订阅所用的同一份数据）把它变成构建期红灯。
 */
@GameTestHolder(TheNextForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WiringAuditTests {
    /**
     * 已知「声明了订阅方法但不接线」的开发期类：必须逐个登记并写清原因。
     *
     * <p>新增条目等于给自己开一次性口子，应当谨慎。
     *
     * <p>本轮审计在 1.21.1 上扫出这两个类：它们的方法从写下来那天起就没被调用过，
     * 且都不进发行包（build.gradle 的 jar.exclude 排除）。这里只登记、不擅自接线——
     * 要接线就让它们进 {@link TheNextForge#GAME_BUS_LISTENERS}，并把对应条目从这里删掉。
     */
    private static final Set<String> KNOWN_UNWIRED_DEV_TOOLS = Set.of(
        "dev.anvilcraft.thenextforge.event.AutomationTestPump",
        "dev.anvilcraft.thenextforge.event.ForgeDebugDump"
    );

    private WiringAuditTests() {
    }

    @GameTest(template = "empty")
    public static void everySubscribeEventClassIsWired(GameTestHelper helper) {
        var modFile = ModList.get().getModFileById(TheNextForge.MOD_ID);
        helper.assertTrue(modFile != null, "ModList 里找不到 " + TheNextForge.MOD_ID);
        if (modFile == null) return;
        var scan = modFile.getFile().getScanResult();

        // 类级 @EventBusSubscriber：FML 据此自动注册
        Set<String> annotated = scan
            .getAnnotatedBy(EventBusSubscriber.class, ElementType.TYPE)
            .map(data -> data.clazz().getClassName())
            .collect(Collectors.toSet());
        // 显式注册清单：构造器按同一张表注册
        Set<String> listed = TheNextForge.GAME_BUS_LISTENERS.stream()
            .map(Class::getName)
            .collect(Collectors.toSet());
        // 声明了订阅方法的类（同一个类有多个方法时只算一次）
        List<String> subscribing = scan
            .getAnnotatedBy(SubscribeEvent.class, ElementType.METHOD)
            .map(data -> data.clazz().getClassName())
            .distinct()
            .sorted()
            .toList();

        List<String> offenders = subscribing.stream()
            .filter(name -> !annotated.contains(name))
            .filter(name -> !listed.contains(name))
            .filter(name -> !KNOWN_UNWIRED_DEV_TOOLS.contains(name))
            .toList();

        helper.assertTrue(!subscribing.isEmpty(),
            "扫描结果里一个 @SubscribeEvent 方法都没有，审计自身失效（扫描数据没拿到？）");
        helper.assertTrue(offenders.isEmpty(),
            "以下类声明了 @SubscribeEvent 但没有任何订阅者（加 @EventBusSubscriber，或写进 GAME_BUS_LISTENERS）："
                + offenders);
        TheNextForge.LOGGER.info(
            "[TNF] wiring audit: {} classes with @SubscribeEvent ({} by annotation, {} by explicit list), 0 offenders",
            subscribing.size(), annotated.size(), listed.size());
        helper.succeed();
    }
}
