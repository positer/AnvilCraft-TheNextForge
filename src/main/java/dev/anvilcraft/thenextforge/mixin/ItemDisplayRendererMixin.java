package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.client.DisplayScaleContext;
import dev.anvilcraft.thenextforge.forging.ModelScaling;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DisplayRenderer.ItemDisplayRenderer.class)
public abstract class ItemDisplayRendererMixin {
    @WrapMethod(method = "renderInner(Lnet/minecraft/world/entity/Display$ItemDisplay;Lnet/minecraft/world/entity/Display$ItemDisplay$ItemRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V")
    private void tnf$display(Display.ItemDisplay entity, Display.ItemDisplay.ItemRenderState state,
                             PoseStack pose, MultiBufferSource buffers, int light, float progress, Operation<Void> original) {
        var renderState = entity.renderState();
        float correction = renderState == null || !state.itemStack().has(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get()) ? 1.0F : ModelScaling.displayCorrection(renderState.transformation().get(progress));
        var previous = DisplayScaleContext.enter(state.itemStack(), correction);
        try {
            original.call(entity, state, pose, buffers, light, progress);
        } finally {
            DisplayScaleContext.restore(previous);
        }
    }
}
