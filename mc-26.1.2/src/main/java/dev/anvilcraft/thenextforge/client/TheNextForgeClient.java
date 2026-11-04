package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = TheNextForge.MOD_ID, dist = Dist.CLIENT)
public class TheNextForgeClient {
    public TheNextForgeClient(IEventBus modBus, ModContainer container) {
        // 自建渲染管线必须在 mod bus 上注册（26.1.2 的要求）
        modBus.addListener(GlowHighlight::registerPipelines);
    }
}
