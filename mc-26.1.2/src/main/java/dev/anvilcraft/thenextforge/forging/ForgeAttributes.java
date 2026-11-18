package dev.anvilcraft.thenextforge.forging;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** 写入前保留物品按构造生成的默认属性；不冻结平台事件产生的词缀／宝石。 */
public final class ForgeAttributes {
    private ForgeAttributes() {}

    public static ItemAttributeModifiers current(ItemStack stack) {
        var stored = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return stored != null ? stored : stack.getItem().getDefaultAttributeModifiers(stack);
    }

    public static void restoreOwned(ItemStack stack, ItemAttributeModifiers before) {
        var owned = before.modifiers().stream()
            .filter(e -> e.modifier().id().getNamespace().equals("thenextforge")).toList();
        if (owned.isEmpty()) return;
        var updated = current(stack);
        for (var entry : owned) updated = updated.withModifierAdded(entry.attribute(), entry.modifier(), entry.slot());
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, updated);
    }
}
