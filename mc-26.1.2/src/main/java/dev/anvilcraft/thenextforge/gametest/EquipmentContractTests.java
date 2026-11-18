package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.event.*;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;


public final class EquipmentContractTests {

    public static void automationAndPayment(GameTestHelper helper) {
        var royal = BuiltInRegistries.BLOCK.getValue(Identifier.parse("anvilcraft:royal_anvil"));
        helper.setBlock(1, 1, 1, royal);
        helper.setBlock(1, 2, 1, BuiltInRegistries.BLOCK.getValue(Identifier.parse("anvilcraft:fish_tank")));
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var tank = (FishTankBlockEntity) helper.getLevel().getBlockEntity(pos);
        fill(tank, 1000);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 4));
        set(tank, sword);
        helper.assertTrue(AutomationHandler.applyAutomatedHit(helper.getLevel(), pos, royal.defaultBlockState(), RandomSource.create(0)),
            "皇家自动锤必须执行");
        var after = get(tank).getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        helper.assertTrue(after.anvilLevel() == 5 && after.protectionUsed("royal_base_anvil"), "自动化必须消费独立皇家来源");
        helper.assertTrue(amount(tank) == 230, "22 点经验应扣 770 mB");
        // 不足以支付下一锤时，物品与已存经验流体保持原状。
        int before = amount(tank);
        helper.assertTrue(!AutomationHandler.consumeExperience(helper.getLevel(), pos, 1000), "不足时拒绝");
        helper.assertTrue(amount(tank) == before, "支付失败不得吞掉残余经验");
        TheNextForge.LOGGER.info("[TNF-CONTRACT] automation_royal=5 fluid_remaining={} failed_payment_unchanged=true", before);
        helper.succeed();
    }


    public static void carriedAttributesAndCleanup(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        AmuletAttributes.sync(player);
        double before = player.getAttributeValue(Attributes.MAX_HEALTH);
        ItemStack amulet = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("anvilcraft:armadillo_amulet")));
        amulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:emerald"));
        EmbedMaterials.apply(amulet, EmbedMaterials.of(new ItemStack(Items.EMERALD)), TargetKind.AMULET, 1, helper.getLevel());
        player.getInventory().setItem(1, amulet);
        AmuletAttributes.sync(player);
        double expected = before + 3;
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MAX_HEALTH) - expected) < 0.001, "背包护符应提供 +3 生命");
        var first = player.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream()
            .filter(m -> m.id().getPath().startsWith("carried/")).findFirst().orElseThrow();
        AmuletAttributes.sync(player);
        var second = player.getAttribute(Attributes.MAX_HEALTH).getModifiers().stream()
            .filter(m -> m.id().getPath().startsWith("carried/")).findFirst().orElseThrow();
        helper.assertTrue(first == second, "未变化的属性不得删除重加");
        player.getInventory().clearContent();
        AmuletAttributes.sync(player);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MAX_HEALTH) - before) < 0.001, "移除护符必须撤回属性");
        helper.succeed();
    }


    public static void effectStacksAndScope(GameTestHelper helper) {
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:nether_star").withEmbed("anvilcraft:neutronium_ingot"));
        var effects = ForgeItemTicker.combineEffects(ForgeItemTicker.armourEffects(chest));
        var resistance = effects.stream().filter(e -> e.getEffect().equals(net.minecraft.world.effect.MobEffects.RESISTANCE)).findFirst().orElseThrow();
        helper.assertTrue(resistance.getAmplifier() == 3, "抗性 III + I 应为 IV");
        helper.assertTrue(ForgeItemTicker.repairAmount(ForgingData.EMPTY.withEmbed("#minecraft:logs").withEmbed("#minecraft:logs"), false, false, false) == 2,
            "两个原木嵌入每 5 秒修复 2 点");
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:transcendium_nugget"));
        player.getInventory().setItem(1, pick);
        helper.assertTrue(!EmbedPresence.carried(player, "anvilcraft:transcendium_nugget"), "背包工具不能冒充飞行护符");
        player.getInventory().clearContent();
        helper.succeed();
    }


    public static void frameAttachmentAndUniqueness(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlockAndUpdate(base, Blocks.ANVIL.defaultBlockState());
        ItemStack amulet = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("anvilcraft:cat_amulet")));
        amulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 3));
        var one = new ItemFrame(helper.getLevel(), base.south(), Direction.SOUTH);
        one.setItem(amulet);
        helper.getLevel().addFreshEntity(one);
        var two = new ItemFrame(helper.getLevel(), base.north(), Direction.NORTH);
        two.setItem(amulet.copy());
        helper.getLevel().addFreshEntity(two);
        var scan = Amulets.scan(helper.getLevel(), base);
        helper.assertTrue(scan.effects().contains("cat") && scan.totalLevel() == 3, "同名展示框护符不能重复叠等级");
        helper.assertTrue(Amulets.scan(helper.getLevel(), base.east()).effects().isEmpty(), "邻近基底不能借用未附着的展示框");
        one.discard();
        two.discard();
        helper.succeed();
    }

    private static void fill(FishTankBlockEntity tank, int amount) {
        FishTankFluids.insert(tank, new net.neoforged.neoforge.fluids.FluidStack(
            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), amount));
    }
    private static int amount(FishTankBlockEntity tank) { return FishTankFluids.amount(tank); }
    private static void set(FishTankBlockEntity tank, ItemStack stack) { FishTankSlots.set(tank, 8, stack); }
    private static ItemStack get(FishTankBlockEntity tank) { return FishTankSlots.get(tank, 8); }
}
