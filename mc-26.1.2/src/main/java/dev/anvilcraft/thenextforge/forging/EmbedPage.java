package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.client.ForgeJeiData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 「嵌入材料」统合页的数据：一页 = 一种材料。
 *
 * <p>刻意不依赖 JEI：这样 GameTest 在没有 JEI 的环境里也能验证页面内容，
 * 插件那边只负责把这里产出的列表交给 JEI。
 */
public record EmbedPage(String materialId, ItemStack material, List<TargetKind> kinds, List<Component> lines) {

    /** 按材料表顺序生成全部页面（跳过标签条���与解析不到物品的 id）。 */
    public static List<EmbedPage> buildAll() {
        List<EmbedPage> pages = new ArrayList<>();
        for (var entry : EmbedMaterials.table().entrySet()) {
            String id = entry.getKey();
            if (id.startsWith("#")) continue;
            ItemStack stack = EmbedMaterials.itemOf(id);
            if (stack.isEmpty()) continue;
            var definition = entry.getValue();
            if (definition == null) continue;
            List<Component> lines = ForgeJeiData.describe(id, definition);
            if (lines.isEmpty()) continue;
            pages.add(new EmbedPage(id, stack, List.copyOf(definition.kinds()), lines));
        }
        return List.copyOf(pages);
    }

    /** 每个类别的示例物品（护符优先用铁砧工艺的护符）；取不到时返回空栈。 */
    public static ItemStack sampleFor(TargetKind kind) {
        return switch (kind) {
            case TOOL -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
            case WEAPON -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            case ARMOR -> new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE);
            case AMULET -> net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "anvilcraft", "armadillo_amulet"))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        };
    }
}
