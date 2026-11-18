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
        LABELS.put("weapon_attack", new String[]{"thenextforge.tooltip.stat.weapon_attack", "anvil"});
        LABELS.put("tool_mining", new String[]{"thenextforge.tooltip.stat.tool_mining", "anvil"});
        LABELS.put("armor_value", new String[]{"thenextforge.tooltip.stat.armor_value", "anvil"});
        LABELS.put("armor_health", new String[]{"thenextforge.tooltip.stat.armor_health", "anvil"});
        LABELS.put("weapon_cooldown", new String[]{"thenextforge.tooltip.stat.weapon_cooldown", "grindstone"});
        LABELS.put("armor_speed", new String[]{"thenextforge.tooltip.stat.armor_speed", "grindstone"});
        LABELS.put("amulet_damage", new String[]{"thenextforge.tooltip.stat.amulet_damage", "anvil"});
        LABELS.put("weapon_reach", new String[]{"thenextforge.tooltip.stat.weapon_reach", "anvil"});
        LABELS.put("weapon_sweep", new String[]{"thenextforge.tooltip.stat.weapon_sweep", "grindstone"});
    }

    private ForgeTooltip() {
    }

    /** 嵌入材料行：常显材料名（材料主体色），按住 Shift 时追加效果说明。 */
    private static void appendEmbeds(ItemTooltipEvent event, ForgingData data, ItemStack stack) {
        if (data.embeds().isEmpty()) return;
        // 26.1.2：Screen.hasShiftDown() 已移除。可选的三个来源里只有第三个成立：
        //   1) event.getFlags().hasShiftDown() —— TooltipFlagExtension 的默认实现直接 return false，
        //      TooltipFlag.Default 没有覆盖它，所以恒为 false（javap 核对过）；
        //   2) options.keyShift.isDown() —— 这是游戏内按键映射：屏幕接管按键时 KeyboardHandler 会把它复位成 false
        //      （见 KeyboardHandler#keyPress 在 Screen#keyPressed 返回 true 后调用 KeyMapping.set(key,false)），
        //      打开背包按 Shift 永远是 false，正是「按住 Shift 不展开」的原因；
        //   3) Minecraft#hasShiftDown() —— 直接轮询 GLFW 的 340/344，与屏幕无关，等价于旧版 Screen.hasShiftDown()。
        boolean shift = net.minecraft.client.Minecraft.getInstance().hasShiftDown();
        event.getToolTip().add(Component.translatable("thenextforge.tooltip.embeddings").withStyle(ChatFormatting.DARK_GRAY));
        for (String id : data.embeds()) {
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.byId(id);
            if (definition == null) {
                String frozen = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.describeAppliedLocalized(stack, id);
                event.getToolTip().add(Component.literal("  " + id + (shift && !frozen.isBlank() ? "：" + frozen : "")).withStyle(ChatFormatting.GRAY));
                continue;
            }
            event.getToolTip().add(
                Component.literal("  ").append(EmbedTooltipText.property(id, definition))
                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(definition.color()))
            );
            if (shift) {
                event.getToolTip().add(
                        Component.literal("    " + EmbedTooltipText.bonus(stack, id, data.anvilLevel()))
                        .withStyle(ChatFormatting.GRAY)
                );
            }
        }
        if (!shift) {
            event.getToolTip().add(Component.translatable("thenextforge.tooltip.shift").withStyle(ChatFormatting.DARK_GRAY));
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
        tip.add(Component.translatable("thenextforge.tooltip.forging").withStyle(ChatFormatting.GOLD));
        if (data.anvilLevel() > 0) {
            tip.add(line(Component.translatable("thenextforge.tooltip.anvil", data.anvilLevel()).getString(), anvil, flat));
        }
        if (data.grindstoneLevel() > 0) {
            tip.add(line(Component.translatable("thenextforge.tooltip.grindstone", data.grindstoneLevel()).getString(), grindstone, flat));
        }
    }

    /**
     * 摘掉原版为每条锻造修饰符单独打印的行，以及因此变空的「主手时」类表头。
     *
     * <p>只按原版同一套 key 与格式重算文本再逐个匹配，匹配不到就不动，
     * 因此不会误删物品自身的原生属性行。
     */
    private static void stripForgeModifierLines(List<Component> tip, ItemStack stack) {
        // 第三方属性格式化器会聚合原生属性；不按文本删除它的合计行。
        if (net.neoforged.fml.ModList.get().isLoaded("apothic_attributes")) return;
        ItemAttributeModifiers modifiers = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY
        );
        // 26.1.2：ItemAttributeModifiers 不再有 showInTooltip；可见性由 TooltipDisplay 组件控制，这里直接显示
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!ForgingSummary.isForgeModifier(entry.modifier().id()) || !LABELS.containsKey(ForgingSummary.keyOf(entry.modifier().id()))) continue;
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
        if (values.isEmpty()) return Component.translatable("thenextforge.tooltip.none").getString();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : values.entrySet()) {
            if (sb.length() > 0) sb.append(Component.translatable("thenextforge.tooltip.separator").getString());
            String[] label = LABELS.get(e.getKey());
            sb.append(label == null ? e.getKey() : Component.translatable(label[0]).getString());
            sb.append(Boolean.TRUE.equals(flat.get(e.getKey()))
                ? String.format(java.util.Locale.ROOT, " +%s", format(e.getValue()))
                : String.format(java.util.Locale.ROOT, " +%.1f%%", e.getValue() * 100.0));
        }
        return sb.toString();
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
