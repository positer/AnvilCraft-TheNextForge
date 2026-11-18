package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.client.*;
import dev.anvilcraft.thenextforge.forging.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.*;

/** 开发环境验证：真实资源、模型解析及流光通道；不进入发行 JAR。 */
public final class ClientQualityProbe {
    private static void require(boolean ok, String reason) { if (!ok) throw new IllegalStateException(reason); }

    public static void run(Minecraft mc) {
        int cases = 0;
        int[] levels = {0, 4, 7, 10};
        boolean old = TheNextForge.CONFIG.experimentalModelScaling;
        TheNextForge.CONFIG.experimentalModelScaling = false;
        try {
            for (var item : new Item[] {Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_CHESTPLATE, Items.TRIDENT, Items.SHIELD}) {
                for (int tier = 0; tier < 4; tier++) {
                    ItemStack stack = new ItemStack(item);
                    ForgingRules.write(stack, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, levels[tier]));
                    for (var context : ItemDisplayContext.values()) {
                        java.util.Set<net.minecraft.client.renderer.RenderType> requested = new java.util.HashSet<>();
                        var pose = new com.mojang.blaze3d.vertex.PoseStack();
                        mc.getItemRenderer().renderStatic(stack, context, 15728880, 0, pose, type -> {
                            requested.add(type); return new Sink();
                        }, null, 42);
                        boolean quality = requested.stream().anyMatch(type -> type.toString().contains("tnf_quality_"));
                        require(quality == (tier > 0), "品质流光通道 " + item + "/" + context + "/" + tier + " " + requested);
                        for (var type : requested) {
                            if (type.toString().contains("tnf_quality_")) require(type.toString().contains("tnf_quality_" + tier + "_"), "串色");
                        }
                        require(pose.last().pose().equals(new org.joml.Matrix4f()), "矩阵泄漏");
                        cases++;
                    }
                }
            }
        } finally { TheNextForge.CONFIG.experimentalModelScaling = old; }
        TheNextForge.LOGGER.info("[TNF-QUALITY-PROBE] PASS cases={} blue-purple-gold model-buffers", cases);
    }
    private static final class Sink implements com.mojang.blaze3d.vertex.VertexConsumer {
        public Sink addVertex(float x,float y,float z) { return this; }
        public Sink setColor(int r,int g,int b,int a) { return this; }
        public Sink setUv(float u,float v) { return this; }
        public Sink setUv1(int u,int v) { return this; }
        public Sink setUv2(int u,int v) { return this; }
        public Sink setNormal(float x,float y,float z) { return this; }
    }
}
