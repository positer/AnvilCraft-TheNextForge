package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.forging.EmbedMaterials;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 信息文本的数据层（不依赖任何 JEI 渲染 API）。
 *
 * <p>26.1.2 的 GUI 渲染体系换了（没有 {@code GuiGraphics}），JEI 的 category 绘制必须重写；
 * 但「每种材料该显示什么文字」是纯数据，先拆出来让测试与未来的 category 共用。
 */
public final class ForgeJeiData {
    private ForgeJeiData() {
    }

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

    private static String label(TargetKind kind) {
    return switch (kind) {
    case TOOL -> "工具";
    case WEAPON -> "武器";
    case ARMOR -> "装甲";
    case AMULET -> "护符";
    };
    }

    private static boolean hasExtraSlots(String id) {
    return EmbedMaterials.extraSlots(id) > 0;
    }
}
