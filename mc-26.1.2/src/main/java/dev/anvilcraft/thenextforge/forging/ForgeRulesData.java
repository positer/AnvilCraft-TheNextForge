package dev.anvilcraft.thenextforge.forging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 锻造规则的数据驱动层（设计 L19：对象集合、数值增幅、模型放大开放数据驱动与黑白名单）。
 *
 * <p>规则文件：`config/thenextforge/forge_rules.json`；文件不存在时写出默认值并直接使用默认值。
 * 默认值与原先硬编码的数值逐条一致（攻击 5%-10%、触及 0.10 且 30% 权重、工具 3%-8%、
 * 装甲 50% +1 护甲或 5%-10% 耐久、护符 1%-5% 伤害、每级模型 +10%）。
 *
 * <p>集合匹配支持物品 id（`minecraft:diamond_sword`）与标签（`#minecraft:swords`）。
 */
public final class ForgeRulesData {
    /** 一个升级结果：效果键、权重、数值区间与附加限制。 */
    public record Outcome(String effect, int weight, double min, double max, boolean swordsOnly) {
        public double roll(RandomSource random) {
            return this.min >= this.max ? this.min : this.min + random.nextDouble() * (this.max - this.min);
        }
    }

    /** 模型放大设置。 */
    public record ModelScale(boolean enabled, double perLevel, List<String> blacklist) {
    }

    private static final Path FILE = Path.of("config", "thenextforge", "forge_rules.json");
    private static ForgeRulesData current = defaults();

    private final List<String> targetWhitelist;
    private final List<String> targetBlacklist;
    private final ModelScale modelScale;
    private final List<String> amuletWhitelist;
    private final List<String> amuletBlacklist;
    private final Map<String, Map<String, List<Outcome>>> kinds;

    private ForgeRulesData(List<String> targetWhitelist, List<String> targetBlacklist, ModelScale modelScale,
                           Map<String, Map<String, List<Outcome>>> kinds,
                           List<String> amuletWhitelist, List<String> amuletBlacklist) {
        this.targetWhitelist = List.copyOf(targetWhitelist);
        this.targetBlacklist = List.copyOf(targetBlacklist);
        this.modelScale = modelScale;
        this.kinds = kinds;
        this.amuletWhitelist = List.copyOf(amuletWhitelist);
        this.amuletBlacklist = List.copyOf(amuletBlacklist);
    }

    // region 访问器（测试与调试用）

    public List<String> targetWhitelist() {
        return this.targetWhitelist;
    }

    public List<String> targetBlacklist() {
        return this.targetBlacklist;
    }

    public List<String> amuletWhitelist() {
        return this.amuletWhitelist;
    }

    public List<String> amuletBlacklist() {
        return this.amuletBlacklist;
    }

    public ModelScale modelScale() {
        return this.modelScale;
    }

    public Map<String, Map<String, List<Outcome>>> kinds() {
        return this.kinds;
    }

    // endregion

    // region 默认值（与既有硬编码逐条一致）

    /** 设计里各对象 / 强化线的默认数值区间。 */
    public static ForgeRulesData defaults() {
        Map<String, Map<String, List<Outcome>>> kinds = new LinkedHashMap<>();
        kinds.put("WEAPON", Map.of(
            "ANVIL", List.of(
                new Outcome("attack", 70, 0.05, 0.10, false),
                new Outcome("reach", 30, 0.10, 0.10, false)),
            "GRINDSTONE", List.of(
                new Outcome("cooldown", 70, 0.05, 0.10, false),
                new Outcome("sweep", 30, 0.10, 0.10, true))
        ));
        kinds.put("TOOL", Map.of(
            "ANVIL", List.of(new Outcome("mining", 100, 0.03, 0.08, false)),
            "GRINDSTONE", List.of()
        ));
        kinds.put("ARMOR", Map.of(
            "ANVIL", List.of(
                new Outcome("durability", 50, 0.05, 0.10, false),
                new Outcome("armor_flat", 50, 1.0, 1.0, false)),
            "GRINDSTONE", List.of(new Outcome("move_speed", 100, 0.05, 0.10, false))
        ));
        kinds.put("AMULET", Map.of(
            "ANVIL", List.of(new Outcome("amulet_damage", 100, 0.01, 0.05, false)),
            "GRINDSTONE", List.of()
        ));
        return new ForgeRulesData(
            List.of(),
            List.of(),
            new ModelScale(true, 0.10D, List.of()),
            kinds,
            List.of(),
            List.of()
        );
    }

