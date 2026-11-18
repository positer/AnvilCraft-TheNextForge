package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.client.QualityGlint;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EquipmentLayerRenderer.class)
public abstract class QualityArmorMixin {
    @WrapMethod(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V")
    private void tnf$armor(EquipmentClientInfo.LayerType layer, ResourceKey<EquipmentAsset> asset, Model<?> model,
                           Object state, ItemStack item, PoseStack pose, SubmitNodeCollector collector, int light,
                           Identifier texture, int outline, int order, Operation<Void> original) {
        int before = QualityGlint.enter(QualityGlint.tier(item));
        try { original.call(layer, asset, model, state, item, pose, collector, light, texture, outline, order); }
        finally { QualityGlint.restore(before); }
    }
}
