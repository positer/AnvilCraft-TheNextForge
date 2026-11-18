package dev.anvilcraft.thenextforge.forging;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** 两条强化线取最高等级决定品质；命名仅在实际升级时重掷，外观在任何等级写回时刷新。 */
public final class ForgeQuality {
    public static final int AFFIX_COUNT = 5;
    private static final ChatFormatting[] COLORS = {
        ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.AQUA, ChatFormatting.GOLD
    };
    private static final Rarity[] RARITIES = {Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};

    private ForgeQuality() { }

    public static int tier(int level) {
        return level >= 10 ? 3 : level >= 7 ? 2 : level >= 4 ? 1 : 0;
    }

    public static int level(ForgingData data) {
        return Math.max(data.anvilLevel(), data.grindstoneLevel());
    }

    public static String affixKey(int tier, int index) {
        return "thenextforge.affix." + tier + "." + index;
    }

    /** CUSTOM_NAME 是原生铁砧改名通道；不拦截铁砧，下一次升级才替换它。 */
    public static void renameOnUpgrade(ItemStack stack, ForgingData data, RandomSource random) {
        if (TheNextForge.CONFIG == null || !TheNextForge.CONFIG.experimentalQualityAffixes) return;
        Component base = stack.get(ModDataComponents.AFFIX_BASE_NAME.get());
        if (base == null) {
            base = stack.getOrDefault(DataComponents.ITEM_NAME, stack.getItem().getName(stack)).copy();
            stack.set(ModDataComponents.AFFIX_BASE_NAME.get(), base);
        }
        int tier = tier(level(data));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable(affixKey(tier, random.nextInt(AFFIX_COUNT)))
            .append(base.copy()).withStyle(COLORS[tier]));
    }

    /** 保留进入 TNF 前的光效 override：降级不能误关原版附魔，也不能留下高级光效。 */
    public static void refreshAppearance(ItemStack stack, ForgingData data) {
        ForgingData before = stack.get(ModDataComponents.FORGING.get());
        if (level(data) == 0 && (before == null || level(before) == 0)) return;
        if (!stack.has(ModDataComponents.ORIGINAL_GLINT.get())) {
            Boolean original = stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            // 旧版从 7 级起写入 true，未保存来源；回退时移除它，原生附魔仍自行发光。
            if (before != null && level(before) >= 7 && Boolean.TRUE.equals(original)) original = null;
            stack.set(ModDataComponents.ORIGINAL_GLINT.get(), original == null ? -1 : original ? 1 : 0);
        }
        int tier = tier(level(data));
        stack.set(DataComponents.RARITY, RARITIES[tier]);
        if (tier >= 1) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            int original = stack.getOrDefault(ModDataComponents.ORIGINAL_GLINT.get(), -1);
            if (original < 0) stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            else stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, original == 1);
        }
    }
}
