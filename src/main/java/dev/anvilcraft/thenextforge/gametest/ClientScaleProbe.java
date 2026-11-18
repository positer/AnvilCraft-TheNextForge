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
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (done || mc.getOverlay() != null || mc.screen == null) return;
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
        // 旧展示倍率 1.5 × 当前渲染修正 1/1.5 × 目标倍率 1.5，应仍为 1.5。
        ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
        item.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 5));
        var transform = new com.mojang.math.Transformation(new org.joml.Vector3f(0,0.12F,0), new org.joml.Quaternionf(),
            new org.joml.Vector3f(1.5F), new org.joml.Quaternionf().rotateX((float)Math.PI/2));
        float correction = dev.anvilcraft.thenextforge.forging.ModelScaling.displayCorrection(transform);
        require(near(correction*1.5,1), "旧展示变换未抵消");
        TheNextForge.CONFIG.experimentalModelScaling = true;
        var previous = dev.anvilcraft.thenextforge.client.DisplayScaleContext.enter(item, correction);
        try { require(near(dev.anvilcraft.thenextforge.client.DisplayScaleContext.scaleFor(item)*1.5,1.5), "展示重复放大"); }
        finally { dev.anvilcraft.thenextforge.client.DisplayScaleContext.restore(previous); }

        TheNextForge.LOGGER.info("[TNF-SCALE-PROBE] PASS cases={} contexts=8 items=5", cases);
    }
    private static final class Bounds implements com.mojang.blaze3d.vertex.VertexConsumer {
        final double[] box = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        int vertices;
        public Bounds addVertex(float x, float y, float z) {
            vertices++;
            float[] p = {x,y,z};
            for(int i=0;i<3;i++){ box[i]=Math.min(box[i],p[i]); box[i+3]=Math.max(box[i+3],p[i]); }
            return this;
        }
        public Bounds setColor(int r,int g,int b,int a){return this;}
        public Bounds setUv(float u,float v){return this;}
        public Bounds setUv1(int u,int v){return this;}
        public Bounds setUv2(int u,int v){return this;}
        public Bounds setNormal(float x,float y,float z){return this;}
    }
    private static double[] bounds(Minecraft mc, ItemStack item, ItemDisplayContext context) {
        Bounds bounds = new Bounds();
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        mc.getItemRenderer().renderStatic(item, context, 15728880, 0, pose, type -> bounds, null, 42);
        require(bounds.vertices > 0, "没有实际模型顶点 " + item + "/" + context);
        require(pose.last().pose().equals(new org.joml.Matrix4f()), "矩阵栈未恢复");
        return bounds.box;
    }
}
