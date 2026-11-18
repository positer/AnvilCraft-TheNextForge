package dev.anvilcraft.thenextforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** 原生 glint 管线和深度测试，仅替换本物品流光纹理；不改全局着色器或基础模型。 */
public final class QualityGlintRenderTypes extends RenderStateShard {
    private static final RenderType[][] TYPES = new RenderType[3][5];
    static {
        for (int tier = 1; tier <= 3; tier++) {
            for (int mode = 0; mode < 5; mode++) {
                var state = RenderType.CompositeState.builder()
                    .setShaderState(switch (mode) {
                        case 1 -> RENDERTYPE_GLINT_TRANSLUCENT_SHADER;
                        case 2 -> RENDERTYPE_ENTITY_GLINT_SHADER;
                        case 3 -> RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER;
                        case 4 -> RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER;
                        default -> RENDERTYPE_GLINT_SHADER;
                    })
                    .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thenextforge", QualityGlint.texture(tier)), true, false))
                    .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST)
                    .setTransparencyState(GLINT_TRANSPARENCY)
                    .setTexturingState(mode >= 2 ? ENTITY_GLINT_TEXTURING : GLINT_TEXTURING);
                if (mode == 1 || mode == 2) state.setOutputState(ITEM_ENTITY_TARGET);
                if (mode == 4) state.setLayeringState(VIEW_OFFSET_Z_LAYERING);
                TYPES[tier-1][mode] = RenderType.create("tnf_quality_" + tier + "_" + mode,
                    DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, state.createCompositeState(false));
            }
        }
    }
    private QualityGlintRenderTypes() { super("tnf_quality", () -> { }, () -> { }); }
    public static RenderType remap(RenderType type, int tier) {
        if (tier == 0) return type;
        int mode = type == RenderType.glint() ? 0 : type == RenderType.glintTranslucent() ? 1
            : type == RenderType.entityGlint() ? 2 : type == RenderType.entityGlintDirect() ? 3
            : type == RenderType.armorEntityGlint() ? 4 : -1;
        return mode < 0 ? type : TYPES[tier-1][mode];
    }
    public static MultiBufferSource wrap(MultiBufferSource source, int tier) {
        return tier == 0 ? source : type -> source.getBuffer(remap(type, tier));
    }
}
