package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.client.QualityGlint;
import dev.anvilcraft.thenextforge.client.QualityTierAccess;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class QualityLayerStateMixin implements QualityTierAccess {
    @Unique private int tnf$qualityTier;
    public int tnf$getQualityTier() { return tnf$qualityTier; }
    public void tnf$setQualityTier(int tier) { tnf$qualityTier = tier; }
    @Inject(method = "clear", at = @At("RETURN"))
    private void tnf$clearQuality(CallbackInfo ci) { tnf$qualityTier = 0; }
    @WrapMethod(method = "submit")
    private void tnf$submitQuality(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline,
                                   Operation<Void> original) {
        int before = QualityGlint.enter(tnf$qualityTier);
        try { original.call(pose, collector, light, overlay, outline); }
        finally { QualityGlint.restore(before); }
    }
}
