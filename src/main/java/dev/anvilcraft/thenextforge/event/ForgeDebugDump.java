package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 调试导出：定期把在线玩家手持物品的锻造数据写入日志。
 *
 * <p>MCP 状态接口只给物品 id，读不到组件；把等级、嵌入与最终属性值打进日志，
 * 让实机测试能从日志核对数值。
 */
public final class ForgeDebugDump {
    private static final boolean ENABLED = java.nio.file.Files.exists(java.nio.file.Path.of(".dumpforge"));

    private ForgeDebugDump() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        var server = event.getServer();
        if (server.getTickCount() % 100 != 0) return;
        // 伙伴可能是自定义实体，所以遍历所有活体而不是只遍历 ServerPlayer
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) continue;
                var stack = living.getMainHandItem();
                var data = stack.get(ModDataComponents.FORGING.get());
                if (data == null) continue;
                TheNextForge.LOGGER.info(
                    "[TNF] dump {} held={} anvil={} grindstone={} embeds={}",
                    entity.getName().getString(), stack.getItem().toString(),
                    data.anvilLevel(), data.grindstoneLevel(), data.embeds()
                );
            }
        }
    }
}