    // endregion

    /** 当前生效的规则（未加载时为默认值）。 */
    public static ForgeRulesData current() {
        return current;
    }

    /** 测试注入：直接换成给定规则。 */
    public static void override(ForgeRulesData data) {
        current = data == null ? defaults() : data;
    }

    /** 恢复默认（测试收尾用）。 */
    public static void resetToDefaults() {
        current = defaults();
    }

    /** 模组初始化：有文件就加载，没有就写出默认值。 */
    public static void init() {
        try {
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, toJson(defaults()));
                current = defaults();
                TheNextForge.LOGGER.info("[TNF] wrote default forge rules to {}", FILE.toAbsolutePath());
                return;
            }
            current = parse(Files.readString(FILE));
            TheNextForge.LOGGER.info("[TNF] loaded forge rules from {} (target whitelist={}, blacklist={})",
                FILE.toAbsolutePath(), current.targetWhitelist.size(), current.targetBlacklist.size());
        } catch (Exception exception) {
            TheNextForge.LOGGER.warn("[TNF] failed to read forge rules, falling back to defaults", exception);
            current = defaults();
        }
    }

    // region 查询

    /** 设计 L19 的对象黑白名单：黑名单直接拒绝，白名单非空时必须在其中。 */
    public static boolean isTargetAllowed(ItemStack stack) {
        ForgeRulesData data = current;
        if (matchesAny(stack, data.targetBlacklist)) return false;
        return data.targetWhitelist.isEmpty() || matchesAny(stack, data.targetWhitelist);
    }

    /** 设计 L91：护符锤击加成的黑白名单（数据驱动）。 */
    public static boolean isAmuletAllowed(ItemStack stack) {
        ForgeRulesData data = current;
        if (matchesAny(stack, data.amuletBlacklist)) return false;
        return data.amuletWhitelist.isEmpty() || matchesAny(stack, data.amuletWhitelist);
    }

    /** 该对象是否允许模型放大。 */
    public static boolean modelScaleEnabled(ItemStack stack) {
        return current.modelScale.enabled() && !matchesAny(stack, current.modelScale.blacklist());
    }

    /** 每级模型放大比例。 */
    public static double modelScalePerLevel() {
        return current.modelScale.perLevel();
    }

    /** 该（对象, 强化线）下当前可用的结果列表（已按对象过滤 swords_only 这类限制）。 */
    public static List<Outcome> eligible(TargetKind kind, ForgingLine line, ItemStack stack) {
        List<Outcome> rules = current.kinds
            .getOrDefault(kind.name(), Map.of())
            .getOrDefault(line.name(), List.of());
        boolean sword = stack.is(net.minecraft.tags.ItemTags.SWORDS);
        List<Outcome> out = new ArrayList<>(rules.size());
        for (Outcome outcome : rules) {
            if (outcome.swordsOnly() && !sword) continue;
            out.add(outcome);
        }
        return out;
    }

    /** 按权重抽一个结果；列表为空返回 null。 */
    public static Outcome pick(List<Outcome> outcomes, RandomSource random) {
        int total = 0;
        for (Outcome outcome : outcomes) total += Math.max(0, outcome.weight());
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (Outcome outcome : outcomes) {
            roll -= Math.max(0, outcome.weight());
            if (roll < 0) return outcome;
        }
        return outcomes.get(outcomes.size() - 1);
    }

    /** 集合条目匹配：支持 `#namespace:tag` 与物品 id。 */
    public static boolean matchesAny(ItemStack stack, List<String> entries) {
        if (entries.isEmpty() || stack.isEmpty()) return false;
        // 物品 id 只取一次并 toString 一次，避免在循环里反复分配
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            if (entry.startsWith("#")) {
                TagKey<net.minecraft.world.item.Item> tag = TagKey.create(
                    net.minecraft.core.registries.Registries.ITEM,
                    Identifier.parse(entry.substring(1)));
                if (stack.is(tag)) return true;
            } else if (itemId.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    // endregion

    // region 序列化

    public static ForgeRulesData parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<String> whitelist = strings(root, "target_whitelist");
        List<String> blacklist = strings(root, "target_blacklist");
        List<String> amuletWhitelist = strings(root, "amulet_whitelist");
        List<String> amuletBlacklist = strings(root, "amulet_blacklist");
        JsonObject scale = root.has("model_scale") ? root.getAsJsonObject("model_scale") : new JsonObject();
        ModelScale modelScale = new ModelScale(
            !scale.has("enabled") || scale.get("enabled").getAsBoolean(),
            scale.has("per_level") ? scale.get("per_level").getAsDouble() : 0.10D,
            strings(scale, "blacklist")
        );
        Map<String, Map<String, List<Outcome>>> kinds = new LinkedHashMap<>();
        if (root.has("kinds")) {
            JsonObject kindsJson = root.getAsJsonObject("kinds");
            for (String kind : kindsJson.keySet()) {
                JsonObject linesJson = kindsJson.getAsJsonObject(kind);
                Map<String, List<Outcome>> lines = new LinkedHashMap<>();
                for (String line : linesJson.keySet()) {
                    List<Outcome> outcomes = new ArrayList<>();
                    for (JsonElement element : linesJson.getAsJsonArray(line)) {
                        JsonObject object = element.getAsJsonObject();
                        outcomes.add(new Outcome(
                            object.get("effect").getAsString(),
                            object.has("weight") ? object.get("weight").getAsInt() : 100,
                            object.has("min") ? object.get("min").getAsDouble() : 0.0D,
                            object.has("max") ? object.get("max").getAsDouble() : 0.0D,
                            object.has("swords_only") && object.get("swords_only").getAsBoolean()
                        ));
                    }
                    lines.put(line, List.copyOf(outcomes));
                }
                kinds.put(kind, lines);
            }
        }
        return new ForgeRulesData(whitelist, blacklist, modelScale, kinds, amuletWhitelist, amuletBlacklist);
    }

    private static List<String> strings(JsonObject object, String key) {
        if (!object.has(key)) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) out.add(element.getAsString());
        return out;
    }

    /** 写出默认规则（便于玩家照着改）。 */
    public static String toJson(ForgeRulesData data) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"target_whitelist\": [");
        appendStrings(builder, data.targetWhitelist);
        builder.append("],\n  \"target_blacklist\": [");
        appendStrings(builder, data.targetBlacklist);
        builder.append("],\n  \"amulet_whitelist\": [");
        appendStrings(builder, data.amuletWhitelist);
        builder.append("],\n  \"amulet_blacklist\": [");
        appendStrings(builder, data.amuletBlacklist);
        builder.append("],\n  \"model_scale\": {\"enabled\": ").append(data.modelScale.enabled());
        builder.append(", \"per_level\": ").append(data.modelScale.perLevel()).append(", \"blacklist\": [");
        appendStrings(builder, data.modelScale.blacklist());
        builder.append("]},\n  \"kinds\": {\n");
        int kindIndex = 0;
        for (Map.Entry<String, Map<String, List<Outcome>>> kind : data.kinds.entrySet()) {
            builder.append("    \"").append(kind.getKey()).append("\": {\n");
            int lineIndex = 0;
            for (Map.Entry<String, List<Outcome>> line : kind.getValue().entrySet()) {
                builder.append("      \"").append(line.getKey()).append("\": [");
                int outcomeIndex = 0;
                for (Outcome outcome : line.getValue()) {
                    if (outcomeIndex++ > 0) builder.append(", ");
                    builder.append("{\"effect\": \"").append(outcome.effect())
                        .append("\", \"weight\": ").append(outcome.weight())
                        .append(", \"min\": ").append(outcome.min())
                        .append(", \"max\": ").append(outcome.max());
                    if (outcome.swordsOnly()) builder.append(", \"swords_only\": true");
                    builder.append("}");
                }
                builder.append("]").append(++lineIndex < kind.getValue().size() ? "," : "").append("\n");
            }
            builder.append("    }").append(++kindIndex < data.kinds.size() ? "," : "").append("\n");
        }
        builder.append("  }\n}\n");
        return builder.toString();
    }

    private static void appendStrings(StringBuilder builder, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append('"').append(values.get(i)).append('"');
        }
    }

    // endregion
}
