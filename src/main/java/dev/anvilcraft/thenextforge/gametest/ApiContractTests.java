package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.api.*;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

@net.neoforged.neoforge.gametest.GameTestHolder(TheNextForge.MOD_ID)
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class ApiContractTests {
    private static final class Access implements ForgingService.StrikeAccess {
        ItemStack saved; int cost; Component message;
        public ItemStack material() { return ItemStack.EMPTY; }
        public RandomSource random() { return RandomSource.create(42); }
        public boolean pay(int value) { cost += value; return true; }
        public void refund(int value) { }
        public boolean onFire() { return false; }
        public void rubyCost() { }
        public void save(ItemStack value) { saved = value.copy(); }
        public void destroy() { saved = ItemStack.EMPTY; }
        public void message(Component value) { message = value; }
    }
    private static TnfDefinitions.Embedding material(double factor, boolean flag, List<TnfDefinitions.Effect> effects) {
        return new TnfDefinitions.Embedding(true, "API 材料", false, Set.of(TargetKind.TOOL, TargetKind.WEAPON), 0x33AAFF,
            Map.of(TargetKind.TOOL, List.of(new TnfDefinitions.AttributeGain("minecraft:attack_damage", "add_value", 1)),
                TargetKind.WEAPON, List.of(new TnfDefinitions.AttributeGain("minecraft:attack_damage", "add_value", 2))),
            factor, 1, flag, 1, false, effects);
    }
    private static TnfDefinitions.UpgradeRule rule(ResourceLocation effect, ForgingLine line, double amount, JsonObject config) {
        return new TnfDefinitions.UpgradeRule(TargetKind.WEAPON, line,
            List.of(new TnfDefinitions.Upgrade(new TnfDefinitions.Effect(effect, config), 100, amount, amount, false)));
    }
    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void mappedTargetsBasesAndRealStrike(GameTestHelper helper) {
        JsonObject config = new JsonObject(); config.addProperty("attribute", "minecraft:attack_damage");
        var pool = new TnfDefinitions.Upgrades(List.of(rule(ResourceLocation.parse("thenextforge:attribute"), ForgingLine.ANVIL, 2, config)));
        try (var target = TnfApi.registerTarget("minecraft:carrot", new TnfDefinitions.Target(true, List.of(TargetKind.WEAPON), List.of(ForgingLine.ANVIL)));
             var base = TnfApi.registerBase("minecraft:dirt", new TnfDefinitions.Base(true, ForgingLine.ANVIL, ForgingMaterial.ROYAL, 0.5, 0, 1));
             var upgrades = TnfApi.registerUpgrades("minecraft:carrot", pool);
             var hammer = TnfApi.registerHammer("minecraft:stick", ForgingMaterial.ROYAL)) {
            ItemStack item = new ItemStack(Items.CARROT);
            helper.assertTrue(TargetKind.of(item) == TargetKind.WEAPON && !TnfApi.canUseLine(item, ForgingLine.GRINDSTONE), "显式对象与强化线必须生效");
            helper.assertTrue(ForgingMaterial.ofHammer(new ItemStack(Items.STICK)) == ForgingMaterial.ROYAL, "附属锤映射");
            helper.setBlock(1, 1, 1, Blocks.DIRT);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
            Access access = new Access();
            var result = ForgingService.settle(access, item, helper.getLevel(), pos, ForgingLine.ANVIL,
                ForgingMaterial.ofAnvilBlock(Blocks.DIRT.defaultBlockState()), ForgingMaterial.BASE);
            helper.assertTrue(result.success() && access.cost == 3, "基底的经验倍率必须进入真实结算");
            helper.assertTrue(access.saved.get(ModDataComponents.FORGING.get()).anvilLevel() == 2, "基底额外等级必须进入真实结算");
            helper.assertTrue(Math.abs(ForgingSummary.groups(access.saved).stream().mapToDouble(ForgingSummary.Group::amount).sum() - 4) < 1e-6,
                "额外两级每级都应通过新效果接口写入 +2");
            var context = new dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext(helper.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(pos.above(2)), null);
            helper.assertTrue(new dev.anvilcraft.thenextforge.recipe.ForgeBasePredicate(ForgingLine.ANVIL).test(context), "自定义普通方块必须被自动配方动态谓词认出");
        }
        try (var externalAmulet = TnfApi.registerTarget("minecraft:feather", new TnfDefinitions.Target(true,
                List.of(TargetKind.AMULET), List.of(ForgingLine.ANVIL)))) {
            BlockPos base = helper.absolutePos(new BlockPos(3, 1, 3));
            helper.getLevel().setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
            ItemStack feather = new ItemStack(Items.FEATHER);
            ForgingRules.write(feather, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 4));
            var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), base.south(), net.minecraft.core.Direction.SOUTH);
            frame.setItem(feather); helper.getLevel().addFreshEntity(frame);
            var scan = Amulets.scan(helper.getLevel(), base);
            helper.assertTrue(scan.totalLevel() == 4 && scan.effects().isEmpty(), "附属护符应贡献等级且不误获原生同名效果");
            frame.discard();
        }
        helper.assertTrue(TargetKind.of(new ItemStack(Items.CARROT)) == null && ForgingMaterial.ofAnvilBlock(Blocks.DIRT.defaultBlockState()) == null, "注销代码默认映射必须恢复原逻辑");
        helper.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void customUpgradeRollbackAndFailure(GameTestHelper helper) {
        var id = ResourceLocation.parse("tnf_test:power");
        var other = ResourceLocation.parse("tnf_other:power");
        try (var one = TnfApi.registerUpgradeEffect(id, (context, json) -> {
                context.addAttribute("power", Attributes.ATTACK_DAMAGE, context.amount(), AttributeModifier.Operation.ADD_VALUE); return "power";
             });
             var two = TnfApi.registerUpgradeEffect(other, (context, json) -> {
                context.addAttribute("power", Attributes.ATTACK_DAMAGE, context.amount(), AttributeModifier.Operation.ADD_VALUE); return "other";
             })) {
            ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
            TnfApi.applyUpgradeRule(item, TargetKind.WEAPON, ForgingLine.ANVIL, 1, RandomSource.create(1), 1, rule(id, ForgingLine.ANVIL, 2, new JsonObject()));
            TnfApi.applyUpgradeRule(item, TargetKind.WEAPON, ForgingLine.ANVIL, 1, RandomSource.create(1), 1, rule(other, ForgingLine.ANVIL, 3, new JsonObject()));
            TnfApi.applyUpgradeRule(item, TargetKind.WEAPON, ForgingLine.GRINDSTONE, 3, RandomSource.create(1), 1, rule(id, ForgingLine.GRINDSTONE, 7, new JsonObject()));
            ForgingRules.write(item, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 1).withLevel(ForgingLine.GRINDSTONE, 2));
            helper.assertTrue(ForgingSummary.groups(item).size() == 2, "同路径不同命名空间不能互相覆盖，砂轮降级只能撤砂轮效果");
            ForgingRules.write(item, ForgingData.EMPTY);
            helper.assertTrue(ForgingSummary.groups(item).isEmpty(), "归零清理全部托管升级效果");
        }
        try (var failing = TnfApi.registerUpgradeEffect(id, (context, json) -> {
            context.addAttribute("half", Attributes.ATTACK_DAMAGE, 99, AttributeModifier.Operation.ADD_VALUE);
            throw new IllegalArgumentException("intentional callback failure");
        })) {
            ItemStack item = new ItemStack(Items.DIAMOND_SWORD);
            String result = TnfApi.applyUpgradeRule(item, TargetKind.WEAPON, ForgingLine.ANVIL, 1, RandomSource.create(1), 1, rule(id, ForgingLine.ANVIL, 2, new JsonObject()));
            helper.assertTrue(ForgingSummary.groups(item).isEmpty() && result.contains("未写入"), "坏回调不能残留半条属性，且必须反馈失败");
        }
        helper.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void embeddingSnapshotAndCallbackState(GameTestHelper helper) {
        var id = ResourceLocation.parse("tnf_test:memory");
        var effect = new TnfDefinitions.Effect(id, new JsonObject());
        TnfApi.EmbedEffect handler = new TnfApi.EmbedEffect() {
            public void apply(TnfApi.EmbedContext context, JsonObject json) {
                context.state().addProperty("damage", context.target().getDamageValue());
                context.target().setDamageValue(99);
                context.addAttribute("bonus", Attributes.LUCK, 5, AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.MAINHAND);
            }
            public void remove(TnfApi.EmbedContext context, JsonObject json) { context.target().setDamageValue(context.state().get("damage").getAsInt()); }
            public String describe(JsonObject json) { return "记忆效果"; }
        };
        try (var callback = TnfApi.registerEmbedEffect(id, handler)) {
            java.util.List<ForgeDataChangedEvent> events = new java.util.ArrayList<>();
            java.util.function.Consumer<ForgeDataChangedEvent> observer = events::add;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ForgeDataChangedEvent.class, observer);
            try {
            ItemStack item = new ItemStack(Items.DIAMOND_AXE); item.setDamageValue(7);
            int original = item.getMaxDamage();
            TnfApi.Registration first = TnfApi.registerEmbedding("minecraft:brick", material(1.3, true, List.of(effect)));
            try {
                ItemStack ingredient = new ItemStack(Items.BRICK, 2);
                helper.assertTrue(TnfApi.tryEmbed(item, ingredient, helper.getLevel()).applied() && ingredient.getCount() == 1, "API 嵌入实际消耗一份材料");
                helper.assertTrue(item.getMaxDamage() == Math.round(original * 1.3), "工具/武器双角色只乘一次耐久");
                helper.assertTrue(item.has(DataComponents.UNBREAKABLE) && item.getDamageValue() == 99, "定义与自定义回调均执行");
                var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
                try { ItemStack.STREAM_CODEC.encode(buffer, item); item = ItemStack.STREAM_CODEC.decode(buffer); }
                finally { buffer.release(); }
            } finally { first.close(); }
            try (var replaced = TnfApi.registerEmbedding("minecraft:brick", material(2, false, List.of()))) {
                ForgeDurability.upgrade(item, ForgingLine.ANVIL, 1, 1.2);
                ForgingRules.write(item, item.get(ModDataComponents.FORGING.get()).withLevel(ForgingLine.ANVIL, 1));
                ItemStack returned = TnfApi.removeLatestEmbedding(item, helper.getLevel());
                helper.assertTrue(returned.is(Items.BRICK) && item.getDamageValue() == 7, "移除使用旧回调状态并返回材料");
                helper.assertTrue(item.getMaxDamage() == Math.round(original * 1.2), "撤回旧耐久快照，保留后来的升级倍率");
                helper.assertTrue(!item.has(DataComponents.UNBREAKABLE) && !item.has(ModDataComponents.EMBED_JOURNAL.get()), "撤回不毁并清空快照");
                helper.assertTrue(item.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().noneMatch(v -> v.modifier().id().getPath().startsWith("embed_api/")), "自定义及定义属性全部撤回");
            }
            helper.assertTrue(events.size() == 3 && events.get(0).before().embeds().isEmpty()
                && events.get(0).after().embeds().size() == 1 && events.get(2).after().embeds().isEmpty(),
                "嵌入、升级、取出均应发出提交后的变更通知");
            } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(observer); }
        }
        helper.succeed();
    }
    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void validationAndEmbedFailure(GameTestHelper helper) {
        helper.assertTrue(TnfDefinitions.Base.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"line\":\"ANVIL\",\"material\":\"BASE\",\"experience_multiplier\":-1}")).error().isPresent(), "拒绝非法基底参数");
        helper.assertTrue(TnfDefinitions.AttributeGain.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"attribute\":\"minecraft:luck\",\"amount\":1,\"operation\":\"oops\"}")).error().isPresent(), "属性 operation 不得静默降级");
        var spec = material(1.3, true, List.of(new TnfDefinitions.Effect(ResourceLocation.parse("missing:effect"), new JsonObject())));
        try (var registration = TnfApi.registerEmbedding("minecraft:brick", spec)) {
            ItemStack item = new ItemStack(Items.DIAMOND_AXE); int maximum = item.getMaxDamage();
            ItemStack ingredient = new ItemStack(Items.BRICK, 2);
            helper.assertTrue(!TnfApi.tryEmbed(item, ingredient, helper.getLevel()).applied(), "缺失附属效果应明确拒绝");
            helper.assertTrue(ingredient.getCount() == 2 && item.getMaxDamage() == maximum && !item.has(DataComponents.UNBREAKABLE)
                && !item.has(ModDataComponents.EMBED_JOURNAL.get()), "错误回调不能扣材料或留下部分效果");
        }
        var glow = new TnfDefinitions.Embedding(true, "替换发光", true, Set.of(TargetKind.TOOL), 0xFFFFFF, Map.of(), 1, 0, false, 0, false, List.of());
        try (var registration = TnfApi.registerEmbedding("minecraft:glow_ink_sac", glow)) {
            var data = ForgingData.EMPTY.withEmbed("minecraft:glow_ink_sac");
            helper.assertTrue(data.hasEmbed("minecraft:glow_ink_sac") && !data.hasActiveEmbed("minecraft:glow_ink_sac"), "覆盖内置事件效果不得破坏原始记录和唯一性判断");
        }
        helper.succeed();
    }
}
