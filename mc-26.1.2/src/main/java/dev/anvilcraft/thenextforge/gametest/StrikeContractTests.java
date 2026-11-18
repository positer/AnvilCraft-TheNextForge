package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;


public final class StrikeContractTests {
    private static final class Access implements ForgingService.StrikeAccess {
        ItemStack saved;
        ItemStack material = ItemStack.EMPTY;
        RandomSource rng = RandomSource.create(0);
        int cost;
        int refund;
        boolean destroyed;
        Component feedback;
        public ItemStack material() { return material; }
        public RandomSource random() { return rng; }
        public boolean pay(int value) { cost += value; return true; }
        public void refund(int value) { refund += value; }
        public boolean onFire() { return false; }
        public void rubyCost() { }
        public void save(ItemStack stack) { saved = stack.copy(); }
        public void destroy() { saved = ItemStack.EMPTY; destroyed = true; }
        public void message(Component message) { feedback = message; }
    }

    private static void hit(GameTestHelper helper, Access access, ItemStack item,
                            ForgingMaterial base, ForgingMaterial hammer) {
        ForgingService.settle(access, item, helper.getLevel(),
            helper.absolutePos(new BlockPos(2, 1, 2)), ForgingLine.ANVIL, base, hammer);
    }


    public static void royalAndFrost(GameTestHelper helper) {
        Access a = new Access();
        hit(helper, a, new ItemStack(Items.DIAMOND_SWORD), ForgingMaterial.ROYAL, ForgingMaterial.ROYAL);
        helper.assertTrue(data(a).anvilLevel() == 1, "皇家基底保一次成功");
        helper.assertTrue(data(a).protectionUsed("royal_base_anvil"), "保存基底来源");
        helper.assertTrue(!data(a).protectionUsed("royal_hammer_anvil"), "皇家锤来源未耗");
        hit(helper, a, a.saved.copy(), ForgingMaterial.ROYAL, ForgingMaterial.ROYAL);
        helper.assertTrue(data(a).anvilLevel() == 2 && data(a).protectionUsed("royal_hammer_anvil"), "皇家锤另保一次");
        Access frost = new Access();
        hit(helper, frost, new ItemStack(Items.DIAMOND_SWORD), ForgingMaterial.FROST, ForgingMaterial.FROST);
        helper.assertTrue(frost.cost == 2, "双浮霜按 6*0.25 四舍五入为 2");
        TheNextForge.LOGGER.info("[TNF-CONTRACT] royal_levels=2 double_frost_cost={}", frost.cost);
        helper.succeed();
    }


    public static void emberAndDualRoles(GameTestHelper helper) {
        Access a = new Access();
        long seed = 0;
        for (;; seed++) {
            RandomSource test = RandomSource.create(seed);
            if (test.nextDouble() < 0.9 && test.nextDouble() < 0.5 && test.nextDouble() < 0.5) break;
        }
        // 工具升级本身也消耗随机值，因此独立验证材质追加后每级都有修饰符。
        a.rng = RandomSource.create(seed);
        hit(helper, a, new ItemStack(Items.DIAMOND_AXE), ForgingMaterial.ROYAL, ForgingMaterial.EMBER);
        var modifiers = a.saved.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        helper.assertTrue(modifiers.modifiers().stream().anyMatch(e -> e.modifier().id().getPath().contains("tool_mining")),
            "斧头必须获得工具升级");
        helper.assertTrue(modifiers.modifiers().stream().anyMatch(e -> e.modifier().id().getPath().contains("weapon_")),
            "斧头必须同时获得武器升级");
        boolean doubleEmberSeen = false;
        for (int trial = 0; trial < 128 && !doubleEmberSeen; trial++) {
            Access ember = new Access();
            ember.rng = RandomSource.create(trial);
            hit(helper, ember, new ItemStack(Items.DIAMOND_PICKAXE), ForgingMaterial.EMBER, ForgingMaterial.EMBER);
            if (!ember.destroyed && data(ember).anvilLevel() == 3) {
                doubleEmberSeen = true;
                helper.assertTrue(ember.feedback.getString().startsWith("锻造成功｜铁砧 Lv.0 → 3（+3）｜普通"), "反馈必须显示余烬结算后的最终等级");
                helper.assertTrue(ember.feedback.getString().contains("余烬 +2 级"), "余烬追加来源合并显示");
                String attributes = ember.feedback.getString().split("\n")[1];
                helper.assertTrue(attributes.split("；").length == 3, "每个实际提升等级的属性均须保留");
                helper.assertTrue(ember.cost == 6, "余烬额外两级不能另扣经验");
                helper.assertTrue(ForgingSummary.groups(ember.saved).stream().anyMatch(g ->
                    g.key().equals("tool_mining") && g.entries() == 3), "额外每一级必须有属性");
            }
        }
        helper.assertTrue(doubleEmberSeen, "两侧余烬必须允许额外 +2");
        helper.succeed();
    }


