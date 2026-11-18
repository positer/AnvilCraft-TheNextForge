package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.client.ItemScaleTarget;
import dev.anvilcraft.thenextforge.forging.ModelScaling;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 顶层模型解析覆盖 GUI、手持、掉落物、展示框及特殊模型，复合模型也只缩放一次。 */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @Inject(method = "updateForTopItem", at = @At("RETURN"))
    private void tnf$scale(ItemStackRenderState output, ItemStack item, ItemDisplayContext context,
                           Level level, ItemOwner owner, int seed, CallbackInfo ci) {
        int tier = dev.anvilcraft.thenextforge.client.QualityGlint.tier(item);
        ((dev.anvilcraft.thenextforge.client.QualityTierAccess) (Object) output).tnf$setQualityTier(tier);
        output.appendModelIdentityElement(new dev.anvilcraft.thenextforge.client.QualityTierAccess.Identity(tier));
        float scale = ModelScaling.scaleOf(item);
        if (owner instanceof Display.ItemDisplay display && item.has(ModDataComponents.FORGING.get())) {
            var state = display.renderState();
            if (state != null) scale *= ModelScaling.displayCorrection(state.transformation().get(1.0F));
        }
        if (scale == 1.0F) return;
        ((ItemScaleTarget) (Object) output).tnf$setScale(scale);
        output.appendModelIdentityElement(new ItemScaleTarget.Identity(scale));
        // 放大后的边界交给原生 oversized GUI 路径，避免 16×16 图集裁切。
        if (context == ItemDisplayContext.GUI && scale > 1.0F) output.setOversizedInGui(true);
    }
}
