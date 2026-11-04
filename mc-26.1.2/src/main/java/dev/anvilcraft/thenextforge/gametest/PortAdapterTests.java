package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.event.FishTankFluids;
import dev.anvilcraft.thenextforge.event.FishTankSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * 26.1.2 专属：Transfer API 适配层的往返测试。
 *
 * <p>这些适配层（`FishTankSlots` / `FishTankFluids`）是移植时新写的代码，
 * 1.21.1 没有对应实现，因此 1.21.1 的测试集覆盖不到它们——这里单独补。
 */
public final class PortAdapterTests {
    private PortAdapterTests() {
    }

    public static void registerAll(RegisterGameTestsEvent registerEvent) {
        TestRegistry.register(registerEvent, "fish_tank_slots_round_trip", helper -> {
            if (!placeTank(helper)) return;
            var tank = tankAt(helper);
            FishTankSlots.set(tank, 8, new ItemStack(Items.DIAMOND, 7));
            ItemStack read = FishTankSlots.get(tank, 8);
            helper.assertTrue(read.is(Items.DIAMOND), "读回的材料必须仍是钻石，实际 " + read);
            helper.assertTrue(read.getCount() == 7, "数量必须保持 7，实际 " + read.getCount());
            FishTankSlots.set(tank, 8, ItemStack.EMPTY);
            helper.assertTrue(FishTankSlots.get(tank, 8).isEmpty(), "写空必须清空槽位");
            helper.succeed();
        });

        TestRegistry.register(registerEvent, "fish_tank_fluids_set_and_amount", helper -> {
            if (!placeTank(helper)) return;
            var tank = tankAt(helper);
            FishTankFluids.set(tank, new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 12_345));
            helper.assertTrue(FishTankFluids.amount(tank) == 12_345,
                "set 之后 amount 必须等于设定值，实际 " + FishTankFluids.amount(tank));
            helper.assertTrue(FishTankFluids.isExperience(tank), "内容必须是经验流体");
            helper.succeed();
        });

        TestRegistry.register(registerEvent, "fish_tank_output_insert_returns_leftover", helper -> {
            if (!placeTank(helper)) return;
            var tank = tankAt(helper);
            ItemStack leftover = FishTankSlots.insertOutput(tank, new ItemStack(Items.REDSTONE, 3));
            helper.assertTrue(leftover.isEmpty() || leftover.getCount() <= 3,
                "剩余量不能超过投入量，实际 " + leftover);
            helper.succeed();
        });
    }

    /** 放一个鱼缸到 (1,1,1)；模组缺失或方块实体没就位时直接跳过。 */
    private static boolean placeTank(GameTestHelper helper) {
        var fishTank = BuiltInRegistries.BLOCK
            .getOptional(Identifier.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return false; }
        helper.setBlock(1, 1, 1, fishTank);
        if (tankAt(helper) == null) { helper.succeed(); return false; }
        return true;
    }

    private static dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tankAt(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        return helper.getLevel().getBlockEntity(pos)
            instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank ? tank : null;
    }
}
