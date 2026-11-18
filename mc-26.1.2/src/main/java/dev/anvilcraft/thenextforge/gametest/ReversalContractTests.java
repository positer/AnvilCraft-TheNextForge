package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;


public final class ReversalContractTests {

    public static void durabilityHistory(GameTestHelper helper) {
        ItemStack armour = new ItemStack(Items.DIAMOND_CHESTPLATE);
        int baseline = armour.getMaxDamage();
        ForgeDurability.upgrade(armour, ForgingLine.ANVIL, 1, 1.1);
        ForgeDurability.upgrade(armour, ForgingLine.GRINDSTONE, 1, 0.95);
        ForgeDurability.upgrade(armour, ForgingLine.ANVIL, 2, 1.08);
        ForgingData data = ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 2).withLevel(ForgingLine.GRINDSTONE, 1);
        ForgingRules.write(armour, data);
        EmbedMaterials.applyForRoles(armour, EmbedMaterials.of(new ItemStack(Items.DIAMOND)), 2, helper.getLevel());
        data = data.withEmbed("minecraft:diamond");
        ForgingRules.write(armour, data.withLevel(ForgingLine.ANVIL, 1));
        int embeddedBase = (int) Math.round(baseline * 1.3);
        int expected = (int) Math.round(Math.round(embeddedBase * 1.1) * 0.95);
        helper.assertTrue(armour.getMaxDamage() == expected, "降级只撤回第二级耐久，保留砂轮和嵌入");
        ForgingRules.write(armour, data.withLevel(ForgingLine.ANVIL, 0).withLevel(ForgingLine.GRINDSTONE, 0));
        helper.assertTrue(armour.getMaxDamage() == embeddedBase, "归零仍保留嵌入耐久");
        EmbedRemoval.removeLatest(armour, helper.getLevel());
        helper.assertTrue(armour.getMaxDamage() == baseline, "取出钻石后恢复基底耐久");
        var history = armour.get(ModDataComponents.DURABILITY_HISTORY.get());
        var json = ForgeDurability.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, history).getOrThrow();
        helper.assertTrue(ForgeDurability.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow().equals(history), "历史组件持久化往返");
        helper.succeed();
    }


    public static void repeatedEmbedRemoval(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        var quartz = EmbedMaterials.of(new ItemStack(Items.QUARTZ));
        ForgingData data = ForgingData.EMPTY;
        for (int i = 0; i < 2; i++) {
            // 故意两次都传等级 1，模拟归零后再嵌入。
            EmbedMaterials.applyForRoles(sword, quartz, 1, helper.getLevel());
            data = data.withEmbed(quartz.id());
            sword.set(ModDataComponents.FORGING.get(), data);
        }
        helper.assertTrue(Math.abs(quartzAmount(sword) - 0.4) < 0.0001, "同材料再次嵌入不能覆盖旧条目");
        EmbedRemoval.removeLatest(sword, helper.getLevel());
        helper.assertTrue(Math.abs(quartzAmount(sword) - 0.2) < 0.0001, "只取出最新一份材料");
        EmbedRemoval.removeLatest(sword, helper.getLevel());
        helper.assertTrue(quartzAmount(sword) == 0, "第二次取出后无幽灵属性");

        ItemStack pick = new ItemStack(Items.WOODEN_PICKAXE);
        var original = pick.get(DataComponents.TOOL);
        EmbedMaterials.applyForRoles(pick, EmbedMaterials.of(new ItemStack(Items.DIAMOND)), 1, helper.getLevel());
        pick.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:diamond"));
        EmbedRemoval.removeLatest(pick, helper.getLevel());
        helper.assertTrue(original.equals(pick.get(DataComponents.TOOL)), "取出钻石必须撤回挖掘等级");
        helper.succeed();
    }

    private static double quartzAmount(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().stream()
            .filter(e -> e.modifier().id().getPath().startsWith("embed/quartz/"))
            .mapToDouble(e -> e.modifier().amount()).sum();
    }
}