    public static void failureLedgerAndSeal(GameTestHelper helper) {
        Access a = new Access();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 20).withXpSpent(100).withCatStreak(2));
        // 找到失败且无损毁的种子，验证失败花费也写回物品。
        long seed = 0;
        for (;; seed++) {
            RandomSource test = RandomSource.create(seed);
            if (test.nextDouble() >= 0.3 && test.nextDouble() >= 0.5) break;
        }
        a.rng = RandomSource.create(seed);
        hit(helper, a, sword, ForgingMaterial.BASE, ForgingMaterial.BASE);
        helper.assertTrue(data(a).anvilLevel() == 20, "无损失败不改等级");
        helper.assertTrue(data(a).xpSpent() == 100 + a.cost, "失败经验必须入账");
        helper.assertTrue(data(a).catStreak() == 0, "失败必须断开连续，不受当前是否挂猫护符影响");
        a.cost = 0;
        ItemStack locked = a.saved.copy();
        locked.set(ModDataComponents.FORGING.get(), data(a).withLocked());
        hit(helper, a, locked, ForgingMaterial.ROYAL, ForgingMaterial.ROYAL);
        helper.assertTrue(a.cost == 0, "封印对象不可扣款");
        helper.succeed();
    }


    public static void dogRefundAcrossOutcomes(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState());
        var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), pos.south(), net.minecraft.core.Direction.SOUTH);
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
            net.minecraft.resources.Identifier.parse("anvilcraft:dog_amulet")).orElseThrow();
        frame.setItem(new ItemStack(item));
        helper.getLevel().addFreshEntity(frame);
        for (int mode = 0; mode < 3; mode++) {
            Access access = new Access();
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            ForgingData data = ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 20).withXpSpent(100);
            if (mode == 2) data = data.withEmbed("anvilcraft:transcendium_nugget");
            sword.set(ModDataComponents.FORGING.get(), data);
            hit(helper, access, sword, mode == 1 ? ForgingMaterial.TRANSCENDENCE : ForgingMaterial.BASE, ForgingMaterial.BASE);
            helper.assertTrue(access.refund == (100 + access.cost) / 2, "损毁 / 降级 / 归零都退已付经验的一半");
            if (!access.destroyed) helper.assertTrue(data(access).xpSpent() == 100 + access.cost - access.refund, "已退经验不能反复退");
        }
        frame.discard();
        helper.succeed();
    }

    public static void feedbackTransport(GameTestHelper helper) {
        Access access = new Access();
        hit(helper, access, new ItemStack(Items.DIAMOND_SWORD), ForgingMaterial.ROYAL, ForgingMaterial.BASE);
        java.util.List<Component> overlays = new java.util.ArrayList<>();
        java.util.List<Component> receipts = new java.util.ArrayList<>();
        ForgeFeedback.deliver(Component.literal("检阅之剑"), access.feedback, false, overlays::add, receipts::add);
        helper.assertTrue(overlays.size() == 1 && !overlays.getFirst().getString().contains("\n"), "快捷栏只发一条单行摘要");
        helper.assertTrue(!overlays.getFirst().getString().contains("属性提升"), "属性明细不得挤占快捷栏");
        helper.assertTrue(receipts.size() == 1 && receipts.getFirst().getString().contains("属性提升：")
            && receipts.getFirst().getString().contains("皇家基底必成") && receipts.getFirst().getString().contains("经验消耗：6 点"), "实际结算属性、来源与支付进入同一份明细");
        receipts.clear();
        ForgeFeedback.deliver(Component.literal("检阅之剑"), access.feedback, true, value -> {}, receipts::add);
        helper.assertTrue(receipts.getFirst().getString().contains("经验消耗：0 点（创造模式）"), "创造模式不能谎报经验支出");
        receipts.clear();
        ForgeFeedback.deliver(Component.empty(), Component.literal("升级失败｜Lv.4 不变"), false, value -> {}, receipts::add);
        helper.assertTrue(receipts.isEmpty(), "普通失败不刷聊天明细");
        ItemStack locked = new ItemStack(Items.DIAMOND_SWORD);
        locked.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLocked());
        Access rejected = new Access();
        hit(helper, rejected, locked, ForgingMaterial.ROYAL, ForgingMaterial.BASE);
        helper.assertTrue(rejected.feedback != null && rejected.feedback.getString().contains("已被黄玉封印") && rejected.cost == 0, "拒绝锤击必须提示且不付费");
        helper.succeed();
    }

    private static ForgingData data(Access a) {
        return a.saved.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
    }
}
