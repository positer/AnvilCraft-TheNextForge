package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.event.ForgeItemTicker;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 有界服务端探针；耗时只代表该处理器，不代表整体 MSPT。 */
@net.neoforged.neoforge.gametest.GameTestHolder(TheNextForge.MOD_ID)
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class TickerContractTests {
    @net.minecraft.gametest.framework.GameTest(template = "empty")
    public static void tickerProbe(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("#minecraft:logs"));
        tool.setDamageValue(1000);
        player.setItemSlot(EquipmentSlot.MAINHAND, tool);
        int originalTick = player.tickCount;
        try {
            for (int tick = 1; tick <= 100; tick++) {
                player.tickCount = tick;
                ForgeItemTicker.onPlayerTick(new PlayerTickEvent.Post(player));
            }
            int repaired = 1000 - tool.getDamageValue();
            TheNextForge.LOGGER.info("[TNF-TICKER] repair_per_100_ticks={}", repaired);
            helper.assertTrue(repaired == 1, "原木主手每 100 tick 必须只修复 1 点，实际 " + repaired);
            ItemStack offhand = tool.copy();
            tool.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:topaz"));
            offhand.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:topaz"));
            player.setItemSlot(EquipmentSlot.OFFHAND, offhand);
            int[] queries = {0};
            ForgeItemTicker.repairEquipped(player, () -> { queries[0]++; return true; });
            helper.assertTrue(queries[0] == 1, "两件黄玉工具同轮只查一次电网");
            helper.assertTrue(offhand.getDamageValue() == 997, "副手黄玉只恢复 2 点");
            tool.setDamageValue(0);
            offhand.setDamageValue(0);
            queries[0] = 0;
            ForgeItemTicker.repairEquipped(player, () -> { queries[0]++; return true; });
            helper.assertTrue(queries[0] == 0, "满耐久不查询电网");
            tool.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("#minecraft:logs"));
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            // 无副作用的满耐久工具，测量真实生产事件入口，先预热后取 5 个窗口。
            tool.setDamageValue(0);
            player.tickCount = 400;
            for (int i = 0; i < 2000; i++) ForgeItemTicker.onPlayerTick(new PlayerTickEvent.Post(player));
            for (int window = 0; window < 5; window++) {
                long started = System.nanoTime();
                for (int i = 0; i < 10000; i++) ForgeItemTicker.onPlayerTick(new PlayerTickEvent.Post(player));
                TheNextForge.LOGGER.info("[TNF-PERF] ticker_ns_per_call window={} value={}",
                    window, (System.nanoTime() - started) / 10000);
            }
        } finally {
            player.tickCount = originalTick;
            player.getInventory().clearContent();
        }
        helper.succeed();
    }
}
