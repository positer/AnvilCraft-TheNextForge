package dev.anvilcraft.thenextforge.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;

/** 原生 GLINT 管线，仅替换品质纹理，保持输出目标、深度与 UV 动画。 */
public final class QualityGlintRenderTypes {
    private static final RenderType[][] TYPES = new RenderType[3][4];
    static {
        for (int tier = 1; tier <= 3; tier++) {
            for (int mode = 0; mode < 4; mode++) {
                var setup = RenderSetup.builder(RenderPipelines.GLINT)
                    .withTexture("Sampler0", Identifier.fromNamespaceAndPath("thenextforge", QualityGlint.texture(tier)))
                    .setTextureTransform(mode == 3 ? TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING
                        : mode == 2 ? TextureTransform.ENTITY_GLINT_TEXTURING : TextureTransform.GLINT_TEXTURING);
                if (mode == 1) setup.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET);
                if (mode == 3) setup.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING);
                TYPES[tier-1][mode] = RenderType.create("tnf_quality_" + tier + "_" + mode, setup.createRenderSetup());
            }
        }
    }
    private QualityGlintRenderTypes() { }
    public static RenderType remap(RenderType type, int tier) {
        if (tier == 0) return type;
        int mode = type == RenderTypes.glint() ? 0 : type == RenderTypes.glintTranslucent() ? 1
            : type == RenderTypes.entityGlint() ? 2 : type == RenderTypes.armorEntityGlint() ? 3 : -1;
        return mode < 0 ? type : TYPES[tier-1][mode];
    }
    public static MultiBufferSource wrap(MultiBufferSource source, int tier) {
        return tier == 0 ? source : type -> source.getBuffer(remap(type, tier));
    }
}
