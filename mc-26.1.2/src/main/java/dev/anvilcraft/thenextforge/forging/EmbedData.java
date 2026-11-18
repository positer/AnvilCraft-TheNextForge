package dev.anvilcraft.thenextforge.forging;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 嵌入材料的数据驱动层（设计 L40：效果与集合开放 API 与数据驱动，可新增映射/黑白名单材料）。
 *
 * <p>文件：`config/thenextforge/embed_materials.json`。内置材料仍然是默认值；文件里的同 id 条目会**覆盖**内置效果，
 * 新 id 则是新增材料。第三方模组可以走 {@link #register(String, Spec)} 在代码里注册，不需要改文件。
 */
public final class EmbedData {
    /** 一个材料对某类对象的属性效果。 */
    public record AttributeEffect(String attribute, String operation, double amount) {
    }

    /** 一个材料的完整数据定义。 */
    public record Spec(String id, String label, boolean unique, Set<TargetKind> kinds, int color,
                       Map<TargetKind, List<AttributeEffect>> attributes, double durabilityFactor, int fortune,
                       boolean unbreakable, int extraSlots) {
    }

    private static final Path FILE = Path.of("config", "thenextforge", "embed_materials.json");

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();
    private static final Map<String, EmbedMaterials.Definition> REPLACED = new LinkedHashMap<>();
    private static final Set<String> REGISTERED = new java.util.LinkedHashSet<>();
    private static final Set<String> BLACKLIST = new java.util.LinkedHashSet<>();

    private EmbedData() {
    }

    /** 已加载的数据定义。 */
    public static Spec spec(String id) { return SPECS.get(id); }

    public static Map<String, Spec> specs() {
        return Map.copyOf(SPECS);
    }

    /** 是否被黑名单禁用。 */
    public static boolean isBlacklisted(String materialId) {
        return BLACKLIST.contains(materialId);
    }

    /** 该材料的额外槽位（数据驱动，默认 0）。 */
    public static int extraSlots(String materialId) {
        Spec spec = SPECS.get(materialId);
        return spec == null ? 0 : Math.max(0, spec.extraSlots());
    }

    /** 第三方注册入口：新增或覆盖一个材料。 */
    public static void register(String materialId, Spec spec) {
        if (materialId == null || materialId.isBlank() || spec == null) return;
        if (!SPECS.containsKey(materialId) && EmbedMaterials.hasDefinition(materialId)) REPLACED.put(materialId, EmbedMaterials.table().get(materialId));
        SPECS.put(materialId, spec);
        if (!EmbedMaterials.hasDefinition(materialId)) REGISTERED.add(materialId);
        EmbedMaterials.registerDefinition(new EmbedMaterials.Definition(
            materialId, spec.label(), spec.unique(), spec.kinds(), spec.color()));
        TheNextForge.LOGGER.info("[TNF] registered embed material {}", materialId);
    }

    /** 把材料加入黑名单（不可嵌入）。 */
    public static void blacklist(String materialId) {
        if (materialId != null && !materialId.isBlank()) BLACKLIST.add(materialId);
    }

    /** 清空数据（测试用）。 */
    public static void clear() {
        for (String id : REGISTERED) EmbedMaterials.removeDefinition(id);
        REGISTERED.clear();
        REPLACED.values().forEach(EmbedMaterials::registerDefinition);
        REPLACED.clear();
        SPECS.clear();
        BLACKLIST.clear();
    }

    /** 模组初始化：有文件就加载，没有就写出默认（空列表 + 纸的槽位作为示例）。 */
    public static void init() {
        try {
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, toJson());
                TheNextForge.LOGGER.info("[TNF] wrote default embed material file to {}", FILE.toAbsolutePath());
                return;
            }
            parse(Files.readString(FILE));
            TheNextForge.LOGGER.info("[TNF] loaded embed material data from {} (specs={}, blacklist={})",
                FILE.toAbsolutePath(), SPECS.size(), BLACKLIST.size());
        } catch (Exception exception) {
            TheNextForge.LOGGER.warn("[TNF] failed to read embed material data", exception);
        }
    }

    /** 解析 JSON（测试与 init 共用）。 */
    public static void parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (JsonElement element : root.has("blacklist") ? root.getAsJsonArray("blacklist") : new JsonArray()) {
            blacklist(element.getAsString());
        }
        if (!root.has("materials")) return;
        for (JsonElement element : root.getAsJsonArray("materials")) {
            JsonObject object = element.getAsJsonObject();
            String id = object.get("id").getAsString();
            Set<TargetKind> kinds = EnumSet.noneOf(TargetKind.class);
            for (JsonElement kind : object.getAsJsonArray("kinds")) {
                kinds.add(TargetKind.valueOf(kind.getAsString()));
            }
            Map<TargetKind, List<AttributeEffect>> attributes = new LinkedHashMap<>();
            if (object.has("attributes")) {
                JsonObject attributesJson = object.getAsJsonObject("attributes");
                for (String kind : attributesJson.keySet()) {
                    List<AttributeEffect> effects = new ArrayList<>();
                    for (JsonElement effect : attributesJson.getAsJsonArray(kind)) {
                        JsonObject effectJson = effect.getAsJsonObject();
                        effects.add(new AttributeEffect(
                            effectJson.get("attribute").getAsString(),
                            effectJson.has("operation") ? effectJson.get("operation").getAsString() : "add_value",
                            effectJson.has("amount") ? effectJson.get("amount").getAsDouble() : 1.0D));
                    }
                    attributes.put(TargetKind.valueOf(kind), List.copyOf(effects));
                }
            }
            register(id, new Spec(
                id,
                object.has("label") ? object.get("label").getAsString() : id,
                object.has("unique") && object.get("unique").getAsBoolean(),
                kinds,
                object.has("color") ? object.get("color").getAsInt() : 0xFFFFFF,
                attributes,
                object.has("durability_factor") ? object.get("durability_factor").getAsDouble() : 1.0D,
                object.has("fortune") ? object.get("fortune").getAsInt() : 0,
                object.has("unbreakable") && object.get("unbreakable").getAsBoolean(),
                object.has("extra_slots") ? object.get("extra_slots").getAsInt() : 0
            ));
        }
    }

    /** 输出默认文件模板（空列表，附一个注释性示例条目注释在外层文档里）。 */
    public static String toJson() {
        return "{\n" +
            "  \"blacklist\": [],\n" +
            "  \"materials\": []\n" +
            "}\n";
    }

    /** 把一条属性效果解析成可用的修饰符；属性无效时返回 null。 */
    public static AttributeModifier.Operation operation(String name) {
        for (AttributeModifier.Operation operation : AttributeModifier.Operation.values()) {
            if (operation.getSerializedName().equals(name)) return operation;
        }
        return AttributeModifier.Operation.ADD_VALUE;
    }

    /** 属性 id → Holder；无效时返回 null。 */
    /** 属性 id → Holder；兼容 `minecraft:attack_damage` 与注册名 `minecraft:generic.attack_damage`。 */
    public static Holder<Attribute> attribute(String id) {
        Identifier key = Identifier.parse(id);
        // 26.1.2：注册表 get 返回 Optional<Reference<T>>
        var holder = BuiltInRegistries.ATTRIBUTE.get(key);
        if (holder.isEmpty() && "minecraft".equals(key.getNamespace())) {
            holder = BuiltInRegistries.ATTRIBUTE.get(Identifier.withDefaultNamespace("generic." + key.getPath()));
        }
        return holder.map(h -> (net.minecraft.core.Holder<Attribute>) h).orElse(null);
    }
}
