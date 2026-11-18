package dev.anvilcraft.thenextforge.api;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.thenextforge.forging.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** API v1 的不可变、可同步定义；构造定义后通过 TnfApi 注册，或交给原生 Data Map Codec。 */
public final class TnfDefinitions {
    private TnfDefinitions() { }
    public static final Codec<TargetKind> KIND = Codec.STRING.comapFlatMap(s -> enumValue(TargetKind.class, s), Enum::name);
    public static final Codec<ForgingLine> LINE = Codec.STRING.comapFlatMap(s -> enumValue(ForgingLine.class, s), Enum::name);
    public static final Codec<ForgingMaterial> MATERIAL = Codec.STRING.comapFlatMap(s -> enumValue(ForgingMaterial.class, s), Enum::name);
    private static <T extends Enum<T>> DataResult<T> enumValue(Class<T> type, String value) {
        try { return DataResult.success(Enum.valueOf(type, value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + value); }
    }
    public static final Codec<Double> FINITE = Codec.DOUBLE.validate(v -> Double.isFinite(v)
        ? DataResult.success(v) : DataResult.error(() -> "Number must be finite"));
    public static final Codec<JsonObject> OBJECT = Codec.PASSTHROUGH.comapFlatMap(dynamic -> {
        var value = dynamic.convert(JsonOps.INSTANCE).getValue();
        if (!value.isJsonObject() || value.toString().length() > 8192) return DataResult.error(() -> "Effect config must be an object of at most 8192 characters");
        return DataResult.success(value.getAsJsonObject().deepCopy());
    }, value -> new Dynamic<>(JsonOps.INSTANCE, value.deepCopy()));

    public record Target(boolean enabled, List<TargetKind> roles, List<ForgingLine> lines) {
        public Target { roles = List.copyOf(roles); lines = List.copyOf(lines); }
        public static final Codec<Target> CODEC = RecordCodecBuilder.<Target>create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Target::enabled),
            KIND.listOf(0, 4).fieldOf("roles").forGetter(Target::roles),
            LINE.listOf(0, 2).optionalFieldOf("lines", List.of(ForgingLine.ANVIL, ForgingLine.GRINDSTONE)).forGetter(Target::lines)
        ).apply(i, Target::new)).validate(v -> v.roles.size() == new HashSet<>(v.roles).size()
            && v.lines.size() == new HashSet<>(v.lines).size() && (!v.enabled || !v.roles.isEmpty())
            ? DataResult.success(v) : DataResult.error(() -> "Roles/lines must be distinct; enabled targets require roles"));
    }
    public record Base(boolean enabled, ForgingLine line, ForgingMaterial material, double experienceMultiplier,
                       double successBonus, int extraLevels) {
        public Base(boolean enabled, ForgingLine line, ForgingMaterial material) { this(enabled, line, material, 1, 0, 0); }
        public static final Codec<Base> CODEC = RecordCodecBuilder.<Base>create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Base::enabled),
            LINE.fieldOf("line").forGetter(Base::line), MATERIAL.fieldOf("material").forGetter(Base::material),
            FINITE.optionalFieldOf("experience_multiplier", 1.0).forGetter(Base::experienceMultiplier),
            FINITE.optionalFieldOf("success_bonus", 0.0).forGetter(Base::successBonus),
            Codec.intRange(0, 16).optionalFieldOf("extra_levels", 0).forGetter(Base::extraLevels)
        ).apply(i, Base::new)).validate(v -> v.experienceMultiplier > 0 && v.experienceMultiplier <= 100
            && v.successBonus >= -1 && v.successBonus <= 1 ? DataResult.success(v) : DataResult.error(() -> "Invalid base multipliers"));
    }
    public record Effect(Identifier type, JsonObject config) {
        public Effect { config = config.deepCopy(); }
        @Override public JsonObject config() { return config.deepCopy(); }
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("type").forGetter(Effect::type),
            OBJECT.optionalFieldOf("config", new JsonObject()).forGetter(Effect::config)
        ).apply(i, Effect::new));
    }
    public record Upgrade(Effect effect, int weight, double min, double max, boolean swordsOnly) {
        public static final Codec<Upgrade> CODEC = RecordCodecBuilder.<Upgrade>create(i -> i.group(
            Effect.CODEC.fieldOf("effect").forGetter(Upgrade::effect),
            Codec.intRange(1, 1_000_000).optionalFieldOf("weight", 100).forGetter(Upgrade::weight),
            FINITE.fieldOf("min").forGetter(Upgrade::min), FINITE.fieldOf("max").forGetter(Upgrade::max),
            Codec.BOOL.optionalFieldOf("swords_only", false).forGetter(Upgrade::swordsOnly)
        ).apply(i, Upgrade::new)).validate(v -> v.min <= v.max ? DataResult.success(v) : DataResult.error(() -> "Upgrade min exceeds max"));
    }
    public record UpgradeRule(TargetKind kind, ForgingLine line, List<Upgrade> outcomes) {
        public UpgradeRule { outcomes = List.copyOf(outcomes); }
        public static final Codec<UpgradeRule> CODEC = RecordCodecBuilder.create(i -> i.group(
            KIND.fieldOf("kind").forGetter(UpgradeRule::kind), LINE.fieldOf("line").forGetter(UpgradeRule::line),
            Upgrade.CODEC.listOf(0, 256).fieldOf("outcomes").forGetter(UpgradeRule::outcomes)
        ).apply(i, UpgradeRule::new));
    }
    public record Upgrades(List<UpgradeRule> rules) {
        public Upgrades { rules = List.copyOf(rules); }
        public static final Codec<Upgrades> CODEC = RecordCodecBuilder.<Upgrades>create(i -> i.group(
            UpgradeRule.CODEC.listOf(0, 8).fieldOf("rules").forGetter(Upgrades::rules)
        ).apply(i, Upgrades::new)).validate(v -> v.rules.stream().map(r -> r.kind + "/" + r.line).distinct().count() == v.rules.size()
            ? DataResult.success(v) : DataResult.error(() -> "Duplicate upgrade kind/line"));
        public UpgradeRule rule(TargetKind kind, ForgingLine line) {
            return rules.stream().filter(r -> r.kind == kind && r.line == line).findFirst().orElse(null);
        }
    }
    public record AttributeGain(String attribute, String operation, double amount) {
        public static final Codec<AttributeGain> CODEC = RecordCodecBuilder.<AttributeGain>create(i -> i.group(
            Identifier.CODEC.xmap(Identifier::toString, Identifier::parse).fieldOf("attribute").forGetter(AttributeGain::attribute),
            Codec.STRING.optionalFieldOf("operation", "add_value").forGetter(AttributeGain::operation),
            FINITE.fieldOf("amount").forGetter(AttributeGain::amount)
        ).apply(i, AttributeGain::new)).validate(v -> Set.of("add_value", "add_multiplied_base", "add_multiplied_total").contains(v.operation)
            ? DataResult.success(v) : DataResult.error(() -> "Invalid attribute operation: " + v.operation));
    }
    public record Embedding(boolean enabled, String label, boolean unique, Set<TargetKind> kinds, int color,
                            Map<TargetKind, List<AttributeGain>> attributes, double durabilityFactor, int fortune,
                            boolean unbreakable, int extraSlots, boolean inheritBuiltinEffects, List<Effect> effects) {
        public Embedding {
            kinds = Collections.unmodifiableSet(kinds.isEmpty() ? EnumSet.noneOf(TargetKind.class) : EnumSet.copyOf(kinds));
            Map<TargetKind, List<AttributeGain>> copied = new EnumMap<>(TargetKind.class);
            attributes.forEach((k,v) -> copied.put(k, List.copyOf(v)));
            attributes = Collections.unmodifiableMap(copied); effects = List.copyOf(effects);
        }
        public static final Codec<Embedding> CODEC = RecordCodecBuilder.<Embedding>create(i -> i.group(
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Embedding::enabled),
            Codec.STRING.optionalFieldOf("label", "").forGetter(Embedding::label),
            Codec.BOOL.optionalFieldOf("unique", false).forGetter(Embedding::unique),
            KIND.listOf(0, 4).xmap(Set::copyOf, List::copyOf).fieldOf("kinds").forGetter(Embedding::kinds),
            Codec.intRange(0, 0xFFFFFF).optionalFieldOf("color", 0xFFFFFF).forGetter(Embedding::color),
            Codec.unboundedMap(KIND, AttributeGain.CODEC.listOf(0, 64)).optionalFieldOf("attributes", Map.of()).forGetter(Embedding::attributes),
            FINITE.optionalFieldOf("durability_factor", 1.0).forGetter(Embedding::durabilityFactor),
            Codec.intRange(0, 255).optionalFieldOf("fortune", 0).forGetter(Embedding::fortune),
            Codec.BOOL.optionalFieldOf("unbreakable", false).forGetter(Embedding::unbreakable),
            Codec.intRange(0, 1024).optionalFieldOf("extra_slots", 0).forGetter(Embedding::extraSlots),
            Codec.BOOL.optionalFieldOf("inherit_builtin_effects", false).forGetter(Embedding::inheritBuiltinEffects),
            Effect.CODEC.listOf(0, 32).optionalFieldOf("effects", List.of()).forGetter(Embedding::effects)
        ).apply(i, Embedding::new)).validate(v -> v.durabilityFactor > 0 && (!v.enabled || !v.kinds.isEmpty())
            && v.attributes.keySet().stream().allMatch(v.kinds::contains)
            ? DataResult.success(v) : DataResult.error(() -> "Embedding requires positive durability, valid kinds and matching attributes"));
    }
}
