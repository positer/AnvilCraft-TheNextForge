package dev.anvilcraft.thenextforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.anvilcraft.thenextforge.forging.ForgeAttributes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * 可选寂静装备适配。Pre/Post 事件分离，无法用局部变量保证异常／嵌套调用时的快照生命周期；
 * 包裹单次重算，只恢复 TNF 自有条目，不保留装备、玩家或世界的跨调用引用。
 */
@Pseudo
@Mixin(targets = "net.silentchaos512.gear.util.GearData", remap = false)
public abstract class SilentGearRecalculateMixin {
    @WrapMethod(method = "recalculateGearData", remap = false)
    private static void tnf$preserve(ItemStack stack, Player player, Operation<Void> original) {
        var before = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        original.call(stack, player);
        ForgeAttributes.restoreOwned(stack, before);
        dev.anvilcraft.thenextforge.forging.ForgeDurability.rebaseDynamic(stack);
    }
}
