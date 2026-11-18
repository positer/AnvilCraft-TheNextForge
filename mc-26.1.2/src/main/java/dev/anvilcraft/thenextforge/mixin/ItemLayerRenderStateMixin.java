package dev.anvilcraft.thenextforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.client.ItemScaleTarget;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 与原生模型变换组合，几何提交、特殊模型、模型边界全部使用相同倍率。 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class ItemLayerRenderStateMixin implements ItemScaleTarget {
    @Unique private Matrix4f tnf$scale;

    @Override
    public void tnf$setScale(float scale) {
        if (tnf$scale == null) tnf$scale = new Matrix4f();
        tnf$scale.scaling(scale);
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void tnf$clear(CallbackInfo ci) {
        tnf$scale = null;
    }

    @Inject(method = "applyTransform", at = @At("HEAD"))
    private void tnf$transform(PoseStack.Pose pose, CallbackInfo ci) {
        if (tnf$scale != null) pose.mulPose(tnf$scale);
    }
}
