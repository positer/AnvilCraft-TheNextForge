package dev.anvilcraft.thenextforge;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.thenextforge.data.AddonDatagen;
import dev.anvilcraft.thenextforge.event.AmuletAttributes;
import dev.anvilcraft.thenextforge.event.ForgeCombatEvents;
import dev.anvilcraft.thenextforge.event.ForgeDefenceEvents;
import dev.anvilcraft.thenextforge.event.ForgeDropEvents;
import dev.anvilcraft.thenextforge.event.ForgeEvents;
import dev.anvilcraft.thenextforge.event.ForgeHarvestEvents;
import dev.anvilcraft.thenextforge.event.ForgeItemTicker;
import dev.anvilcraft.thenextforge.event.ForgeMiningEvents;
import dev.anvilcraft.thenextforge.init.ModAttachments;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TheNextForge.MOD_ID)
public class TheNextForge {
    public static final String MOD_ID = "thenextforge";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final AddonConfig CONFIG = ConfigManager.register(TheNextForge.MOD_ID, AddonConfig::new);
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    /**
     * 显式挂到游戏总线（{@code NeoForge.EVENT_BUS}）的类。
     *
     * <p>构造器按这张表注册，{@code WiringAuditTests} 按同一张表审计：
     * 任何声明了 {@code @SubscribeEvent} 方法的类，要么带 {@code @EventBusSubscriber}，要么必须在这张表里。
     * 新增事件类忘了登记就是构建期红灯，而不是等玩家发现玩法是死的。
     */
    public static final java.util.List<Class<?>> GAME_BUS_LISTENERS = java.util.List.of(
        AmuletAttributes.class,
        dev.anvilcraft.thenextforge.recipe.ForgeRecipeRegistration.class,
        ForgeCombatEvents.class,
        dev.anvilcraft.thenextforge.event.ForgeCraftEvents.class,
        ForgeDefenceEvents.class,
        ForgeDropEvents.class,
        ForgeEvents.class,
        ForgeHarvestEvents.class,
        ForgeItemTicker.class,
        ForgeMiningEvents.class
    );

    public TheNextForge(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        modEventBus.addListener(dev.anvilcraft.thenextforge.api.TnfDataMaps::register);
        dev.anvilcraft.thenextforge.forging.ForgeRulesData.init();
        dev.anvilcraft.thenextforge.forging.EmbedData.init();
        ModAttachments.register(modEventBus);
        dev.anvilcraft.thenextforge.recipe.ModRecipes.register(modEventBus);
        dev.anvilcraft.thenextforge.recipe.ModRecipeOutcomes.register(modEventBus);

        // 统一按 GAME_BUS_LISTENERS 注册：同一张表被 WiringAuditTests 审计，
        // 避免「接线写在构造器里、测试却看不见」再次发生（26.1.2 移植就是这么丢掉 11 个类的）。
        GAME_BUS_LISTENERS.forEach(net.neoforged.neoforge.common.NeoForge.EVENT_BUS::register);
        AddonDatagen.init();
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
