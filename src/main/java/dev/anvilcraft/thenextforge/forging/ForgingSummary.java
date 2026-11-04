package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把物品上本模组写下的锻造修饰符按属性键汇总。
 *
 * <p>每次成功锤击写入一条 {@code thenextforge:forging/<key>/<level>}。等级段保证
 * {@link ItemAttributeModifiers#withModifierAdded} 不会用新条目替换掉旧条目（它按
 * 「属性 + id」去重），因此多条加成能同时生效；同一属性键的多级加成在这里求和，
 * tooltip 与验收都只读这里的最终值。
 */
public final class ForgingSummary {
    /** 锻造修饰符 id 的路径前缀。 */
    public static final String PREFIX = "forging/";

    /** 一个属性键求和后的加成：flat 表示该键用 ADD_VALUE，否则按百分比展示。 */
    public record Group(String key, Holder<Attribute> attribute, double amount, boolean flat, int entries) {
    }

    private ForgingSummary() {
    }

    /** 是否是本模组锻造写下的修饰符 id。 */
    public static boolean isForgeModifier(ResourceLocation id) {
        return id != null && TheNextForge.MOD_ID.equals(id.getNamespace()) && id.getPath().startsWith(PREFIX);
    }

    /** 取属性键：{@code forging/weapon_attack/7} → {@code weapon_attack}。 */
    public static String keyOf(ResourceLocation id) {
        String[] parts = id.getPath().split("/");
        return parts.length >= 2 ? parts[1] : "";
    }

    /** 取该条修饰符对应的等级；旧格式（没有等级段）返回 -1。 */
    public static int levelOf(ResourceLocation id) {
        String[] parts = id.getPath().split("/");
        if (parts.length < 3) return -1;
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /** 按属性键汇总，保持写入顺序。 */
    public static List<Group> groups(ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY
        );
        Map<String, Group> acc = new LinkedHashMap<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            ResourceLocation id = entry.modifier().id();
            if (!isForgeModifier(id)) continue;
            String key = keyOf(id);
            boolean flat = entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE;
            Group previous = acc.get(key);
            acc.put(key, previous == null
                ? new Group(key, entry.attribute(), entry.modifier().amount(), flat, 1)
                : new Group(key, previous.attribute(), previous.amount() + entry.modifier().amount(),
                    previous.flat() && flat, previous.entries() + 1));
        }
        return List.copyOf(acc.values());
    }

    /** 某个属性键的求和结果；没有该键时返回 0。 */
    public static double amount(ItemStack stack, String key) {
        for (Group group : groups(stack)) {
            if (group.key().equals(key)) return group.amount();
        }
        return 0.0;
    }
}
