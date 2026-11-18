package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.thenextforge.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelPartFeatureRenderer.class)
public abstract class QualityModelPartFeatureMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private VertexConsumer tnf$qualityPart(MultiBufferSource buffers, RenderType type, boolean sheeted, boolean foil,
                                          Operation<VertexConsumer> original, @Local SubmitNodeStorage.ModelPartSubmit submit) {
        return original.call(QualityGlintRenderTypes.wrap(buffers, ((QualityTierAccess) (Object) submit).tnf$getQualityTier()), type, sheeted, foil);
    }
}
