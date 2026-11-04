package dev.anvilcraft.thenextforge.forging;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * 铁砧/砂轮周围物品展示框里的护符。
 *
 * <p>同名护符只生效一次；宝石护符与自然护符按并集展开，不重复触发。
 */
public final class Amulets {
    private Amulets() {
    }

    /**
     * 把组合护符展开成并集：宝石 = 红 + 蓝 + 黄玉，自然 = 犰狳 + 羽毛 + 猫 + 狗。
     */
    public static Set<String> expand(Set<String> found) {
        Set<String> result = new HashSet<>(found);
        if (result.contains("gem")) {
            result.add("ruby");
            result.add("sapphire");
            result.add("topaz");
        }
        if (result.contains("nature")) {
            result.add("armadillo");
            result.add("feather");
            result.add("cat");
            result.add("dog");
        }
        return result;
    }

    /**
     * 展示框里护符的最高锻造等级之和，用于给锤击提供成功率加成。
     *
     * <p>升级过的护符放在展示框里也应当生效：每个等级提供 {@link ForgingRules#AMULET_LEVEL_BONUS} 的成功率。
     */
    public static int totalLevel(ServerLevel level, BlockPos pos) {
        int total = 0;
        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(pos).inflate(1.5))) {
            ItemStack item = frame.getItem();
            if (item.isEmpty()) continue;
            String path = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
            if (!path.endsWith("_amulet")) continue;
            ForgingData data = item.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
            if (data == null) continue;
            for (ForgingLine line : ForgingLine.values()) {
                total += data.level(line);
            }
        }
        return total;
    }

    public static Set<String> collect(ServerLevel level, BlockPos pos) {
        Set<String> found = new HashSet<>();
        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, new AABB(pos).inflate(1.5))) {
            ItemStack item = frame.getItem();
            if (item.isEmpty()) continue;
            String path = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
            if (!path.endsWith("_amulet")) continue;
            found.add(path.substring(0, path.length() - "_amulet".length()));
        }
        return filterAllowed(expand(found));
    }

    /** 设计 L91：按数据驱动的黑白名单过滤护符（按 `anvilcraft:<名字>_amulet` 匹配）。 */
    public static Set<String> filterAllowed(Set<String> names) {
        Set<String> allowed = new HashSet<>();
        for (String name : names) {
            var stack = amuletStack(name);
            if (stack.isEmpty() || ForgeRulesData.isAmuletAllowed(stack)) allowed.add(name);
        }
        return allowed;
    }

    private static ItemStack amuletStack(String name) {
        return BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", name + "_amulet"))
            .map(ItemStack::new)
            .orElse(ItemStack.EMPTY);
    }
}
