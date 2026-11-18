package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.anvilcraft.thenextforge.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFeatureRenderer.class)
public abstract class QualityItemFeatureMixin {
    @WrapMethod(method = "renderItem")
    private void tnf$renderQuality(MultiBufferSource.BufferSource buffers, OutlineBufferSource outlines,
                                   SubmitNodeStorage.ItemSubmit submit, Operation<Void> original) {
        int before = QualityGlint.enter(((QualityTierAccess) (Object) submit).tnf$getQualityTier());
        try { original.call(buffers, outlines, submit); }
        finally { QualityGlint.restore(before); }
    }
    @ModifyReturnValue(method = "getFoilRenderType", at = @At("RETURN"))
    private static RenderType tnf$qualityFoil(RenderType type) {
        return QualityGlintRenderTypes.remap(type, QualityGlint.current());
    }
}
