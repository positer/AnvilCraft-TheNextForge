package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.client.DisplayScaleContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** GUI 调用前已居中，只缩放一次；异常时同样恢复矩阵栈。 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @WrapMethod(method = "render")
    private void tnf$scale(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                           PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                           BakedModel model, Operation<Void> original) {
        buffers = dev.anvilcraft.thenextforge.client.QualityGlintRenderTypes.wrap(buffers,
            dev.anvilcraft.thenextforge.client.QualityGlint.tier(stack));
        float scale = DisplayScaleContext.scaleFor(stack);
        if (scale == 1.0F) {
            original.call(stack, context, leftHand, pose, buffers, light, overlay, model);
            return;
        }
        pose.pushPose();
        try {
            pose.scale(scale, scale, scale);
            original.call(stack, context, leftHand, pose, buffers, light, overlay, model);
        } finally {
            pose.popPose();
        }
    }
}
