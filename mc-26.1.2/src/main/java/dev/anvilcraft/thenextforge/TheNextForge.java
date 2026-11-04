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

    /** 模组配置（含实验性开关）。 */
    public static final dev.anvilcraft.thenextforge.AddonConfig CONFIG =
        dev.anvilcraft.lib.v2.config.ConfigManager.register(MOD_ID, AddonConfig::new);

    public TheNextForge(IEventBus modBus, ModContainer container) {
        LOGGER.info("[TNF] loaded on 26.1.2");
        // 数据组件 / 附着 / 配方结果类型都要注册到 mod bus，否则测试里访问就是 unbound
        dev.anvilcraft.thenextforge.init.ModDataComponents.register(modBus);
        dev.anvilcraft.thenextforge.init.ModAttachments.register(modBus);
        dev.anvilcraft.thenextforge.recipe.ModRecipeOutcomes.register(modBus);
        // RegisterGameTestsEvent 是 mod bus 事件：显式挂上，否则测试一条都注册不进去
        modBus.addListener(dev.anvilcraft.thenextforge.gametest.TestRegistry::onRegisterTests);
        // 配置页语言（Registrum LANG provider）
        dev.anvilcraft.thenextforge.data.AddonDatagen.init();
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
