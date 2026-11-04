package dev.anvilcraft.thenextforge;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.thenextforge.data.AddonDatagen;
import dev.anvilcraft.thenextforge.init.AddonBlocks;
import dev.anvilcraft.thenextforge.init.AddonItemGroups;
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
import dev.anvilcraft.thenextforge.init.AddonItems;
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

    public TheNextForge(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.register(modEventBus);
        dev.anvilcraft.thenextforge.forging.ForgeRulesData.init();
        dev.anvilcraft.thenextforge.forging.EmbedData.init();
        ModAttachments.register(modEventBus);
        dev.anvilcraft.thenextforge.recipe.ModRecipes.register(modEventBus);
        dev.anvilcraft.thenextforge.recipe.ModRecipeOutcomes.register(modEventBus);

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(AmuletAttributes.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(dev.anvilcraft.thenextforge.recipe.ForgeRecipeRegistration.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeCombatEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(dev.anvilcraft.thenextforge.event.ForgeCraftEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeDefenceEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeDropEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeHarvestEvents.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeItemTicker.class);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ForgeMiningEvents.class);
        AddonItemGroups.register(modEventBus);
        AddonBlocks.register();
        AddonItems.register();
        AddonDatagen.init();
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
