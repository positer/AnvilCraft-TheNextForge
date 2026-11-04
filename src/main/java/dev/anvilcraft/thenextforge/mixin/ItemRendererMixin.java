package dev.anvilcraft.thenextforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.forging.ModelScaling;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被锻造物品的**真实渲染放大**。
 *
 * <p>手持（第一/第三人称）、物品栏图标、掉落物、展示框——所有形态最终都经过
 * {@link ItemRenderer#render}，因此只在这里按 {@link ModelScaling} 给出的倍率缩放
 * {@code PoseStack}，四种形态就是同一套真实放大。
 *
 * <p>**只做一件事**：push + scale + pop，不读写任何字段、不改任何逻辑，
 * 因此与其他模组冲突的概率极低。
 *
 * <p>GUI 的 16×16 槽位以左上角为原点，缩放后向中心补偿，避免图标偏出格子；
 * 世界内的物品以自身原点为中心，只做等比放大。
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void tnf$pushScale(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                               PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight,
                               int combinedOverlay, BakedModel model, CallbackInfo ci) {
        float scale = ModelScaling.scaleOf(stack);
        if (scale == 1.0F) return;
        poseStack.pushPose();
        if (displayContext == ItemDisplayContext.GUI) {
            // 槽位 16×16：围绕格子中心缩放
            float offset = (1.0F - scale) * 8.0F;
            poseStack.translate(offset / 16.0F, offset / 16.0F, 0.0F);
        }
        poseStack.scale(scale, scale, scale);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void tnf$popScale(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                              PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight,
                              int combinedOverlay, BakedModel model, CallbackInfo ci) {
        if (ModelScaling.scaleOf(stack) != 1.0F) poseStack.popPose();
    }
}
