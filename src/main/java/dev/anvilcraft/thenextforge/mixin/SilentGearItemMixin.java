package dev.anvilcraft.thenextforge.mixin;

import dev.anvilcraft.thenextforge.forging.DynamicDurabilityItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** 将可选标记附加到 GearItem，让各类寂静装备共享同一原生耐久读取适配。 */
@Pseudo
@Mixin(targets = "net.silentchaos512.gear.api.item.GearItem", remap = false)
public interface SilentGearItemMixin extends DynamicDurabilityItem {}
