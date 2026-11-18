package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.forging.DynamicDurabilityItem;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 动态物品的 getMaxDamage 绕过 MAX_DAMAGE，平台没有该读取的事件；只接管带 TNF 耐久状态的寂静装备。 */
@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void tnf$maximum(CallbackInfoReturnable<Integer> result) {
        ItemStack stack = (ItemStack) (Object) this;
        int maximum = dev.anvilcraft.thenextforge.forging.ForgeDurability.managedMaximum(stack);
        if (maximum > 0) result.setReturnValue(maximum);
    }
}
