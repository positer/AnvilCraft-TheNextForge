package dev.anvilcraft.thenextforge.forging;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 可锻造对象的类别，决定每级提升的属性与可用槽位。
 */
public enum TargetKind {
    TOOL(3),
    WEAPON(3),
    ARMOR(1),
    AMULET(1);

    private final int slots;

    TargetKind(int slots) {
        this.slots = slots;
    }

    public int slots() {
        return this.slots;
    }

    public boolean canUseGrindstone() {
        return this == WEAPON || this == ARMOR;
    }

    /**
     * 判定物品属于哪一类可锻造对象，不可锻造返回 null。
     */
    private static boolean hasAttackDamage(ItemStack stack) {
        var modifiers = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS,
            net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY
        );
        return modifiers.modifiers().stream().anyMatch(
            entry -> entry.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
        );
    }

    /**
     * 物品的**全部**锻造角色（设计 L41：同时是武器与工具的物品——斧头/龙杖/共振器——两种强化都要有）。
     *
     * <p>主角色仍是 {@link #of} 的结果（决定升级线判定），这里是加成与描述的并集。
     */
    public static java.util.List<TargetKind> rolesOf(ItemStack stack) {
        if (stack.isEmpty() || !ForgeRulesData.isTargetAllowed(stack)) return java.util.List.of();
        var mapped = dev.anvilcraft.thenextforge.api.TnfApi.target(stack);
        if (mapped != null) return mapped.enabled() ? mapped.roles() : java.util.List.of();
        TargetKind primary = of(stack);
        if (primary == null) return java.util.List.of();
        if (primary == TargetKind.TOOL && isWeaponizableTool(stack)) {
            return java.util.List.of(TargetKind.TOOL, TargetKind.WEAPON);
        }
        return java.util.List.of(primary);
    }

    /** 既是工具又能当武器用的物品：斧头、龙杖、共振器。 */
    private static boolean isWeaponizableTool(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.AXES)
            || stack.is(dev.dubhe.anvilcraft.init.item.ModItemTags.DRAGON_ROD)
            || stack.is(dev.dubhe.anvilcraft.init.item.ModItemTags.RESONATOR);
    }

    public static TargetKind of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        // 设计 L19：对象集合走数据驱动的黑白名单
        if (!ForgeRulesData.isTargetAllowed(stack)) return null;
        var mapped = dev.anvilcraft.thenextforge.api.TnfApi.target(stack);
        if (mapped != null) return mapped.enabled() && !mapped.roles().isEmpty() ? mapped.roles().getFirst() : null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getPath().endsWith("_amulet")) return AMULET;
        // 设计：龙杖 / 铁砧锤 也算工具（它们继承 Item，没有原版工具标签，只能按铁砧工艺的标签判）
        if (stack.is(dev.dubhe.anvilcraft.init.item.ModItemTags.ANVIL_HAMMER)
            || stack.is(dev.dubhe.anvilcraft.init.item.ModItemTags.DRAGON_ROD)) return TOOL;
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) return WEAPON;
        if (stack.is(net.minecraft.tags.ItemTags.PICKAXES)
            || stack.is(net.minecraft.tags.ItemTags.SHOVELS)
            || stack.is(net.minecraft.tags.ItemTags.HOES)
            || stack.is(net.minecraft.tags.ItemTags.AXES)) {
            return TOOL;
        }
        if (hasAttackDamage(stack)) return WEAPON;
        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem) return ARMOR;
        return null;
    }
}
