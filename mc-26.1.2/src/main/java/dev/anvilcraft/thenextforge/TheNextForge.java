package dev.anvilcraft.thenextforge;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AnvilCraft: TheNextForge —— 26.1.2 移植骨架。
 *
 * <p>先验证「能编译 + 能跑 GameTest」，再逐模块从 1.21.1 分支搬入。
 * 注意 26.1.2 把 {@code ResourceLocation} 改名为 {@link Identifier}。
 */
@Mod(TheNextForge.MOD_ID)
public class TheNextForge {
    public static final String MOD_ID = "thenextforge";
    public static final Logger LOGGER = LoggerFactory.getLogger("TheNextForge");
    /** AnvilLib 的数据生成注册表（用于配置页语言）。 */
    public static final dev.anvilcraft.lib.v2.registrum.Registrum REGISTRUM =
        dev.anvilcraft.lib.v2.registrum.Registrum.create(MOD_ID);

    /**
     * 显式挂到游戏总线（NeoForge.EVENT_BUS）的类。
     *
     * <p>本代全部走 {@code @EventBusSubscriber} 注解接线，因此这里是空的；
     * 保留这个常量是因为 {@code WiringAuditTests} 用它判断「某个类是否被显式注册」，两代同一把尺子：
     * 任何声明了 {@code @SubscribeEvent} 方法的类，要么带注解，要么必须出现在这个列表里。
     */
    public static final java.util.List<Class<?>> GAME_BUS_LISTENERS = java.util.List.of();

    /** 模组配置（含实验性开关）。 */
    public static final dev.anvilcraft.thenextforge.AddonConfig CONFIG =
        dev.anvilcraft.lib.v2.config.ConfigManager.register(MOD_ID, AddonConfig::new);

    public TheNextForge(IEventBus modBus, ModContainer container) {
        LOGGER.info("[TNF] loaded on 26.1.2");
        // 数据组件 / 附着 / 配方结果类型都要注册到 mod bus，否则测试里访问就是 unbound
        dev.anvilcraft.thenextforge.init.ModDataComponents.register(modBus);
        modBus.addListener(dev.anvilcraft.thenextforge.api.TnfDataMaps::register);
        dev.anvilcraft.thenextforge.init.ModAttachments.register(modBus);
        dev.anvilcraft.thenextforge.recipe.ModRecipeOutcomes.register(modBus);
        // 配置文件驱动的规则表与材料表：1.21.1 在构造器里加载，26.1.2 移植时漏了这两行，
        // 结果是 config/thenextforge/*.json 既不生成也不读取（改配置毫无效果）。
        dev.anvilcraft.thenextforge.forging.ForgeRulesData.init();
        dev.anvilcraft.thenextforge.forging.EmbedData.init();
        // RegisterGameTestsEvent 是 mod bus 事件：显式挂上，否则测试一条都注册不进去。
        // 但 gametest 包被 build.gradle 的 jar.exclude 排除，发行 jar 里没有这些类：
        // 无条件引用会让正式客户端在构造期抛 NoClassDefFoundError 直接启动失败（2026-11-04 实测崩溃）。
        // 所以只在开发环境（runClient / runGameTestServer）挂载，正式环境跳过。
        if (!net.neoforged.fml.loading.FMLEnvironment.isProduction()) {
            modBus.addListener(dev.anvilcraft.thenextforge.gametest.TestRegistry::onRegisterTests);
        }
        // 配置页语言（Registrum LANG provider）
        dev.anvilcraft.thenextforge.data.AddonDatagen.init();
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
