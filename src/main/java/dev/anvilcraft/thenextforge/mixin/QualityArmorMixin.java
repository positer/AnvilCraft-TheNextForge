package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.thenextforge.client.QualityGlint;
import dev.anvilcraft.thenextforge.client.QualityGlintRenderTypes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HumanoidArmorLayer.class)
public abstract class QualityArmorMixin {
    @WrapMethod(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V")
    private void tnf$qualityArmor(PoseStack pose, MultiBufferSource buffers, LivingEntity entity, EquipmentSlot slot,
                                  int light, HumanoidModel<?> model, float a, float b, float c, float d, float e, float f,
                                  Operation<Void> original) {
        original.call(pose, QualityGlintRenderTypes.wrap(buffers, QualityGlint.tier(entity.getItemBySlot(slot))),
            entity, slot, light, model, a, b, c, d, e, f);
    }
}
