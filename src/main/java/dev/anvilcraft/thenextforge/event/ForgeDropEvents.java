package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 掉落相关的嵌入材料效果：末影之眼把掉落直接塞进背包，余烬金属锭自动熔炼。
 */
public final class ForgeDropEvents {
    private ForgeDropEvents() {
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        ItemStack tool = player.getMainHandItem();
        ForgingData data = tool.get(ModDataComponents.FORGING.get());

        if (data != null && data.hasActiveEmbed("anvilcraft:ember_metal_ingot")) {
            smeltAll(event);
        }
        // 设计：浮霜「挖掘不掉落、转化为经验」——方块经验 ×4、最低 1 点（与浮霜铁砧一致）
        if (data != null && data.hasActiveEmbed("anvilcraft:frost_metal_ingot")) {
            int baseXp = event.getDroppedExperience();
            int xp = frostXpFor(baseXp);
            event.getDrops().clear();
            event.setDroppedExperience(xp);
            TheNextForge.LOGGER.info("[TNF] frost embed converted block xp {} -> {} (drops cleared)", baseXp, xp);
        }
        // 设计：末影珍珠的掉落归仓是「工具/护符」两列
        if (collectsDrops(player)) {
            int collected = collect(player, event.getDrops());
            event.getDrops().clear();
            TheNextForge.LOGGER.info("[TNF] ender_pearl embed sent {} drop(s) to inventory", collected);
        }
    }

    /** 浮霜经验倍率：与浮霜铁砧（湮灭）一致——方块经验 ×4，最低 1 点。 */
    public static final int FROST_XP_MULTIPLIER = 4;

    /** 一个方块被浮霜工具挖掉时给的经验。 */
    public static int frostXpFor(int blockExperience) {
        return Math.max(1, blockExperience * FROST_XP_MULTIPLIER);
    }

    /** 末影珍珠是否让本次挖掘的掉落直接进背包：手持工具或随身护符。 */
    public static boolean collectsDrops(net.minecraft.world.entity.player.Player player) {
        ForgingData held = player.getMainHandItem().get(ModDataComponents.FORGING.get());
        if (held != null && held.hasActiveEmbed("minecraft:ender_pearl")) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;
            String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            if (!path.endsWith("_amulet")) continue;
            ForgingData data = stack.get(ModDataComponents.FORGING.get());
            if (data != null && data.hasActiveEmbed("minecraft:ender_pearl")) return true;
        }
        return false;
    }

    private static void smeltAll(BlockDropsEvent event) {
        var level = event.getLevel();
        for (ItemEntity entity : event.getDrops()) {
            ItemStack smelted = smelt(level, entity.getItem());
            if (smelted != null) entity.setItem(smelted);
        }
    }

    /**
     * 把一件掉落实体换成熔炉产物；没有配方的返回 null。
     *
     * <p>抽成公共方法，GameTest 可以直接验证熔炼映射。
     */
    public static ItemStack smelt(net.minecraft.world.level.Level level, ItemStack input) {
        if (input.isEmpty()) return null;
        var found = level.getRecipeManager().getRecipeFor(
            RecipeType.SMELTING, new SingleRecipeInput(input), level
        );
        if (found.isEmpty()) return null;
        ItemStack result = found.get().value().getResultItem(level.registryAccess());
        if (result.isEmpty()) return null;
        ItemStack smelted = result.copy();
        smelted.setCount(result.getCount() * input.getCount());
        return smelted;
    }

    /**
     * 把掉落物塞进背包；设计 L61 要求先进「身上大型潜影盒」——这里优先塞进携带的潜影盒，满了再进背包。
     *
     * @return 塞入的件数
     */
    public static int collect(net.minecraft.world.entity.player.Player player, java.util.List<ItemEntity> drops) {
        int count = 0;
        for (ItemEntity entity : drops) {
            ItemStack left = insertIntoShulker(player, entity.getItem().copy());
            if (!left.isEmpty()) player.getInventory().placeItemBackInInventory(left);
            count++;
        }
        return count;
    }

    /**
     * 尝试把物品放进身上携带的潜影盒（`DataComponents.CONTAINER`）。
     *
     * @return 放不下的剩余（空栈表示全部放进去了）
     */
    public static ItemStack insertIntoShulker(net.minecraft.world.entity.player.Player player, ItemStack drop) {
        if (drop.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = drop.copy();
        for (ItemStack carried : player.getInventory().items) {
            if (carried.isEmpty() || remaining.isEmpty()) continue;
            if (!(carried.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) continue;
            if (!(blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock)) continue;
            remaining = insertIntoShulker(carried, remaining);
        }
        return remaining;
    }

    /** 往单个潜影盒里塞；返回塞不下的剩余。 */
    public static ItemStack insertIntoShulker(ItemStack shulker, ItemStack drop) {
        var contents = shulker.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.EMPTY);
        java.util.List<ItemStack> slots = new java.util.ArrayList<>(
            java.util.Collections.nCopies(27, ItemStack.EMPTY));
        int index = 0;
        for (ItemStack stack : contents.nonEmptyItems()) {
            if (index >= slots.size()) break;
            slots.set(index++, stack.copy());
        }
        ItemStack remaining = drop.copy();
        // 先叠加同类
        for (ItemStack stack : slots) {
            if (remaining.isEmpty()) break;
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, remaining)) continue;
            int move = Math.min(remaining.getCount(), stack.getMaxStackSize() - stack.getCount());
            if (move <= 0) continue;
            stack.grow(move);
            remaining.shrink(move);
        }
        // 再找空位
        for (int i = 0; i < slots.size() && !remaining.isEmpty(); i++) {
            if (!slots.get(i).isEmpty()) continue;
            slots.set(i, remaining.copy());
            remaining.setCount(0);
        }
        if (remaining.getCount() != drop.getCount()) {
            shulker.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(slots));
        }
        return remaining;
    }
}
