package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.client.QualityTierAccess;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStackRenderState.class)
public abstract class QualityStackStateMixin implements QualityTierAccess {
    @Shadow private int activeLayerCount;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;
    public int tnf$getQualityTier() { return activeLayerCount == 0 ? 0 : ((QualityTierAccess) layers[0]).tnf$getQualityTier(); }
    public void tnf$setQualityTier(int tier) {
        for (int i = 0; i < activeLayerCount; i++) ((QualityTierAccess) layers[i]).tnf$setQualityTier(tier);
    }
}
