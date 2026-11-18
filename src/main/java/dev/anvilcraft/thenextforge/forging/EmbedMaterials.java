package dev.anvilcraft.thenextforge.forging;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 副手材料的嵌入效果表。
 *
 * <p>当前 demo 只实现少数能直接作用于物品属性的材料，其余材料只记录名称并提示，
 * 完整 26 种效果按设计合同 M4 逐条接入。
 */
public final class EmbedMaterials {
    public record Definition(String id, String label, boolean unique, java.util.Set<TargetKind> kinds, int color) {
        public boolean allows(TargetKind kind) {
            return this.kinds.contains(kind);
        }
    }

    /**
     * 可逆副作用的**单一数据源**：嵌入时按它生效，撤销时按同一份表反算。
     *
     * <p>之前这些数值散落在 `apply` 的大 switch 与 `EmbedRemoval` 的三处 switch 里，
     * 加材料要改多处、且出现过「按 0.8 乘、按 0.8 除却用错对象类别」的隐患。
     */
    public record DurabilitySpec(double factor, java.util.Set<TargetKind> kinds) {
        /** 该对象类别下是否真的乘过这个倍率。 */
        public boolean appliesTo(TargetKind kind) {
            return kinds.contains(kind);
        }
    }

    private static final java.util.Set<TargetKind> TOOL_AND_WEAPON =
        java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON);
    /** 设计：钻石/碎片/锭/纸 的耐久倍率只作用于工具/武器/装甲，护符不吃。 */
    private static final java.util.Set<TargetKind> TOOL_WEAPON_ARMOR =
        java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR);
    private static final java.util.Set<TargetKind> ALL_KINDS =
        java.util.EnumSet.allOf(TargetKind.class);

    /** 材料 id → 耐久倍率与它生效的对象类别。 */
    private static final Map<String, DurabilitySpec> DURABILITY = Map.of(
        "minecraft:gold_nugget", new DurabilitySpec(0.8D, TOOL_AND_WEAPON),
        "minecraft:gold_ingot", new DurabilitySpec(0.8D, TOOL_AND_WEAPON),
        "minecraft:diamond", new DurabilitySpec(1.3D, TOOL_WEAPON_ARMOR),
        "minecraft:netherite_scrap", new DurabilitySpec(1.4D, TOOL_WEAPON_ARMOR),
        "minecraft:netherite_ingot", new DurabilitySpec(2.0D, TOOL_WEAPON_ARMOR),
        "anvilcraft:royal_steel_ingot", new DurabilitySpec(1.2D, ALL_KINDS),
        "minecraft:paper", new DurabilitySpec(0.5D, TOOL_WEAPON_ARMOR)
    );

    /** 材料 id → 时运等级。 */
    private static final Map<String, Integer> FORTUNE = Map.of(
        "minecraft:gold_nugget", 1,
        "minecraft:gold_ingot", 3
    );

    /** 会写入「不毁」标志的材料。 */
    private static final java.util.Set<String> UNBREAKABLE = java.util.Set.of(
        "anvilcraft:transcendium_nugget"
    );

    /** 该材料在该类别下是否会改耐久。 */
    public static boolean durabilityApplies(String materialId, TargetKind kind) {
        DurabilitySpec spec = DURABILITY.get(materialId);
        return spec != null && spec.appliesTo(kind);
    }

    /** 该材料在指定类别下的耐久倍率（1.0 表示不改）。 */
    public static double durabilityFactor(String materialId, TargetKind kind) {
        DurabilitySpec spec = DURABILITY.get(materialId);
        if (spec == null) return 1.0D;
        if (kind != null && !spec.appliesTo(kind)) return 1.0D;
        return spec.factor();
    }

    /** 该材料的时运等级（0 表示不给）。 */
    public static int fortuneLevel(String materialId) {
        return FORTUNE.getOrDefault(materialId, 0);
    }

    /** 该材料是否会写入不毁标志。 */
    public static boolean grantsUnbreakable(String materialId) {
        return UNBREAKABLE.contains(materialId);
    }

    private static final Map<String, Definition> TABLE = new LinkedHashMap<>();

    static {
        add("minecraft:quartz", "石英·锋利", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.AMULET), 0xE8E8E8);
        add("minecraft:redstone", "红石·急迫", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xFF0000);
        add("minecraft:lapis_lazuli", "青金石·附魔", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x1C4FB8);
        add("minecraft:iron_ingot", "铁锭·便利", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.AMULET), 0xD8D8D8);
        add("minecraft:gold_nugget", "金粒·华丽", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.AMULET), 0xFFD700);
        add("minecraft:gold_ingot", "金锭·华丽", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.AMULET), 0xFFC400);
        add("minecraft:diamond", "钻石·珍贵", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x4AEDD9);
        add("minecraft:netherite_scrap", "下界合金碎片·坚固", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR), 0x5A4A42);
        add("minecraft:netherite_ingot", "下界合金锭·坚固", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR), 0x4B3F3A);
        add("minecraft:glow_ink_sac", "发光墨囊·发光", true, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x1FE0B0);
        add("minecraft:echo_shard", "回响碎片·回响", true, java.util.EnumSet.of(TargetKind.WEAPON), 0x1B3A4A);
        add("minecraft:fermented_spider_eye", "发酵蜘蛛眼·猛毒", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x7A2B5A);
        add("minecraft:slime_ball", "粘液球·黏糊", false, java.util.EnumSet.of(TargetKind.WEAPON), 0x7ADB5A);
        add("minecraft:ender_pearl", "末影珍珠·传送归仓", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x1FE07A);
        add("minecraft:heart_of_the_sea", "海洋之心·海洋", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x3A7ADB);
        add("minecraft:nether_star", "下界之星·辉煌", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xFFF0A0);
        add("anvilcraft:royal_steel_ingot", "皇家钢锭·皇家", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xB060FF);
        add("anvilcraft:frost_metal_ingot", "浮霜金属锭·浮霜", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.AMULET), 0x8AD8FF);
        // 设计：装甲/护符 都被攻击着火
        add("anvilcraft:ember_metal_ingot", "余烬金属锭·余烬", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xFF7A1A);
        add("anvilcraft:transcendium_nugget", "超限合金粒·超限", true, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xC060FF);
        add("anvilcraft:cursed_gold_ingot", "诅咒金锭·诅咒", false, java.util.EnumSet.of(TargetKind.AMULET), 0x8A6A00);
        add("anvilcraft:neutronium_ingot", "中子锭·致密", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xA0A0A0);
        // 设计：命中(工具/武器) + 被攻击(装甲/护符)
        add("anvilcraft:negative_matter_nugget", "负物质粒·反相湮灭", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x6A2BA0);
        add("anvilcraft:void_matter", "虚空物质·虚无", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.AMULET), 0x101010);
        add("anvilcraft:amber", "琥珀碎片·琥珀封印", false, java.util.EnumSet.of(TargetKind.WEAPON), 0xFFB020);
        add("minecraft:emerald", "绿宝石·苍翠", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x1FD060);
        add("anvilcraft:sapphire", "蓝宝石·海洋亲和", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x2050FF);
        add("anvilcraft:ruby", "红宝石·烈焰亲和", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xFF2040);
        // 设计：黄玉只写工具/武器/装甲
        add("anvilcraft:topaz", "黄玉·雷电亲和", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR), 0xFFD020);
        add("#minecraft:logs", "原木·生态", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x8B5A2B);
        // 设计「铁砧(铁砧)：视为铁砧锤(工具/武器)」——嵌入后该工具/武器同时算作铁砧锤
        // 设计：纸(扩张) 可嵌入槽位+3、最大耐久-50%（工具/武器/装甲）
        add("minecraft:feather", "羽毛·轻柔", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.AMULET), 0xF2F5F7);
        add("minecraft:golden_carrot", "金胡萝卜·饱和", true, java.util.EnumSet.of(TargetKind.ARMOR, TargetKind.AMULET), 0xF0A72A);
        add("minecraft:enchanted_golden_apple", "附魔金苹果·神之力", false, java.util.EnumSet.of(TargetKind.ARMOR, TargetKind.AMULET), 0xFFE45C);
        add("minecraft:breeze_rod", "旋风棒·风暴", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON), 0xB7D3E8);
        add("minecraft:heavy_core", "沉重核心·沉重", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x5C5C63);
        add("minecraft:totem_of_undying", "不死图腾·不朽生命", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0xF0C64A);
        add("minecraft:dragon_egg", "龙蛋·龙之气息", false, java.util.EnumSet.of(TargetKind.WEAPON, TargetKind.ARMOR, TargetKind.AMULET), 0x1E1B2E);
        add("minecraft:paper", "纸·延展", true, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON, TargetKind.ARMOR), 0xF2F0E4);
        // 设计：凋零玫瑰(凋零) 命中目标获得 5s 凋零（工具/武器）
        add("minecraft:wither_rose", "凋零玫瑰·凋零", false, java.util.EnumSet.of(TargetKind.TOOL, TargetKind.WEAPON), 0x2B2B2B);
    }

    private static void fortune(ItemStack stack, int levels, net.minecraft.server.level.ServerLevel serverLevel) {
        try {
            var registry = serverLevel.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var key = net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.ENCHANTMENT,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("fortune")
            );
            stack.enchant(registry.getOrThrow(key), levels);
        } catch (RuntimeException ignored) {
            // 附魔表缺失时静默跳过，材料效果仍然保留在嵌入记录里
        }
    }

    public static void scaleDurability(ItemStack stack, double factor) {
        int maximum = stack.getMaxDamage();
        if (maximum <= 1 || !Double.isFinite(factor) || factor <= 0) return;
        if (ForgeDurability.scaleBaseline(stack, factor)) return;
        int scaled = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(maximum * factor)));
        stack.set(DataComponents.MAX_DAMAGE, scaled);
        ForgeDurability.markManaged(stack);
    }

    private static void add(String id, String label, boolean unique, java.util.Set<TargetKind> kinds, int color) {
        TABLE.put(id, new Definition(id, label, unique, kinds, color));
    }

    private EmbedMaterials() {
    }

    /**
     * 发光墨囊高亮是否生效：主手带嵌入且副手是可放置方块的物品。
     *
     * <p>判定放在服务端也能调用的位置，渲染层只负责画框。
     */
    public static boolean glowHighlightApplies(ItemStack tool, ItemStack offhand) {
        ForgingData data = tool.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
        if (data == null || !data.hasActiveEmbed("minecraft:glow_ink_sac")) return false;
        return offhand.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

    /**
     * 事件型材料登记：材料 id → 触发事件与效果说明。
     *
     * <p>这些材料不在 apply 阶段产生属性变化，效果由事件类执行，登记表用于验收时确认没有遗漏。
     */
    public static final java.util.Map<String, String> EVENT_EFFECTS = java.util.Map.ofEntries(
        java.util.Map.entry("minecraft:lapis_lazuli", "PlayerXpEvent.PickupXp: 经验 +20%"),
        java.util.Map.entry("minecraft:glow_ink_sac", "ForgeItemTicker + GlowHighlight: 夜视与方块高亮"),
        java.util.Map.entry("minecraft:echo_shard", "ForgeCombatEvents: 命中追加 50% 范围伤害"),
        java.util.Map.entry("minecraft:fermented_spider_eye", "ForgeCombatEvents/ForgeDefenceEvents: 中毒"),
        java.util.Map.entry("minecraft:slime_ball", "ForgeCombatEvents: 命中减速"),
        java.util.Map.entry("minecraft:breeze_rod", "EmbedCombat: 命中后原地风弹爆炸"),
        java.util.Map.entry("minecraft:dragon_egg", "EmbedCombat: 魔法伤害追加 / 免疫魔法伤害"),
        java.util.Map.entry("minecraft:golden_carrot", "ForgeItemTicker: 每 20s 饱和 5s"),
        java.util.Map.entry("minecraft:enchanted_golden_apple", "ForgeItemTicker: 抗火 / 抗性 / 生命恢复"),
        java.util.Map.entry("minecraft:feather", "EmbedCombat/ForgeItemTicker: 武器减攻加速、护符缓降"),
        java.util.Map.entry("minecraft:heavy_core", "ForgeItemTicker: 击退与抗性、移速与水中限制"),
        java.util.Map.entry("minecraft:totem_of_undying", "EmbedCombat: 魔法伤害与致命伤害触发"),
        java.util.Map.entry("minecraft:ender_pearl", "ForgeCombatEvents/ForgeDefenceEvents/ForgeDropEvents: 传送与掉落进背包"),
        java.util.Map.entry("minecraft:heart_of_the_sea", "ForgeCombatEvents/ForgeItemTicker/ForgeMiningEvents: 水伤、潮涌、水下挖掘"),
        java.util.Map.entry("anvilcraft:frost_metal_ingot", "ForgeHarvestEvents: 挖掘给经验"),
        java.util.Map.entry("anvilcraft:ember_metal_ingot", "ForgeCombatEvents/ForgeDefenceEvents/ForgeDropEvents: 点燃与熔炼"),
        java.util.Map.entry("anvilcraft:transcendium_nugget", "EmbedMaterials + ForgeItemTicker: 不毁与飞行"),
        java.util.Map.entry("anvilcraft:cursed_gold_ingot", "ForgeCombatEvents: 每诅咒 +5% 伤害"),
        java.util.Map.entry("anvilcraft:negative_matter_nugget", "ForgeCombatEvents/ForgeDefenceEvents: 漂浮与增伤"),
        java.util.Map.entry("anvilcraft:void_matter", "ForgeCombatEvents: 30% 伤害转虚空"),
        java.util.Map.entry("anvilcraft:amber", "AmberMarking: 命中 +20 怨恨"),
        java.util.Map.entry("anvilcraft:sapphire", "ForgeItemTicker: 水中回耐久与水下呼吸"),
        java.util.Map.entry("anvilcraft:ruby", "ForgeItemTicker: 燃烧回耐久与抗火"),
        java.util.Map.entry("anvilcraft:topaz", "ForgeItemTicker: 回耐久"),
        java.util.Map.entry("#minecraft:logs", "ForgeItemTicker: 回耐久与手持物品修复"),
        java.util.Map.entry("minecraft:wither_rose", "CombatEffects: 命中附加凋零 5s")
    );

    /** 按材料 id 查定义。 */
    public static Definition byId(String id) {
        var dynamic = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.definition(id);
        if (dynamic != null) return dev.anvilcraft.thenextforge.api.ManagedEmbeddings.materialDefinition(id, dynamic);
        return TABLE.get(id);
    }

    /** 从完整 label（「材料·性质」）里取出性质名，tooltip 只显示它。 */
    public static String propertyOf(String label) {
        int mark = label.indexOf('·');
        return mark >= 0 ? label.substring(mark + 1) : label;
    }

    /** 按定义生成该材料的加成文本，供 tooltip 使用（不依赖物品属性组件）。 */
    public static String bonusText(String id, int level) {
        return bonusText(id, level, null);
    }

    /** 按对象类型返回该材料的加成文本；kind 为 null 时返回通用描述。 */
    /** 按对象类型返回该材料的加成文本；kind 为 null 时返回通用描述。 */
    public static String bonusText(String id, int level, TargetKind kind) {
        var dynamic = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.definition(id);
        if (dynamic != null) return dynamic.enabled() && (kind == null || dynamic.kinds().contains(kind))
            ? dev.anvilcraft.thenextforge.api.ManagedEmbeddings.describe(dynamic, kind) : "";
        Definition definition = TABLE.get(id);
        if (definition == null) return "";
        if (kind != null && !definition.allows(kind)) return "";
        String path = id.substring(id.indexOf(':' ) + 1);
        return switch (path) {
            case "quartz" -> kind == TargetKind.WEAPON || kind == TargetKind.AMULET ? "伤害 +20%" : "";
            case "redstone" -> switch (kind) {
                case WEAPON -> "冷却 -20%";
                case TOOL -> "急迫 I";
                case ARMOR -> "速度 I";
                default -> "急迫 I 与速度 I";
            };
            case "glow_ink_sac" -> switch (kind) {
                case WEAPON -> "命中发光 5s";
                case TOOL -> "副手方块高亮 13 格";
                case ARMOR, AMULET -> "夜视";
                default -> "夜视";
            };
            case "iron_ingot" -> switch (kind) {
                case WEAPON -> "实体触及 +1 格";
                case AMULET -> "方块触及 +1 格，实体触及 +1 格";
                default -> "方块触及 +1 格";
            };
            case "gold_nugget" -> switch (kind) {
                case WEAPON -> "伤害 +30%（最大耐久 -20%）";
                case TOOL -> "时运 +1（最大耐久 -20%）";
                default -> "时运 +1";
            };
            case "gold_ingot" -> switch (kind) {
                case WEAPON -> "伤害 +50%（最大耐久 -20%）";
                case TOOL -> "时运 +3（最大耐久 -20%）";
                default -> "时运 +3";
            };
            case "diamond" -> switch (kind) {
                case TOOL -> "挖掘等级与钻石镐取并集，耐久 +30%";
                case WEAPON -> "耐久 +30%";
                case ARMOR -> "耐久 +30%";
                case AMULET -> "护甲 +2";
                default -> "耐久 +30%";
            };
            case "emerald" -> switch (kind) {
                case ARMOR, AMULET -> "最大生命值 +3";
                default -> "经验修补";
            };
            case "lapis_lazuli" -> switch (kind) {
                case TOOL, WEAPON -> "击杀经验 +20%";
                default -> "获得经验 +20%";
            };
            case "sapphire" -> kind == TargetKind.AMULET ? "水下呼吸" : "水中每 5s 恢复 2 点耐久";
            case "ruby" -> kind == TargetKind.AMULET ? "抗火" : "燃烧时每 5s 恢复 2 点耐久";
            case "topaz" -> "电网内每 5s 恢复 2 点耐久，遭雷劈恢复 10 点";
            case "netherite_scrap" -> switch (kind) {
                case ARMOR -> "护甲 +1，耐久 +40%";
                default -> "耐久 +40%";
            };
            case "netherite_ingot" -> switch (kind) {
                case ARMOR -> "护甲 +3，耐久 +100%";
                case AMULET -> "抗性提升 +1（最高 4）";
                default -> "耐久 +100%";
            };
            case "nether_star" -> switch (kind) {
                case ARMOR -> "抗性提升 +3（最高 4）";
                case AMULET -> "伤害 +100%，抗性提升 +3（最高 4）";
                default -> "伤害 +100%";
            };
            case "echo_shard" -> "近战命中额外 50% 范围伤害";
            case "fermented_spider_eye" -> switch (kind) {
                case ARMOR -> "被攻击时攻击者中毒";
                default -> "命中附加中毒 5s（每嵌入 +1 级）";
            };
            case "oak_log", "logs" -> switch (kind) {
                case ARMOR -> "最大生命值 +1，每 5s 恢复 1 点耐久";
                case AMULET -> "最大生命值 +1，每 5s 恢复手持物品 1 点耐久";
                default -> "每 5s 恢复 1 点耐久";
            };
            case "slime_ball" -> "命中降低 50% 移速";
            // 设计：末影珍珠（原末影之眼）；键名曾写错成 ender_eye，导致落到 default 只显示材料名
            case "ender_pearl" -> switch (kind) {
                case WEAPON -> "命中后目标随机 13 格传送";
                case ARMOR -> "被攻击后攻击者随机 13 格传送";
                default -> "挖掘掉落直接进入背包（优先潜影盒）";
            };
            case "heart_of_the_sea" -> switch (kind) {
                case WEAPON -> "水中伤害 +75%";
                case TOOL -> "水下无视挖掘惩罚";
                default -> "水中获得海洋庇佑";
            };
            case "royal_steel_ingot" -> switch (kind) {
                case WEAPON -> "伤害 +10%，耐久 +20%";
                case ARMOR -> "护甲 +1，耐久 +20%";
                case AMULET -> "伤害 +10%，护甲 +1，耐久 +20%";
                default -> "耐久 +20%";
            };
            case "frost_metal_ingot" -> switch (kind) {
                case WEAPON -> "每附魔 +5% 伤害";
                case AMULET -> "每级 +1% 伤害";
                default -> "挖掘掉落转化为经验（方块经验 ×4，至少 1 点）";
            };
            case "ember_metal_ingot" -> switch (kind) {
                case TOOL -> "挖掘自动熔炼（受时运影响）";
                case WEAPON -> "火焰附加";
                default -> "被攻击后攻击者燃烧";
            };
            case "transcendium_nugget" -> kind == TargetKind.AMULET
                ? "创造飞行，无视飞行挖掘惩罚，嵌入槽 +1，损毁降为等级 0"
                : "不毁，嵌入槽 +1，损毁降为等级 0";
            case "cursed_gold_ingot" -> "全身装备/护符每诅咒 +5% 伤害";
            case "neutronium_ingot" -> switch (kind) {
                case WEAPON -> "伤害 +80%，冷却 +30%";
                default -> "护甲 +2，抗性提升 +1（最高 4）";
            };
            case "negative_matter_nugget" -> switch (kind) {
                case ARMOR, AMULET -> "被攻击后攻击者漂浮且易伤";
                default -> "命中漂浮且易伤";
            };
            case "void_matter" -> kind == TargetKind.AMULET ? "10% 伤害转虚空" : "30% 伤害转虚空";
            case "amber" -> "命中后基础怨恨值 +20%（上限 100%）";
            case "feather" -> switch (kind) {
                case WEAPON -> "攻击力 -75%，武器冷却 -75%";
                default -> "缓降";
            };
            case "golden_carrot" -> "每 20s 获得 5s 饱和（唯一嵌入）";
            case "enchanted_golden_apple" -> "抗火、抗性提升 +1、生命恢复 +1";
            case "breeze_rod" -> "命中后原地生成一次风弹爆炸";
            case "heavy_core" -> switch (kind) {
                case WEAPON -> "击退 +150%，武器冷却 +50%";
                default -> "抗性 +2（最高 4），移速 -20%，水中无法游泳";
            };
            case "totem_of_undying" -> switch (kind) {
                case WEAPON -> "额外造成目标最大生命 5% 的魔法伤害";
                case AMULET -> "最大生命 +5，生命恢复 +2；致命伤害时触发不死图腾（冷却 240s）";
                default -> "最大生命 +5，生命恢复 +2";
            };
            case "dragon_egg" -> switch (kind) {
                case WEAPON -> "额外附加 25% 魔法伤害";
                default -> "额外附加 10% 魔法伤害，且免疫魔法伤害";
            };
            case "paper" -> "可嵌入槽位 +3，最大耐久 -50%";
            case "wither_rose" -> "命中附加凋零 5s";
            default -> definition.label();
        };
    }


    private static String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.1f", value);
    }

    /**
     * 该对象当前的嵌入槽位数：基础槽位 + 超限合金粒的 +1 + 纸的 +3。
     */
    public static int slotCount(TargetKind kind, ForgingData data) {
        int slots = kind.slots();
        for (String embed : data.embeds()) {
            slots += extraSlots(embed);
        }
        return slots;
    }

    /** 每种材料提供的额外槽位：内置（超限合金粒 +1、纸 +3）可被数据定义覆盖。 */
    public static int extraSlots(String materialId) {
        var dynamic = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.definition(materialId);
        if (dynamic != null) return dynamic.extraSlots();
        int fromData = EmbedData.extraSlots(materialId);
        if (fromData > 0) return fromData;
        return switch (materialId) {
            case "anvilcraft:transcendium_nugget" -> 1;
            case "minecraft:paper" -> 3;
            default -> 0;
        };
    }

    /** 按数据定义把效果落到物品上；返回是否产生了变化。 */
    private static boolean applySpec(ItemStack target, EmbedData.Spec spec, TargetKind kind, int level,
                                     net.minecraft.server.level.ServerLevel serverLevel) {
        boolean changed = false;
        for (EmbedData.AttributeEffect effect : spec.attributes().getOrDefault(kind, java.util.List.of())) {
            var attribute = EmbedData.attribute(effect.attribute());
            if (attribute == null) continue;
            add(spec.id(), target, attribute, effect.amount(), EmbedData.operation(effect.operation()),
                "embed_" + spec.id().substring(spec.id().indexOf(':') + 1), level);
            changed = true;
        }
        if (spec.durabilityFactor() != 1.0D) {
            scaleDurability(target, spec.durabilityFactor());
            changed = true;
        }
        if (spec.fortune() > 0 && (kind == TargetKind.TOOL || kind == TargetKind.AMULET)) {
            fortune(target, spec.fortune(), serverLevel);
            changed = true;
        }
        if (spec.unbreakable()) {
            target.set(net.minecraft.core.component.DataComponents.UNBREAKABLE,
                new net.minecraft.world.item.component.Unbreakable(true));
            changed = true;
        }
        return changed;
    }

    /** 材料 id → 属性修饰符名前缀（如 minecraft:quartz → embed_quartz）。 */
    public static String modifierKey(String id) {
        if (id == null) return null;
        String path = id.substring(id.indexOf(':') + 1);
        return "embed_" + path;
    }

    /** 性质对应的效果说明；事件型材料有专门文本，其余走通用描述。 */
    public static String effectOf(String id) {
        return EVENT_EFFECTS.getOrDefault(id, "嵌入属性加成");
    }

    /** 全部材料定义，供验收矩阵遍历。 */
    public static java.util.Map<String, Definition> all() {
        return table();
    }

    public static Definition of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String rawId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        // 设计 L40：数据驱动的黑名单材料不可嵌入
        if (EmbedData.isBlacklisted(rawId)) return null;
        var mapped = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.definition(rawId);
        if (mapped != null) return dev.anvilcraft.thenextforge.api.ManagedEmbeddings.materialDefinition(rawId, mapped);
        if (stack.is(net.minecraft.tags.ItemTags.LOGS)) {
            return EmbedData.isBlacklisted("#minecraft:logs") ? null : TABLE.get("#minecraft:logs");
        }
        return TABLE.get(rawId);
    }

    /** 该 id 是否已有定义（区分「新增」与「覆盖」）。 */
    public static boolean hasDefinition(String id) {
        return TABLE.containsKey(id);
    }

    /** 移除数据注册的定义（回滚用）。 */
    public static void removeDefinition(String id) {
        TABLE.remove(id);
    }

    /** 内置材料表（只读视图，测试与调试用）。 */
    public static Map<String, Definition> table() {
        if (!dev.anvilcraft.thenextforge.api.TnfApi.hasEmbeddingOverrides()) return Map.copyOf(TABLE);
        Map<String, Definition> merged = new LinkedHashMap<>(TABLE);
        for (var item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            var dynamic = dev.anvilcraft.thenextforge.api.TnfApi.embedding(new ItemStack(item));
            if (dynamic == null) continue;
            var definition = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.materialDefinition(id, dynamic);
            if (definition == null) merged.remove(id); else merged.put(id, definition);
        }
        return java.util.Collections.unmodifiableMap(merged);
    }

    /** 材料 id → 物品栈；未知或无效 id 返回空栈。 */
    public static ItemStack itemOf(String id) {
        if (id.startsWith("#")) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.parse(id))
            .map(ItemStack::new)
            .orElse(ItemStack.EMPTY);
    }

    /**
     * 该材料在**这一类对象**上的效果是否只体现在事件/持续效果里（不落在物品属性上）。
     *
     * <p>集中放这里而不是写在测试里：测试用它区分「应该写出属性」与「只登记事件效果」，
     * 未来新增材料只要在同处补一行，判定与测试就不会各写一份。
     */
    public static boolean eventOnly(String id, TargetKind kind) {
        return switch (id) {
            case "minecraft:lapis_lazuli", "minecraft:glow_ink_sac", "minecraft:echo_shard",
                 "minecraft:fermented_spider_eye", "minecraft:slime_ball", "minecraft:ender_pearl",
                 "minecraft:heart_of_the_sea", "anvilcraft:frost_metal_ingot",
                 "anvilcraft:ember_metal_ingot", "minecraft:wither_rose",
                 "anvilcraft:negative_matter_nugget", "anvilcraft:void_matter", "anvilcraft:amber",
                 "anvilcraft:cursed_gold_ingot", "anvilcraft:sapphire", "anvilcraft:ruby",
                 "anvilcraft:topaz", "minecraft:breeze_rod", "minecraft:dragon_egg" -> true;
            case "minecraft:golden_carrot", "minecraft:enchanted_golden_apple" -> true;
            case "minecraft:feather", "minecraft:heavy_core" -> kind == TargetKind.ARMOR || kind == TargetKind.AMULET;
            case "minecraft:totem_of_undying" -> kind == TargetKind.WEAPON;
            default -> false;
        };
    }

    /** 第三方 / 数据驱动注册入口：新增或覆盖一个材料定义。 */
    public static void registerDefinition(Definition definition) {
        if (definition == null || definition.id() == null) return;
        TABLE.put(definition.id(), definition);
    }

    /** 该材料能不能作用于这个对象（按多重角色判断：任一角色在白名单内即可）。 */
    public static boolean allowsFor(Definition definition, ItemStack stack) {
        if (definition == null || stack.isEmpty()) return false;
        for (TargetKind role : TargetKind.rolesOf(stack)) {
            if (definition.allows(role)) return true;
        }
        return false;
    }

    /**
     * 按物品的多重角色应用嵌入：工具/武器双角色时两边效果都生效，耐久倍率只应用一次。
     */
    public static boolean applyForRoles(ItemStack target, Definition definition, int level,
                                        net.minecraft.server.level.ServerLevel serverLevel) {
        if (definition == null) return false;
        var dynamic = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.definition(definition.id());
        if (dynamic != null) return dev.anvilcraft.thenextforge.api.ManagedEmbeddings.apply(target, definition.id(), dynamic, TargetKind.rolesOf(target), serverLevel);
        boolean changed = false;
        // 与锻造等级解耦：归零后再次嵌入同材料也不能覆盖旧条目。
        String prefix = "embed/" + definition.id().substring(definition.id().indexOf(':') + 1) + "/";
        int embeddingOrder = dev.anvilcraft.thenextforge.forging.ForgeAttributes.current(target)
            .modifiers().stream().map(entry -> entry.modifier().id())
            .filter(id -> id.getNamespace().equals("thenextforge") && id.getPath().startsWith(prefix))
            .mapToInt(id -> {
                try { return Integer.parseInt(id.getPath().substring(id.getPath().lastIndexOf('/') + 1)); }
                catch (NumberFormatException ignored) { return 0; }
            }).max().orElse(0) + 1;
        boolean durabilityApplied = false;
        for (TargetKind role : TargetKind.rolesOf(target)) {
            if (!definition.allows(role)) continue;
            changed |= apply(target, definition, role, embeddingOrder, serverLevel, !durabilityApplied);
            durabilityApplied = true;
        }
        return changed;
    }

    /**
     * 多重角色对象的加成描述：把各角色的描述并起来并去重（设计 L41）。
     */
    public static String bonusTextFor(ItemStack stack, String id, int level) {
        String frozen = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.describeApplied(stack, id);
        if (!frozen.isBlank()) return frozen;
        java.util.LinkedHashSet<String> texts = new java.util.LinkedHashSet<>();
        for (TargetKind role : TargetKind.rolesOf(stack)) {
            String text = bonusText(id, level, role);
            if (text != null && !text.isBlank()) texts.add(text);
        }
        String joined = String.join("，", texts);
        // 唯一嵌入（发光墨囊/回响碎片/金胡萝卜/纸/超限金属粒）在 tooltip 末尾标注
        Definition definition = byId(id);
        return definition != null && definition.unique() && !joined.isBlank()
            ? joined + "（唯一嵌入）" : joined;
    }

    /**
     * 把材料效果作用到目标物品，返回是否产生了实际属性变化。
     */
    public static boolean apply(ItemStack target, Definition definition, TargetKind kind, int level,
                                net.minecraft.server.level.ServerLevel serverLevel) {
        return apply(target, definition, kind, level, serverLevel, true);
    }

    /** 同上，但可禁止耐久倍率（多重角色同一次嵌入只该乘一次）。 */
    public static boolean apply(ItemStack target, Definition definition, TargetKind kind, int level,
                                net.minecraft.server.level.ServerLevel serverLevel, boolean allowDurability) {
        if (definition == null || !definition.allows(kind)) return false;
        // 设计 L40：数据驱动定义优先（同 id 可覆盖内置效果）
        EmbedData.Spec spec = EmbedData.specs().get(definition.id());
        if (spec != null) return applySpec(target, spec, kind, level, serverLevel);
        return switch (definition.id()) {
            case "minecraft:quartz" -> {
                // 设计：伤害 +20%（武器 / 护符）
                if (kind == TargetKind.WEAPON || kind == TargetKind.AMULET) {
                    add("minecraft:quartz", target, Attributes.ATTACK_DAMAGE, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "embed_quartz", level);
                }
                yield true;
            }
            case "minecraft:iron_ingot" -> {
                // 设计：方块触及 +1 格（工具 / 护符），实体触及 +1 格（武器 / 护符）→ 护符两条都要
                if (kind == TargetKind.WEAPON || kind == TargetKind.AMULET) {
                    add("minecraft:iron_ingot", target, Attributes.ENTITY_INTERACTION_RANGE, 1.0, AttributeModifier.Operation.ADD_VALUE, "embed_iron_reach", level);
                }
                if (kind == TargetKind.TOOL || kind == TargetKind.AMULET) {
                    add("minecraft:iron_ingot", target, Attributes.BLOCK_INTERACTION_RANGE, 1.0, AttributeModifier.Operation.ADD_VALUE, "embed_iron_block_reach", level);
                }
                yield true;
            }
            case "minecraft:redstone" -> {
                if (kind == TargetKind.WEAPON) {
                    // 设计「武器冷却 -20%」：冷却 = 1/攻速，攻速要 ×1.25
                    add("minecraft:redstone", target, Attributes.ATTACK_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_redstone", level);
                    yield true;
                }
                yield true;
            }
            case "minecraft:emerald" -> {
                if (kind == TargetKind.AMULET || kind == TargetKind.ARMOR) {
                    add("minecraft:emerald", target, Attributes.MAX_HEALTH, 3.0, AttributeModifier.Operation.ADD_VALUE, "embed_emerald", level);
                    yield true;
                }
                yield true;
            }
            case "#minecraft:logs" -> {
                if (kind == TargetKind.AMULET || kind == TargetKind.ARMOR) {
                    add("#minecraft:logs", target, Attributes.MAX_HEALTH, 1.0, AttributeModifier.Operation.ADD_VALUE, "embed_logs", level);
                    yield true;
                }
                yield true;
            }
            case "minecraft:gold_nugget" -> {
                if (kind == TargetKind.TOOL || kind == TargetKind.AMULET) fortune(target, fortuneLevel("minecraft:gold_nugget"), serverLevel);
                if (kind == TargetKind.WEAPON) {
                    add("minecraft:gold_nugget", target, Attributes.ATTACK_DAMAGE, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "embed_gold_nugget_damage", level);
                }
                if (durabilityApplies("minecraft:gold_nugget", kind)) scaleDurability(target, durabilityFactor("minecraft:gold_nugget", kind));
                yield true;
            }
            case "minecraft:gold_ingot" -> {
                if (kind == TargetKind.TOOL || kind == TargetKind.AMULET) fortune(target, fortuneLevel("minecraft:gold_ingot"), serverLevel);
                if (kind == TargetKind.WEAPON) {
                    add("minecraft:gold_ingot", target, Attributes.ATTACK_DAMAGE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "embed_gold_ingot_damage", level);
                }
                if (durabilityApplies("minecraft:gold_ingot", kind)) scaleDurability(target, durabilityFactor("minecraft:gold_ingot", kind));
                yield true;
            }
            case "minecraft:diamond" -> {
                if (kind == TargetKind.AMULET) {
                    add("minecraft:diamond", target, Attributes.ARMOR, 2.0, AttributeModifier.Operation.ADD_VALUE, "embed_diamond_armor", level);
                    yield true;
                }
                if (allowDurability && durabilityApplies("minecraft:diamond", kind)) scaleDurability(target, durabilityFactor("minecraft:diamond", kind));
                if (kind == TargetKind.TOOL) {
                    // 设计：挖掘等级 +1 → 直接获得钻石镐的挖掘等级，并与原工具的可挖集合取并集
                    upgradeMiningTier(target);
                }
                yield true;
            }
            case "minecraft:netherite_scrap" -> {
                if (kind == TargetKind.ARMOR) {
                    add("minecraft:netherite_scrap", target, Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADD_VALUE, "embed_scrap_armor", level);
                }
                if (allowDurability && durabilityApplies("minecraft:netherite_scrap", kind)) scaleDurability(target, durabilityFactor("minecraft:netherite_scrap", kind));
                yield true;
            }
            case "minecraft:netherite_ingot" -> {
                if (kind == TargetKind.ARMOR) {
                    add("minecraft:netherite_ingot", target, Attributes.ARMOR, 3.0, AttributeModifier.Operation.ADD_VALUE, "embed_netherite_armor", level);
                }
                if (allowDurability && durabilityApplies("minecraft:netherite_ingot", kind)) scaleDurability(target, durabilityFactor("minecraft:netherite_ingot", kind));
                yield true;
            }
            case "minecraft:nether_star" -> {
                if (kind == TargetKind.WEAPON || kind == TargetKind.AMULET) {
                    add("minecraft:nether_star", target, Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "embed_star", level);
                }
                yield true;
            }
            case "anvilcraft:royal_steel_ingot" -> {
                if (kind == TargetKind.WEAPON || kind == TargetKind.AMULET) {
                    add("anvilcraft:royal_steel_ingot", target, Attributes.ATTACK_DAMAGE, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "embed_royal_damage", level);
                }
                if (kind == TargetKind.ARMOR || kind == TargetKind.AMULET) {
                    add("anvilcraft:royal_steel_ingot", target, Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADD_VALUE, "embed_royal_armor", level);
                }
                if (allowDurability && durabilityApplies("anvilcraft:royal_steel_ingot", kind)) scaleDurability(target, durabilityFactor("anvilcraft:royal_steel_ingot", kind));
                yield true;
            }
            case "anvilcraft:cursed_gold_ingot" -> {
                // 伤害由 ForgeCombatEvents 按「携带护符时身上每 1 个诅咒 +5%」计算，这里不加固定属性
                yield kind == TargetKind.AMULET;
            }
            case "minecraft:feather" -> {
                if (kind == TargetKind.WEAPON) {
                    add("minecraft:feather", target, Attributes.ATTACK_DAMAGE, -0.75,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_feather_damage", level);
                    // 设计「武器冷却 -75%」：攻速 ×4
                    add("minecraft:feather", target, Attributes.ATTACK_SPEED, 3.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_feather_speed", level);
                }
                yield true;
            }
            case "minecraft:heavy_core" -> {
                if (kind == TargetKind.WEAPON) {
                    // 设计「击退 +150%」是倍率
                    add("minecraft:heavy_core", target, Attributes.ATTACK_KNOCKBACK, 1.5,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_heavy_knockback", level);
                    add("minecraft:heavy_core", target, Attributes.ATTACK_SPEED, -1.0 / 3.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_heavy_speed", level);
                }
                if (kind == TargetKind.ARMOR || kind == TargetKind.AMULET) {
                    add("minecraft:heavy_core", target, Attributes.MOVEMENT_SPEED, -0.2,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_heavy_move", level);
                }
                yield true;
            }
            case "minecraft:totem_of_undying" -> {
                if (kind == TargetKind.ARMOR || kind == TargetKind.AMULET) {
                    add("minecraft:totem_of_undying", target, Attributes.MAX_HEALTH, 5.0,
                        AttributeModifier.Operation.ADD_VALUE, "embed_totem_health", level);
                }
                yield true;
            }
            case "minecraft:paper" -> {
                // 设计：纸(扩张) —— 槽位 +3 由 slotBonus 提供，这里只砍耐久
                if (allowDurability && durabilityApplies("minecraft:paper", kind)) scaleDurability(target, durabilityFactor("minecraft:paper", kind));
                yield true;
            }
            case "anvilcraft:transcendium_nugget" -> {
                if (grantsUnbreakable("anvilcraft:transcendium_nugget")) {
                    target.set(net.minecraft.core.component.DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
                }
                yield true;
            }
            case "anvilcraft:neutronium_ingot" -> {
                if (kind == TargetKind.WEAPON) {
                    add("anvilcraft:neutronium_ingot", target, Attributes.ATTACK_DAMAGE, 0.8, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "embed_neutronium_damage", level);
                    // 设计「武器冷却 +30%」：攻速 ×(1/1.3)
                    add("anvilcraft:neutronium_ingot", target, Attributes.ATTACK_SPEED, -(1.0D - 1.0D / 1.3D), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "embed_neutronium_speed", level);
                }
                if (kind == TargetKind.ARMOR || kind == TargetKind.AMULET) {
                    add("anvilcraft:neutronium_ingot", target, Attributes.ARMOR, 2.0, AttributeModifier.Operation.ADD_VALUE, "embed_neutronium", level);
                }
                yield true;
            }
            // 以下材料的效果由事件类处理（战斗/掉落/破坏/防具），嵌入本身成功
            case "minecraft:glow_ink_sac", "minecraft:echo_shard", "minecraft:fermented_spider_eye",
                 "minecraft:slime_ball", "minecraft:ender_pearl", "minecraft:heart_of_the_sea",
                 "anvilcraft:amber", "anvilcraft:sapphire", "anvilcraft:ruby", "anvilcraft:topaz",
                 "anvilcraft:frost_metal_ingot", "anvilcraft:ember_metal_ingot",
                 "anvilcraft:negative_matter_nugget", "anvilcraft:void_matter",
                 "minecraft:wither_rose" -> true;
            default -> false;
        };
    }

    /**
     * 把工具的可挖集合升到钻石等级：与原工具有效集合取并集，不覆盖它本来能挖的方块。
     *
     * <p>1.21 的挖掘等级由 {@code Tool} 组件的规则表达，规则是「第一条命中即生效」，
     * 所以这里把「钻石镐可挖但不属于 {@code incorrect_for_diamond_tool}」的方块作为一条无速度的
     * {@code correct_for_drops=true} 规则**插在最前面**：原工具本就能挖的方块继续走它自己的规则（速度不变），
     * 原本挖不动的（如木镐挖钻石矿）从此能正常掉落。
     *
     * <p>只提升「等级」，不搬钻石镐的 8.0 挖掘速度。
     */
    public static void upgradeMiningTier(ItemStack stack) {
        net.minecraft.world.item.component.Tool current = stack.get(DataComponents.TOOL);
        if (current == null) return;
        if (!stack.has(dev.anvilcraft.thenextforge.init.ModDataComponents.PRE_EMBED_TOOL.get())) {
            stack.set(dev.anvilcraft.thenextforge.init.ModDataComponents.PRE_EMBED_TOOL.get(), current);
        }
        var mineable = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getOrCreateTag(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE);
        var diamondCannot = net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getOrCreateTag(net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        java.util.List<net.minecraft.core.Holder<net.minecraft.world.level.block.Block>> allowed =
            new java.util.ArrayList<>();
        for (net.minecraft.core.Holder<net.minecraft.world.level.block.Block> block : mineable) {
            if (!diamondCannot.contains(block)) allowed.add(block);
        }
        if (allowed.isEmpty()) return;
        net.minecraft.world.item.component.Tool.Rule grant = new net.minecraft.world.item.component.Tool.Rule(
            net.minecraft.core.HolderSet.direct(allowed),
            java.util.Optional.empty(),
            java.util.Optional.of(true)
        );
        java.util.List<net.minecraft.world.item.component.Tool.Rule> rules = new java.util.ArrayList<>();
        rules.add(grant);
        rules.addAll(current.rules());
        stack.set(DataComponents.TOOL, new net.minecraft.world.item.component.Tool(
            java.util.List.copyOf(rules), current.defaultMiningSpeed(), current.damagePerBlock()));
    }

    private static void add(
        String materialId,
        ItemStack stack,
        Holder<Attribute> attribute,
        double amount,
        AttributeModifier.Operation operation,
        String key,
        int level
    ) {
        ItemAttributeModifiers current = dev.anvilcraft.thenextforge.forging.ForgeAttributes.current(stack);
        // id 里带材料名：取出材料时按前缀一次撤干净
        String materialPath = materialId.substring(materialId.indexOf(':') + 1);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
            "thenextforge", "embed/" + materialPath + "/" + key + "/" + level
        );
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, current.withModifierAdded(
            attribute,
            new AttributeModifier(id, amount, operation),
            TargetKind.of(stack) == TargetKind.ARMOR ? EquipmentSlotGroup.ARMOR
                : TargetKind.of(stack) == TargetKind.AMULET ? EquipmentSlotGroup.BODY : EquipmentSlotGroup.MAINHAND
        ).withTooltip(false));
    }
}
