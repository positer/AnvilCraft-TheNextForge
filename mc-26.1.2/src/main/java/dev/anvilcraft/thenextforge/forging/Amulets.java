package dev.anvilcraft.thenextforge.forging;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
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
    public record Snapshot(Set<String> effects, int totalLevel) { }

    /** 一次空间查询，只认贴在该基底四个侧面的展示框；同名护符去重。 */
    public static Snapshot scan(ServerLevel level, BlockPos pos) {
        java.util.Map<String, Integer> levels = new java.util.HashMap<>();
        java.util.Set<String> nativeEffects = new java.util.HashSet<>();
        java.util.Set<net.minecraft.core.Direction> faces = new java.util.HashSet<>();
        var frames = level.getEntitiesOfClass(ItemFrame.class, new AABB(pos).inflate(1.5));
        frames.sort(java.util.Comparator.comparing(frame -> frame.getDirection().ordinal()));
        for (ItemFrame frame : frames) {
            var direction = frame.getDirection();
            if (!direction.getAxis().isHorizontal()
                || !frame.getPos().relative(direction.getOpposite()).equals(pos)
                || faces.contains(direction)) continue;
            ItemStack item = frame.getItem();
            if (item.isEmpty() || !ForgeRulesData.isAmuletAllowed(item)) continue;
            var id = BuiltInRegistries.ITEM.getKey(item.getItem());
            String path = id.getPath();
            if (TargetKind.of(item) != TargetKind.AMULET) continue;
            faces.add(direction);
            boolean nativeAmulet = id.getNamespace().equals("anvilcraft") && path.endsWith("_amulet");
            String name = nativeAmulet ? path.substring(0, path.length() - "_amulet".length()) : id.toString();
            if (nativeAmulet) nativeEffects.add(name);
            ForgingData data = item.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
            int highest = data == null ? 0 : Math.max(data.anvilLevel(), data.grindstoneLevel());
            levels.merge(name, highest, Math::max);
            if (faces.size() == 4) break;
        }
        return new Snapshot(Set.copyOf(filterAllowed(expand(nativeEffects))),
            levels.values().stream().mapToInt(Integer::intValue).sum());
    }

    public static int totalLevel(ServerLevel level, BlockPos pos) {
        return scan(level, pos).totalLevel();
    }

    public static Set<String> collect(ServerLevel level, BlockPos pos) {
        return scan(level, pos).effects();
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
            .getOptional(Identifier.fromNamespaceAndPath("anvilcraft", name + "_amulet"))
            .map(ItemStack::new)
            .orElse(ItemStack.EMPTY);
    }
}
