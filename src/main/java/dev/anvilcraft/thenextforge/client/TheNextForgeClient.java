package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = TheNextForge.MOD_ID, dist = Dist.CLIENT)
public class TheNextForgeClient {
    public TheNextForgeClient(IEventBus modBus, ModContainer container) {
        if (!net.neoforged.fml.loading.FMLEnvironment.production && Boolean.getBoolean("tnf.scaleProbe")) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                dev.anvilcraft.thenextforge.gametest.ClientScaleProbe::tick);
        }
    }
}
