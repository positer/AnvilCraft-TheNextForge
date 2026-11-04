package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 携带护符的属性生效。
 *
 * <p>护符不是装备，写在护符物品上的属性修饰符默认不会作用到玩家；
 * 这里每 20 tick 把随身护符的属性同步到玩家身上，并清掉上一轮的副本。
 */
public final class AmuletAttributes {
    private static final String NAMESPACE = "thenextforge";
    private static final Map<UUID, Set<ResourceLocation>> APPLIED = new ConcurrentHashMap<>();
    private static int counter;

    private AmuletAttributes() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        sync(player);
    }

    /**
     * 把玩家随身护符的属性修饰符同步到玩家，返回应用的条目数。
     */
    public static int sync(net.minecraft.world.entity.player.Player player) {
        clear(player);
        Set<ResourceLocation> applied = new HashSet<>();
        int index = 0;
        for (ItemStack stack : carriedAmulets(player)) {
            ItemAttributeModifiers mods = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY
            );
            for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                AttributeInstance instance = player.getAttributes().getInstance(entry.attribute());
                if (instance == null) continue;
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    NAMESPACE, "amulet/" + index + "/" + (counter++)
                );
                AttributeModifier modifier = entry.modifier();
                instance.addTransientModifier(new AttributeModifier(
                    id, modifier.amount(), modifier.operation()
                ));
                applied.add(id);
            }
            index++;
        }
        // 护甲的 MAX_HEALTH 不会经装备槽自动生效，这里补一次
        int armorIndex = 0;
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.isEmpty()) { armorIndex++; continue; }
            ItemAttributeModifiers mods = armor.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY
            );
            for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
                if (!entry.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)) continue;
                AttributeInstance instance = player.getAttributes().getInstance(entry.attribute());
                if (instance == null) continue;
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    NAMESPACE, "armor_health/" + armorIndex + "/" + (counter++)
                );
                instance.addTransientModifier(new AttributeModifier(
                    id, entry.modifier().amount(), entry.modifier().operation()
                ));
                applied.add(id);
            }
            armorIndex++;
        }
        APPLIED.put(player.getUUID(), applied);
        if (!applied.isEmpty()) {
            TheNextForge.LOGGER.info("[TNF] amulet attributes applied: {} modifiers", applied.size());
        }
        return applied.size();
    }

    private static void clear(net.minecraft.world.entity.player.Player player) {
        Set<ResourceLocation> old = APPLIED.remove(player.getUUID());
        if (old == null || old.isEmpty()) return;
        for (AttributeInstance instance : player.getAttributes().getSyncableAttributes()) {
            for (ResourceLocation id : old) {
                instance.removeModifier(id);
            }
        }
    }

    /**
     * 只收集展示框里（{@code ItemDisplay}）的护符——护符的锻造加成只在被供奉时提供。
     */
    /**
     * 收集基底铁砧/砂轮侧面展示框里的护符（设计：至多 4 件）。
     *
     * <p>物品展示框是原版 {@link ItemFrame}，其附着方块必须是铁砧或砂轮。
     */
    private static List<ItemStack> carriedAmulets(net.minecraft.world.entity.player.Player player) {
        List<ItemStack> result = new ArrayList<>();
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return result;
        int anvilCount = 0;
        for (net.minecraft.world.entity.decoration.ItemFrame frame : level.getEntitiesOfClass(
            net.minecraft.world.entity.decoration.ItemFrame.class,
            player.getBoundingBox().inflate(16.0))) {
            net.minecraft.world.level.block.state.BlockState attached =
                level.getBlockState(frame.getPos().relative(frame.getDirection().getOpposite()));
            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(attached.getBlock()).getPath();
            if (!(blockId.contains("anvil") || blockId.contains("grindstone"))) continue;
            ItemStack stack = frame.getItem();
            if (!isAmulet(stack)) continue;
            result.add(stack);
            if (++anvilCount >= 4) break;
        }
        return result;
    }

    private static boolean isAmulet(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getPath().endsWith("_amulet")) return false;
        var forging = stack.get(ModDataComponents.FORGING.get());
        return forging != null && !forging.embeds().isEmpty();
    }
}
