package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

@net.neoforged.neoforge.gametest.GameTestHolder(TheNextForge.MOD_ID)
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class QualityContractTests {
    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void affixesAndVanillaAnvil(GameTestHelper helper) {
        boolean previous = TheNextForge.CONFIG.experimentalQualityAffixes;
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            var data = ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 4);
            TheNextForge.CONFIG.experimentalQualityAffixes = false;
            ForgeQuality.renameOnUpgrade(sword, data, RandomSource.create(1));
            helper.assertTrue(!sword.has(DataComponents.CUSTOM_NAME), "关闭时不得生成词缀");
            TheNextForge.CONFIG.experimentalQualityAffixes = true;
            Component base = sword.getHoverName().copy();
            // 每档五个词缀，固定种子覆盖全部槽位；名称必须恰好是一层词缀加原始名称。
            int[] levels = {1, 4, 7, 10};
            for (int tier = 0; tier < 4; tier++) {
                boolean[] seen = new boolean[5];
                for (int seed = 0; seed < 128; seed++) {
                    int index = RandomSource.create(seed).nextInt(5);
                    seen[index] = true;
                    data = ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, levels[tier]);
                    ForgeQuality.renameOnUpgrade(sword, data, RandomSource.create(seed));
                    String expected = Component.translatable(ForgeQuality.affixKey(tier, index)).append(base.copy()).getString();
                    helper.assertTrue(expected.equals(sword.getHoverName().getString()), "词缀必须替换且匹配品质");
                }
                for (boolean value : seen) helper.assertTrue(value, "每档必须有五个可选词缀");
            }
            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            AnvilMenu menu = new AnvilMenu(0, player.getInventory());
            menu.getSlot(0).set(sword.copy());
            menu.setItemName("检阅之剑");
            ItemStack renamed = menu.getSlot(2).getItem().copy();
            helper.assertTrue(!renamed.isEmpty() && renamed.getHoverName().getString().equals("检阅之剑"), "真实铁砧菜单必须允许改名");
            TheNextForge.CONFIG.experimentalQualityAffixes = false;
            ForgeQuality.renameOnUpgrade(renamed, data, RandomSource.create(2));
            helper.assertTrue(renamed.getHoverName().getString().equals("检阅之剑"), "关闭后保留铁砧名称");
            TheNextForge.CONFIG.experimentalQualityAffixes = true;
            ItemStack[] saved = {ItemStack.EMPTY};
            var access = new ForgingService.StrikeAccess() {
                public ItemStack material() { return ItemStack.EMPTY; }
                public RandomSource random() { return RandomSource.create(2); }
                public boolean pay(int value) { return true; }
                public void refund(int value) { }
                public boolean onFire() { return false; }
                public void rubyCost() { }
                public void save(ItemStack value) { saved[0] = value.copy(); }
                public void destroy() { }
                public void message(Component value) { }
            };
            var result = ForgingService.settle(access, renamed, helper.getLevel(),
                helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1)),
                ForgingLine.ANVIL, ForgingMaterial.ROYAL, ForgingMaterial.BASE);
            helper.assertTrue(result.success() && saved[0].has(DataComponents.CUSTOM_NAME), "实际升级结算必须保存词缀");
            helper.assertTrue(!renamed.getHoverName().getString().contains("检阅之剑"), "下次升级替换铁砧名称");
            helper.assertTrue(renamed.get(ModDataComponents.AFFIX_BASE_NAME.get()).equals(base), "铁砧不得丢失基础名组件");
            var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            var encoded = ItemStack.CODEC.encodeStart(ops, renamed).getOrThrow();
            var decoded = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            helper.assertTrue(decoded.getHoverName().equals(renamed.getHoverName())
                && decoded.get(ModDataComponents.AFFIX_BASE_NAME.get()).equals(base), "保存读取保留名称和基础名");
        } finally {
            TheNextForge.CONFIG.experimentalQualityAffixes = previous;
        }
        helper.succeed();
    }

    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void qualityGlintRollback(GameTestHelper helper) {
        int[] levels = {0, 3, 4, 6, 7, 9, 10, 100};
        int[] tiers = {0, 0, 1, 1, 2, 2, 3, 3};
        for (int i = 0; i < levels.length; i++) helper.assertTrue(ForgeQuality.tier(levels[i]) == tiers[i], "品质边界");
        for (int original = -1; original <= 1; original++) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            if (original >= 0) sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, original == 1);
            ForgingRules.write(sword, ForgingData.EMPTY.withLevel(ForgingLine.GRINDSTONE, 10));
            helper.assertTrue(Boolean.TRUE.equals(sword.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "传说强制光效");
            ForgingRules.write(sword, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 4));
            helper.assertTrue(Boolean.TRUE.equals(sword.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)), "优秀开始附魔流光");
            ForgingRules.write(sword, ForgingData.EMPTY);
            Boolean after = sword.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            helper.assertTrue(original < 0 ? after == null : Boolean.valueOf(original == 1).equals(after), "归零恢复原有 override，不关闭原生附魔");
        }
        // 模型表面流光跨越所有高品质边界；与实验性名称开关独立。
        ItemStack visual = new ItemStack(Items.DIAMOND_SWORD);
        for (int level : levels) {
            ForgingRules.write(visual, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, level));
            helper.assertTrue(visual.hasFoil() == (level >= 4), "模型流光边界 " + level);
        }
        ForgingRules.write(visual, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 3));
        helper.assertTrue(!visual.hasFoil(), "降回普通时移除品质覆盖光效");
        var mixed = ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 1).withLevel(ForgingLine.GRINDSTONE, 10);
        helper.assertTrue(ForgeQuality.level(mixed) == 10, "双线按最高等级决定品质");
        ItemStack legacy = new ItemStack(Items.DIAMOND_SWORD);
        legacy.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 7));
        legacy.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        ForgingRules.write(legacy, ForgingData.EMPTY);
        helper.assertTrue(!legacy.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE), "旧版强制光效降级后不能残留");
        helper.succeed();
    }
}
