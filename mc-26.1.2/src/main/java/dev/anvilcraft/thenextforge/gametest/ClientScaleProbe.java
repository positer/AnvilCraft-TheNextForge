package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.forging.ForgingLine;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** 仅开发环境：真实客户端资源加载后检查实际渲染路径，并自动退出本探针客户端。 */
public final class ClientScaleProbe {
    private static boolean done;
    private static int waiting;
    private static boolean opening;
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (done || mc.getOverlay() != null || (mc.screen == null && mc.level == null)) return;
        // 26.1.2 的物品默认组件在连接世界后绑定，标题界面不足以构造有效 ItemStack。
        if (mc.level == null) {
            if (!opening) {
                opening = true;
                String world = "tnf_scale_probe_" + System.currentTimeMillis();
                TheNextForge.LOGGER.info("[TNF-SCALE-PROBE] creating isolated flat world {}", world);
                mc.createWorldOpenFlows().createFreshLevel(world,
                    new net.minecraft.world.level.LevelSettings("TNF Scale Probe", net.minecraft.world.level.GameType.CREATIVE,
                        net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT, true,
                        net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                    net.minecraft.world.level.levelgen.WorldOptions.defaultWithRandomSeed(),
                    net.minecraft.world.level.levelgen.presets.WorldPresets::createFlatWorldDimensions, mc.screen);
                return;
            }
            if (++waiting > 1200) {
                done = true;
                TheNextForge.LOGGER.error("[TNF-SCALE-PROBE] FAIL: world not ready");
                mc.stop();
            }
            return;
        }
        done = true;
        boolean previous = TheNextForge.CONFIG.experimentalModelScaling;
        try {
            run(mc);
        } catch (Throwable failure) {
            TheNextForge.LOGGER.error("[TNF-SCALE-PROBE] FAIL", failure);
        } finally {
            TheNextForge.CONFIG.experimentalModelScaling = previous;
            mc.stop();
        }
    }
    private static void require(boolean ok, String reason) {
        if (!ok) throw new IllegalStateException(reason);
    }
    private static boolean near(double a, double b) { return Math.abs(a-b) < 0.002; }
    private static void run(Minecraft mc) {
        ClientQualityProbe.run(mc);
        int cases = 0;
        for (var item : new net.minecraft.world.item.Item[] {Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE,
                Items.DIAMOND_CHESTPLATE, Items.TRIDENT, Items.SHIELD}) {
            ItemStack plain = new ItemStack(item);
            ItemStack forged = plain.copy();
            forged.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 5));
            for (var context : new ItemDisplayContext[] {ItemDisplayContext.GUI, ItemDisplayContext.GROUND,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    ItemDisplayContext.FIXED, ItemDisplayContext.NONE}) {
                TheNextForge.CONFIG.experimentalModelScaling = false;
                double[] baseline = bounds(mc, plain, context);
                TheNextForge.CONFIG.experimentalModelScaling = true;
                double[] scaled = bounds(mc, forged, context);
                TheNextForge.CONFIG.experimentalModelScaling = false;
                double[] disabled = bounds(mc, forged, context);
                for (int i = 0; i < 6; i++) {
                    require(near(scaled[i], baseline[i]*1.5), "倍率或中心错误 " + item + "/" + context + "/" + i + ": " + scaled[i] + " vs " + baseline[i]);
                    require(near(disabled[i], baseline[i]), "关闭后未恢复 " + item + "/" + context);
                }
                cases++;
            }
        }
        // 复用同一个 render state，必须清掉上一物品的倍率和模型边界缓存。
        var output = new net.minecraft.client.renderer.item.ItemStackRenderState();
        ItemStack forged = new ItemStack(Items.DIAMOND_SWORD);
        forged.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 5));
        TheNextForge.CONFIG.experimentalModelScaling = true;
        mc.getItemModelResolver().updateForTopItem(output, forged, ItemDisplayContext.GUI, null, null, 42);
        output.getModelBoundingBox();
        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
        mc.getItemModelResolver().updateForTopItem(output, plain, ItemDisplayContext.GUI, null, null, 42);
        var b = output.getModelBoundingBox();
        double[] expected = bounds(mc, plain, ItemDisplayContext.GUI);
        require(near(b.minX,expected[0]) && near(b.maxX,expected[3]), "复用 render state 残留缩放");

        TheNextForge.LOGGER.info("[TNF-SCALE-PROBE] PASS cases={} contexts=8 items=5", cases);
    }
    private static double[] bounds(Minecraft mc, ItemStack item, ItemDisplayContext context) {
        var output = new net.minecraft.client.renderer.item.TrackingItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(output, item, context, null, null, 42);
        require(!output.isEmpty(), "没有模型层 " + item + "/" + context);
        var b = output.getModelBoundingBox();
        if (TheNextForge.CONFIG.experimentalModelScaling && item.has(ModDataComponents.FORGING.get()) && context == ItemDisplayContext.GUI) {
            require(output.isOversizedInGui(), "放大 GUI 没有开启边界路径");
            require(output.getModelIdentity().toString().contains("Identity[scale=1.5]"), "GUI 缓存键缺少倍率");
        }
        return new double[] {b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};
    }
}
