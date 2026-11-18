package dev.anvilcraft.thenextforge.forging;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Consumer;

/** 文字展示与结算分离：快捷栏只显示结果，长明细保留在聊天记录中。 */
public final class ForgeFeedback {
    private ForgeFeedback() { }

    public static String success(ForgingLine line, int before, int after, int cost,
                                 List<String> upgrades, List<String> sources, String embed) {
        StringBuilder text = new StringBuilder("锻造成功｜")
            .append(line == ForgingLine.ANVIL ? "铁砧" : "砂轮")
            .append(" Lv.").append(before).append(" → ").append(after)
            .append("（+").append(after - before).append("）｜").append(ForgingService.quality(after));
        if (!upgrades.isEmpty()) text.append("\n属性提升：").append(String.join("；", upgrades));
        if (!sources.isEmpty()) text.append("\n触发效果：").append(String.join("；", sources));
        if (!embed.isBlank()) text.append("\n").append(embed);
        return text.append("\n经验消耗：").append(cost).append(" 点").toString();
    }

    public static Component summary(Component message) {
        String headline = message.getString().split("\n", 2)[0];
        ChatFormatting color = headline.startsWith("锻造成功") || headline.startsWith("取出成功") ? ChatFormatting.GREEN
            : headline.contains("已损毁") || headline.contains("不足") || headline.contains("无法")
                || headline.contains("不能") || headline.contains("没有") ? ChatFormatting.RED
            : ChatFormatting.YELLOW;
        return Component.literal(headline).withStyle(color);
    }

    public static void deliver(Component itemName, Component message, boolean creative,
                               Consumer<Component> overlay, Consumer<Component> chat) {
        Component heading = summary(message);
        overlay.accept(heading);
        String[] lines = message.getString().split("\n");
        if (lines.length == 1) return;
        var receipt = Component.literal("[下一锤] ").withStyle(ChatFormatting.GOLD)
            .append(itemName.copy().withStyle(ChatFormatting.WHITE)).append("\n").append(heading);
        for (int i = 1; i < lines.length; i++) {
            String line = creative && lines[i].startsWith("经验消耗：") ? "经验消耗：0 点（创造模式）" : lines[i];
            ChatFormatting color = line.startsWith("属性提升：") ? ChatFormatting.AQUA
                : line.startsWith("未嵌入") || line.startsWith("触发效果：") ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
            receipt.append("\n").append(Component.literal(line).withStyle(color));
        }
        chat.accept(receipt);
    }
}
