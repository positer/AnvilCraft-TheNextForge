package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** 携带护符的嵌入属性；展示框的锻造效果由 Amulets 独立处理。 */

public final class AmuletAttributes {
    // 弱键且值不含玩家/世界；不把离线玩家 UUID 永久留在静态表。
    private static final Map<Player, Map<ResourceLocation, Applied>> APPLIED = new WeakHashMap<>();
    private record Applied(Holder<Attribute> attribute, AttributeModifier modifier) { }
    private AmuletAttributes() { }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) sync(player);
    }

    /** 稳定 id，按差异增删；装备没变时不重建玩家的属性修饰符。 */
    public static int sync(Player player) {
        Map<ResourceLocation, Applied> desired = new HashMap<>();
        int index = 0;
        for (ItemStack amulet : ForgeItemTicker.carriedAmulets(player)) {
            collect(player, desired, amulet, "amulet/" + index++, null);
        }
        // 旧版装甲嵌入是 MAINHAND；新写入使用 ARMOR，不重复补偿。
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()) collect(player, desired, player.getItemBySlot(slot), "legacy_armour/" + slot.getName(), slot);
        }
        Map<ResourceLocation, Applied> old = APPLIED.getOrDefault(player, Map.of());
        for (var entry : old.entrySet()) {
            if (!entry.getValue().equals(desired.get(entry.getKey()))) {
                var attribute = player.getAttribute(entry.getValue().attribute());
                if (attribute != null) attribute.removeModifier(entry.getKey());
            }
        }
        for (var entry : desired.entrySet()) {
            var instance = player.getAttribute(entry.getValue().attribute());
            if (instance == null) continue;
            if (!entry.getValue().equals(old.get(entry.getKey())) || !instance.hasModifier(entry.getKey())) {
                instance.removeModifier(entry.getKey());
                instance.addTransientModifier(entry.getValue().modifier());
            }
        }
        if (desired.isEmpty()) APPLIED.remove(player);
        else APPLIED.put(player, desired);
        return desired.size();
    }

    private static void collect(Player player, Map<ResourceLocation, Applied> desired, ItemStack stack,
                                String source, EquipmentSlot legacySlot) {
        var modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        int index = 0;
        for (var entry : modifiers.modifiers()) {
            var original = entry.modifier();
            if (!original.id().getNamespace().equals(TheNextForge.MOD_ID)) continue;
            if (legacySlot != null && entry.slot().test(legacySlot)) continue;
            if (legacySlot == null && stack == player.getMainHandItem() && entry.slot().test(EquipmentSlot.MAINHAND)) continue;
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(TheNextForge.MOD_ID, "carried/" + source + "/" + index++);
            desired.put(id, new Applied(entry.attribute(),
                new AttributeModifier(id, original.amount(), original.operation())));
        }
    }
}
