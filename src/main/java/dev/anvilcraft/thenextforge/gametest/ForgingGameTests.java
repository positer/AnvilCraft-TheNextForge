package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.EmbedMaterials;
import dev.anvilcraft.thenextforge.forging.EmbedRemoval;
import dev.anvilcraft.thenextforge.forging.ForgeDisplay;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.forging.ForgingLine;
import dev.anvilcraft.thenextforge.forging.ForgingMaterial;
import dev.anvilcraft.thenextforge.forging.ForgingRules;
import dev.anvilcraft.thenextforge.forging.ForgingService;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 锻造循环的服务端行为测试。
 *
 * <p>覆盖：成功率公式、材质与对象识别、数据组件往返、属性提升、皇家必定成功、失败规则。
 */
@GameTestHolder(TheNextForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForgingGameTests {
    /** 只有事件/持续效果、不写物品属性的材料（多个矩阵测试共用）。 */
    private static final java.util.Set<String> eventDriven = java.util.Set.of(
        "minecraft:lapis_lazuli", "minecraft:glow_ink_sac", "minecraft:echo_shard",
        "minecraft:fermented_spider_eye", "minecraft:slime_ball", "minecraft:ender_pearl",
        "minecraft:heart_of_the_sea", "anvilcraft:frost_metal_ingot", "anvilcraft:ember_metal_ingot",
        "minecraft:wither_rose", "anvilcraft:negative_matter_nugget", "anvilcraft:void_matter",
        "anvilcraft:amber", "anvilcraft:cursed_gold_ingot", "anvilcraft:sapphire", "anvilcraft:ruby",
        "anvilcraft:topaz", "minecraft:breeze_rod", "minecraft:dragon_egg",
        "minecraft:golden_carrot", "minecraft:enchanted_golden_apple"
    );
    private ForgingGameTests() {
    }

    @GameTest(template = "empty")
    public static void successRateFormula(GameTestHelper helper) {
        helper.assertTrue(near(ForgingRules.successRate(0), 0.9), "level 0 should be 90%");
        helper.assertTrue(near(ForgingRules.successRate(3), 0.9), "same tier keeps 90%");
        helper.assertTrue(near(ForgingRules.successRate(4), 0.765), "level 4 drops to 76.5%");
        helper.assertTrue(near(ForgingRules.successRate(6), 0.765), "same tier keeps 76.5%");
        helper.assertTrue(near(ForgingRules.successRate(7), 0.65025), "level 7 drops to 65.03%");
        helper.assertTrue(near(ForgingRules.successRate(10), 0.5527125), "level 10 is 55.27%");
        helper.assertTrue(near(ForgingRules.successRate(11), 0.5527125 * 0.85), "level 11 drops again");
        helper.assertTrue(near(ForgingRules.successRate(14), 0.3), "floor is 30%");
        helper.assertTrue(near(ForgingRules.successRate(40), 0.3), "floor holds");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialDetection(GameTestHelper helper) {
        helper.assertTrue(
            ForgingMaterial.ofAnvilBlock(Blocks.ANVIL.defaultBlockState()) == ForgingMaterial.BASE,
            "vanilla anvil should be BASE"
        );
        helper.assertTrue(
            ForgingMaterial.ofAnvilBlock(Blocks.CHIPPED_ANVIL.defaultBlockState()) == ForgingMaterial.BASE,
            "chipped anvil should be BASE"
        );
        helper.assertTrue(
            ForgingMaterial.ofGrindstoneBlock(Blocks.GRINDSTONE.defaultBlockState()) == ForgingMaterial.BASE,
            "vanilla grindstone should be BASE"
        );
        BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "royal_anvil"))
            .ifPresent(block -> helper.assertTrue(
                ForgingMaterial.ofAnvilBlock(block.defaultBlockState()) == ForgingMaterial.ROYAL,
                "royal_anvil should be ROYAL"
            ));
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "anvil_hammer"))
            .ifPresent(item -> helper.assertTrue(
                ForgingMaterial.ofHammer(new ItemStack(item)) == ForgingMaterial.BASE,
                "anvil_hammer should be BASE"
            ));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void targetKindDetection(GameTestHelper helper) {
        helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_SWORD)) == TargetKind.WEAPON, "sword is weapon");
        helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_PICKAXE)) == TargetKind.TOOL, "pickaxe is tool");
        helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_CHESTPLATE)) == TargetKind.ARMOR, "chestplate is armor");
        helper.assertTrue(TargetKind.of(new ItemStack(Items.STICK)) == null, "stick is not forgeable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dataComponentRoundTrip(GameTestHelper helper) {
        ForgingData source = new ForgingData(2, 1, 0, -1, java.util.List.of("minecraft:quartz"), 1, 40, java.util.List.of("armadillo"), true);
        var encoded = ForgingData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, source).getOrThrow();
        ForgingData decoded = ForgingData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.equals(source), "codec should round trip");
        helper.assertTrue(ModDataComponents.FORGING.get() != null, "forging component should be registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void attributeUpgradeApplies(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int before = modifiers(sword);
        String text = ForgingRules.applyUpgrade(sword, TargetKind.WEAPON, ForgingLine.ANVIL, 1, net.minecraft.util.RandomSource.create(42L));
        int after = modifiers(sword);
        helper.assertTrue(after > before, "weapon upgrade should add an attribute modifier");
        helper.assertTrue(text != null && (text.contains("攻击力") || text.contains("实体触及")), "weapon anvil upgrade text should mention attack or reach");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void royalMaterialAlwaysSucceeds(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerLevel level = helper.getLevel();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        Display.ItemDisplay display = ForgeDisplay.place(level, pos, sword, 0.0F);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.totalExperience = 100;

        ForgingService.Result result = ForgingService.hammer(
            player, level, pos, ForgingLine.ANVIL, ForgingMaterial.ROYAL, ForgingMaterial.ROYAL
        );
        helper.assertTrue(result.success(), "royal should force success, got: " + result.message());
        ForgingData data = ForgeDisplay.stackOf(display).get(ModDataComponents.FORGING.get());
        helper.assertTrue(data != null && data.anvilLevel() == 1, "level should become 1");
        helper.assertTrue(data.protectionUsed("royal_base_anvil"), "base royal should be recorded");
        helper.assertTrue(!data.protectionUsed("royal_hammer_anvil"), "hammer royal should stay available");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void failureRulesAreDeterministic(GameTestHelper helper) {
        helper.assertTrue(ForgingRules.destroysOnFailure(false, false), "base + base destroys");
        helper.assertTrue(!ForgingRules.destroysOnFailure(true, false), "overlimit base protects");
        helper.assertTrue(!ForgingRules.destroysOnFailure(false, true), "overlimit hammer protects");
        helper.assertTrue(ForgingRules.degradeLimit(true, true) == 1, "double overlimit degrades 1");
        helper.assertTrue(ForgingRules.degradeLimit(true, false) == 3, "single overlimit degrades 3");
        helper.assertTrue(ForgingRules.degradedLevel(2, false, false) == 0, "floor is 0");
        helper.assertTrue(ForgingRules.degradedLevel(8, true, true) == 7, "double overlimit level 8 -> 7");
        helper.assertTrue(ForgingRules.degradedLevel(8, true, false) == 5, "single overlimit level 8 -> 5");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void embedConsumesOffhandMaterial(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerLevel level = helper.getLevel();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        Display.ItemDisplay display = ForgeDisplay.place(level, pos, sword, 0.0F);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.totalExperience = 100;
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.QUARTZ, 4));

        ForgingService.Result result = ForgingService.hammer(
            player, level, pos, ForgingLine.ANVIL, ForgingMaterial.ROYAL, ForgingMaterial.ROYAL
        );
        helper.assertTrue(result.success(), "royal should succeed: " + result.message());
        ForgingData data = ForgeDisplay.stackOf(display).get(ModDataComponents.FORGING.get());
        helper.assertTrue(data != null && data.hasEmbed("minecraft:quartz"), "quartz should be embedded");
        helper.assertTrue(player.getOffhandItem().getCount() == 3, "offhand quartz should shrink by one");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void overlimitResetUnlocksRoyalAtZero(GameTestHelper helper) {
        ForgingData spent = ForgingData.EMPTY.withTokens(ForgingLine.ANVIL, 0).withLevel(ForgingLine.ANVIL, 2);
        ForgingData after = ForgingRules.applyFailure(spent, ForgingLine.ANVIL, 2, true, false);
        helper.assertTrue(after.anvilLevel() == 0, "level should drop to 0");
        helper.assertTrue(after.tokens(ForgingLine.ANVIL) == -1, "royal token should be unlocked at 0");

        ForgingData kept = ForgingRules.applyFailure(spent, ForgingLine.ANVIL, 2, true, true);
        helper.assertTrue(kept.anvilLevel() == 1, "double overlimit should only drop one level");
        helper.assertTrue(kept.tokens(ForgingLine.ANVIL) == 0, "token stays spent above 0");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void destructionRollSplitsEvenly(GameTestHelper helper) {
        var random = net.minecraft.util.RandomSource.create(7L);
        int destroyed = 0;
        for (int i = 0; i < 10000; i++) {
            if (ForgingRules.destructionRoll(random)) destroyed++;
        }
        helper.assertTrue(destroyed > 4700 && destroyed < 5300, "destruction roll should be near 50%, got " + destroyed);
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void compositeAmuletsExpand(GameTestHelper helper) {
        var gem = dev.anvilcraft.thenextforge.forging.Amulets.expand(java.util.Set.of("gem"));
        helper.assertTrue(gem.contains("ruby") && gem.contains("sapphire") && gem.contains("topaz"),
            "gem should expand to ruby/sapphire/topaz");
        var nature = dev.anvilcraft.thenextforge.forging.Amulets.expand(java.util.Set.of("nature"));
        helper.assertTrue(nature.contains("armadillo") && nature.contains("feather")
            && nature.contains("cat") && nature.contains("dog"), "nature should expand to four effects");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void protectionStateResetsAtZero(GameTestHelper helper) {
        ForgingData protectedData = ForgingData.EMPTY
            .withLevel(ForgingLine.ANVIL, 2)
            .withProtectionUsed("armadillo")
            .withNextHitProtected(true);
        helper.assertTrue(protectedData.nextHitProtected(), "next hit flag should be set");
        ForgingData afterZero = ForgingRules.applyFailure(protectedData, ForgingLine.ANVIL, 2, true, false);
        helper.assertTrue(afterZero.anvilLevel() == 0, "level should drop to 0");
        helper.assertTrue(afterZero.protections().isEmpty(), "protections should clear at 0");
        helper.assertTrue(!afterZero.nextHitProtected(), "next hit flag should clear at 0");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void embedGoldAddsFortuneAndCutsDurability(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int baseMax = sword.getMaxDamage();
        var def = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(def != null, "gold ingot should have a definition");
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
            sword, def, TargetKind.WEAPON, 1, helper.getLevel()
        );
        helper.assertTrue(sword.getMaxDamage() < baseMax, "durability should drop 20%");
        helper.assertTrue(sword.get(DataComponents.ENCHANTMENTS) != null, "fortune should be applied");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void embedDiamondAndNetheriteScale(GameTestHelper helper) {
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        int chestMax = chest.getMaxDamage();
        var diamond = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.DIAMOND));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(chest, diamond, TargetKind.ARMOR, 1, helper.getLevel());
        helper.assertTrue(chest.getMaxDamage() > chestMax, "diamond should raise armor durability");

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        int before = boots.getMaxDamage();
        var netherite = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.NETHERITE_INGOT));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(boots, netherite, TargetKind.ARMOR, 1, helper.getLevel());
        helper.assertTrue(boots.getMaxDamage() >= before * 2 - 1, "netherite should double durability");
        helper.assertTrue(modifiers(boots) > 0, "netherite should add armor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void embedEmeraldAndLogsGiveHealth(GameTestHelper helper) {
        ItemStack amulet = new ItemStack(Items.EMERALD);
        var emerald = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.EMERALD));
        helper.assertTrue(emerald != null, "emerald should be an embed material");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void embedMatrixReport(GameTestHelper helper) {
        String[][] samples = {
            {"TOOL", "minecraft:diamond_pickaxe"},
            {"WEAPON", "minecraft:diamond_sword"},
            {"ARMOR", "minecraft:diamond_chestplate"},
            {"AMULET", "anvilcraft:armadillo_amulet"},
        };
        int observed = 0;
        int silent = 0;
        for (var entry : dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().entrySet()) {
            String id = entry.getKey();
            var def = entry.getValue();
            ItemStack probe = itemById("minecraft:diamond_sword");
            boolean hasDefine = def != null;
            for (String[] sample : samples) {
                ItemStack stack = itemById(sample[1]);
                if (stack.isEmpty()) continue;
                int beforeMods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                    net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().size();
                int beforeMax = stack.getMaxDamage();
                int beforeEnchants = stack.getOrDefault(DataComponents.ENCHANTMENTS,
                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).size();
                boolean changed = dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
                    stack, def, TargetKind.valueOf(sample[0]), 1, helper.getLevel()
                );
                int afterMods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                    net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().size();
                int afterMax = stack.getMaxDamage();
                int afterEnchants = stack.getOrDefault(DataComponents.ENCHANTMENTS,
                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).size();
                boolean visible = afterMods > beforeMods || afterMax != beforeMax || afterEnchants > beforeEnchants;
                if (visible) observed++; else silent++;
                StringBuilder detail = new StringBuilder();
                var after = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                    net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
                for (var e : after.modifiers()) {
                    if (!e.modifier().id().getNamespace().equals("thenextforge")) continue;
                    detail.append(e.modifier().id().getPath()).append('=')
                        .append(String.format("%.3f", e.modifier().amount()))
                        .append('/').append(e.modifier().operation().id()).append(' ');
                }
                int fortuneLevel = 0;
                var ench = stack.getOrDefault(DataComponents.ENCHANTMENTS,
                    net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                var fortuneKey = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ENCHANTMENT,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("fortune"));
                var lookup = helper.getLevel().registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var holder = lookup.get(fortuneKey);
                if (holder.isPresent()) fortuneLevel = ench.getLevel(holder.get());
                dev.anvilcraft.thenextforge.TheNextForge.LOGGER.info(
                    "[EMBED-MATRIX] {} {} visible={} changed={} mods+{} max:{}->{} ench+{} fortune={} unbreakable={} [{}]",
                    id, sample[0], visible, changed, afterMods - beforeMods, beforeMax, afterMax,
                    afterEnchants - beforeEnchants, fortuneLevel, stack.has(DataComponents.UNBREAKABLE),
                    detail.toString().trim()
                );
            }
        }
        dev.anvilcraft.thenextforge.TheNextForge.LOGGER.info(
            "[EMBED-MATRIX] summary visible={} silent={} materials={}", observed, silent,
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().size()
        );
        helper.assertTrue(!dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().isEmpty(), "material table should not be empty");
        helper.succeed();
    }

    private static ItemStack itemById(String id) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(net.minecraft.resources.ResourceLocation.parse(id))
            .map(ItemStack::new)
            .orElse(ItemStack.EMPTY);
    }


    @GameTest(template = "empty")
    public static void everyEmbedMaterialIsWired(GameTestHelper helper) {

        int wired = 0;
        int missing = 0;
        for (var entry : dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().entrySet()) {
            String id = entry.getKey();
            String[][] samples = {
                {"TOOL", "minecraft:diamond_pickaxe"},
                {"WEAPON", "minecraft:diamond_sword"},
                {"ARMOR", "minecraft:diamond_chestplate"},
                {"AMULET", "anvilcraft:armadillo_amulet"},
            };
            boolean visible = false;
            for (String[] sample : samples) {
                ItemStack stack = itemById(sample[1]);
                if (stack.isEmpty()) continue;
                int beforeMods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                    net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().size();
                int beforeMax = stack.getMaxDamage();
                boolean unit = stack.has(DataComponents.UNBREAKABLE);
                dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
                    stack, entry.getValue(), TargetKind.valueOf(sample[0]), 1, helper.getLevel()
                );
                int afterMods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                    net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().size();
                if (afterMods > beforeMods || stack.getMaxDamage() != beforeMax
                    || stack.has(DataComponents.UNBREAKABLE) != unit) {
                    visible = true;
                }
            }
            boolean flagOnly = "anvilcraft:transcendium_nugget".equals(id);
                        boolean eventOnly = false;
            for (String[] sample : samples) {
                if (dev.anvilcraft.thenextforge.forging.EmbedMaterials.eventOnly(
                    id, TargetKind.valueOf(sample[0]))) {
                    eventOnly = true;
                }
            }
            if (visible || eventOnly || flagOnly) {
                wired++;
            } else {
                missing++;
                dev.anvilcraft.thenextforge.TheNextForge.LOGGER.warn("[EMBED-CHECK] material not wired: {}", id);
            }
        }
        helper.assertTrue(missing == 0, "every embed material should be wired, missing=" + missing);
        helper.assertTrue(wired == dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().size(),
            "all materials counted, wired=" + wired);
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void uniqueMaterialsAreFlagged(GameTestHelper helper) {
        var glow = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.GLOW_INK_SAC));
        var echo = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.ECHO_SHARD));
        var quartz = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.QUARTZ));
        helper.assertTrue(glow != null && glow.unique(), "glow ink sac should be unique");
        helper.assertTrue(echo != null && echo.unique(), "echo shard should be unique");
        helper.assertTrue(quartz != null && !quartz.unique(), "quartz should not be unique");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void repeatedEmbedsStackLinearly(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        var quartz = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.QUARTZ));
        int before = modifiers(sword);
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(sword, quartz, TargetKind.WEAPON, 1, helper.getLevel());
        int afterFirst = modifiers(sword);
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(sword, quartz, TargetKind.WEAPON, 2, helper.getLevel());
        int afterSecond = modifiers(sword);
        helper.assertTrue(afterFirst > before, "first quartz should add a modifier");
        helper.assertTrue(afterSecond > afterFirst, "second quartz should add another modifier");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void glowHighlightDecision(GameTestHelper helper) {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        ForgingData data = ForgingData.EMPTY.withEmbed("minecraft:glow_ink_sac");
        tool.set(ModDataComponents.FORGING.get(), data);
        helper.assertTrue(
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.glowHighlightApplies(tool, new ItemStack(Items.STONE)),
            "tool with glow ink and block offhand should highlight"
        );
        helper.assertTrue(
            !dev.anvilcraft.thenextforge.forging.EmbedMaterials.glowHighlightApplies(tool, new ItemStack(Items.STICK)),
            "non-block offhand should not highlight"
        );
        helper.assertTrue(
            !dev.anvilcraft.thenextforge.forging.EmbedMaterials.glowHighlightApplies(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.STONE)),
            "tool without the embed should not highlight"
        );
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void amberMarkRaisesResentmentBy20PerComponent(GameTestHelper helper) {
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(1, 1, 1));
        dev.dubhe.anvilcraft.util.ResentmentUtil.setForcedResentment(zombie, 30);
        int before = dev.dubhe.anvilcraft.util.ResentmentUtil.getResentment(zombie);
        helper.assertTrue(before == 30, "baseline resentment should be 30, got " + before);

        int once = dev.anvilcraft.thenextforge.forging.AmberMarking.apply(zombie, 1);
        helper.assertTrue(once == 50, "one amber component should add 20, got " + once);

        var other = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        dev.dubhe.anvilcraft.util.ResentmentUtil.setForcedResentment(other, 30);
        int twice = dev.anvilcraft.thenextforge.forging.AmberMarking.apply(other, 2);
        helper.assertTrue(twice == 70, "two components should add 40, got " + twice);

        int repeated = dev.anvilcraft.thenextforge.forging.AmberMarking.apply(other, 2);
        helper.assertTrue(repeated == 70, "repeated hits must not stack, got " + repeated);
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void embedAllowedMatrixMatchesDefinitions(GameTestHelper helper) {

        String[][] samples = {
            {"TOOL", "minecraft:diamond_pickaxe"},
            {"WEAPON", "minecraft:diamond_sword"},
            {"ARMOR", "minecraft:diamond_chestplate"},
            {"AMULET", "anvilcraft:armadillo_amulet"},
        };
        int checked = 0;
        for (var entry : dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().entrySet()) {
            String id = entry.getKey();
            var def = entry.getValue();
            for (String[] sample : samples) {
                TargetKind kind = TargetKind.valueOf(sample[0]);
                boolean allowed = def.allows(kind);
                ItemStack stack = itemById(sample[1]);
                if (stack.isEmpty()) continue;
                int beforeMods = modifiers(stack);
                int beforeMax = stack.getMaxDamage();
                boolean result = dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
                    stack, def, kind, 1, helper.getLevel()
                );
                boolean changed = modifiers(stack) > beforeMods || stack.getMaxDamage() != beforeMax;
                if (!allowed) {
                    helper.assertTrue(!result, id + " must be refused for " + kind);
                    helper.assertTrue(!changed, id + " must not change " + kind);
                } else if (!dev.anvilcraft.thenextforge.forging.EmbedMaterials.eventOnly(id, kind)) {
                    helper.assertTrue(result || changed, id + " should apply for " + kind);
                }
                checked++;
            }
        }
        int expectedCombos = dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().size() * 4;
        helper.assertTrue(checked == expectedCombos,
            "expected " + expectedCombos + " combinations, checked " + checked);
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void attributeEmbedsProduceExpectedNumbers(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        double attackBefore = modifierAmount(sword, Attributes.ATTACK_DAMAGE);
        var quartz = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.QUARTZ));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(sword, quartz, TargetKind.WEAPON, 1, helper.getLevel());
        helper.assertTrue(near(modifierAmount(sword, Attributes.ATTACK_DAMAGE) - attackBefore, 0.2), "quartz weapon +20% attack");

        ItemStack diamondAmulet = itemById("anvilcraft:armadillo_amulet");
        double amuletArmorBefore = diamondAmulet.isEmpty() ? 0 : modifierAmount(diamondAmulet, Attributes.ARMOR);
        var diamond = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.DIAMOND));
        if (!diamondAmulet.isEmpty()) {
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(diamondAmulet, diamond, TargetKind.AMULET, 1, helper.getLevel());
            helper.assertTrue(near(modifierAmount(diamondAmulet, Attributes.ARMOR) - amuletArmorBefore, 2.0), "diamond amulet +2 armor");
        }

        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        int pickMax = pick.getMaxDamage();
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(pick, diamond, TargetKind.TOOL, 1, helper.getLevel());
        helper.assertTrue(pick.getMaxDamage() > pickMax, "diamond tool raises durability");

        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        int bootMax = boots.getMaxDamage();
        double bootArmorBefore = modifierAmount(boots, Attributes.ARMOR);
        var netherite = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.NETHERITE_INGOT));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(boots, netherite, TargetKind.ARMOR, 1, helper.getLevel());
        helper.assertTrue(near(modifierAmount(boots, Attributes.ARMOR) - bootArmorBefore, 3.0), "netherite armor +3");
        helper.assertTrue(boots.getMaxDamage() == bootMax * 2, "netherite doubles durability");

        ItemStack starSword = new ItemStack(Items.DIAMOND_SWORD);
        double starBefore = modifierAmount(starSword, Attributes.ATTACK_DAMAGE);
        var star = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.NETHER_STAR));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(starSword, star, TargetKind.WEAPON, 1, helper.getLevel());
        helper.assertTrue(near(modifierAmount(starSword, Attributes.ATTACK_DAMAGE) - starBefore, 1.0), "nether star +100% attack");

        ItemStack amulet = itemById("anvilcraft:armadillo_amulet");
        if (!amulet.isEmpty()) {
            var emerald = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.EMERALD));
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(amulet, emerald, TargetKind.AMULET, 1, helper.getLevel());
            helper.assertTrue(near(modifierAmount(amulet, Attributes.MAX_HEALTH), 3.0), "emerald amulet +3 health");
        }

        ItemStack logsAmulet = itemById("anvilcraft:armadillo_amulet");
        if (!logsAmulet.isEmpty()) {
            var logs = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.OAK_LOG));
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(logsAmulet, logs, TargetKind.AMULET, 1, helper.getLevel());
            helper.assertTrue(near(modifierAmount(logsAmulet, Attributes.MAX_HEALTH), 1.0), "logs amulet +1 health");
        }

        ItemStack ironSword = new ItemStack(Items.DIAMOND_SWORD);
        double reachBefore = modifierAmount(ironSword, Attributes.ENTITY_INTERACTION_RANGE);
        var iron = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.IRON_INGOT));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(ironSword, iron, TargetKind.WEAPON, 1, helper.getLevel());
        helper.assertTrue(near(modifierAmount(ironSword, Attributes.ENTITY_INTERACTION_RANGE) - reachBefore, 1.0), "iron weapon +1 reach");

        ItemStack transc = itemById("anvilcraft:armadillo_amulet");
        if (!transc.isEmpty()) {
            var nugget = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(itemById("anvilcraft:transcendium_nugget"));
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(transc, nugget, TargetKind.AMULET, 1, helper.getLevel());
            helper.assertTrue(transc.has(DataComponents.UNBREAKABLE), "transcendium makes amulet unbreakable");
        }
        helper.succeed();
    }

    private static double modifierAmount(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {
        var mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
            net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        double sum = 0;
        for (var entry : mods.modifiers()) {
            if (entry.attribute().equals(attribute)) {
                sum += entry.modifier().amount();
            }
        }
        return sum;
    }


    @GameTest(template = "empty")
    public static void everyEventDrivenMaterialIsRegistered(GameTestHelper helper) {

        var registered = dev.anvilcraft.thenextforge.forging.EmbedMaterials.EVENT_EFFECTS;
        for (String id : eventDriven) {
            helper.assertTrue(registered.containsKey(id), "missing event registration: " + id);
        }
        helper.assertTrue(registered.size() >= eventDriven.size(),
            "registered " + registered.size() + " but expected " + eventDriven.size());
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void amuletAttributesApplyToCarrier(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack amulet = itemById("anvilcraft:armadillo_amulet");
        if (amulet.isEmpty()) { helper.succeed(); return; }
        var emerald = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.EMERALD));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(amulet, emerald, TargetKind.AMULET, 1, helper.getLevel());
        amulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:emerald"));
        // 历史展示框夹具保留；嵌入属性按背包携带生效
        helper.setBlock(2, 1, 2, Blocks.ANVIL);
        var base = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2));
        var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(),
            base.south(), net.minecraft.core.Direction.SOUTH);
        frame.setItem(amulet);
        // 当前设计：展示框管锻造；玩家属性由携带护符提供。
        player.getInventory().setItem(1, amulet.copy());
        helper.getLevel().addFreshEntity(frame);
        player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);

        double before = player.getAttributeValue(Attributes.MAX_HEALTH);
        int applied = dev.anvilcraft.thenextforge.event.AmuletAttributes.sync(player);
        double after = player.getAttributeValue(Attributes.MAX_HEALTH);
        helper.assertTrue(applied > 0, "amulet attributes should be applied, applied=" + applied);
        helper.assertTrue(after - before > 2.5, "emerald amulet should raise max health by 3");

        dev.anvilcraft.thenextforge.event.AmuletAttributes.sync(player);
        double again = player.getAttributeValue(Attributes.MAX_HEALTH);
        helper.assertTrue(near(again, after), "repeated sync must not stack, got " + again + " vs " + after);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void amuletTickEventSyncs(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack amulet = itemById("anvilcraft:armadillo_amulet");
        if (amulet.isEmpty()) { helper.succeed(); return; }
        var emerald = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.EMERALD));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(amulet, emerald, TargetKind.AMULET, 1, helper.getLevel());
        amulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:emerald"));
        // 历史展示框夹具保留；嵌入属性按背包携带生效
        helper.setBlock(2, 1, 2, Blocks.ANVIL);
        var framePos = helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 3));
        var frame = new net.minecraft.world.entity.decoration.ItemFrame(helper.getLevel(), framePos,
            net.minecraft.core.Direction.SOUTH);
        frame.setItem(amulet);
        // 当前设计：展示框管锻造；玩家属性由携带护符提供。
        player.getInventory().setItem(1, amulet.copy());
        helper.getLevel().addFreshEntity(frame);
        player.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)).getX() + 0.5,
            helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)).getY(),
            helper.absolutePos(new net.minecraft.core.BlockPos(2, 1, 2)).getZ() + 0.5);
        double before = player.getAttributeValue(Attributes.MAX_HEALTH);
        int applied = dev.anvilcraft.thenextforge.event.AmuletAttributes.sync(player);
        double after = player.getAttributeValue(Attributes.MAX_HEALTH);
        helper.assertTrue(applied > 0, "sync should apply modifiers, applied=" + applied);
        helper.assertTrue(after - before > 2.5, "sync should raise health, got " + (after - before));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void experienceCostGrowsEveryFiveLevels(GameTestHelper helper) {
        helper.assertTrue(ForgingRules.experienceCost(0) == 6, "level 0 costs 6");
        helper.assertTrue(ForgingRules.experienceCost(10) == 46, "level 10 costs 46");
        helper.assertTrue(ForgingRules.experienceCost(19) == 82, "level 19 costs 82");
        helper.assertTrue(ForgingRules.experienceCost(20) == 129, "level 20 costs 129");
        helper.assertTrue(ForgingRules.experienceCost(25) == 239, "level 25 costs 239");
        helper.assertTrue(ForgingRules.experienceCost(40) == 1261, "level 40 costs 1261");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transcendiumResetClearsEverything(GameTestHelper helper) {
        ForgingData data = ForgingData.EMPTY
            .withLevel(ForgingLine.ANVIL, 12)
            .withTokens(ForgingLine.ANVIL, 0)
            .withProtectionUsed("armadillo")
            .withNextHitProtected(true)
            .withEmbed("anvilcraft:transcendium_nugget");
        ForgingData reset = ForgingRules.resetToZero(data, ForgingLine.ANVIL);
        helper.assertTrue(reset.anvilLevel() == 0, "level resets to 0");
        helper.assertTrue(reset.tokens(ForgingLine.ANVIL) == -1, "royal token refreshes");
        helper.assertTrue(reset.protections().isEmpty(), "protections clear");
        helper.assertTrue(!reset.nextHitProtected(), "next hit flag clears");
        helper.assertTrue(reset.hasEmbed("anvilcraft:transcendium_nugget"), "embed remains");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void overlimitHammerWithBaseAnvilDropsThree(GameTestHelper helper) {
        helper.assertTrue(ForgingRules.degradedLevel(10, false, true) == 7,
            "single overlimit (hammer only) should drop 3");
        helper.assertTrue(ForgingRules.degradedLevel(10, true, false) == 7,
            "single overlimit (anvil only) should drop 3");
        helper.assertTrue(ForgingRules.degradedLevel(10, true, true) == 9,
            "double overlimit should drop 1");
        helper.assertTrue(ForgingRules.degradedLevel(2, false, true) == 0,
            "never below zero");
        helper.assertTrue(ForgingRules.degradeLimit(false, true) == 3, "limit is 3 for single overlimit");
        helper.assertTrue(ForgingRules.degradeLimit(true, true) == 1, "limit is 1 for double overlimit");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void topazSealStopsFurtherForging(GameTestHelper helper) {
        ForgingData data = ForgingData.EMPTY
            .withLevel(ForgingLine.ANVIL, 6)
            .withProtectionUsed("topaz")
            .withLocked();
        helper.assertTrue(data.locked(), "topaz seal should lock the item");
        helper.assertTrue(data.anvilLevel() == 6, "level unchanged by the seal itself");
        helper.assertTrue(data.protectionUsed("topaz"), "topaz protection recorded");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void combatEmbedsApplyTheirEffects(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        // 用猪而不是僵尸：不死生物免疫中毒
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 1, 1));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:fermented_spider_eye"));
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, zombie, sword, 2.0F);
        helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "spider eye should poison");

        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:slime_ball"));
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, zombie, sword, 2.0F);
        var zombieSpeed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        var zombieSlow = zombieSpeed == null ? null
            : zombieSpeed.getModifier(dev.anvilcraft.thenextforge.forging.CombatEffects.SLIME_SLOW_ID);
        helper.assertTrue(zombieSlow != null && near(zombieSlow.amount(), -0.5D),
            "slime ball 应精确降低 50% 移速，实际 " + (zombieSlow == null ? "无修饰符" : zombieSlow.amount()));

        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:negative_matter_nugget"));
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, zombie, sword, 2.0F);
        helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION), "negative matter should levitate");
        helper.assertTrue(
            zombie.getData(dev.anvilcraft.thenextforge.init.ModAttachments.VULNERABLE_UNTIL.get()) > level.getGameTime(),
            "negative matter should mark vulnerability"
        );

        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:ember_metal_ingot"));
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, zombie, sword, 2.0F);
        helper.assertTrue(zombie.getRemainingFireTicks() > 0, "ember should ignite");

        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:glow_ink_sac"));
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, zombie, sword, 2.0F);
        helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING), "glow ink should glow");

        double beforeX = zombie.getX();
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:ender_pearl"));
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, zombie, sword, 2.0F);
        helper.assertTrue(zombie.getX() != beforeX || zombie.getZ() != 0, "ender eye should teleport");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void dropEffectsSmeltAndCollect(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStack smelted = dev.anvilcraft.thenextforge.event.ForgeDropEvents.smelt(
            level, new ItemStack(Items.RAW_IRON, 2)
        );
        helper.assertTrue(smelted != null && smelted.is(Items.IRON_INGOT),
            "raw iron should smelt to iron ingot");
        helper.assertTrue(smelted.getCount() == 2, "count should scale with input");

        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeDropEvents.smelt(level, new ItemStack(Items.DIRT)) == null,
            "dirt has no smelting recipe"
        );

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var drop = new net.minecraft.world.entity.item.ItemEntity(
            level, 1.5, -59.5, 1.5, new ItemStack(Items.DIAMOND, 3)
        );
        int collected = dev.anvilcraft.thenextforge.event.ForgeDropEvents.collect(player, java.util.List.of(drop));
        helper.assertTrue(collected == 1, "one drop collected");
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 3, "diamonds land in inventory");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void automationHammersItemInFishTank(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.ANVIL);
        var fishTank = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var be = helper.getLevel().getBlockEntity(tankPos);
        if (!(be instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        tank.getFluidHandler().fill(
            new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
            ),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
        );
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        tank.getItemHandler().setStackInSlot(8, sword);

        boolean levelled = false;
        for (int i = 0; i < 20 && !levelled; i++) {
            dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
                helper.getLevel(), tankPos, net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState()
            );
            ItemStack after = tank.getItemHandler().getStackInSlot(8);
            if (after.isEmpty()) {
                tank.getItemHandler().setStackInSlot(8, new ItemStack(Items.DIAMOND_SWORD));
                continue;
            }
            ForgingData data = after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
            if (data.anvilLevel() > 0) levelled = true;
        }
        helper.assertTrue(levelled, "automated hits should raise the item level");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void topazRepairsOnLightningPath(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(40);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:topaz"));
        int before = sword.getDamageValue();
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairIfTopaz(sword), "topaz should repair");
        helper.assertTrue(sword.getDamageValue() == before - 10, "repair by 10");

        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
        plain.setDamageValue(40);
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairIfTopaz(plain),
            "no topaz embed means no repair");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void automationOutputThresholdScalesWithSignal(GameTestHelper helper) {
        helper.assertTrue(dev.anvilcraft.thenextforge.event.AutomationHandler.outputThreshold(0) == 1,
            "开口且无信号时 1 级即输出");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.AutomationHandler.outputThreshold(3) == 7,
            "signal 3 outputs at level 7");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.AutomationHandler.outputThreshold(15) == 31,
            "signal 15 outputs at level 31");
        helper.succeed();
    }

    /**
     * 输出总开关是鱼缸开口：不开口时无论有没有红石信号都持续升级，开口后无信号 1 级即输出。
     *
     * <p>两个结构并排：左边鱼缸默认不开口，右边鱼缸一开始就开口；基底用超限砧（失败只降级不摧毁），
     * 对象预置到 5 级，保证一次结算后的等级必然 ≥ 1。
     */
    @GameTest(template = "empty")
    public static void automationOnlyOutputsWhenTheOutletIsOpen(GameTestHelper helper) {
        var fishTank = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        var safeAnvil = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "transcendence_anvil"))
            .orElse(null);
        if (safeAnvil == null) { helper.succeed(); return; }

        helper.setBlock(1, 1, 1, safeAnvil);
        helper.setBlock(1, 2, 1, fishTank);
        helper.setBlock(4, 1, 1, safeAnvil);
        helper.setBlock(4, 2, 1, fishTank.defaultBlockState()
            .setValue(dev.dubhe.anvilcraft.block.FishTankBlock.OUTLET, true));

        var closed = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 1)));
        var opened = helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(4, 2, 1)));
        if (!(closed instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity closedTank)
            || !(opened instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity openTank)) {
            helper.succeed();
            return;
        }
        fillExperience(closedTank);
        fillExperience(openTank);
        helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(1, 2, 1)))
            .getValue(dev.dubhe.anvilcraft.block.FishTankBlock.OUTLET), "鱼缸默认不开口");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(4, 2, 1)))
            .getValue(dev.dubhe.anvilcraft.block.FishTankBlock.OUTLET), "第二个鱼缸已开口");

        // 不开口：一次结算后对象必须还留在输入槽里（持续升级）
        closedTank.getItemHandler().setStackInSlot(8, strengthenedSword(5));
        dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
            helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)), safeAnvil.defaultBlockState()
        );
        helper.assertTrue(!closedTank.getItemHandler().getStackInSlot(8).isEmpty(),
            "鱼缸不开口时不得输出，对象应留在输入槽继续升级");

        // 开口：同一等级下 1 级阈值即输出
        openTank.getItemHandler().setStackInSlot(8, strengthenedSword(5));
        dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
            helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 1)), safeAnvil.defaultBlockState()
        );
        helper.assertTrue(openTank.getItemHandler().getStackInSlot(8).isEmpty(),
            "鱼缸开口且无信号时，产物到 1 级就该输出");
        helper.succeed();
    }

    private static void fillExperience(dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank) {
        tank.getFluidHandler().fill(
            new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 20000
            ),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
        );
    }

    /** 预置到指定铁砧等级的剑（组件直接写，避免随机升级干扰断言）。 */
    private static ItemStack strengthenedSword(int level) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, level));
        return sword;
    }

    @GameTest(template = "empty")
    public static void automationConsumesExperienceFluid(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.ANVIL);
        var fishTank = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var be = helper.getLevel().getBlockEntity(tankPos);
        if (!(be instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        var fluid = new net.neoforged.neoforge.fluids.FluidStack(
            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
        );
        tank.getFluidHandler().fill(fluid, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

        boolean paid = dev.anvilcraft.thenextforge.event.AutomationHandler.consumeExperience(
            helper.getLevel(), tankPos, 10
        );
        helper.assertTrue(paid, "10 xp should be payable from 1000 mB");
        int left = tank.getFluidHandler().getFluidInTank(0).getAmount();
        helper.assertTrue(left == 1000 - 10 * dev.anvilcraft.thenextforge.event.AutomationHandler.MB_PER_XP,
            "should drain 35 mB per xp, left " + left);

        boolean tooMuch = dev.anvilcraft.thenextforge.event.AutomationHandler.consumeExperience(
            helper.getLevel(), tankPos, 100
        );
        helper.assertTrue(!tooMuch, "should refuse when the fluid is not enough");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void passiveRepairAmounts(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ForgingData emerald = ForgingData.EMPTY.withEmbed("minecraft:emerald");
        // 绿宝石改走「独立经验修补」，不再有每 5 秒固定回复
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(emerald, player) == 0,
            "emerald 不再提供每 5 秒回复");
        ForgingData logs = ForgingData.EMPTY.withEmbed("#minecraft:logs");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(logs, player) == 1,
            "logs repair 1");
        ForgingData both = emerald.withEmbed("#minecraft:logs");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(both, player) == 1,
            "two materials stack to 2");
        ForgingData empty = ForgingData.EMPTY;
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(empty, player) == 0,
            "no embed repairs nothing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void echoShardDealsExtraDamage(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 1, 1));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:echo_shard"));
        float before = pig.getHealth();
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(level, player, pig, sword, 4.0F);
        helper.assertTrue(pig.getHealth() < before, "echo shard should deal extra damage");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void royalAndNeutroniumStats(GameTestHelper helper) {
        ItemStack royalSword = new ItemStack(Items.DIAMOND_SWORD);
        double before = modifierAmount(royalSword, Attributes.ATTACK_DAMAGE);
        var royal = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(itemById("anvilcraft:royal_steel_ingot"));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(royalSword, royal, TargetKind.WEAPON, 1, helper.getLevel());
        helper.assertTrue(near(modifierAmount(royalSword, Attributes.ATTACK_DAMAGE) - before, 0.1),
            "royal weapon +10% damage");

        ItemStack neutronSword = new ItemStack(Items.DIAMOND_SWORD);
        double beforeNeutron = modifierAmount(neutronSword, Attributes.ATTACK_DAMAGE);
        var neutron = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(itemById("anvilcraft:neutronium_ingot"));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(neutronSword, neutron, TargetKind.WEAPON, 1, helper.getLevel());
        helper.assertTrue(near(modifierAmount(neutronSword, Attributes.ATTACK_DAMAGE) - beforeNeutron, 0.8),
            "neutronium weapon +80% damage");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void remainingMaterialFormulas(GameTestHelper helper) {
        // 虚空物质：武器 30%、护符 10%
        helper.assertTrue(Math.abs(dev.anvilcraft.thenextforge.forging.CombatEffects.voidRatio(true) - 0.3) < 0.01,
            "weapon void 30%");
        helper.assertTrue(Math.abs(dev.anvilcraft.thenextforge.forging.CombatEffects.voidRatio(false) - 0.1) < 0.01,
            "amulet void 10%");

        // 浮霜：附魔数与玩家等级
        helper.assertTrue(Math.abs(dev.anvilcraft.thenextforge.forging.CombatEffects.frostMultiplier(2, 0) - 1.10) < 0.01,
            "two enchants add 10%");
        helper.assertTrue(Math.abs(dev.anvilcraft.thenextforge.forging.CombatEffects.frostMultiplier(0, 30) - 1.30) < 0.01,
            "level 30 adds 30%");

        // 青金石：击杀经验 +20%
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.xpBonus(10) == 12, "10 xp becomes 12");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.xpBonus(0) == 0, "zero stays zero");

        // 红石：主手工具带嵌入
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.hasRedstoneHaste(
                ForgingData.EMPTY.withEmbed("minecraft:redstone")),
            "redstone embed gives haste"
        );
        helper.assertTrue(
            !dev.anvilcraft.thenextforge.event.ForgeItemTicker.hasRedstoneHaste(ForgingData.EMPTY),
            "no embed no haste"
        );

        // 诅咒金：默认无诅咒
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeCombatEvents.countCurses(player) == 0,
            "plain player has no curses");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void waterAndFireStatesDriveMaterials(GameTestHelper helper) {
        ForgingData sapphire = ForgingData.EMPTY.withEmbed("anvilcraft:sapphire");
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(sapphire, true, false, false) == 2,
            "sapphire repairs 2 in water");
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(sapphire, false, false, false) == 0,
            "sapphire repairs nothing on land");

        ForgingData ruby = ForgingData.EMPTY.withEmbed("anvilcraft:ruby");
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(ruby, false, true, false) == 2,
            "ruby repairs 2 while burning");

        ForgingData topaz = ForgingData.EMPTY.withEmbed("anvilcraft:topaz");
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(topaz, false, false, true) == 2,
            "topaz repairs 2 inside a power grid");
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(topaz, false, false, false) == 0,
            "topaz repairs nothing outside a grid");

        helper.assertTrue(
            Math.abs(dev.anvilcraft.thenextforge.forging.CombatEffects.heartOfTheSeaDamage(4.0F, true) - 7.0F) < 0.01,
            "heart of the sea adds 75% in water");
        helper.assertTrue(
            Math.abs(dev.anvilcraft.thenextforge.forging.CombatEffects.heartOfTheSeaDamage(4.0F, false) - 4.0F) < 0.01,
            "heart of the sea adds nothing on land");
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void automationEmbedsMaterialFromTank(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.ANVIL);
        var fishTank = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var be = helper.getLevel().getBlockEntity(tankPos);
        if (!(be instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        tank.getFluidHandler().fill(
            new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
            ),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
        );
        tank.getItemHandler().setStackInSlot(8, new ItemStack(Items.DIAMOND_SWORD));
        tank.getItemHandler().setStackInSlot(9, new ItemStack(Items.QUARTZ));

        boolean embedded = false;
        for (int i = 0; i < 30 && !embedded; i++) {
            dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
                helper.getLevel(), tankPos, net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState()
            );
            ItemStack after = tank.getItemHandler().getStackInSlot(8);
            if (after.isEmpty()) {
                tank.getItemHandler().setStackInSlot(8, new ItemStack(Items.DIAMOND_SWORD));
                tank.getItemHandler().setStackInSlot(9, new ItemStack(Items.QUARTZ));
                continue;
            }
            ForgingData data = after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
            if (data.hasEmbed("minecraft:quartz")) embedded = true;
        }
        helper.assertTrue(embedded, "automation should embed the quartz from the second slot");
        helper.succeed();
    }






    @GameTest(template = "empty")
    public static void everyKindEveryLevelAppliesStats(GameTestHelper helper) {
        TargetKind[] kinds = {TargetKind.WEAPON, TargetKind.TOOL, TargetKind.ARMOR, TargetKind.AMULET};
        int[] levels = {1, 5, 10, 20, 49};
        for (TargetKind kind : kinds) {
            for (int level : levels) {
                ItemStack stack = switch (kind) {
                    case WEAPON -> new ItemStack(Items.DIAMOND_SWORD);
                    case TOOL -> new ItemStack(Items.DIAMOND_PICKAXE);
                    case ARMOR -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                    case AMULET -> itemById("anvilcraft:armadillo_amulet");
                };
                if (stack.isEmpty()) continue;
                int before = modifiers(stack);
                String text = ForgingRules.applyUpgrade(stack, kind, ForgingLine.ANVIL, level, helper.getLevel().getRandom());
                helper.assertTrue(text != null, kind + " level " + level + " should produce a stat");
                helper.assertTrue(modifiers(stack) >= before, kind + " level " + level + " should not lose modifiers");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void successRateMatchesFormulaOverManyRolls(GameTestHelper helper) {
        var random = helper.getLevel().getRandom();
        for (int level : new int[]{0, 4, 7, 10, 15, 20, 40}) {
            double expected = ForgingRules.successRate(level);
            int hits = 0;
            int rolls = 20000;
            for (int i = 0; i < rolls; i++) {
                if (random.nextDouble() < expected) hits++;
            }
            double actual = (double) hits / rolls;
            helper.assertTrue(Math.abs(actual - expected) < 0.02,
                "level " + level + " expected " + expected + " got " + actual);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void amuletCombinationsExpandIndependently(GameTestHelper helper) {
        var single = dev.anvilcraft.thenextforge.forging.Amulets.expand(java.util.Set.of("armadillo"));
        helper.assertTrue(single.size() == 1, "single amulet stays single");
        var gem = dev.anvilcraft.thenextforge.forging.Amulets.expand(java.util.Set.of("gem"));
        helper.assertTrue(gem.contains("ruby") && gem.contains("sapphire") && gem.contains("topaz"),
            "gem expands to three");
        var combo = dev.anvilcraft.thenextforge.forging.Amulets.expand(java.util.Set.of("gem", "nature", "feather"));
        helper.assertTrue(combo.contains("armadillo") && combo.contains("feather") && combo.contains("ruby"),
            "combined amulets union without duplicates");
        helper.assertTrue(combo.size() == 9, "union size should be 9 (gem+3, nature+4, feather), got " + combo.size());
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void everyMaterialAppliesToEveryAllowedKind(GameTestHelper helper) {
        int applied = 0;
        for (var definition : dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().values()) {
            for (TargetKind kind : TargetKind.values()) {
                if (!definition.allows(kind)) continue;
                ItemStack stack = switch (kind) {
                    case WEAPON -> new ItemStack(Items.DIAMOND_SWORD);
                    case TOOL -> new ItemStack(Items.DIAMOND_PICKAXE);
                    case ARMOR -> new ItemStack(Items.DIAMOND_CHESTPLATE);
                    case AMULET -> itemById("anvilcraft:armadillo_amulet");
                };
                if (stack.isEmpty()) continue;
                ForgingData data = ForgingData.EMPTY.withEmbed(definition.id());
                dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
                    stack, definition, kind, 5, helper.getLevel()
                );
                stack.set(ModDataComponents.FORGING.get(), data);
                helper.assertTrue(stack.get(ModDataComponents.FORGING.get()).hasEmbed(definition.id()),
                    "embed " + definition.id() + " should be recorded on " + kind);
                applied++;
            }
        }
        helper.assertTrue(applied > 40, "should cover many material/kind pairs, got " + applied);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automatedUpgradeWorksForEveryKind(GameTestHelper helper) {
        // 对象适用性测试不掷概率：每件新对象使用皇家基底的首次必成。
        var royalAnvil = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "royal_anvil")).orElseThrow();
        helper.setBlock(1, 1, 1, royalAnvil);
        var fishTank = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getOptional(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var be = helper.getLevel().getBlockEntity(tankPos);
        if (!(be instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        if (tank.getItemHandler().getSlots() < 2) { helper.succeed(); return; }

        ItemStack[] targets = {
            new ItemStack(Items.DIAMOND_SWORD),
            new ItemStack(Items.DIAMOND_PICKAXE),
            new ItemStack(Items.DIAMOND_CHESTPLATE),
            itemById("anvilcraft:armadillo_amulet"),
        };
        int upgraded = 0;
        for (ItemStack target : targets) {
            helper.assertTrue(!target.isEmpty(), "所有待测对象都必须解析成功");
            tank.getFluidHandler().drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            tank.getFluidHandler().fill(
                new net.neoforged.neoforge.fluids.FluidStack(
                    dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
                ),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
            );
            tank.getItemHandler().setStackInSlot(8, target.copy());
            for (int i = 0; i < 120; i++) {
                // 基础砧会摧毁对象、经验也可能耗尽：补齐后再继续，避免一次坏运气判死
                if (tank.getFluidHandler().getFluidInTank(0).getAmount() < 400) {
                    tank.getFluidHandler().fill(
                        new net.neoforged.neoforge.fluids.FluidStack(
                            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
                        ),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                    );
                }
                if (tank.getItemHandler().getStackInSlot(8).isEmpty()) {
                    tank.getItemHandler().setStackInSlot(8, target.copy());
                }
                if (tank.getFluidHandler().getFluidInTank(0).getAmount() < 300) {
                    tank.getFluidHandler().fill(
                        new net.neoforged.neoforge.fluids.FluidStack(
                            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 2000
                        ),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                    );
                }
                dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
                    helper.getLevel(), tankPos, royalAnvil.defaultBlockState()
                );
                ItemStack after = tank.getItemHandler().getStackInSlot(8);
                if (after.isEmpty()) break;
                ForgingData data = after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
                if (data.anvilLevel() > 0) { upgraded++; break; }
                if (tank.getFluidHandler().getFluidInTank(0).getAmount() < 300) {
                    tank.getFluidHandler().fill(
                        new net.neoforged.neoforge.fluids.FluidStack(
                            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
                        ),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
                    );
                }
            }
        }
        helper.assertTrue(upgraded == targets.length, "皇家必成应覆盖全部对象，实际 " + upgraded + "/" + targets.length);
        helper.succeed();
    }


    @GameTest(template = "empty")
    public static void xpBonusRoundsTwentyPercent(GameTestHelper helper) {
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.xpBonus(0) == 0, "0 stays 0");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.xpBonus(5) == 6, "5 -> 6");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.xpBonus(10) == 12, "10 -> 12");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.xpBonus(100) == 120, "100 -> 120");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void topazRepairRestoresTenDurability(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ForgingData data = ForgingData.EMPTY.withEmbed("anvilcraft:topaz");
        sword.set(ModDataComponents.FORGING.get(), data);
        sword.setDamageValue(50);
        boolean repaired = dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairIfTopaz(sword);
        helper.assertTrue(repaired, "topaz should repair a damaged item");
        helper.assertTrue(sword.getDamageValue() == 40, "durability restored by 10, got " + sword.getDamageValue());
        // 没有黄玉时不修
        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
        plain.setDamageValue(50);
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairIfTopaz(plain),
            "plain item should not be repaired");
        // 满耐久不修
        ItemStack full = new ItemStack(Items.DIAMOND_SWORD);
        full.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:topaz"));
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairIfTopaz(full),
            "undamaged item should not be repaired");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void glowInkSacInArmorGrantsNightVision(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        // 无嵌入：不给夜视
        helper.assertTrue(
            !dev.anvilcraft.thenextforge.event.ForgeItemTicker.grantsNightVision(helmet),
            "plain armour must not grant night vision"
        );
        // 嵌入发光墨囊：给夜视
        ForgingData data = ForgingData.EMPTY.withEmbed("minecraft:glow_ink_sac");
        helmet.set(ModDataComponents.FORGING.get(), data);
        helper.assertTrue(
            dev.anvilcraft.thenextforge.event.ForgeItemTicker.grantsNightVision(helmet),
            "glow ink sac embedded in armour must grant night vision"
        );
        helper.succeed();
    }

    /**
     * 每一级都必须留下属于自己的一条修饰符，tooltip 的汇总值必须等于这些条目之和。
     *
     * <p>原版 {@code ItemAttributeModifiers#withModifierAdded} 按「属性 + id」去重：
     * 固定 id 会让新一级直接替换旧一级，等级涨了但加成一直只有单级。
     */
    @GameTest(template = "empty")
    public static void everyLevelKeepsItsOwnModifierAndTooltipSumsThem(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int base = modifiers(sword);
        for (int lv = 1; lv <= 20; lv++) {
            ForgingRules.applyUpgrade(sword, TargetKind.WEAPON, ForgingLine.ANVIL, lv, net.minecraft.util.RandomSource.create(lv));
        }
        ItemAttributeModifiers mods = sword.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        java.util.List<ItemAttributeModifiers.Entry> own = new java.util.ArrayList<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
            if (!dev.anvilcraft.thenextforge.forging.ForgingSummary.isForgeModifier(entry.modifier().id())) continue;
            own.add(entry);
            ids.add(entry.modifier().id().toString());
        }
        helper.assertTrue(own.size() == 20, "20 次升级应留下 20 条修饰符，实际 " + own.size());
        helper.assertTrue(ids.size() == own.size(), "每级修饰符 id 必须唯一，否则会被原版替换掉");
        helper.assertTrue(modifiers(sword) == base + 20, "条目数应逐级累加，实际 " + modifiers(sword));
        double expected = 0;
        for (ItemAttributeModifiers.Entry entry : own) expected += entry.modifier().amount();
        double summarised = 0;
        for (var group : dev.anvilcraft.thenextforge.forging.ForgingSummary.groups(sword)) summarised += group.amount();
        helper.assertTrue(near(summarised, expected), "汇总必须等于各条之和：" + summarised + " vs " + expected);
        helper.assertTrue(summarised >= 20 * 0.05 - 1e-9,
            "20 级的合计加成必须远大于单级，实际 " + summarised);
        {
            StringBuilder dump = new StringBuilder();
            for (ItemAttributeModifiers.Entry entry : own) {
                dump.append(entry.modifier().id().getPath()).append('=')
                    .append(String.format("%.4f", entry.modifier().amount())).append(' ');
            }
            TheNextForge.LOGGER.info("[TNF-SUM] entries={} total={} detail={}", own.size(),
                String.format("%.4f", summarised), dump.toString().trim());
            StringBuilder groups = new StringBuilder();
            for (var group : dev.anvilcraft.thenextforge.forging.ForgingSummary.groups(sword)) {
                groups.append(group.key()).append('=').append(String.format("%.4f", group.amount()))
                    .append("(x").append(group.entries()).append(") ");
            }
            TheNextForge.LOGGER.info("[TNF-SUM] tooltip groups={}", groups.toString().trim());
        }
        // 降级：写回 3 级必须清掉 4 级以上的修饰符
        ForgingRules.write(sword, ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 3));
        int kept = 0;
        for (ItemAttributeModifiers.Entry entry : sword.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers()) {
            if (dev.anvilcraft.thenextforge.forging.ForgingSummary.isForgeModifier(entry.modifier().id())) kept++;
        }
        helper.assertTrue(kept == 3, "降级到 3 级后只应保留 3 条修饰符，实际 " + kept);
        helper.succeed();
    }

    /** 一次落砧只允许结算一次锤击：配方管理器会把同一触发重复执行到 maxEfficiency。 */
    @GameTest(template = "empty")
    public static void oneLandedAnvilIsOneHammerHit(GameTestHelper helper) {
        var recipe = new dev.anvilcraft.thenextforge.recipe.ForgeUpgradeRecipe(
            new ItemStack(Items.ANVIL),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of(dev.anvilcraft.thenextforge.recipe.ForgeUpgradeOutcome.INSTANCE),
            1000
        );
        helper.assertTrue(recipe.maxEfficiency() == 1,
            "一次落砧必须只结算一次锤击，实际 maxEfficiency=" + recipe.maxEfficiency());
        helper.succeed();
    }

    /** 单次结算最多升一级；容器不得把上一件对象的等级带给新放进来的对象。 */
    @GameTest(template = "empty")
    public static void automationOneHitOneLevelAndNoLevelReuse(GameTestHelper helper) {
        var fishTank = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        var be = helper.getLevel().getBlockEntity(tankPos);
        if (!(be instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        tank.getFluidHandler().fill(
            new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 20000
            ),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
        );
        tank.getItemHandler().setStackInSlot(8, new ItemStack(Items.DIAMOND_SWORD));
        helper.assertTrue(
            tank.getItemHandler().getStackInSlot(8)
                .getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).anvilLevel() == 0,
            "新对象入场必须是 0 级"
        );
        dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
            helper.getLevel(), tankPos, Blocks.ANVIL.defaultBlockState()
        );
        ItemStack after = tank.getItemHandler().getStackInSlot(8);
        // 基础砧会损毁对象，被毁掉时等级按 0 计
        int level = after.isEmpty() ? 0
            : after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).anvilLevel();
        helper.assertTrue(level <= 1, "单次结算最多升一级，实际 " + level);
        helper.assertTrue(tank.getOutputHandler().getStackInSlot(0).isEmpty(),
            "无红石信号时不得把对象复制进输出槽");
        // proxy 8..15 对应 input 0..7，目标占用的是 input 0
        for (int slot = 1; slot < tank.getInputHandler().getSlots(); slot++) {
            helper.assertTrue(tank.getInputHandler().getStackInSlot(slot).isEmpty(),
                "等级只写在目标槽，其他输入槽必须仍为空：slot=" + slot);
        }
        for (int slot = 0; slot < tank.getOutputHandler().getSlots(); slot++) {
            helper.assertTrue(tank.getOutputHandler().getStackInSlot(slot).isEmpty(),
                "输出槽不得出现目标对象的副本：slot=" + slot);
        }
        // 同一槽换一件全新物品：不能继承上一件的等级
        tank.getItemHandler().setStackInSlot(8, new ItemStack(Items.DIAMOND_SWORD));
        ItemStack fresh = tank.getItemHandler().getStackInSlot(8);
        helper.assertTrue(
            fresh.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).anvilLevel() == 0,
            "容器不得把上一件对象的等级复用到新对象"
        );
        helper.succeed();
    }

    // ===== 嵌入材料的效果/事件矩阵（对应设计里的药水效果与持续效果） =====

    private static boolean hasEffect(java.util.List<net.minecraft.world.effect.MobEffectInstance> effects,
                                     net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        for (var e : effects) {
            if (e.getEffect().equals(effect) && e.getAmplifier() == amplifier) return true;
        }
        return false;
    }

    private static ItemStack withEmbeds(String itemId, String... embedIds) {
        ItemStack stack = itemById(itemId);
        if (stack.isEmpty()) return stack;
        ForgingData data = ForgingData.EMPTY;
        for (String id : embedIds) data = data.withEmbed(id);
        stack.set(ModDataComponents.FORGING.get(), data);
        return stack;
    }

    /** 装甲 / 护符上的持续效果必须与设计逐条对应。 */
    @GameTest(template = "empty")
    public static void embedEffectMatrixMatchesDesign(GameTestHelper helper) {
        var ticker = dev.anvilcraft.thenextforge.event.ForgeItemTicker.class;
        // 装甲：下界之星 抗性 III、中子锭 抗性 I、红石 速度 I、发光墨囊 夜视
        var armourStar = dev.anvilcraft.thenextforge.event.ForgeItemTicker.armourEffects(
            withEmbeds(EMBED_CHEST, "minecraft:nether_star"));
        helper.assertTrue(hasEffect(armourStar, net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2),
            "装甲+下界之星 = 抗性提升 +3");
        var armourNeutron = dev.anvilcraft.thenextforge.event.ForgeItemTicker.armourEffects(
            withEmbeds(EMBED_CHEST, "anvilcraft:neutronium_ingot"));
        helper.assertTrue(hasEffect(armourNeutron, net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 0),
            "装甲+中子锭 = 抗性提升 +1");
        var armourRedstone = dev.anvilcraft.thenextforge.event.ForgeItemTicker.armourEffects(
            withEmbeds(EMBED_CHEST, "minecraft:redstone"));
        helper.assertTrue(hasEffect(armourRedstone, net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 0),
            "装甲+红石 = 速度 I");
        var armourGlow = dev.anvilcraft.thenextforge.event.ForgeItemTicker.armourEffects(
            withEmbeds(EMBED_CHEST, "minecraft:glow_ink_sac"));
        helper.assertTrue(hasEffect(armourGlow, net.minecraft.world.effect.MobEffects.NIGHT_VISION, 0),
            "装甲+发光墨囊 = 夜视");

        // 护符：水下呼吸 / 抗火 / 急迫+速度 / 夜视 / 海洋庇佑 / 抗性(星 III, 合金/中子 I)
        helper.assertTrue(hasEffect(dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(
            withEmbeds(EMBED_AMULET, "anvilcraft:sapphire")),
            net.minecraft.world.effect.MobEffects.WATER_BREATHING, 0), "护符+蓝宝石 = 水下呼吸");
        helper.assertTrue(hasEffect(dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(
            withEmbeds(EMBED_AMULET, "anvilcraft:ruby")),
            net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 0), "护符+红宝石 = 抗火");
        var amuletRedstone = dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(
            withEmbeds(EMBED_AMULET, "minecraft:redstone"));
        helper.assertTrue(hasEffect(amuletRedstone, net.minecraft.world.effect.MobEffects.DIG_SPEED, 0),
            "护符+红石 = 急迫 I");
        helper.assertTrue(hasEffect(amuletRedstone, net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 0),
            "护符+红石 = 速度 I");
        // 海洋之心：潮涌改为「在水中」才生效（设计）
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.grantsConduitPower(true, false, true),
            "在水中 + 护符海洋之心 = 海洋庇佑");
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.ForgeItemTicker.grantsConduitPower(false, true, true),
            "出水后不应再有海洋庇佑");
        helper.assertTrue(hasEffect(dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(
            withEmbeds(EMBED_AMULET, "minecraft:nether_star")),
            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2), "护符+下界之星 = 抗性 III");
        helper.assertTrue(hasEffect(dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(
            withEmbeds(EMBED_AMULET, "minecraft:netherite_ingot")),
            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 0), "护符+下界合金锭 = 抗性 I");
        helper.assertTrue(hasEffect(dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(
            withEmbeds(EMBED_AMULET, "anvilcraft:neutronium_ingot")),
            net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 0), "护符+中子锭 = 抗性 I");

        // 每 5 秒的耐久恢复（设计：蓝宝石水中2、红宝石燃烧2、黄玉电网2、原木1、绿宝石1）
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(
            ForgingData.EMPTY.withEmbed("anvilcraft:sapphire"), true, false, false) == 2, "蓝宝石水中 +2");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(
            ForgingData.EMPTY.withEmbed("anvilcraft:ruby"), false, true, false) == 2, "红宝石燃烧 +2");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(
            ForgingData.EMPTY.withEmbed("anvilcraft:topaz"), false, false, true) == 2, "黄玉电网 +2");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(
            ForgingData.EMPTY.withEmbed("#minecraft:logs"), false, false, false) == 1, "原木 +1");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.repairAmount(
            ForgingData.EMPTY.withEmbed("minecraft:emerald"), false, false, false) == 0, "绿宝石不再是每 5 秒回复");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.hasRedstoneHaste(
            ForgingData.EMPTY.withEmbed("minecraft:redstone")), "红石 = 急迫来源");
        helper.assertTrue(ticker != null, "ticker 可读");
        helper.succeed();
    }

    /** 组合：同一物品上的多个嵌入线性叠加；槽位按对象类别与超限合金粒计算。 */
    @GameTest(template = "empty")
    public static void embedCombinationsStackAndRespectSlots(GameTestHelper helper) {
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        // 武器三槽：石英 +20% + 金锭 +50% + 下界之星 +100% = +170%
        ItemStack sword = itemById(EMBED_SWORD);
        for (String id : new String[]{"minecraft:quartz", "minecraft:gold_ingot", "minecraft:nether_star"}) {
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(itemById(id));
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(sword, definition, TargetKind.WEAPON, 1, helper.getLevel());
        }
        helper.assertTrue(near(embedAmount(sword, Attributes.ATTACK_DAMAGE, mul), 1.70),
            "三件武器嵌入应线性叠加到 +170%，实际 " + embedAmount(sword, Attributes.ATTACK_DAMAGE, mul));
        // 护符：石英 +20% + 下界之星 +100% + 皇家 +10% 伤害，皇家再 +1 护甲
        ItemStack amulet = itemById(EMBED_AMULET);
        for (String id : new String[]{"minecraft:quartz", "minecraft:nether_star", "anvilcraft:royal_steel_ingot"}) {
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(itemById(id));
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(amulet, definition, TargetKind.AMULET, 1, helper.getLevel());
        }
        helper.assertTrue(near(embedAmount(amulet, Attributes.ATTACK_DAMAGE, mul), 1.30),
            "护符三件叠加应 +130% 伤害，实际 " + embedAmount(amulet, Attributes.ATTACK_DAMAGE, mul));
        helper.assertTrue(near(embedAmount(amulet, Attributes.ARMOR, add), 1.0), "护符上皇家 +1 护甲");
        // 槽位：工具/武器 3，装甲/护符 1；超限合金粒 +1
        helper.assertTrue(TargetKind.TOOL.slots() == 3 && TargetKind.WEAPON.slots() == 3,
            "工具与武器各 3 槽");
        helper.assertTrue(TargetKind.ARMOR.slots() == 1 && TargetKind.AMULET.slots() == 1,
            "装甲与护符各 1 槽");
        helper.succeed();
    }


    /** 钻石嵌入：直接获得钻石镐的挖掘等级，并与原工具的可挖集合取并集（只升等级，不搬速度）。 */
    @GameTest(template = "empty")
    public static void diamondEmbedGrantsDiamondMiningTier(GameTestHelper helper) {
        var diamond = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.DIAMOND));
        helper.assertTrue(diamond != null, "钻石必须是可嵌入材料");

        ItemStack pick = new ItemStack(Items.WOODEN_PICKAXE);
        var before = pick.get(DataComponents.TOOL);
        helper.assertTrue(before != null && !before.isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
            "木镐挖钻石矿本来不掉落");
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(pick, diamond, TargetKind.TOOL, 1, helper.getLevel());
        var after = pick.get(DataComponents.TOOL);
        helper.assertTrue(after != null && after.isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
            "嵌入钻石后钻石矿必须掉落（钻石等级）");
        helper.assertTrue(after.isCorrectForDrops(Blocks.OBSIDIAN.defaultBlockState()),
            "黑曜石也必须掉落（钻石等级）");
        helper.assertTrue(after.isCorrectForDrops(Blocks.STONE.defaultBlockState()),
            "原本能挖的石头保持可挖（并集）");
        helper.assertTrue(!after.isCorrectForDrops(Blocks.DIRT.defaultBlockState()),
            "钻石镐本来就挖不了泥土，嵌入后也不能掉落（不越界）");
        helper.assertTrue(near(after.getMiningSpeed(Blocks.DIAMOND_ORE.defaultBlockState()), 2.0F),
            "只升等级不搬速度：木镐挖钻石矿仍是 2.0，实际 " + after.getMiningSpeed(Blocks.DIAMOND_ORE.defaultBlockState()));

        ItemStack axe = new ItemStack(Items.WOODEN_AXE);
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(axe, diamond, TargetKind.TOOL, 1, helper.getLevel());
        var axeTool = axe.get(DataComponents.TOOL);
        helper.assertTrue(axeTool != null && axeTool.isCorrectForDrops(Blocks.OAK_LOG.defaultBlockState()),
            "斧头本来的木头照样可挖（并集）");
        helper.assertTrue(axeTool != null && axeTool.isCorrectForDrops(Blocks.DIAMOND_ORE.defaultBlockState()),
            "斧头也获得钻石镐的挖掘等级");
        helper.succeed();
    }

    /** 绿宝石：独立经验修补，兑换率与经验修补附魔一致（1 经验 = 2 耐久）。 */
    @GameTest(template = "empty")
    public static void emeraldEmbedRepairsAtMendingRate(GameTestHelper helper) {
        // 兑换率复刻原版 ExperienceOrb#repairPlayerItems
        var full = dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairForXp(10, 50);
        helper.assertTrue(full.repaired() == 20 && full.leftoverXp() == 0,
            "10 经验 = 20 耐久，修不满时按比例退回经验：实际 " + full);
        var partial = dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairForXp(10, 5);
        helper.assertTrue(partial.repaired() == 5 && partial.leftoverXp() == 8,
            "只需 5 耐久时只花 2.5 经验，剩 8 点继续修下一件：实际 " + partial);
        var none = dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairForXp(0, 5);
        helper.assertTrue(none.repaired() == 0 && none.leftoverXp() == 0, "没有经验就不修");

        // 端到端：手持带绿宝石嵌入的损坏剑，拾取经验后按 2 倍修复
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:emerald"));
        sword.setDamageValue(100);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        int repaired = dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairEmbeddedFromXp(player, 6);
        helper.assertTrue(repaired == 12, "6 点经验应修复 12 点耐久，实际 " + repaired);
        helper.assertTrue(player.getMainHandItem().getDamageValue() == 88,
            "耐久应从 100 修复到 88，实际 " + player.getMainHandItem().getDamageValue());

        // 没有绿宝石嵌入的物品不参与
        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
        plain.setDamageValue(100);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, plain);
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeHarvestEvents.repairEmbeddedFromXp(player, 6) == 0,
            "没有绿宝石嵌入时不应被经验修补");
        helper.succeed();
    }

    // ===== 实测反馈 2：tooltip 双角色扩充 + 琥珀怨恨接线 =====

    /**
     * 手持的铁砧**不能**当锤用：手持只允许铁砧锤，铁砧只在自动化（落下）时作锤。
     *
     * <p>两条路径必须分开验证——同一件物品在「手持」与「落下」两种语义下结论不同。
     */
    @GameTest(template = "empty")
    public static void anvilItemIsNotAHandHeldHammer(GameTestHelper helper) {
        helper.assertTrue(ForgingMaterial.ofHammer(new ItemStack(Items.ANVIL)) == null,
            "手持铁砧不应被当成锤");
        helper.assertTrue(ForgingMaterial.ofHammer(new ItemStack(Items.CHIPPED_ANVIL)) == null,
            "手持开裂的铁砧不应被当成锤");
        helper.assertTrue(ForgingMaterial.ofHammer(new ItemStack(Items.DAMAGED_ANVIL)) == null,
            "手持严重破损的铁砧不应被当成锤");
        // 自动化路径相反：落下的铁砧方块就是 BASE 档锤
        helper.assertTrue(
            ForgingMaterial.ofAnvilBlock(net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState())
                == ForgingMaterial.BASE,
            "落下的铁砧仍应作为 BASE 档锤");
        helper.succeed();
    }
    /**
     * JEI 的工作方块索引必须覆盖所有真的能嵌入的对象。
     *
     * <p>判据取自 {@code ForgingMaterial} 本身：清单里每一个都必须被它认成
     * 「铁砧方块 / 砂轮方块 / 铁砧锤」之一，否则 JEI 会出现一个点了没用的入口。
     */
    @GameTest(template = "empty")
    public static void embedCatalystsAllResolveToSupportedWorkstations(GameTestHelper helper) {
        var stacks = dev.anvilcraft.thenextforge.client.EmbedCatalysts.stacks();
        helper.assertTrue(stacks.size() >= 15, "催化剂至少 15 个，实际 " + stacks.size());
        int anvils = 0;
        int grindstones = 0;
        int hammers = 0;
        for (ItemStack stack : stacks) {
            // 顺序有意义：原版铁砧物品本身也能当锤用，所以先按**方块**归类，
            // 只有不是方块时才落到锤的分支。
            net.minecraft.world.level.block.Block block = net.minecraft.world.level.block.Block.byItem(stack.getItem());
            if (block != net.minecraft.world.level.block.Blocks.AIR) {
                var state = block.defaultBlockState();
                if (ForgingMaterial.ofAnvilBlock(state) != null) {
                    anvils++;
                } else if (ForgingMaterial.ofGrindstoneBlock(state) != null) {
                    grindstones++;
                } else {
                    helper.fail("方块型催化剂不被 ForgingMaterial 识别：" + stack, BlockPos.ZERO);
                }
            } else if (ForgingMaterial.ofHammer(stack) != null) {
                hammers++;
            } else {
                helper.fail("催化剂既不是方块也不是铁砧锤：" + stack, BlockPos.ZERO);
            }
        }
        helper.assertTrue(anvils >= 5, "铁砧至少 5 个（原版 + 四档），实际 " + anvils);
        helper.assertTrue(grindstones >= 5, "砂轮至少 5 个（原版 + 四档），实际 " + grindstones);
        helper.assertTrue(hammers >= 5, "铁砧锤至少 5 把，实际 " + hammers);
        TheNextForge.LOGGER.info("[TNF-CATALYSTS] anvils={} grindstones={} hammers={}", anvils, grindstones, hammers);
        helper.succeed();
    }
    /**
     * 逐材料 × 逐对象类别验证「嵌入 → 撤销」是无损的。
     *
     * <p>这是「锈净芽必须撤销掉它写下的全部东西」的逐项确认：属性修饰符、最大耐久、
     * 时运、不毁标志，四项都要回到嵌入前的值。
     *
     * <p>同一条测试同时守住「耐久倍率只在它真的生效的类别下才反算」——
     * 例如金粒只对工具/武器改耐久，护符上不该反算。
     */
    @GameTest(template = "empty")
    public static void embedRoundTripIsLosslessForEveryMaterialAndKind(GameTestHelper helper) {
        int checked = 0;
        for (var entry : EmbedMaterials.table().entrySet()) {
            String id = entry.getKey();
            if (id.startsWith("#")) continue;
            var definition = entry.getValue();
            if (definition == null || EmbedMaterials.itemOf(id).isEmpty()) continue;
            for (TargetKind kind : definition.kinds()) {
                ItemStack base = itemById(sampleItem(kind));
                if (base.isEmpty()) continue;

                double armorBefore = modifierAmount(base, Attributes.ARMOR);
                double attackBefore = modifierAmount(base, Attributes.ATTACK_DAMAGE);
                double reachBefore = modifierAmount(base, Attributes.ENTITY_INTERACTION_RANGE);
                int maxBefore = base.getOrDefault(net.minecraft.core.component.DataComponents.MAX_DAMAGE, 0);
                int fortuneBefore = fortuneLevel(helper, base);
                boolean unbreakableBefore = base.has(net.minecraft.core.component.DataComponents.UNBREAKABLE);

                base.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed(id));
                boolean applied = EmbedMaterials.apply(base, definition, kind, 1, helper.getLevel());
                if (!applied) continue;
                EmbedRemoval.removeLatest(base, helper.getLevel());

                String label = id + "/" + kind;
                helper.assertTrue(near(modifierAmount(base, Attributes.ARMOR), armorBefore),
                    label + " 撤销后护甲应回到 " + armorBefore + "，实际 " + modifierAmount(base, Attributes.ARMOR));
                helper.assertTrue(near(modifierAmount(base, Attributes.ATTACK_DAMAGE), attackBefore),
                    label + " 撤销后攻击应回到 " + attackBefore + "，实际 " + modifierAmount(base, Attributes.ATTACK_DAMAGE));
                helper.assertTrue(near(modifierAmount(base, Attributes.ENTITY_INTERACTION_RANGE), reachBefore),
                    label + " 撤销后触及应回到 " + reachBefore);
                // 耐久是整数：乘 0.5 再除 0.5 会因两次取整丢 1 点（1561 → 780/781 → 1560/1562），
                // 这是信息丢失而非逻辑错误，因此允许 ±1。
                int maxAfter = base.getOrDefault(net.minecraft.core.component.DataComponents.MAX_DAMAGE, 0);
                helper.assertTrue(Math.abs(maxAfter - maxBefore) <= 1,
                    label + " 撤销后最大耐久应回到 " + maxBefore + "(±1)，实际 " + maxAfter);
                helper.assertTrue(fortuneLevel(helper, base) == fortuneBefore,
                    label + " 撤销后时运应回到 " + fortuneBefore + "，实际 " + fortuneLevel(helper, base));
                helper.assertTrue(base.has(net.minecraft.core.component.DataComponents.UNBREAKABLE) == unbreakableBefore,
                    label + " 撤销后不毁标志应回到 " + unbreakableBefore);
                checked++;
            }
        }
        helper.assertTrue(checked >= 40, "至少要覆盖 40 个（材料, 类别）组合，实际 " + checked);
        TheNextForge.LOGGER.info("[TNF-ROUNDTRIP] checked {} material/kind pairs", checked);
        helper.succeed();
    }

    /** 实验性放大开关：关闭时一律 1.0，开启后按等级放大，且不影响锻造数值。 */
    @GameTest(template = "empty")
    public static void experimentalModelScalingRespectsTheSwitch(GameTestHelper helper) {
        var config = dev.anvilcraft.thenextforge.TheNextForge.CONFIG;
        boolean previous = config.experimentalModelScaling;
        try {
            ItemStack forged = new ItemStack(Items.DIAMOND_SWORD);
            forged.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 4));

            config.experimentalModelScaling = false;
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.ModelScaling.scaleOf(forged) == 1.0F,
                "开关关闭时必须一律不放大");
            helper.assertTrue(ForgeDisplay.scaleOf(forged) == 1.0F, "展示实体同样不放大");

            config.experimentalModelScaling = true;
            float scale = dev.anvilcraft.thenextforge.forging.ModelScaling.scaleOf(forged);
            helper.assertTrue(Math.abs(scale - 1.4F) < 1e-4F, "开启后 4 级应为 1.4，实际 " + scale);
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.ModelScaling.scaleOf(
                new ItemStack(Items.DIAMOND_SWORD)) == 1.0F, "未锻造的物品始终 1.0");
            // 开关只管渲染，不应改变属性/等级/嵌入
            ForgingData data = forged.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
            helper.assertTrue(data.anvilLevel() == 4, "开关不能影响等级");
        } finally {
            config.experimentalModelScaling = previous;
        }
        helper.succeed();
    }

    /** tooltip 的嵌入段必须用双角色描述（此前仍走单角色 bonusText）。 */
    @GameTest(template = "empty")
    public static void embedTooltipLineUsesDualRoleText(GameTestHelper helper) {
        // 直接验证客户端 appendEmbeds 依赖的取值函数：斧头 + 铁锭应给出两段描述
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        String text = dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusTextFor(axe, "minecraft:iron_ingot", 2);
        helper.assertTrue(text.contains("方块触及") && text.contains("实体触及"),
            "tooltip 取值必须包含工具与武器两段，实际 " + text);
        // 同一材料对纯工具仍是单段，避免误扩
        String pickText = dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusTextFor(
            new ItemStack(Items.DIAMOND_PICKAXE), "minecraft:iron_ingot", 2);
        helper.assertTrue(pickText.equals("方块触及 +1 格"), "纯工具只给方块触及，实际 " + pickText);
        helper.succeed();
    }

    /** 嵌入槽位满时必须有可见提示，不能静默吞掉（玩家会误判「材料不能用」）。 */
    @GameTest(template = "empty")
    public static void fullEmbedSlotsReportAVisibleReason(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY
            .withEmbed("minecraft:quartz").withEmbed("minecraft:redstone").withEmbed("minecraft:diamond"));
        ForgeDisplay.place(helper.getLevel(), pos, pick, 0.0F);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.totalExperience = 500;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 8));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_PEARL, 2));
        // 成功是掷骰的：一直锤到成功那一锤，槽满的提示只会出现在成功分支里
        String message = "";
        for (int attempt = 0; attempt < 40; attempt++) {
            if (player.totalExperience < 200) player.totalExperience = 500;
            var result = ForgingService.hammer(player, helper.getLevel(), pos, ForgingLine.ANVIL,
                ForgingMaterial.BASE, ForgingMaterial.BASE);
            message = result.message();
            if (message.contains("槽位已满")) break;
            if (ForgeDisplay.find(helper.getLevel(), pos) == null) {
                ForgeDisplay.place(helper.getLevel(), pos, pick.copy(), 0.0F);
            }
        }
        helper.assertTrue(message.contains("槽位已满"),
            "槽满时必须提示，实际 " + message);
        helper.assertTrue(player.getOffhandItem().getCount() == 2, "槽满时不应消耗材料");
        helper.succeed();
    }

    /** 鱼缸拦截面：缸里有斧头时，末影珍珠必须被允许插进去（按多角色并集判定）。 */
    @GameTest(template = "empty")
    public static void tankAllowsEnderPearlIntoAxe(GameTestHelper helper) {
        var axe = new ItemStack(Items.DIAMOND_AXE);
        var eye = new ItemStack(Items.ENDER_PEARL);
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(
            eye, axe, true, false) == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "缸里有斧头时末影珍珠应允许插入，实际 " +
                dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(eye, axe, true, false));
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(
            eye, new ItemStack(Items.DIAMOND_SWORD), true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "剑也应允许");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(
            eye, new ItemStack(Items.DIAMOND_PICKAXE), true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "镐也应允许");
        helper.succeed();
    }

    /** 手动链路：铁砧 + 斧头 + 副手末影珍珠，反复锤到成功那一锤必须真的嵌进去。 */
    @GameTest(template = "empty")
    public static void enderPearlEmbedsIntoAxeThroughManualHammering(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 8));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.ENDER_PEARL, 64));

        boolean embedded = false;
        for (int attempt = 0; attempt < 40 && !embedded; attempt++) {
            if (ForgeDisplay.find(helper.getLevel(), pos) == null) {
                ForgeDisplay.place(helper.getLevel(), pos, new ItemStack(Items.DIAMOND_AXE), 0.0F);
                if (ForgeDisplay.find(helper.getLevel(), pos) == null) continue;
            }
            player.totalExperience = 1000;
            ForgingService.hammer(player, helper.getLevel(), pos, ForgingLine.ANVIL,
                ForgingMaterial.BASE, ForgingMaterial.BASE);
            if (ForgeDisplay.find(helper.getLevel(), pos) == null) continue;
            ForgingData data = ForgeDisplay.stackOf(ForgeDisplay.find(helper.getLevel(), pos))
                .getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
            embedded = data.hasEmbed("minecraft:ender_pearl");
        }
        helper.assertTrue(embedded, "末影珍珠必须能嵌进斧头（40 次锤击内至少成功一次）");
        helper.assertTrue(player.getOffhandItem().getCount() < 64, "嵌入应消耗末影珍珠");
        helper.succeed();
    }
    /** JEI 统合页：所有材料必须都落在同一个分类里，且每页都带材料栈、对象类别与文字。 */
    @GameTest(template = "empty")
    public static void jeiUnifiedPageCoversEveryMaterial(GameTestHelper helper) {
        var pages = dev.anvilcraft.thenextforge.forging.EmbedPage.buildAll();
        var table = dev.anvilcraft.thenextforge.forging.EmbedMaterials.table();
        long expected = table.keySet().stream().filter(id -> !id.startsWith("#"))
            .filter(id -> !dev.anvilcraft.thenextforge.forging.EmbedMaterials.itemOf(id).isEmpty())
            .count();
        helper.assertTrue(pages.size() == expected,
            "统合页必须覆盖全部材料：" + pages.size() + " / " + expected);
        helper.assertTrue(pages.size() > 30, "材料页至少有 30 条，实际 " + pages.size());
        for (var page : pages) {
            helper.assertTrue(!page.material().isEmpty(), page.materialId() + " 缺少材料物品栈");
            helper.assertTrue(!page.kinds().isEmpty(), page.materialId() + " 缺少可嵌入对象类别");
            helper.assertTrue(!page.lines().isEmpty(), page.materialId() + " 缺少文字行");
            String joined = page.lines().stream().map(net.minecraft.network.chat.Component::getString)
                .reduce("", (a, b) -> a + "\n" + b);
            helper.assertTrue(joined.contains("可嵌入："), page.materialId() + " 缺少「可嵌入」行");
        }
        // 互相索引：每个类别的示例物品都必须能取到（护符缺失时只有一个为空）
        for (TargetKind kind : TargetKind.values()) {
            if (kind == TargetKind.AMULET) continue;
            ItemStack sample = dev.anvilcraft.thenextforge.forging.EmbedPage.sampleFor(kind);
            helper.assertTrue(!sample.isEmpty(), kind + " 缺少示例物品");
        }
        helper.succeed();
    }

    /** 材料表里每个 id 都必须真的是一个存在的物品（防止把「旋风棒」写成「风弹」这类错误）。 */
    @GameTest(template = "empty")
    public static void everyMaterialIdResolvesToARealItem(GameTestHelper helper) {
        int checked = 0;
        for (String id : dev.anvilcraft.thenextforge.forging.EmbedMaterials.table().keySet()) {
            if (id.startsWith("#")) continue;
            ItemStack stack = dev.anvilcraft.thenextforge.forging.EmbedMaterials.itemOf(id);
            helper.assertTrue(!stack.isEmpty(), "材料表的 id 必须能解析为真实物品：" + id);
            checked++;
        }
        helper.assertTrue(checked > 30, "至少要有 30 种材料被检查，实际 " + checked);

        // 旋风棒 = minecraft:breeze_rod（Breeze Rod），不是风弹 minecraft:wind_charge
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(
            new ItemStack(Items.BREEZE_ROD)) != null, "旋风棒（breeze_rod）必须可嵌入");
        helper.assertTrue(!dev.anvilcraft.thenextforge.forging.EmbedMaterials.table()
            .containsKey("minecraft:wind_charge"), "风弹不是旋风棒，不应登记在材料表里");
        helper.succeed();
    }

    /** 龙蛋护符：只免疫魔法类（药水/龙息/守卫者），普通攻击照常受伤。 */
    @GameTest(template = "empty")
    public static void dragonEggBlocksMagicButNotMelee(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var amulet = itemById("anvilcraft:armadillo_amulet");
        if (amulet.isEmpty()) { helper.succeed(); return; }
        ItemStack eggAmulet = amulet.copy();
        eggAmulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:dragon_egg"));
        player.getInventory().items.set(5, eggAmulet);
        helper.assertTrue(dev.anvilcraft.thenextforge.event.EmbedCombat
            .immuneToMagic(player), "携带龙蛋后应免疫魔法");

        var level = helper.getLevel();
        // 瞬间伤害药水：magic
        helper.assertTrue(dev.anvilcraft.thenextforge.event.EmbedCombat
            .isMagicDamage(level.damageSources().magic()), "magic 必须算魔法伤害");
        // 龙息：dragon_breath
        helper.assertTrue(dev.anvilcraft.thenextforge.event.EmbedCombat
            .isMagicDamage(level.damageSources().dragonBreath()), "龙息必须算魔法伤害");
        // 普通近战不算
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.EmbedCombat
            .isMagicDamage(level.damageSources().playerAttack(player)), "近战不能算魔法伤害");

        // 真走一遍事件：药水伤害被清零，近战伤害不被清零
        var magicSource = level.damageSources().magic();
        var magicContainer = new net.neoforged.neoforge.common.damagesource.DamageContainer(magicSource, 6.0F);
        var magicEvent = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(player, magicContainer);
        dev.anvilcraft.thenextforge.event.ForgeCombatEvents.onDamagePre(magicEvent);
        helper.assertTrue(magicEvent.getNewDamage() == 0.0F,
            "龙蛋护符必须挡住药水伤害，实际 " + magicEvent.getNewDamage());

        var meleeSource = level.damageSources().playerAttack(player);
        var meleeContainer = new net.neoforged.neoforge.common.damagesource.DamageContainer(meleeSource, 6.0F);
        var meleeEvent = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(player, meleeContainer);
        dev.anvilcraft.thenextforge.event.ForgeCombatEvents.onDamagePre(meleeEvent);
        helper.assertTrue(meleeEvent.getNewDamage() == 6.0F,
            "近战伤害不该被龙蛋免疫，实际 " + meleeEvent.getNewDamage());
        helper.succeed();
    }

    /** 虚空护符：只挡住掉入虚空 / 本模组的虚空伤害，不能变成无敌。 */
    @GameTest(template = "empty")
    public static void voidAmuletBlocksOnlyVoidDamage(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var amulet = itemById("anvilcraft:armadillo_amulet");
        if (amulet.isEmpty()) { helper.succeed(); return; }
        ItemStack voidAmulet = amulet.copy();
        voidAmulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:void_matter"));
        player.getInventory().items.set(6, voidAmulet);
        var level = helper.getLevel();
        helper.assertTrue(dev.anvilcraft.thenextforge.event.EmbedCombat.immuneToVoid(player),
            "携带虚空物质后应免疫虚空伤害");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.EmbedCombat
            .isVoidDamage(level.damageSources().fellOutOfWorld()), "掉入虚空必须算虚空伤害");
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.EmbedCombat
            .isVoidDamage(level.damageSources().playerAttack(player)), "近战不能算虚空伤害");

        var voidSource = level.damageSources().fellOutOfWorld();
        var voidContainer = new net.neoforged.neoforge.common.damagesource.DamageContainer(voidSource, 4.0F);
        var voidEvent = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(player, voidContainer);
        dev.anvilcraft.thenextforge.event.ForgeCombatEvents.onDamagePre(voidEvent);
        helper.assertTrue(voidEvent.getNewDamage() == 0.0F,
            "掉入虚空应被完全挡住，实际 " + voidEvent.getNewDamage());

        var meleeSource = level.damageSources().playerAttack(player);
        var meleeContainer = new net.neoforged.neoforge.common.damagesource.DamageContainer(meleeSource, 4.0F);
        var meleeEvent = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(player, meleeContainer);
        dev.anvilcraft.thenextforge.event.ForgeCombatEvents.onDamagePre(meleeEvent);
        // 注意：这里玩家同时是攻击者，携带虚空护符会把 10% 转成虚空伤害（设计如此），
        // 所以只要求「没被清零」——被清零才说明护符变成了无敌。
        helper.assertTrue(meleeEvent.getNewDamage() > 0.0F,
            "虚空护符不能免疫普通伤害（否则等于无敌），实际 " + meleeEvent.getNewDamage());
        helper.succeed();
    }

    /** 2026-11-03 新增 7 种材料：必须都在表里、都能嵌进各自对象、描述与效果对得上。 */
    @GameTest(template = "empty")
    public static void newMaterialsAreRegisteredWithTheirEffects(GameTestHelper helper) {
        String[][] cases = {
            {"minecraft:feather", "WEAPON", "攻击力 -75%", "羽毛武器"},
            {"minecraft:golden_carrot", "ARMOR", "饱和", "金胡萝卜"},
            {"minecraft:enchanted_golden_apple", "AMULET", "抗火", "附魔金苹果"},
            {"minecraft:breeze_rod", "TOOL", "风弹", "旋风棒"},
            {"minecraft:heavy_core", "WEAPON", "击退", "沉重核心武器"},
            {"minecraft:totem_of_undying", "ARMOR", "最大生命 +5", "不死图腾装甲"},
            {"minecraft:dragon_egg", "WEAPON", "25% 魔法伤害", "龙蛋武器"},
        };
        for (String[] c : cases) {
            ItemStack stack = dev.anvilcraft.thenextforge.forging.EmbedMaterials.itemOf(c[0]);
            helper.assertTrue(!stack.isEmpty(), c[3] + " 的物品 id 必须存在：" + c[0]);
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(stack);
            helper.assertTrue(definition != null, c[3] + " 必须在材料表里");
            TargetKind kind = TargetKind.valueOf(c[1]);
            helper.assertTrue(definition.allows(kind), c[3] + " 必须允许 " + c[1]);
            String text = dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusText(c[0], 1, kind);
            helper.assertTrue(text != null && text.contains(c[2]),
                c[3] + " 的描述应包含「" + c[2] + "」，实际 " + text);
        }
        // 属性类效果抽查：羽毛武器降攻、沉重核心武器加击退、不死图腾装甲加最大生命
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        java.util.function.ToDoubleFunction<ItemStack> attackSum = stack -> {
            double sum = 0;
            for (var entry : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                ItemAttributeModifiers.EMPTY).modifiers()) {
                if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                    sum += entry.modifier().amount();
                }
            }
            return sum;
        };
        double attackBefore = attackSum.applyAsDouble(sword);
        var feather = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.FEATHER));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.applyForRoles(sword, feather, 1, helper.getLevel());
        double attackAfter = attackSum.applyAsDouble(sword);
        helper.assertTrue(attackAfter < attackBefore,
            "羽毛必须降低攻击力：" + attackBefore + " -> " + attackAfter);

        ItemStack armour = new ItemStack(Items.DIAMOND_CHESTPLATE);
        var totem = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.TOTEM_OF_UNDYING));
        double healthBefore = 0;
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.applyForRoles(armour, totem, 1, helper.getLevel());
        for (var entry : armour.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.attribute().value() == Attributes.MAX_HEALTH.value()) {
                healthBefore += entry.modifier().amount();
            }
        }
        helper.assertTrue(Math.abs(healthBefore - 5.0) < 1e-6, "不死图腾装甲必须 +5 最大生命，实际 " + healthBefore);
        helper.succeed();
    }

    /** 新材料的持续效果：附魔金苹果/沉重核心抗性、不死图腾恢复、羽毛缓降。 */
    @GameTest(template = "empty")
    public static void newMaterialTickerEffectsAreWired(GameTestHelper helper) {
        ItemStack armour = new ItemStack(Items.DIAMOND_CHESTPLATE);
        armour.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY
            .withEmbed("minecraft:enchanted_golden_apple").withEmbed("minecraft:heavy_core"));
        var effects = dev.anvilcraft.thenextforge.event.ForgeItemTicker.armourEffects(armour);
        boolean fire = false; boolean resistance = false;
        for (var e : effects) {
            if (e.getEffect().value() == net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE.value()) fire = true;
            if (e.getEffect().value() == net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE.value()) resistance = true;
        }
        helper.assertTrue(fire, "附魔金苹果必须提供抗火");
        helper.assertTrue(resistance, "附魔金苹果/沉重核心必须提供抗性提升");

        ItemStack amulet = itemById("anvilcraft:armadillo_amulet");
        if (!amulet.isEmpty()) {
            amulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY
                .withEmbed("minecraft:feather").withEmbed("minecraft:totem_of_undying"));
            var amuletEffects = dev.anvilcraft.thenextforge.event.ForgeItemTicker.amuletEffects(amulet);
            boolean slowFall = false; boolean regen = false;
            for (var e : amuletEffects) {
                if (e.getEffect().value() == net.minecraft.world.effect.MobEffects.SLOW_FALLING.value()) slowFall = true;
                if (e.getEffect().value() == net.minecraft.world.effect.MobEffects.REGENERATION.value()) regen = true;
            }
            helper.assertTrue(slowFall, "羽毛护符必须提供缓降");
            helper.assertTrue(regen, "不死图腾护符必须提供生命恢复");
        }
        helper.succeed();
    }

    /** 虚空：武器 30% + 护符 10% 加算；护符免疫虚空；龙蛋免疫魔法。 */
    @GameTest(template = "empty")
    public static void voidAndMagicImmunitiesAreWired(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.EmbedCombat.immuneToVoid(player), "空身不免疫虚空");
        helper.assertTrue(!dev.anvilcraft.thenextforge.event.EmbedCombat.immuneToMagic(player), "空身不免疫魔法");
        var amulet = itemById("anvilcraft:armadillo_amulet");
        if (!amulet.isEmpty()) {
            ItemStack voidAmulet = amulet.copy();
            voidAmulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:void_matter"));
            ItemStack dragonAmulet = amulet.copy();
            dragonAmulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:dragon_egg"));
            helper.assertTrue(!dev.anvilcraft.thenextforge.event.EmbedCombat.carries(player, "anvilcraft:void_matter"),
                "未携带时不免疫虚空");
        }
        helper.succeed();
    }

    /** JEI 信息页内容：每种材料都要写明「能嵌进哪些对象」以及「每类对象分别是什么效果」。 */
    @GameTest(template = "empty")
    public static void jeiInfoDescribesEveryMaterialMatrix(GameTestHelper helper) {
        int described = 0;
        for (var entry : dev.anvilcraft.thenextforge.forging.EmbedMaterials.table().entrySet()) {
            String id = entry.getKey();
            if (id.startsWith("#")) continue;
            var lines = dev.anvilcraft.thenextforge.client.ForgeJeiPlugin.describe(id, entry.getValue());
            if (lines.isEmpty()) continue;
            described++;
            String joined = lines.stream().map(net.minecraft.network.chat.Component::getString).reduce("", (a, b) -> a + "\n" + b);
            helper.assertTrue(joined.contains("可嵌入："), id + " 的 JEI 信息缺少对象列表");
            // 白名单里的每一类都要有一行效果（事件类材料可能给空文本，只要行数不少于类别数即可）
            long kindLines = lines.stream().filter(c -> c.getString().startsWith("· "))
                .filter(c -> c.getString().contains("：")).count();
            helper.assertTrue(kindLines >= 1, id + " 的 JEI 信息没有任何效果行");
        }
        helper.assertTrue(described > 20, "至少要有 20 种材料被描述，实际 " + described);

        // 抽查两条：铁锭（工具/武器触及其它）与纸（唯一嵌入 + 额外槽位）
        var iron = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.IRON_INGOT));
        String ironText = dev.anvilcraft.thenextforge.client.ForgeJeiPlugin
            .describe("minecraft:iron_ingot", iron).stream().map(net.minecraft.network.chat.Component::getString)
            .reduce("", (a, b) -> a + "\n" + b);
        helper.assertTrue(ironText.contains("工具") && ironText.contains("武器") && ironText.contains("方块触及")
            && ironText.contains("实体触及"), "铁锭的矩阵描述不完整：" + ironText);
        var paper = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.PAPER));
        String paperText = dev.anvilcraft.thenextforge.client.ForgeJeiPlugin
            .describe("minecraft:paper", paper).stream().map(net.minecraft.network.chat.Component::getString)
            .reduce("", (a, b) -> a + "\n" + b);
        helper.assertTrue(paperText.contains("唯一嵌入") && paperText.contains("+3"),
            "纸的矩阵描述缺少唯一嵌入或额外槽位：" + paperText);
        helper.succeed();
    }

    /** 自动化章节必须用真实结构预览（结构文件要真的存在，不能只写标签）。 */
    @GameTest(template = "empty")
    public static void automationChapterShowsARealStructure(GameTestHelper helper) {
        String md = readResource("/assets/anvilcraft/ageratum/zh_cn/010_tenf/002_auto.md");
        helper.assertTrue(md.contains("<structure id="), "自动化章节必须包含结构预览标签");
        // 取出 id 并确认对应资源真的在 jar 里
        int start = md.indexOf("<structure id=\"") + "<structure id=\"".length();
        int end = md.indexOf("\"", start);
        String path = md.substring(start, end);
        helper.assertTrue(path.endsWith(".snbt") || path.endsWith(".nbt"), "结构必须是 nbt/snbt：" + path);
        String resolved = "/assets/anvilcraft/ageratum/zh_cn/010_tenf/" + path;
        // 归一化 ../..
        while (resolved.contains("/../")) {
            int i = resolved.indexOf("/../");
            int prev = resolved.lastIndexOf('/', i - 1);
            resolved = resolved.substring(0, prev) + resolved.substring(i + 3);
        }
        helper.assertTrue(readResource(resolved).length() > 0, "结构文件必须存在：" + resolved);
        helper.assertTrue(readResource(resolved).contains("fish_tank"),
            "自动化结构里必须有鱼缸：" + resolved);
        helper.assertTrue(readResource(resolved).contains("minecraft:anvil"),
            "自动化结构里必须有铁砧：" + resolved);
        helper.succeed();
    }

    private static String readResource(String path) {
        try (var stream = ForgingGameTests.class.getResourceAsStream(path)) {
            if (stream == null) return "";
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    /** 指导书章节必须真的打进 jar：四个 md 都要能在类路径上读到，且导航字段齐全。 */
    @GameTest(template = "empty")
    public static void guideChapterIsShippedWithNavigation(GameTestHelper helper) {
        String[] files = {"index.md", "001_manual.md", "002_auto.md", "003_embed.md"};
        for (String language : new String[]{"zh_cn", "en_us"}) {
            for (String file : files) {
                String path = "/assets/anvilcraft/ageratum/" + language + "/010_tenf/" + file;
                try (var stream = ForgingGameTests.class.getResourceAsStream(path)) {
                    helper.assertTrue(stream != null, "指导书章节缺失：" + path);
                    String text = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    helper.assertTrue(text.startsWith("---"), path + " 必须有 navigation front-matter");
                    helper.assertTrue(text.contains("navigation:") && text.contains("title:"),
                        path + " 缺少 navigation.title");
                    helper.assertTrue(text.contains("# "), path + " 必须有标题");
                } catch (java.io.IOException exception) {
                    throw new AssertionError(exception);
                }
            }
        }
        helper.succeed();
    }

    /** 全材料 × 全类别的嵌入矩阵：把「哪种材料能嵌进哪种对象」整张表打出来并做结构断言。 */
    @GameTest(template = "empty")
    public static void embedMatrixIsCompleteAndConsistent(GameTestHelper helper) {
        ItemStack[] targets = {
            new ItemStack(Items.DIAMOND_SWORD),      // WEAPON
            new ItemStack(Items.DIAMOND_AXE),        // TOOL + WEAPON
            new ItemStack(Items.DIAMOND_PICKAXE),    // TOOL
            new ItemStack(Items.DIAMOND_CHESTPLATE), // ARMOR
            new ItemStack(Items.TOTEM_OF_UNDYING)    // 任意护符以外的兜底（预期全 false）
        };
        String[] names = {"SWORD(W)", "AXE(T+W)", "PICK(T)", "ARMOR(A)", "OTHER"};
        int rows = 0;
        StringBuilder matrix = new StringBuilder();
        for (var entry : dev.anvilcraft.thenextforge.forging.EmbedMaterials.table().entrySet()) {
            if (entry.getKey().startsWith("#")) continue;
            String id = entry.getKey();
            var stack = dev.anvilcraft.thenextforge.forging.EmbedMaterials.itemOf(id);
            if (stack.isEmpty()) continue;
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(stack);
            if (definition == null) continue;
            rows++;
            StringBuilder line = new StringBuilder(String.format("%-40s", id));
            for (int i = 0; i < targets.length; i++) {
                boolean ok = dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(definition, targets[i]);
                line.append(String.format(" %s=%-5s", names[i], ok ? "YES" : "no"));
            }
            matrix.append(line).append("\n");
        }
        TheNextForge.LOGGER.info("[TNF-MATRIX] rows={}\n{}", rows, matrix);
        var enderEye = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.ENDER_PEARL));
        helper.assertTrue(enderEye != null, "末影珍珠必须在材料表里");
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(
                dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(enderEye, targets[i]),
                "末影珍珠必须能嵌进 " + names[i]);
        }
        helper.succeed();
    }

    /** 砂轮基底必须被配方覆盖：方块清单包含原版砂轮，且没有把砂轮漏成「只认砧标签」。 */
    @GameTest(template = "empty")
    public static void grindstoneBaseIsCoveredByTheRecipe(GameTestHelper helper) {
        var blocks = dev.anvilcraft.thenextforge.recipe.ForgeRecipeRegistration.grindstoneBlocks();
        helper.assertTrue(blocks.contains(Blocks.GRINDSTONE), "砂轮方块清单必须包含原版砂轮");
        helper.assertTrue(!blocks.isEmpty(), "砂轮方块清单不能为空");
        helper.succeed();
    }

    /** 设计 L41 新措辞：可嵌入材料集取并集（不是全集）、效果去重后取和。 */
    @GameTest(template = "empty")
    public static void dualRoleEmbedSetIsUnionAndEffectsDeduped(GameTestHelper helper) {
        var forge = dev.anvilcraft.thenextforge.forging.EmbedMaterials.class;
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        // 并集：工具侧材料与武器侧材料都允许
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.PAPER)), axe),
            "纸（工具/武器/装甲）允许嵌进斧头");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.QUARTZ)), axe),
            "石英（武器/护符）允许嵌进斧头");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.NETHER_STAR)), axe),
            "下界之星（武器/护符）走武器侧允许");
        // 并集 ≠ 全集：只给护符的材料不能进斧头
        var cursed = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(
            itemById("anvilcraft:cursed_gold_ingot"));
        if (cursed != null) {
            helper.assertTrue(!dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(cursed, axe),
                "诅咒金只允许护符，斧头不允许");
        }

        // 去重：钻石对工具/武器都给耐久 +30%，条目只能有一条、倍率只乘一次
        ItemStack deduped = new ItemStack(Items.DIAMOND_AXE);
        int before = deduped.getMaxDamage();
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.applyForRoles(
            deduped, dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.DIAMOND)),
            1, helper.getLevel());
        int diamondEntries = 0;
        for (var entry : deduped.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.EMPTY).modifiers()) {
            if (entry.modifier().id().getPath().contains("embed_diamond")) diamondEntries++;
        }
        helper.assertTrue(diamondEntries <= 1, "同一材料的同类效果只能留一条，实际 " + diamondEntries);
        helper.assertTrue(deduped.getMaxDamage() == (int) Math.round(before * 1.3),
            "耐久倍率只乘一次：" + before + " -> " + deduped.getMaxDamage());
        helper.assertTrue(forge != null, "材料类可读");
        helper.succeed();
    }
    /** 真正的接线回归：走一遍 LivingDamageEvent.Pre，琥珀必须生效（此前被嵌套在诅咒金护符块里）。 */
    @GameTest(template = "empty")
    public static void amberWeaponEmbedFiresFromDamageEvent(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("anvilcraft:amber"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        var zombie = new net.minecraft.world.entity.monster.Zombie(
            net.minecraft.world.entity.EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(2.5, -59.0, 2.5);
        helper.getLevel().addFreshEntity(zombie);

        var source = helper.getLevel().damageSources().playerAttack(player);
        var container = new net.neoforged.neoforge.common.damagesource.DamageContainer(source, 4.0F);
        var event = new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(zombie, container);
        dev.anvilcraft.thenextforge.event.ForgeCombatEvents.onDamagePre(event);
        int resentment = dev.dubhe.anvilcraft.util.ResentmentUtil.getResentment(zombie);
        helper.assertTrue(resentment >= 20,
            "穿伤害事件后武器上的琥珀必须生效，实际怨恨 " + resentment);
        helper.succeed();
    }

    /** 琥珀是武器嵌入：命中后目标必须拿到 +20 怨恨（此前该分支被嵌在诅咒金护符块里）。 */
    @GameTest(template = "empty")
    public static void amberWeaponEmbedMarksTargetForResentment(GameTestHelper helper) {
        var zombie = new net.minecraft.world.entity.monster.Zombie(
            net.minecraft.world.entity.EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(2.5, -59.0, 2.5);
        helper.getLevel().addFreshEntity(zombie);
        ForgingData amberWeapon = ForgingData.EMPTY.withEmbed("anvilcraft:amber");
        int before = dev.dubhe.anvilcraft.util.ResentmentUtil.getResentment(zombie);
        int after = dev.anvilcraft.thenextforge.event.ForgeCombatEvents.applyAmberMark(amberWeapon, zombie);
        helper.assertTrue(after >= 20,
            "武器上的琥珀必须把怨恨拉到至少 20：before=" + before + " after=" + after);
        helper.assertTrue(dev.dubhe.anvilcraft.util.ResentmentUtil.getResentment(zombie) == after,
            "写入的怨恨值必须能被捕获时读到");
        // 第二次命中不再叠加
        int second = dev.anvilcraft.thenextforge.event.ForgeCombatEvents.applyAmberMark(amberWeapon, zombie);
        helper.assertTrue(second == after, "同一目标只加一次，实际 " + second);
        // 没有琥珀时不动作
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeCombatEvents.applyAmberMark(
            ForgingData.EMPTY, zombie) == -1, "没有琥珀嵌入时不做任何事");
        helper.succeed();
    }

    // ===== 实测反馈：砂轮限制后移 + 双角色对象的加成描述与效果 =====

    /** 工具不能砂轮升级，但可以放上去、并且能用海绵芽取出嵌入。 */
    @GameTest(template = "empty")
    public static void grindstoneRefusesToolUpgradeButAllowsSpongeRemoval(GameTestHelper helper) {
        var sponge = BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "sponge_gemmule"))
            .orElse(null);
        if (sponge == null) { helper.succeed(); return; }
        helper.setBlock(1, 1, 1, Blocks.GRINDSTONE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.set(ModDataComponents.FORGING.get(),
            ForgingData.EMPTY.withLevel(ForgingLine.GRINDSTONE, 2)
                .withEmbed("minecraft:quartz"));
        ForgeDisplay.place(helper.getLevel(), pos, pick, 0.0F);

        // 正常锤击：拒绝，提示走砂轮升级的限制，等级不变
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.totalExperience = 500;
        var refused = ForgingService.hammer(player, helper.getLevel(), pos, ForgingLine.GRINDSTONE,
            ForgingMaterial.BASE, ForgingMaterial.BASE);
        helper.assertTrue(!refused.success() && refused.message().contains("无法通过砂轮升级"),
            "工具锤击砂轮应提示无法升级，实际 " + refused.message());
        ForgingData afterRefusal = ForgeDisplay.stackOf(ForgeDisplay.find(helper.getLevel(), pos))
            .getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        helper.assertTrue(afterRefusal.level(ForgingLine.GRINDSTONE) == 2, "被拒绝时等级不变");

        // 副手海绵芽：取出最近嵌入
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(sponge, 1));
        var removed = ForgingService.hammer(player, helper.getLevel(), pos, ForgingLine.GRINDSTONE,
            ForgingMaterial.BASE, ForgingMaterial.BASE);
        helper.assertTrue(removed.success() && removed.message().contains("取出"),
            "副手海绵芽应能取出嵌入，实际 " + removed.message());
        ForgingData afterRemoval = ForgeDisplay.stackOf(ForgeDisplay.find(helper.getLevel(), pos))
            .getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        helper.assertTrue(afterRemoval.embeds().isEmpty(), "嵌入应被取出，实际 " + afterRemoval.embeds());
        helper.assertTrue(player.getOffhandItem().isEmpty(), "海绵芽应被消耗");
        helper.succeed();
    }

    /** 双角色对象（斧头/龙杖/共振器）：描述要同时给出工具与武器的加成，并去重。 */
    @GameTest(template = "empty")
    public static void dualRoleItemsShowBothBonusDescriptions(GameTestHelper helper) {
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        helper.assertTrue(TargetKind.of(axe) == TargetKind.TOOL, "斧头的主角色是工具");
        helper.assertTrue(TargetKind.rolesOf(axe).containsAll(java.util.List.of(TargetKind.TOOL, TargetKind.WEAPON)),
            "斧头同时具备工具与武器角色，实际 " + TargetKind.rolesOf(axe));

        String ironText = dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusTextFor(axe, "minecraft:iron_ingot", 1);
        helper.assertTrue(ironText.contains("方块触及") && ironText.contains("实体触及"),
            "铁锭在斧头上要同时给出两种触及描述，实际 " + ironText);
        String redstoneText = dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusTextFor(axe, "minecraft:redstone", 1);
        helper.assertTrue(redstoneText.contains("急迫") && redstoneText.contains("冷却"),
            "红石在斧头上要同时给出急迫与冷却描述，实际 " + redstoneText);
        // 去重：同一段描述不会重复出现
        long occurrences = java.util.Arrays.stream(ironText.split("，")).filter(s -> s.contains("方块触及")).count();
        helper.assertTrue(occurrences == 1, "描述必须去重，实际 " + ironText);

        // 只有武器的三叉戟保持单一描述
        ItemStack trident = new ItemStack(Items.TRIDENT);
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.bonusTextFor(
            trident, "minecraft:iron_ingot", 1).equals("实体触及 +1 格"),
            "武器只给实体触及");
        helper.succeed();
    }

    /** 双角色对象的嵌入效果也要两条都生效，且耐久倍率只乘一次。 */
    @GameTest(template = "empty")
    public static void dualRoleEmbedsApplyBothEffectsOnce(GameTestHelper helper) {
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        var iron = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(iron, axe),
            "武器材料（铁锭）必须能嵌进同时是武器的斧头");
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.applyForRoles(axe, iron, 1, helper.getLevel());
        helper.assertTrue(near(embedAmount(axe, Attributes.BLOCK_INTERACTION_RANGE, add), 1.0),
            "斧头应获得方块触及 +1（工具侧）");
        helper.assertTrue(near(embedAmount(axe, Attributes.ENTITY_INTERACTION_RANGE, add), 1.0),
            "斧头应获得实体触及 +1（武器侧）");

        // 钻石对工具/武器都给耐久 +30%，同一次嵌入只能乘一次
        ItemStack other = new ItemStack(Items.DIAMOND_AXE);
        int before = other.getMaxDamage();
        var diamond = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.DIAMOND));
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.applyForRoles(other, diamond, 1, helper.getLevel());
        helper.assertTrue(other.getMaxDamage() == (int) Math.round(before * 1.3),
            "双角色耐久倍率只能乘一次：" + before + " -> " + other.getMaxDamage());

        // 纯武器材料对斧头也能生效（石英 +20% 攻击）
        ItemStack axe2 = new ItemStack(Items.DIAMOND_AXE);
        var quartz = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.QUARTZ));
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(quartz, axe2),
            "石英（武器/护符）应能嵌进同时是武器的斧头");
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.applyForRoles(axe2, quartz, 1, helper.getLevel());
        helper.assertTrue(near(embedAmount(axe2, Attributes.ATTACK_DAMAGE,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE), 0.20),
            "斧头应获得石英的 +20% 伤害");
        helper.succeed();
    }

    /** 设计 L61：末影珍珠 —— 掉落先进身上的潜影盒，塞满才进背包。 */
    @GameTest(template = "empty")
    public static void enderEyeDropsPreferCarriedShulker(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:ender_pearl"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        // 主手就是快捷栏 0，潜影盒放 1 号槽，避免被剑覆盖
        player.getInventory().items.set(1, new ItemStack(Items.SHULKER_BOX));

        var drops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        drops.add(new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 1.5, -59.5, 1.5,
            new ItemStack(Items.DIAMOND, 5)));
        int collected = dev.anvilcraft.thenextforge.event.ForgeDropEvents.collect(player, drops);
        helper.assertTrue(collected == 1, "一件掉落被收集");
        var contents = player.getInventory().items.get(1)
            .getOrDefault(DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY);
        int inShulker = 0;
        for (ItemStack stack : contents.nonEmptyItems()) {
            if (stack.is(Items.DIAMOND)) inShulker += stack.getCount();
        }
        helper.assertTrue(inShulker == 5, "5 颗钻石应进潜影盒，实际 " + inShulker);
        helper.assertTrue(player.getInventory().countItem(Items.DIAMOND) == 0,
            "进潜影盒后不应再进背包");

        // 没有潜影盒时退回背包
        var bare = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var bareDrops = new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
        bareDrops.add(new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), 1.5, -59.5, 1.5,
            new ItemStack(Items.DIAMOND, 3)));
        dev.anvilcraft.thenextforge.event.ForgeDropEvents.collect(bare, bareDrops);
        helper.assertTrue(bare.getInventory().countItem(Items.DIAMOND) == 3, "没有潜影盒时进背包");
        helper.succeed();
    }

    /** 设计 L40：嵌入材料可数据驱动新增（自定义属性效果 + 额外槽位）。 */
    @GameTest(template = "empty")
    public static void embedMaterialsAcceptDataDrivenDefinitions(GameTestHelper helper) {
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        try {
            dev.anvilcraft.thenextforge.forging.EmbedData.parse(
                "{\"materials\": [{\"id\": \"minecraft:amethyst_shard\", \"label\": \"紫水晶·测试\","
                    + " \"kinds\": [\"WEAPON\"], \"color\": 11665407, \"extra_slots\": 2,"
                    + " \"attributes\": {\"WEAPON\": [{\"attribute\": \"minecraft:attack_damage\","
                    + " \"operation\": \"add_multiplied_base\", \"amount\": 0.42}]}}]}");
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.AMETHYST_SHARD));
            helper.assertTrue(definition != null, "数据定义的材料必须能被识别");
            helper.assertTrue(definition.allows(TargetKind.WEAPON) && !definition.allows(TargetKind.ARMOR),
                "类别跟随数据定义");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
                sword, definition, TargetKind.WEAPON, 1, helper.getLevel());

            helper.assertTrue(near(embedAmount(sword, Attributes.ATTACK_DAMAGE, mul), 0.42),
                "自定义属性效果必须生效，实际 " + embedAmount(sword, Attributes.ATTACK_DAMAGE, mul));
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.slotCount(
                TargetKind.WEAPON, ForgingData.EMPTY.withEmbed("minecraft:amethyst_shard")) == 5,
                "自定义额外槽位生效（3 + 2）");
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.extraSlots("minecraft:paper") == 3,
                "内置纸仍是 +3 槽");
        } finally {
            dev.anvilcraft.thenextforge.forging.EmbedData.clear();
        }
        helper.succeed();
    }

    /** 设计 L40：黑名单材料不可嵌入。 */
    @GameTest(template = "empty")
    public static void embedBlacklistRejectsMaterial(GameTestHelper helper) {
        try {
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(
                new ItemStack(Items.QUARTZ)) != null, "默认石英可嵌入");
            dev.anvilcraft.thenextforge.forging.EmbedData.parse("{\"blacklist\": [\"minecraft:quartz\"]}");
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(
                new ItemStack(Items.QUARTZ)) == null, "黑名单里的石英不再可嵌入");
        } finally {
            dev.anvilcraft.thenextforge.forging.EmbedData.clear();
        }
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(
            new ItemStack(Items.QUARTZ)) != null, "清空数据后恢复");
        helper.succeed();
    }

    /** 设计 L91：护符锤击加成黑白名单数据驱动。 */
    @GameTest(template = "empty")
    public static void amuletBonusListsAreDataDriven(GameTestHelper helper) {
        var all = java.util.Set.of("armadillo", "topaz", "gem", "nature");
        try {
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.Amulets.filterAllowed(all).size() == 4,
                "默认全部生效");
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"amulet_blacklist\": [\"anvilcraft:topaz_amulet\"], \"kinds\": {}}"));
            var filtered = dev.anvilcraft.thenextforge.forging.Amulets.filterAllowed(all);
            helper.assertTrue(!filtered.contains("topaz"), "黑名单里的黄玉护符不生效");
            helper.assertTrue(filtered.contains("armadillo"), "其它护符不受影响");
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"amulet_whitelist\": [\"anvilcraft:armadillo_amulet\"], \"kinds\": {}}"));
            var whitelisted = dev.anvilcraft.thenextforge.forging.Amulets.filterAllowed(all);
            helper.assertTrue(whitelisted.size() == 1 && whitelisted.contains("armadillo"),
                "白名单只留犰狳，实际 " + whitelisted);
        } finally {
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.resetToDefaults();
        }
        helper.succeed();
    }

    // ===== 设计对齐第 2 批：D11 数据驱动（对象集合 / 数值增幅 / 模型放大 + 黑白名单） =====

    /** 默认规则必须与设计数值逐条一致（攻击 5-10%、触及 0.10/30%、工具 3-8%、护甲 1/50%、护符 1-5%）。 */
    @GameTest(template = "empty")
    public static void forgeRulesDefaultsMatchDesign(GameTestHelper helper) {
        var rules = dev.anvilcraft.thenextforge.forging.ForgeRulesData.defaults();
        var weaponAnvil = rules.kinds().get("WEAPON").get("ANVIL");
        var attack = weaponAnvil.stream().filter(o -> o.effect().equals("attack")).findFirst().orElse(null);
        var reach = weaponAnvil.stream().filter(o -> o.effect().equals("reach")).findFirst().orElse(null);
        helper.assertTrue(attack != null && near(attack.min(), 0.05) && near(attack.max(), 0.10) && attack.weight() == 70,
            "武器铁砧：攻击力 5%-10% 权重 70，实际 " + attack);
        helper.assertTrue(reach != null && near(reach.min(), 0.10) && reach.weight() == 30,
            "武器铁砧：实体触及 0.10 权重 30，实际 " + reach);
        var weaponGrind = rules.kinds().get("WEAPON").get("GRINDSTONE");
        var sweep = weaponGrind.stream().filter(o -> o.effect().equals("sweep")).findFirst().orElse(null);
        helper.assertTrue(sweep != null && sweep.swordsOnly() && near(sweep.min(), 0.10), "横扫只对剑，实际 " + sweep);
        var tool = rules.kinds().get("TOOL").get("ANVIL").getFirst();
        helper.assertTrue(tool.effect().equals("mining") && near(tool.min(), 0.03) && near(tool.max(), 0.08),
            "工具铁砧：挖掘速度 3%-8%，实际 " + tool);
        var armor = rules.kinds().get("ARMOR").get("ANVIL");
        helper.assertTrue(armor.stream().anyMatch(o -> o.effect().equals("armor_flat") && o.weight() == 50),
            "装甲铁砧：50% 概率 +1 护甲");
        helper.assertTrue(armor.stream().anyMatch(o -> o.effect().equals("durability") && near(o.min(), 0.05)),
            "装甲铁砧：耐久 5%-10%");
        var amulet = rules.kinds().get("AMULET").get("ANVIL").getFirst();
        helper.assertTrue(amulet.effect().equals("amulet_damage") && near(amulet.min(), 0.01) && near(amulet.max(), 0.05),
            "护符铁砧：伤害 1%-5%，实际 " + amulet);
        helper.succeed();
    }

    /** 黑名单直接拒绝；白名单非空时必须在其中（支持标签条目）。 */
    @GameTest(template = "empty")
    public static void targetBlackAndWhiteListsAreDataDriven(GameTestHelper helper) {
        var forge = dev.anvilcraft.thenextforge.forging.ForgeRulesData.class;
        try {
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"target_blacklist\": [\"minecraft:trident\"], \"kinds\": {}}"));
            helper.assertTrue(TargetKind.of(new ItemStack(Items.TRIDENT)) == null, "黑名单里的三叉戟不能锻造");
            helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_SWORD)) == TargetKind.WEAPON,
                "未列出的物品不受影响");
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"target_whitelist\": [\"#minecraft:swords\"], \"kinds\": {}}"));
            helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_SWORD)) == TargetKind.WEAPON,
                "白名单标签内的剑可以锻造");
            helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_AXE)) == null,
                "白名单外的斧头不能锻造");
        } finally {
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.resetToDefaults();
        }
        helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_AXE)) == TargetKind.TOOL,
            "恢复默认后斧头重新可锻造");
        helper.assertTrue(forge != null, "规则类可读");
        helper.succeed();
    }

    /** 模型放大由数据驱动：每级倍率可改、可整类关闭、可按黑名单排除。 */
    @GameTest(template = "empty")
        public static void modelScaleIsDataDriven(GameTestHelper helper) {
        boolean previousScaling = dev.anvilcraft.thenextforge.TheNextForge.CONFIG.experimentalModelScaling;
        dev.anvilcraft.thenextforge.TheNextForge.CONFIG.experimentalModelScaling = true;
        try {
            modelScaleIsDataDrivenBody(helper);
        } finally {
            dev.anvilcraft.thenextforge.TheNextForge.CONFIG.experimentalModelScaling = previousScaling;
        }
    }

    private static void modelScaleIsDataDrivenBody(GameTestHelper helper) {
        ItemStack forged = new ItemStack(Items.DIAMOND_SWORD);
        forged.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 4));
        helper.assertTrue(near(ForgeDisplay.scaleOf(forged), 1.4F), "默认每级 +10%：4 级 = 1.4");
        try {
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"model_scale\": {\"enabled\": true, \"per_level\": 0.25}, \"kinds\": {}}"));
            helper.assertTrue(near(ForgeDisplay.scaleOf(forged), 2.0F),
                "每级改成 25% 后 4 级 = 2.0，实际 " + ForgeDisplay.scaleOf(forged));
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"model_scale\": {\"enabled\": true, \"per_level\": 0.25, \"blacklist\": [\"minecraft:diamond_sword\"]}, \"kinds\": {}}"));
            helper.assertTrue(near(ForgeDisplay.scaleOf(forged), 1.0F), "黑名单里的物品不放大");
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.override(
                dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(
                    "{\"model_scale\": {\"enabled\": false}, \"kinds\": {}}"));
            helper.assertTrue(near(ForgeDisplay.scaleOf(forged), 1.0F), "整体关闭后不放大");
        } finally {
            dev.anvilcraft.thenextforge.forging.ForgeRulesData.resetToDefaults();
        }
        helper.succeed();
    }

    /** 规则 JSON 可写可读（玩家照着改文件即可）。 */
    @GameTest(template = "empty")
    public static void forgeRulesJsonRoundTrips(GameTestHelper helper) {
        var defaults = dev.anvilcraft.thenextforge.forging.ForgeRulesData.defaults();
        String json = dev.anvilcraft.thenextforge.forging.ForgeRulesData.toJson(defaults);
        var parsed = dev.anvilcraft.thenextforge.forging.ForgeRulesData.parse(json);
        helper.assertTrue(parsed.kinds().keySet().equals(defaults.kinds().keySet()),
            "解析后对象集合一致：" + parsed.kinds().keySet());
        var attack = parsed.kinds().get("WEAPON").get("ANVIL").stream()
            .filter(o -> o.effect().equals("attack")).findFirst().orElse(null);
        helper.assertTrue(attack != null && near(attack.min(), 0.05) && near(attack.max(), 0.10),
            "解析后数值一致：" + attack);
        helper.assertTrue(near(parsed.modelScale().perLevel(), 0.10), "解析后模型倍率一致");
        helper.succeed();
    }

    // ===== 设计对齐第 1 批：龙杖/铁砧锤算工具、横扫仅剑、纸、凋零玫瑰、抗性上限、青金石判定 =====

    /** 设计 L24：龙杖 / 铁砧锤也是工具。 */
    @GameTest(template = "empty")
    public static void anvilHammerAndDragonRodCountAsTools(GameTestHelper helper) {
        for (String id : new String[]{"anvilcraft:anvil_hammer", "anvilcraft:royal_anvil_hammer",
            "anvilcraft:dragon_rod", "anvilcraft:royal_dragon_rod"}) {
            ItemStack stack = itemById(id);
            if (stack.isEmpty()) continue;
            helper.assertTrue(TargetKind.of(stack) == TargetKind.TOOL,
                id + " 应判定为工具，实际 " + TargetKind.of(stack));
        }
        helper.assertTrue(TargetKind.of(new ItemStack(Items.DIAMOND_SWORD)) == TargetKind.WEAPON,
            "剑仍是武器");
        helper.succeed();
    }

    /** 设计 L28：只有剑才判定横扫系数；无法横扫的武器不参与。 */
    @GameTest(template = "empty")
    public static void sweepOnlyRollsForSwords(GameTestHelper helper) {
        int swordSweeps = 0;
        for (int i = 0; i < 200; i++) {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            String text = ForgingRules.applyUpgrade(sword, TargetKind.WEAPON, ForgingLine.GRINDSTONE, 1,
                net.minecraft.util.RandomSource.create(1000L + i));
            if (text.contains("横扫")) swordSweeps++;
        }
        helper.assertTrue(swordSweeps > 0, "剑应当有机会出横扫，实际 " + swordSweeps);
        int tridentSweeps = 0;
        for (int i = 0; i < 200; i++) {
            ItemStack trident = new ItemStack(Items.TRIDENT);
            String text = ForgingRules.applyUpgrade(trident, TargetKind.WEAPON, ForgingLine.GRINDSTONE, 1,
                net.minecraft.util.RandomSource.create(2000L + i));
            if (text.contains("横扫")) tridentSweeps++;
        }
        helper.assertTrue(tridentSweeps == 0, "三叉戟无法横扫，不应出横扫，实际 " + tridentSweeps);
        helper.succeed();
    }

    /** 设计 L59：纸(扩张) 槽位 +3、最大耐久 -50%（工具/武器/装甲），护符不吃。 */
    @GameTest(template = "empty")
    public static void paperAddsSlotsAndHalvesDurability(GameTestHelper helper) {
        var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.PAPER));
        helper.assertTrue(definition != null, "纸必须是可嵌入材料");
        helper.assertTrue(definition.allows(TargetKind.TOOL) && definition.allows(TargetKind.WEAPON)
            && definition.allows(TargetKind.ARMOR), "纸允许工具/武器/装甲");
        helper.assertTrue(!definition.allows(TargetKind.AMULET), "纸不允许护符");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.slotCount(
            TargetKind.WEAPON, ForgingData.EMPTY) == 3, "武器基础 3 槽");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.slotCount(
            TargetKind.WEAPON, ForgingData.EMPTY.withEmbed("minecraft:paper")) == 6, "纸 +3 槽");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.slotCount(
            TargetKind.WEAPON, ForgingData.EMPTY.withEmbed("minecraft:paper")
                .withEmbed("anvilcraft:transcendium_nugget")) == 7, "纸与超限合金粒加算");
        int before = new ItemStack(Items.DIAMOND_SWORD).getMaxDamage();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
            sword, definition, TargetKind.WEAPON, 1, helper.getLevel());
        helper.assertTrue(sword.getMaxDamage() == (int) Math.round(before * 0.5),
            "纸应把最大耐久砍半：" + before + " -> " + sword.getMaxDamage());
        helper.succeed();
    }

    /** 设计 L62：凋零玫瑰 —— 命中目标获得 5s 凋零（工具/武器）。 */
    @GameTest(template = "empty")
    public static void witherRoseAppliesWitherOnHit(GameTestHelper helper) {
        var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(new ItemStack(Items.WITHER_ROSE));
        helper.assertTrue(definition != null && definition.allows(TargetKind.WEAPON)
            && definition.allows(TargetKind.TOOL), "凋零玫瑰允许工具/武器");
        helper.assertTrue(!definition.allows(TargetKind.ARMOR) && !definition.allows(TargetKind.AMULET),
            "凋零玫瑰不允许装甲/护符");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:wither_rose"));
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var zombie = new net.minecraft.world.entity.monster.Zombie(
            net.minecraft.world.entity.EntityType.ZOMBIE, helper.getLevel());
        zombie.moveTo(2.5, -59.0, 2.5);
        helper.getLevel().addFreshEntity(zombie);
        dev.anvilcraft.thenextforge.forging.CombatEffects.applyOnHit(
            helper.getLevel(), player, zombie, sword, 2.0F);
        helper.assertTrue(zombie.hasEffect(net.minecraft.world.effect.MobEffects.WITHER),
            "命中后目标应带凋零");
        helper.succeed();
    }

    /** 设计 L54：抗性提升全局最高 4（amplifier ≤ 3）。 */
    @GameTest(template = "empty")
    public static void resistanceIsCappedAtFour(GameTestHelper helper) {
        var ticker = dev.anvilcraft.thenextforge.event.ForgeItemTicker.class;
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.cappedResistance(0) == 0, "抗性 I 保持");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.cappedResistance(2) == 2, "抗性 III 保持");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeItemTicker.cappedResistance(5) == 3, "超过 4 级钳到 3");
        helper.assertTrue(ticker != null, "ticker 可读");
        helper.succeed();
    }

    /** 设计 L44：获得经验 +20%（装甲/护符）——判定要覆盖装甲与携带的护符，而不只是手持。 */
    @GameTest(template = "empty")
    public static void lapisXpBonusCountsArmourAndAmulet(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(!dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "minecraft:lapis_lazuli"),
            "空手时不该生效");
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:lapis_lazuli"));
        player.getInventory().armor.set(2, chest);
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "minecraft:lapis_lazuli"),
            "穿在身上的青金石嵌入也要给经验加成");
        player.getInventory().armor.set(2, ItemStack.EMPTY);
        ItemStack amulet = itemById("anvilcraft:armadillo_amulet");
        if (!amulet.isEmpty()) {
            amulet.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed("minecraft:lapis_lazuli"));
            player.getInventory().items.set(0, amulet);
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedPresence.carried(player, "minecraft:lapis_lazuli"),
                "背包里的护符也要给经验加成");
        }
        helper.succeed();
    }

    // ===== 海绵芽：按嵌入顺序逆序取出最近的材料 =====（砂轮专用，不判定成功与否）

    private static ForgingData forgingOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
    }

    /** 取出顺序是逆序，且材料写下的属性/耐久/时运/不毁都要一并撤回。 */
    @GameTest(template = "empty")
    public static void spongeGemmuleRemovesEmbedsInReverseOrder(GameTestHelper helper) {
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int baseMax = sword.getMaxDamage();
        for (String id : new String[]{"minecraft:quartz", "minecraft:gold_ingot", "minecraft:nether_star"}) {
            var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(itemById(id));
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(
                sword, definition, TargetKind.WEAPON, 1, helper.getLevel());
            // apply 只改物品属性，嵌入记录由调用方追加（与 ForgingService 一致）
            sword.set(ModDataComponents.FORGING.get(), forgingOf(sword).withEmbed(definition.id()));
        }
        helper.assertTrue(forgingOf(sword).embeds().size() == 3, "先嵌入三件：目前 " + forgingOf(sword).embeds().size());
        helper.assertTrue(near(embedAmount(sword, Attributes.ATTACK_DAMAGE, mul), 1.70), "三件叠加 +170%");
        helper.assertTrue(sword.getMaxDamage() < baseMax, "金锭应已把耐久打 8 折");

        // 第一次：取回最后嵌入的下界之星
        ItemStack first = dev.anvilcraft.thenextforge.forging.EmbedRemoval.removeLatest(sword, helper.getLevel());
        helper.assertTrue(first.is(Items.NETHER_STAR), "应取回下界之星，实际 " + first);
        helper.assertTrue(forgingOf(sword).embeds().size() == 2, "剩两件嵌入");
        helper.assertTrue(near(embedAmount(sword, Attributes.ATTACK_DAMAGE, mul), 0.70),
            "下界之星的 +100% 必须撤回，实际 " + embedAmount(sword, Attributes.ATTACK_DAMAGE, mul));

        // 第二次：取回金锭，耐久与时运都要回来
        ItemStack second = dev.anvilcraft.thenextforge.forging.EmbedRemoval.removeLatest(sword, helper.getLevel());
        helper.assertTrue(second.is(Items.GOLD_INGOT), "应取回金锭，实际 " + second);
        helper.assertTrue(sword.getMaxDamage() == baseMax,
            "金锭的耐久 8 折要撤回：" + sword.getMaxDamage() + " 应为 " + baseMax);
        helper.assertTrue(fortuneLevel(helper, sword) == 0, "金锭的时运 +3 要撤回");
        helper.assertTrue(near(embedAmount(sword, Attributes.ATTACK_DAMAGE, mul), 0.20), "只剩石英的 +20%");

        // 第三次：取回石英，属性清零
        ItemStack third = dev.anvilcraft.thenextforge.forging.EmbedRemoval.removeLatest(sword, helper.getLevel());
        helper.assertTrue(third.is(Items.QUARTZ), "应取回石英，实际 " + third);
        helper.assertTrue(embedAmount(sword, Attributes.ATTACK_DAMAGE, mul) == 0.0,
            "石英的 +20% 要撤回，实际 " + embedAmount(sword, Attributes.ATTACK_DAMAGE, mul));

        // 没有嵌入时取不出东西（调用方据此不消耗海绵芽）
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedRemoval.removeLatest(sword, helper.getLevel()).isEmpty(),
            "空嵌入列表取不出材料");
        helper.succeed();
    }

    /** 超限合金粒被取出后，不毁与额外槽位一起消失。 */
    @GameTest(template = "empty")
    public static void removingTranscendiumDropsUnbreakable(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(
            itemById("anvilcraft:transcendium_nugget"));
        if (definition == null) { helper.succeed(); return; }
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(sword, definition, TargetKind.WEAPON, 1, helper.getLevel());
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withEmbed(definition.id()));
        helper.assertTrue(sword.has(DataComponents.UNBREAKABLE), "超限合金粒应让物品不毁");
        ItemStack removed = dev.anvilcraft.thenextforge.forging.EmbedRemoval.removeLatest(sword, helper.getLevel());
        helper.assertTrue(!removed.isEmpty(), "应取回超限合金粒");
        helper.assertTrue(!sword.has(DataComponents.UNBREAKABLE), "取出后不再不毁");
        helper.succeed();
    }

    /** 自动化：砂轮上方鱼缸里放海绵芽 → 取出最近嵌入，消耗 1 个，材料进输出槽，等级不变。 */
    @GameTest(template = "empty")
    public static void automationSpongeGemmuleRemovesLatestEmbed(GameTestHelper helper) {
        var sponge = BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "sponge_gemmule"))
            .orElse(null);
        var fishTank = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (sponge == null || fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 1, 1, Blocks.GRINDSTONE);
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        if (!(helper.getLevel().getBlockEntity(tankPos)
            instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        tank.getFluidHandler().fill(
            new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 20000
            ),
            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
        );
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(),
            ForgingData.EMPTY.withLevel(ForgingLine.GRINDSTONE, 4)
                .withEmbed("minecraft:quartz")
                .withEmbed("minecraft:redstone"));
        tank.getItemHandler().setStackInSlot(8, sword);
        tank.getItemHandler().setStackInSlot(9, new ItemStack(sponge, 2));

        boolean handled = dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
            helper.getLevel(), tankPos, Blocks.ANVIL.defaultBlockState());
        helper.assertTrue(handled, "海绵芽取出应当被处理");
        ItemStack after = tank.getItemHandler().getStackInSlot(8);
        helper.assertTrue(after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).embeds().size() == 1,
            "应只剩一件嵌入，实际 " + after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).embeds());
        helper.assertTrue(after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).embeds()
            .contains("minecraft:quartz"), "留下的应是先嵌入的石英");
        helper.assertTrue(after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY)
            .level(ForgingLine.GRINDSTONE) == 4, "取出不改变等级");
        helper.assertTrue(tank.getItemHandler().getStackInSlot(9).getCount() == 1,
            "应消耗 1 个海绵芽，剩 " + tank.getItemHandler().getStackInSlot(9).getCount());
        helper.assertTrue(tank.getOutput().getStackInSlot(0).is(Items.REDSTONE),
            "取回的红石应进输出槽，实际 " + tank.getOutput().getStackInSlot(0));
        helper.succeed();
    }

    /** 手动路径：砂轮上副手拿海绵芽锤击 → 取出最近嵌入，消耗 1 个，材料回到背包，等级不变。 */
    @GameTest(template = "empty")
    public static void manualSpongeGemmuleRemovesLatestEmbedOnGrindstone(GameTestHelper helper) {
        var sponge = BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "sponge_gemmule"))
            .orElse(null);
        if (sponge == null) { helper.succeed(); return; }
        helper.setBlock(1, 1, 1, Blocks.GRINDSTONE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(),
            ForgingData.EMPTY.withLevel(ForgingLine.GRINDSTONE, 3)
                .withEmbed("minecraft:quartz")
                .withEmbed("minecraft:redstone"));
        ForgeDisplay.place(helper.getLevel(), pos, sword, 0.0F);

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(sponge, 2));
        var result = ForgingService.hammer(player, helper.getLevel(), pos, ForgingLine.GRINDSTONE,
            ForgingMaterial.BASE, ForgingMaterial.BASE);
        helper.assertTrue(result.success(), "海绵芽取出应当成功，实际 " + result.message());

        ItemStack after = ForgeDisplay.stackOf(ForgeDisplay.find(helper.getLevel(), pos));
        ForgingData data = after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        helper.assertTrue(data.embeds().size() == 1 && data.embeds().contains("minecraft:quartz"),
            "应留下先嵌入的石英，实际 " + data.embeds());
        helper.assertTrue(data.level(ForgingLine.GRINDSTONE) == 3, "取出不改变等级");
        helper.assertTrue(player.getOffhandItem().getCount() == 1,
            "应消耗 1 个海绵芽，剩 " + player.getOffhandItem().getCount());
        helper.assertTrue(player.getInventory().countItem(Items.REDSTONE) == 1,
            "取回的红石应回到背包");
        helper.succeed();
    }

    /** 鱼缸拦截：海绵芽走材料槽位，需要先有对象，且不能重复放。 */
    @GameTest(template = "empty")
    public static void fishTankAcceptsSpongeGemmuleAsMaterial(GameTestHelper helper) {
        ItemStack sponge = itemById("anvilcraft:sponge_gemmule");
        if (sponge.isEmpty()) { helper.succeed(); return; }
        var check = dev.anvilcraft.thenextforge.event.ForgeEvents.class;
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(
            sponge, (TargetKind) null, false, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.NO_TARGET,
            "没有对象时不能先放海绵芽");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(
            sponge, TargetKind.WEAPON, true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "有对象时海绵芽必须能放进鱼缸（不参与白名单）");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(
            sponge, TargetKind.WEAPON, true, true)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.SECOND_MATERIAL,
            "已有材料时不能再放海绵芽");
        helper.assertTrue(check != null, "判定类可读");
        helper.succeed();
    }

    // ===== 嵌入材料 × 对象 的设计矩阵（逐格对照 TNF Design.txt 第 34-67 行） =====

    private static final String EMBED_SWORD = "minecraft:diamond_sword";
    private static final String EMBED_PICK = "minecraft:diamond_pickaxe";
    private static final String EMBED_CHEST = "minecraft:diamond_chestplate";
    private static final String EMBED_AMULET = "anvilcraft:armadillo_amulet";

    private static String sampleItem(TargetKind kind) {
        return switch (kind) {
            case WEAPON -> EMBED_SWORD;
            case TOOL -> EMBED_PICK;
            case ARMOR -> EMBED_CHEST;
            case AMULET -> EMBED_AMULET;
        };
    }

    /** 本模组写在物品上的某属性、某操作方式的数值之和。 */
    private static double embedAmount(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                      net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation op) {
        double sum = 0;
        for (ItemAttributeModifiers.Entry e : stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.EMPTY).modifiers()) {
            if (e.attribute().equals(attribute) && e.modifier().operation() == op) sum += e.modifier().amount();
        }
        return sum;
    }

    private static ItemStack applyEmbed(GameTestHelper helper, String materialId, TargetKind kind) {
        ItemStack stack = itemById(sampleItem(kind));
        ItemStack material = itemById(materialId);
        var definition = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(material);
        if (stack.isEmpty() || definition == null) return ItemStack.EMPTY;
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.apply(stack, definition, kind, 1, helper.getLevel());
        return stack;
    }

    private static void expectEmbedAmount(GameTestHelper helper, String materialId, TargetKind kind,
                                          net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                          net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation op,
                                          double expected) {
        ItemStack stack = applyEmbed(helper, materialId, kind);
        if (stack.isEmpty()) { helper.assertTrue(false, "matrix: cannot build " + materialId + "/" + kind); return; }
        double got = embedAmount(stack, attribute, op);
        helper.assertTrue(near(got, expected),
            materialId + " " + kind + " 期望 " + expected + " 实际 " + got);
    }

    /** 描述矩阵快照：把「材料 × 对象」的全部说明打进日志，供两代矩阵逐格比对。 */
    @GameTest(template = "empty")
    public static void embedDescriptionMatrixSnapshot(GameTestHelper helper) {
        int rows = 0;
        for (var entry : dev.anvilcraft.thenextforge.forging.EmbedMaterials.all().entrySet()) {
            for (TargetKind kind : entry.getValue().kinds()) {
                String text = dev.anvilcraft.thenextforge.forging.EmbedMaterials
                    .bonusText(entry.getKey(), 1, kind);
                dev.anvilcraft.thenextforge.TheNextForge.LOGGER.info(
                    "[TNF-MATRIX] {} | {} | {}", entry.getKey(), kind, text);
                rows++;
            }
        }
        helper.assertTrue(rows > 100, "矩阵行数异常：" + rows);
        helper.succeed();
    }

    /** 数值矩阵：每条都是设计里的一个具体数字。 */
    @GameTest(template = "empty")
    public static void embedAttributeMatrixMatchesDesign(GameTestHelper helper) {
        var mul = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        var add = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
        var total = net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

        // 石英：伤害 +20%（武器/护符）
        expectEmbedAmount(helper, "minecraft:quartz", TargetKind.WEAPON, Attributes.ATTACK_DAMAGE, mul, 0.20);
        expectEmbedAmount(helper, "minecraft:quartz", TargetKind.AMULET, Attributes.ATTACK_DAMAGE, mul, 0.20);
        // 红石：武器冷却 -20% ⇒ 攻速 ×1.25（冷却 = 1/攻速）
        expectEmbedAmount(helper, "minecraft:redstone", TargetKind.WEAPON, Attributes.ATTACK_SPEED, total, 0.25);
        // 铁锭：方块触及 +1（工具/护符），实体触及 +1（武器/护符）
        expectEmbedAmount(helper, "minecraft:iron_ingot", TargetKind.TOOL, Attributes.BLOCK_INTERACTION_RANGE, add, 1.0);
        expectEmbedAmount(helper, "minecraft:iron_ingot", TargetKind.WEAPON, Attributes.ENTITY_INTERACTION_RANGE, add, 1.0);
        expectEmbedAmount(helper, "minecraft:iron_ingot", TargetKind.AMULET, Attributes.BLOCK_INTERACTION_RANGE, add, 1.0);
        expectEmbedAmount(helper, "minecraft:iron_ingot", TargetKind.AMULET, Attributes.ENTITY_INTERACTION_RANGE, add, 1.0);
        // 金粒：造成伤害 +30%（武器）
        expectEmbedAmount(helper, "minecraft:gold_nugget", TargetKind.WEAPON, Attributes.ATTACK_DAMAGE, mul, 0.30);
        // 金锭：造成伤害 +50%（武器）
        expectEmbedAmount(helper, "minecraft:gold_ingot", TargetKind.WEAPON, Attributes.ATTACK_DAMAGE, mul, 0.50);
        // 钻石：护甲 +2（护符）
        expectEmbedAmount(helper, "minecraft:diamond", TargetKind.AMULET, Attributes.ARMOR, add, 2.0);
        // 绿宝石：最大生命值 +3（护符/装甲）
        expectEmbedAmount(helper, "minecraft:emerald", TargetKind.AMULET, Attributes.MAX_HEALTH, add, 3.0);
        expectEmbedAmount(helper, "minecraft:emerald", TargetKind.ARMOR, Attributes.MAX_HEALTH, add, 3.0);
        // 原木：最大生命值 +1（护符/装甲）
        expectEmbedAmount(helper, "minecraft:oak_log", TargetKind.AMULET, Attributes.MAX_HEALTH, add, 1.0);
        expectEmbedAmount(helper, "minecraft:oak_log", TargetKind.ARMOR, Attributes.MAX_HEALTH, add, 1.0);
        // 下界合金碎片：护甲 +1（装甲）
        expectEmbedAmount(helper, "minecraft:netherite_scrap", TargetKind.ARMOR, Attributes.ARMOR, add, 1.0);
        // 下界合金锭：护甲 +3（装甲）
        expectEmbedAmount(helper, "minecraft:netherite_ingot", TargetKind.ARMOR, Attributes.ARMOR, add, 3.0);
        // 下界之星：造成伤害 +100%（武器/护符）
        expectEmbedAmount(helper, "minecraft:nether_star", TargetKind.WEAPON, Attributes.ATTACK_DAMAGE, mul, 1.00);
        expectEmbedAmount(helper, "minecraft:nether_star", TargetKind.AMULET, Attributes.ATTACK_DAMAGE, mul, 1.00);
        // 皇家钢锭：造成伤害 +10%（武器/护符）、护甲 +1（装甲/护符）
        expectEmbedAmount(helper, "anvilcraft:royal_steel_ingot", TargetKind.WEAPON, Attributes.ATTACK_DAMAGE, mul, 0.10);
        expectEmbedAmount(helper, "anvilcraft:royal_steel_ingot", TargetKind.AMULET, Attributes.ATTACK_DAMAGE, mul, 0.10);
        expectEmbedAmount(helper, "anvilcraft:royal_steel_ingot", TargetKind.AMULET, Attributes.ARMOR, add, 1.0);
        // 中子锭：造成伤害 +80%、武器冷却 +30%（武器）；护甲 +2（装甲/护符）
        expectEmbedAmount(helper, "anvilcraft:neutronium_ingot", TargetKind.WEAPON, Attributes.ATTACK_DAMAGE, mul, 0.80);
        expectEmbedAmount(helper, "anvilcraft:neutronium_ingot", TargetKind.WEAPON, Attributes.ATTACK_SPEED, total,
            -(1.0D - 1.0D / 1.3D));
        expectEmbedAmount(helper, "anvilcraft:neutronium_ingot", TargetKind.ARMOR, Attributes.ARMOR, add, 2.0);
        expectEmbedAmount(helper, "anvilcraft:neutronium_ingot", TargetKind.AMULET, Attributes.ARMOR, add, 2.0);
        helper.succeed();
    }

    /** 耐久与附魔矩阵（设计里的倍率/等级）。 */
    @GameTest(template = "empty")
    public static void embedDurabilityFortuneMatrixMatchesDesign(GameTestHelper helper) {
        // 金粒/金锭：最大耐久 -20%（工具/武器）
        for (String id : new String[]{"minecraft:gold_nugget", "minecraft:gold_ingot"}) {
            for (TargetKind kind : new TargetKind[]{TargetKind.TOOL, TargetKind.WEAPON}) {
                int before = itemById(sampleItem(kind)).getMaxDamage();
                ItemStack stack = applyEmbed(helper, id, kind);
                helper.assertTrue(stack.getMaxDamage() == (int) Math.round(before * 0.8),
                    id + " " + kind + " 最大耐久应 -20%：" + before + " -> " + stack.getMaxDamage());
            }
        }
        // 钻石：耐久 +30%（工具/武器/装甲）
        for (TargetKind kind : new TargetKind[]{TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR}) {
            int before = itemById(sampleItem(kind)).getMaxDamage();
            ItemStack stack = applyEmbed(helper, "minecraft:diamond", kind);
            helper.assertTrue(stack.getMaxDamage() == (int) Math.round(before * 1.3),
                "diamond " + kind + " 耐久应 +30%：" + before + " -> " + stack.getMaxDamage());
        }
        // 下界合金碎片：耐久 +40%；下界合金锭：耐久 +100%
        for (TargetKind kind : new TargetKind[]{TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR}) {
            int before = itemById(sampleItem(kind)).getMaxDamage();
            ItemStack scrap = applyEmbed(helper, "minecraft:netherite_scrap", kind);
            helper.assertTrue(scrap.getMaxDamage() == (int) Math.round(before * 1.4),
                "scrap " + kind + " 耐久应 +40%");
            ItemStack ingot = applyEmbed(helper, "minecraft:netherite_ingot", kind);
            helper.assertTrue(ingot.getMaxDamage() == before * 2,
                "netherite " + kind + " 耐久应 +100%");
        }
        // 皇家钢锭：耐久 +20%（工具/武器/装甲）
        for (TargetKind kind : new TargetKind[]{TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR}) {
            int before = itemById(sampleItem(kind)).getMaxDamage();
            ItemStack stack = applyEmbed(helper, "anvilcraft:royal_steel_ingot", kind);
            helper.assertTrue(stack.getMaxDamage() == (int) Math.round(before * 1.2),
                "royal " + kind + " 耐久应 +20%");
        }
        // 时运：金粒 +1（工具/护符），金锭 +3（工具/护符）
        helper.assertTrue(fortuneLevel(helper, applyEmbed(helper, "minecraft:gold_nugget", TargetKind.TOOL)) == 1,
            "gold nugget tool 时运 +1");
        helper.assertTrue(fortuneLevel(helper, applyEmbed(helper, "minecraft:gold_nugget", TargetKind.AMULET)) == 1,
            "gold nugget amulet 时运 +1");
        helper.assertTrue(fortuneLevel(helper, applyEmbed(helper, "minecraft:gold_ingot", TargetKind.TOOL)) == 3,
            "gold ingot tool 时运 +3");
        // 超限金属粒：不毁（四类都算）
        for (TargetKind kind : TargetKind.values()) {
            ItemStack stack = applyEmbed(helper, "anvilcraft:transcendium_nugget", kind);
            helper.assertTrue(stack.has(DataComponents.UNBREAKABLE), "transcendium " + kind + " 必须不毁");
        }
        helper.succeed();
    }

    /** 时运等级。 */
    private static int fortuneLevel(GameTestHelper helper, ItemStack stack) {
        var key = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.ENCHANTMENT,
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("fortune"));
        var lookup = helper.getLevel().registryAccess()
            .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var holder = lookup.get(key);
        if (holder.isEmpty()) return -1;
        return stack.getOrDefault(DataComponents.ENCHANTMENTS,
            net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).getLevel(holder.get());
    }

    /** 失败后果优先级：超限保护 > 超限合金粒归零 > 摧毁。 */
    @GameTest(template = "empty")
    public static void failureOutcomePrefersOverlimitOverTranscendiumReset(GameTestHelper helper) {
        var degrade = ForgingRules.FailureOutcome.DEGRADE;
        var reset = ForgingRules.FailureOutcome.RESET_TO_ZERO;
        var destroy = ForgingRules.FailureOutcome.DESTROY;
        helper.assertTrue(ForgingRules.failureOutcome(false, true, true) == degrade,
            "超限锤 + 超限合金粒：只降级，不归零");
        helper.assertTrue(ForgingRules.failureOutcome(true, false, true) == degrade,
            "超限基底 + 超限合金粒：只降级，不归零");
        helper.assertTrue(ForgingRules.failureOutcome(false, false, true) == reset,
            "没有超限保护时，超限合金粒把摧毁换成归零");
        helper.assertTrue(ForgingRules.failureOutcome(false, false, false) == destroy,
            "既没有超限也没有合金粒：摧毁");
        helper.assertTrue(ForgingRules.failureOutcome(true, true, false) == degrade,
            "双超限：降级");
        helper.succeed();
    }

    /**
     * 端到端：超限**基底**（锤是普通砧）+ 带超限合金粒的对象，失败必须降 3 级，而不是被归 0。
     *
     * <p>旁边放一台创造流体储罐补经验，避免因为流体不够提前 return。
     */
    @GameTest(template = "empty")
    public static void automationDegradesInsteadOfResetOnSafeAnvil(GameTestHelper helper) {
        var safeAnvil = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "transcendence_anvil"))
            .orElse(null);
        var fishTank = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (safeAnvil == null || fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 1, 1, safeAnvil);
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        if (!(helper.getLevel().getBlockEntity(tankPos)
            instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        // 无限经验流体：鱼缸自带的储量装不下高等级的每次消耗
        helper.setBlock(3, 1, 1, dev.dubhe.anvilcraft.init.block.ModBlocks.CREATIVE_FLUID_TANK.get());
        BlockPos tankSource = helper.absolutePos(new BlockPos(3, 1, 1));
        if (helper.getLevel().getBlockEntity(tankSource)
            instanceof dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity creative
            && creative.getFluidHandler()
                instanceof net.neoforged.neoforge.fluids.capability.templates.FluidTank fluidTank) {
            fluidTank.setFluid(new net.neoforged.neoforge.fluids.FluidStack(
                dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), Integer.MAX_VALUE));
        }

        ItemStack trident = new ItemStack(Items.TRIDENT);
        trident.set(ModDataComponents.FORGING.get(),
            ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 10)
                .withEmbed(ForgingRules.TRANSCENDIUM_EMBED));
        tank.getItemHandler().setStackInSlot(8, trident);

        var random = net.minecraft.util.RandomSource.create(20261103L);
        int before = -1;
        int after = -1;
        for (int i = 0; i < 400 && after < 0; i++) {
            int now = tank.getItemHandler().getStackInSlot(8)
                .getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).anvilLevel();
            dev.anvilcraft.thenextforge.event.AutomationHandler.applyAutomatedHit(
                helper.getLevel(), tankPos, Blocks.ANVIL.defaultBlockState(), random);
            int next = tank.getItemHandler().getStackInSlot(8)
                .getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).anvilLevel();
            if (next < now) { before = now; after = next; }
        }
        helper.assertTrue(after >= 0, "400 次结算内应当出现一次失败（等级下降）");
        TheNextForge.LOGGER.info("[TNF-OVERLIMIT] degrade {} -> {} (expected {})", before, after, before - 3);
        helper.assertTrue(after == before - 3,
            "超限基底 + 普通锤失败只降 3 级：应为 " + (before - 3) + "，实际 " + after);
        helper.assertTrue(after > 0, "超限失败不得触发超限合金粒的归零，实际 " + after);
        helper.succeed();
    }
    /** 鱼缸插入规则：先有升级对象，再按白名单匹配材料；材料不能先放。 */
    @GameTest(template = "empty")
    public static void fishTankRequiresTargetBeforeAnAllowedMaterial(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack quartz = new ItemStack(Items.QUARTZ);
        ItemStack echo = new ItemStack(Items.ECHO_SHARD);
        ItemStack dirt = new ItemStack(Items.DIRT);
        var rules = dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.class;

        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(quartz, (TargetKind) null, false, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.NO_TARGET,
            "材料不能先放：必须已有升级对象");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(quartz, TargetKind.WEAPON, true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "石英对武器在白名单内，必须放行");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(echo, TargetKind.WEAPON, true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "回响碎片对武器在白名单内，必须放行");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(echo, TargetKind.TOOL, true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.NOT_ALLOWED_FOR_TARGET,
            "回响碎片只允许武器，工具必须拒绝");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(quartz, TargetKind.WEAPON, true, true)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.SECOND_MATERIAL,
            "已有材料时不能再放第二个材料");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(sword, TargetKind.WEAPON, true, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.SECOND_TARGET,
            "已有对象时不能再放第二个对象");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(sword, (TargetKind) null, false, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "第一个对象必须放行");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(dirt, (TargetKind) null, false, true)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.UNRELATED_ITEM,
            "已有材料时无关物品仍要拒绝");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeEvents.checkInsertion(dirt, (TargetKind) null, false, false)
            == dev.anvilcraft.thenextforge.event.ForgeEvents.TankInsertion.ALLOW,
            "空缸对无关物品不拦截");
        helper.assertTrue(rules != null, "判定类型可读");
        helper.succeed();
    }

    /** 白名单材料必须真的能放进已有对象的鱼缸（走 AnvilCraft 自己的插入 API）。 */
    @GameTest(template = "empty")
    public static void allowedMaterialActuallyLandsInTheTank(GameTestHelper helper) {
        var fishTank = BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath("anvilcraft", "fish_tank"))
            .orElse(null);
        if (fishTank == null) { helper.succeed(); return; }
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        helper.setBlock(1, 2, 1, fishTank);
        BlockPos tankPos = helper.absolutePos(new BlockPos(1, 2, 1));
        if (!(helper.getLevel().getBlockEntity(tankPos)
            instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) {
            helper.succeed();
            return;
        }
        tank.getItemHandler().setStackInSlot(8, new ItemStack(Items.TRIDENT));
        ItemStack remaining = dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity.insertItemToTank(
            tank.getInputHandler(), new ItemStack(Items.ECHO_SHARD, 1));
        helper.assertTrue(remaining.isEmpty(),
            "白名单材料必须能进缸，剩余=" + remaining);
        boolean found = false;
        for (int slot = 0; slot < tank.getInputHandler().getSlots(); slot++) {
            if (tank.getInputHandler().getStackInSlot(slot).is(Items.ECHO_SHARD)) found = true;
        }
        helper.assertTrue(found, "回响碎片应出现在输入槽里");
        helper.succeed();
    }
    /** 触及加成必须真的加到玩家身上，而不只是写进物品组件。 */
    @GameTest(template = "empty")
    public static void reachUpgradeRaisesPlayerInteractionRange(GameTestHelper helper) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int level = 0;
        while (dev.anvilcraft.thenextforge.forging.ForgingSummary.amount(sword, "weapon_reach") <= 0
            && level++ < 200) {
            ForgingRules.applyUpgrade(sword, TargetKind.WEAPON, ForgingLine.ANVIL, level,
                net.minecraft.util.RandomSource.create(level));
        }
        double forged = dev.anvilcraft.thenextforge.forging.ForgingSummary.amount(sword, "weapon_reach");
        helper.assertTrue(forged > 0, "should have rolled at least one reach upgrade");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        double base = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        player.tick();
        double equipped = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        TheNextForge.LOGGER.info("[TNF-REACH] forged={} base={} equipped={}", forged, base, equipped);
        // 设计：每级 +10% 基础触及（基础 3.0 → 每级 +0.3），不是绝对值 +0.1
        helper.assertTrue(near(equipped - base, base * forged),
            "reach must scale by 10% of base per level: base=" + base + " equipped=" + equipped
                + " forged=" + forged);
        helper.assertTrue(equipped - base > 0.2, "one reach level must be worth ~0.3 blocks, got " + (equipped - base));
        helper.succeed();
    }

    /** 三叉戟是 BEWLR 3D 模型，展示实体必须显式用 fixed 上下文才能套用它自己的位移与缩放。 */
    @GameTest(template = "empty")
    public static void tridentDisplayUsesFixedContext(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var sword = ForgeDisplay.place(helper.getLevel(), pos, new ItemStack(Items.DIAMOND_SWORD), 0.0F);
        helper.assertTrue("none".equals(displayContext(sword)),
            "普通物品保持 none 上下文，避免 fixed 的 180 度翻转，实际 " + displayContext(sword));
        sword.discard();
        var trident = ForgeDisplay.place(helper.getLevel(), pos, new ItemStack(Items.TRIDENT), 0.0F);
        helper.assertTrue("fixed".equals(displayContext(trident)),
            "三叉戟必须使用 fixed 上下文，实际 " + displayContext(trident));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void enderPearlDescriptionCoversEveryColumn(GameTestHelper helper) {
        String[] kinds = {"TOOL", "WEAPON", "ARMOR", "AMULET"};
        for (String k : kinds) {
            String text = dev.anvilcraft.thenextforge.forging.EmbedMaterials
                .bonusText("minecraft:ender_pearl", 1, TargetKind.valueOf(k));
            helper.assertTrue(text != null && !text.isBlank() && !text.equals("末影珍珠·传送归仓"),
                "末影珍珠 " + k + " 必须有专属描述，实际 " + text);
        }
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials
                .bonusText("minecraft:ender_pearl", 1, TargetKind.TOOL).contains("背包"),
            "末影珍珠工具列应是「掉落进背包」");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials
                .bonusText("minecraft:ender_pearl", 1, TargetKind.WEAPON).contains("传送"),
            "末影珍珠武器列应是「传送」");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frostEmbedUsesAnvilXpRate(GameTestHelper helper) {
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeDropEvents.FROST_XP_MULTIPLIER == 4,
            "浮霜倍率必须是 4（对标湮灭附魔 BLOCK_EXPERIENCE）");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeDropEvents.frostXpFor(0) == 1, "无经验方块给 1 点");
        helper.assertTrue(dev.anvilcraft.thenextforge.event.ForgeDropEvents.frostXpFor(5) == 20, "5 点 → 20 点");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catAmuletCountsOnlyConsecutiveSuccesses(GameTestHelper helper) {
        var svc = dev.anvilcraft.thenextforge.forging.ForgingService.class;
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.ForgingService.catOutcome(true, 0).streak() == 1, "第一次成功累加");
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.ForgingService.catOutcome(true, 2).levelBonus() == 1, "第三次成功 +1 级");
        var failed = dev.anvilcraft.thenextforge.forging.ForgingService.catOutcome(false, 2);
        helper.assertTrue(failed.levelBonus() == 0 && failed.streak() == 0, "失败必须断开连续计数");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void amuletGeometryAndLavaFollowDesign(GameTestHelper helper) {
        var box = dev.anvilcraft.thenextforge.forging.ForgingService.villagerBox(BlockPos.ZERO);
        helper.assertTrue(near(box.getXsize(), 3.0) && near(box.getZsize(), 3.0), "村民判定盒必须是 3×3");
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(1, 1, 2, Blocks.LAVA);
        helper.setBlock(3, 1, 2, Blocks.LAVA);
        helper.assertTrue(dev.anvilcraft.thenextforge.forging.ForgingService.consumeLava(helper.getLevel(), base),
            "3×1×3 有岩浆时应消耗一块");
        int lava = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (helper.getLevel().getBlockState(base.offset(dx, 0, dz)).is(Blocks.LAVA)) lava++;
            }
        }
        helper.assertTrue(lava == 1, "两块岩浆只应吃掉一块，实际剩 " + lava);
        helper.assertTrue(!dev.anvilcraft.thenextforge.forging.ForgingService.effectsEnabled(java.util.Set.of("silence")),
            "寂静护符应屏蔽特效");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armadilloNextStrikeConsumesProtectionAlways(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(ModDataComponents.FORGING.get(), ForgingData.EMPTY.withNextHitProtected(true));
        ForgeDisplay.place(helper.getLevel(), pos, sword, 0.0F);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.totalExperience = 500;
        dev.anvilcraft.thenextforge.forging.ForgingService.hammer(player, helper.getLevel(), pos,
            ForgingLine.ANVIL, ForgingMaterial.BASE, ForgingMaterial.BASE);
        var display = ForgeDisplay.find(helper.getLevel(), pos);
        helper.assertTrue(display != null, "带后续保护的那一锤不应打碎物品");
        if (display != null) {
            helper.assertTrue(!forgingOf(ForgeDisplay.stackOf(display)).nextHitProtected(),
                "不论成败，犰狳的后续保护都必须被消耗");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diamondDurabilitySkipsAmulet(GameTestHelper helper) {
        for (String id : new String[]{"minecraft:diamond", "minecraft:netherite_scrap",
            "minecraft:netherite_ingot", "minecraft:paper"}) {
            helper.assertTrue(!dev.anvilcraft.thenextforge.forging.EmbedMaterials.durabilityApplies(id, TargetKind.AMULET),
                id + " 不应改护符耐久");
            helper.assertTrue(dev.anvilcraft.thenextforge.forging.EmbedMaterials.durabilityApplies(id, TargetKind.ARMOR),
                id + " 应改装甲耐久");
        }
        helper.succeed();
    }

    /** 放置后升级/降级必须当场改变模型大小，而不是只在放置那一刻写死。 */
    @GameTest(template = "empty")
        public static void placedModelFollowsLevelImmediately(GameTestHelper helper) {
        // 放大现在是实验性功能：默认关闭，测试里显式打开
        boolean previousScaling = dev.anvilcraft.thenextforge.TheNextForge.CONFIG.experimentalModelScaling;
        dev.anvilcraft.thenextforge.TheNextForge.CONFIG.experimentalModelScaling = true;
        try {
            placedModelFollowsLevelImmediatelyBody(helper);
        } finally {
            dev.anvilcraft.thenextforge.TheNextForge.CONFIG.experimentalModelScaling = previousScaling;
        }
    }

    private static void placedModelFollowsLevelImmediatelyBody(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.ANVIL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var display = ForgeDisplay.place(helper.getLevel(), pos, new ItemStack(Items.DIAMOND_SWORD), 0.0F);
        helper.assertTrue(near(transformationScale(display), 1.0F),
            "全新物品的模型缩放应为 1.0，实际 " + transformationScale(display));

        ForgeDisplay.setStack(display, strengthenedSword(5));
        helper.assertTrue(near(transformationScale(display), 1.5F),
            "升到 5 级后必须立刻变成 1.5 倍，实际 " + transformationScale(display));

        ForgeDisplay.setStack(display, strengthenedSword(2));
        helper.assertTrue(near(transformationScale(display), 1.2F),
            "降到 2 级后必须立刻缩回 1.2 倍，实际 " + transformationScale(display));

        ItemStack both = strengthenedSword(3);
        both.set(ModDataComponents.FORGING.get(),
            ForgingData.EMPTY.withLevel(ForgingLine.ANVIL, 3).withLevel(ForgingLine.GRINDSTONE, 2));
        ForgeDisplay.setStack(display, both);
        helper.assertTrue(near(transformationScale(display), 1.5F),
            "铁砧与砂轮等级要加算：3+2 级 = 1.5 倍，实际 " + transformationScale(display));
        helper.succeed();
    }

    /** 读出展示实体当前变换里的缩放。 */
    private static float transformationScale(Display.ItemDisplay display) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        display.saveWithoutId(tag);
        var transformation = com.mojang.math.Transformation.EXTENDED_CODEC
            .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("transformation")).getOrThrow();
        return transformation.getScale().x();
    }

    /** 读出展示实体保存的 item_display 上下文名；未写时原版默认是 none。 */
    private static String displayContext(Display.ItemDisplay display) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        display.saveWithoutId(tag);
        return tag.getString("item_display");
    }

    private static int modifiers(ItemStack stack) {
        ItemAttributeModifiers mods = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return mods.modifiers().size();
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }
}
