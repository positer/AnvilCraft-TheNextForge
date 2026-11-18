package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.client.ItemScaleTarget;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin implements ItemScaleTarget {
    @Shadow private int activeLayerCount;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;

    @Override
    public void tnf$setScale(float scale) {
        for (int i = 0; i < activeLayerCount; i++) ((ItemScaleTarget) (Object) layers[i]).tnf$setScale(scale);
    }
}
