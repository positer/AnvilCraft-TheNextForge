package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.forging.ForgeDurability;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 原生装备内部的磨损／软损坏判定也直接读取物品方法，必须与 ItemStack 看到同一个上限。 */
@Pseudo
@Mixin(targets = {
    "net.silentchaos512.gear.item.gear.GearArmorItem",
    "net.silentchaos512.gear.item.gear.GearArrowItem",
    "net.silentchaos512.gear.item.gear.GearAxeItem",
    "net.silentchaos512.gear.item.gear.GearBowItem",
    "net.silentchaos512.gear.item.gear.GearCrossbowItem",
    "net.silentchaos512.gear.item.gear.GearCurioItem",
    "net.silentchaos512.gear.item.gear.GearDiggerItem",
    "net.silentchaos512.gear.item.gear.GearElytraItem",
    "net.silentchaos512.gear.item.gear.GearFishingRodItem",
    "net.silentchaos512.gear.item.gear.GearHoeItem",
    "net.silentchaos512.gear.item.gear.GearMaceItem",
    "net.silentchaos512.gear.item.gear.GearPickaxeItem",
    "net.silentchaos512.gear.item.gear.GearShearsItem",
    "net.silentchaos512.gear.item.gear.GearShieldItem",
    "net.silentchaos512.gear.item.gear.GearShovelItem",
    "net.silentchaos512.gear.item.gear.GearSpearItem",
    "net.silentchaos512.gear.item.gear.GearSwordItem",
    "net.silentchaos512.gear.item.gear.GearTridentItem"
}, remap = false)
public abstract class SilentGearDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true, remap = false)
    private void tnf$maximum(ItemStack stack, CallbackInfoReturnable<Integer> result) {
        int maximum = ForgeDurability.managedMaximum(stack);
        if (maximum > 0) result.setReturnValue(maximum);
    }
}
