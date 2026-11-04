package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.forging.ForgingSummary;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 锻造相关 tooltip：嵌入材料行，以及按属性求和后的锻造加成。
 *
 * <p>原版为每一条属性修饰符单独打印一行，而锻造每成功一级就写入一条，
 * 因此这里先摘掉原版为锻造条目打印的行，再按属性键给出求和后的最终加成。
 */
@EventBusSubscriber(modid = TheNextForge.MOD_ID, value = Dist.CLIENT)
public final class ForgeTooltip {
    /** 属性键 → 显示名与所属强化线（anvil / grindstone）。 */
    private static final Map<String, String[]> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("weapon_attack", new String[]{"攻击力", "anvil"});
        LABELS.put("tool_mining", new String[]{"挖掘速度", "anvil"});
        LABELS.put("armor_value", new String[]{"护甲", "anvil"});
        LABELS.put("armor_health", new String[]{"生命上限", "anvil"});
        LABELS.put("weapon_cooldown", new String[]{"攻击冷却", "grindstone"});
        LABELS.put("armor_speed", new String[]{"移速", "grindstone"});
        LABELS.put("amulet_damage", new String[]{"伤害", "anvil"});
        LABELS.put("weapon_reach", new String[]{"实体触及", "anvil"});
        LABELS.put("weapon_sweep", new String[]{"横扫伤害", "grindstone"});
    }

    private ForgeTooltip() {
    }

    /** 嵌入材料行：常显材料名（材料主体色），按住 Shift 时追加效果说明。 */
    private static void appendEmbeds(ItemTooltipEvent event, ForgingData data, ItemStack stack) {
        if (data.embeds().isEmpty()) return;
        // 26.1.2：Screen.hasShiftDown() 已移除，改查按键映射
        boolean shift = net.minecraft.client.Minecraft.getInstance().options.keyShift.isDown();
        event.getToolTip().add(Component.literal("嵌入材料").withStyle(ChatFormatting.DARK_GRAY));
        for (String id : data.embeds()) {
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.byId(id);
            if (definition == null) continue;
            event.getToolTip().add(
                Component.literal("  " + dev.anvilcraft.thenextforge.forging.EmbedMaterials.propertyOf(definition.label()))
                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(definition.color()))
            );
            if (shift) {
                event.getToolTip().add(
                        Component.literal("    " + dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusTextFor(stack, id, data.anvilLevel()))
                        .withStyle(ChatFormatting.GRAY)
                );
            }
        }
        if (!shift) {
            event.getToolTip().add(Component.literal("按住 Shift 查看效果").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ForgingData data = stack.get(ModDataComponents.FORGING.get());
        if (data == null) return;
        List<Component> tip = event.getToolTip();
        // 先摘掉原版逐条打印的锻造行，再追加本模组自己的汇总段
        stripForgeModifierLines(tip, stack);
        appendEmbeds(event, data, stack);

        Map<String, Double> anvil = new LinkedHashMap<>();
        Map<String, Double> grindstone = new LinkedHashMap<>();
        Map<String, Boolean> flat = new LinkedHashMap<>();
        for (ForgingSummary.Group group : ForgingSummary.groups(stack)) {
            String[] label = LABELS.get(group.key());
            if (label == null) continue;
            Map<String, Double> bucket = "grindstone".equals(label[1]) ? grindstone : anvil;
            bucket.merge(group.key(), group.amount(), Double::sum);
            flat.put(group.key(), group.flat());
        }

        tip.add(Component.empty());
        tip.add(Component.literal("锻造").withStyle(ChatFormatting.GOLD));
        if (data.anvilLevel() > 0) {
            tip.add(line("铁砧 Lv." + data.anvilLevel(), anvil, flat));
        }
        if (data.grindstoneLevel() > 0) {
            tip.add(line("砂轮 Lv." + data.grindstoneLevel(), grindstone, flat));
        }
    }

    /**
     * 摘掉原版为每条锻造修饰符单独打印的行，以及因此变空的「主手时」类表头。
     *
     * <p>只按原版同一套 key 与格式重算文本再逐个匹配，匹配不到就不动，
     * 因此不会误删物品自身的原生属性行。
     */
    private static void stripForgeModifierLines(List<Component> tip, ItemStack stack) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY
        );
        // 26.1.2：ItemAttributeModifiers 不再有 showInTooltip；可见性由 TooltipDisplay 组件控制，这里直接显示
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!ForgingSummary.isForgeModifier(entry.modifier().id())) continue;
            String text = vanillaLine(entry);
            for (int i = tip.size() - 1; i >= 0; i--) {
                if (text.equals(tip.get(i).getString())) {
                    tip.remove(i);
                    break;
                }
            }
        }
        for (int i = tip.size() - 1; i >= 0; i--) {
            if (isSlotHeader(tip.get(i)) && !hasModifierLineAfter(tip, i)) tip.remove(i);
        }
    }

    /** 原版 {@code ItemStack#addModifierTooltip} 对这条修饰符会打印的文本。 */
    private static String vanillaLine(ItemAttributeModifiers.Entry entry) {
        AttributeModifier modifier = entry.modifier();
        double amount = modifier.amount();
        double display = switch (modifier.operation()) {
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> amount * 100.0;
            default -> amount;
        };
        String formatted = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(display));
        String key = (amount >= 0 ? "attribute.modifier.plus." : "attribute.modifier.take.")
            + modifier.operation().id();
        return Component.translatable(key, formatted,
            Component.translatable(entry.attribute().value().getDescriptionId())).getString();
    }

    private static boolean isSlotHeader(Component line) {
        return line.getContents() instanceof TranslatableContents contents
            && contents.getKey().startsWith("item.modifiers.");
    }

    private static boolean hasModifierLineAfter(List<Component> tip, int headerIndex) {
        for (int i = headerIndex + 1; i < tip.size(); i++) {
            if (isSlotHeader(tip.get(i))) return false;
            if (!tip.get(i).getString().isBlank()) return true;
        }
        return false;
    }

    private static Component line(String title, Map<String, Double> values, Map<String, Boolean> flat) {
        String stats = summarize(values, flat);
        return Component.literal(" " + title + "  ").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(stats).withStyle(ChatFormatting.AQUA));
    }

    private static String summarize(Map<String, Double> values, Map<String, Boolean> flat) {
        if (values.isEmpty()) return "（无数值加成）";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : values.entrySet()) {
            if (sb.length() > 0) sb.append("、");
            String[] label = LABELS.get(e.getKey());
            sb.append(label == null ? e.getKey() : label[0]);
            sb.append(Boolean.TRUE.equals(flat.get(e.getKey()))
                ? String.format(" +%s", format(e.getValue()))
                : String.format(" +%.1f%%", e.getValue() * 100.0));
        }
        return sb.toString();
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }
}
