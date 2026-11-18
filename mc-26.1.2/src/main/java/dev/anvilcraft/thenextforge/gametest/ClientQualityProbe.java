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
        var output = new net.minecraft.client.renderer.item.TrackingItemStackRenderState();
        var parts = new net.minecraft.client.renderer.feature.ModelPartFeatureRenderer();
        var items = new net.minecraft.client.renderer.feature.ItemFeatureRenderer();
        var models = new net.minecraft.client.renderer.feature.ModelFeatureRenderer();
        var outlines = new net.minecraft.client.renderer.OutlineBufferSource();
        for (var item : new Item[] {Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_CHESTPLATE, Items.TRIDENT, Items.SHIELD}) {
            for (int tier = 3; tier >= 0; tier--) {
                ItemStack stack = new ItemStack(item);
                ForgingRules.write(stack, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, levels[tier]));
                for (var context : ItemDisplayContext.values()) {
                    mc.getItemModelResolver().updateForTopItem(output, stack, context, mc.level, null, 42);
                    require(((QualityTierAccess) (Object) output).tnf$getQualityTier() == tier, "复用状态串色");
                    require(output.getModelIdentity().toString().contains("Identity[tier=" + tier + "]"), "图标缓存缺品质");
                    var storage = new net.minecraft.client.renderer.SubmitNodeStorage();
                    output.submit(new com.mojang.blaze3d.vertex.PoseStack(), storage, 15728880, 0, 0);
                    require(QualityGlint.current() == 0, "提交上下文泄漏");
                    var buffers = new Capture();
                    for (var collection : storage.getSubmitsPerOrder().values()) {
                        for (var submit : collection.getItemSubmits()) {
                            require(((QualityTierAccess) (Object) submit).tnf$getQualityTier() == tier, "延迟提交丢失品质");
                        }
                        items.renderSolid(collection, buffers, outlines);
                        items.renderTranslucent(collection, buffers, outlines);
                        models.renderSolid(collection, buffers, outlines, buffers);
                        models.renderTranslucent(collection, buffers, outlines, buffers);
                        parts.renderSolid(collection, buffers, outlines, buffers);
                        parts.renderTranslucent(collection, buffers, outlines, buffers);
                    }
                    require(QualityGlint.current() == 0, "绘制上下文泄漏");
                    boolean quality = buffers.requested.stream().anyMatch(type -> type.toString().contains("tnf_quality_"));
                    require(quality == (tier > 0), "延迟绘制品质通道 " + item + "/" + context + "/" + tier + " " + buffers.requested);
                    for (var type : buffers.requested) {
                        if (type.toString().contains("tnf_quality_")) require(type.toString().contains("tnf_quality_" + tier + "_"), "延迟绘制串色");
                    }
                    cases++;
                }
            }
        }
        TheNextForge.LOGGER.info("[TNF-QUALITY-PROBE] PASS cases={} blue-purple-gold deferred-draw/cache/reset", cases);
    }
    private static final class Capture extends net.minecraft.client.renderer.MultiBufferSource.BufferSource {
        final java.util.Set<net.minecraft.client.renderer.rendertype.RenderType> requested = new java.util.HashSet<>();
        Capture() { super(null, new java.util.LinkedHashMap<>()); }
        @Override public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.rendertype.RenderType type) {
            requested.add(type); return new Sink();
        }
    }
    private static final class Sink implements com.mojang.blaze3d.vertex.VertexConsumer {
        public Sink addVertex(float x,float y,float z) { return this; }
        public Sink setColor(int r,int g,int b,int a) { return this; }
        public Sink setColor(int color) { return this; }
        public Sink setUv(float u,float v) { return this; }
        public Sink setUv1(int u,int v) { return this; }
        public Sink setUv2(int u,int v) { return this; }
        public Sink setNormal(float x,float y,float z) { return this; }
        public Sink setLineWidth(float width) { return this; }
    }
}
