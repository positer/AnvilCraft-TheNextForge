package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.client.*;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** 特殊物品和穿戴装备的独立流光提交，在批次建立前确定其品质纹理。 */
@Mixin(SubmitNodeCollection.class)
public abstract class QualityModelSubmitMixin {
    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true)
    private RenderType tnf$qualityModel(RenderType type) { return QualityGlintRenderTypes.remap(type, QualityGlint.current()); }
}
