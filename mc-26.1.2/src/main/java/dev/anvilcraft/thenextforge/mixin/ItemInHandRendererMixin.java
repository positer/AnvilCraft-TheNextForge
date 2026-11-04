package dev.anvilcraft.thenextforge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.forging.ModelScaling;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 被锻造物品的**真实渲染放大**（26.1.2）。
 *
 * <p>26.1.2 不再有 {@code ItemRenderer#render}：物品绘制拆成了
 * 「{@link ItemStackRenderState} 描述 + {@code submit} 提交」两段，
 * 而 render state 里**不含 ItemStack**，无法按等级算倍率。
 *
 * <p>真正的统一入口是 {@code ItemInHandRenderer#renderItem(LivingEntity, ItemStack,
 * ItemDisplayContext, PoseStack, SubmitNodeCollector, int)}——它同时拿得到物品与姿态，
 * 手持（第一/第三人称）、掉落物、展示框都从这里画。
 *
 * <p>**只做一件事**：push + scale + pop，不读写字段、不改逻辑。
 */
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void tnf$pushScale(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                               PoseStack poseStack, SubmitNodeCollector collector, int light,
                               CallbackInfo ci) {
        float scale = ModelScaling.scaleOf(stack);
        if (scale == 1.0F) return;
        poseStack.pushPose();
        if (displayContext == ItemDisplayContext.GUI) {
            float offset = (1.0F - scale) * 8.0F;
            poseStack.translate(offset / 16.0F, offset / 16.0F, 0.0F);
        }
        poseStack.scale(scale, scale, scale);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void tnf$popScale(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                              PoseStack poseStack, SubmitNodeCollector collector, int light,
                              CallbackInfo ci) {
        if (ModelScaling.scaleOf(stack) != 1.0F) poseStack.popPose();
    }
}
