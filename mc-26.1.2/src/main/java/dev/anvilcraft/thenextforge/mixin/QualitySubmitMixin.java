package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.client.QualityGlint;
import dev.anvilcraft.thenextforge.client.QualityTierAccess;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 品质随提交记录进入延迟渲染，不能依赖提交阶段的线程变量留到绘制阶段。 */
@Mixin({SubmitNodeStorage.ItemSubmit.class, SubmitNodeStorage.ModelPartSubmit.class})
public abstract class QualitySubmitMixin implements QualityTierAccess {
    @Unique private int tnf$qualityTier;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void tnf$captureQuality(CallbackInfo ci) { tnf$qualityTier = QualityGlint.current(); }
    public int tnf$getQualityTier() { return tnf$qualityTier; }
    public void tnf$setQualityTier(int tier) { tnf$qualityTier = tier; }
}
