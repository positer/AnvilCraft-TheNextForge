package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.EmbedMaterials;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 把「嵌入材料矩阵」写进 JEI 的物品信息页：每种材料能嵌进哪些对象、对每类对象分别是什么效果。
 *
 * <p>只依赖 JEI 的 API（compileOnly）：玩家装了 JEI 才会加载本类，没装 JEI 时这个类根本不会被引用。
 */
@JeiPlugin
public final class ForgeJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(TheNextForge.MOD_ID, "embed_info");
    }

    @Override
    public void registerCategories(mezz.jei.api.registration.IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new dev.anvilcraft.thenextforge.client.ForgeEmbedCategory(
            registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        // 铁砧/砂轮/铁砧锤点「用途」时能看到这一页
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.item.Items.ANVIL),
            dev.anvilcraft.thenextforge.client.ForgeEmbedCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.item.Items.GRINDSTONE),
            dev.anvilcraft.thenextforge.client.ForgeEmbedCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.item.Items.PAPER),
            dev.anvilcraft.thenextforge.client.ForgeEmbedCategory.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 统合页：所有材料落在同一个 JEI 分类里，靠 JEI 前后翻页浏览；
        // 数据由 EmbedPage.buildAll() 产出（不依赖 JEI，可被 GameTest 校验）
        registration.addRecipes(dev.anvilcraft.thenextforge.client.ForgeEmbedCategory.TYPE,
            dev.anvilcraft.thenextforge.forging.EmbedPage.buildAll());

        // 单物品信息页已取消：全部内容集中在「嵌入材料」统合分类里，避免两套入口
    }

    /**
     * 生成一条材料的 JEI 信息文本（抽成静态方法便于测试，不依赖 JEI 运行时）。
     */
    public static List<Component> describe(String id, EmbedMaterials.Definition definition) {
        List<Component> lines = new ArrayList<>();
        if (definition == null) return lines;
        StringBuilder kinds = new StringBuilder();
        for (TargetKind kind : definition.kinds()) {
            if (kinds.length() > 0) kinds.append(" / ");
            kinds.append(label(kind));
        }
        lines.add(Component.literal("可嵌入：" + kinds));

        // 同一段效果在多个类别下相同时合并成一行，避免长文本把页面撑爆
        java.util.LinkedHashMap<String, List<String>> grouped = new java.util.LinkedHashMap<>();
        for (TargetKind kind : definition.kinds()) {
            String text = EmbedMaterials.bonusText(id, 1, kind);
            if (text == null || text.isBlank()) continue;
            grouped.computeIfAbsent(text, key -> new ArrayList<>()).add(label(kind));
        }
        for (var group : grouped.entrySet()) {
            lines.add(Component.literal("· " + String.join("/", group.getValue()) + "：" + group.getKey()));
        }
        if (definition.unique()) {
            lines.add(Component.literal("· 唯一嵌入：同一件物品只能嵌 1 枚"));
        }
        if (hasExtraSlots(id)) {
            lines.add(Component.literal("· 额外槽位：+" + EmbedMaterials.extraSlots(id)));
        }
        return lines;
    }
    private static boolean hasExtraSlots(String id) {
        return EmbedMaterials.extraSlots(id) > 0;
    }

    private static String label(TargetKind kind) {
        return switch (kind) {
            case TOOL -> "工具";
            case WEAPON -> "武器";
            case ARMOR -> "装甲";
            case AMULET -> "护符";
        };
    }
}
