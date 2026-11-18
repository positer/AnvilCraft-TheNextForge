package dev.anvilcraft.thenextforge.forging;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** 保存随机耐久提升的原值，降级不重掷，也不会遗留被降掉的耐久。 */
public record ForgeDurability(int baseline, List<Step> steps) {
    public record Step(String line, int level, double factor) {
        static final Codec<Step> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("line").forGetter(Step::line),
            Codec.INT.fieldOf("level").forGetter(Step::level),
            Codec.DOUBLE.fieldOf("factor").forGetter(Step::factor)
        ).apply(i, Step::new));
    }
    public static final Codec<ForgeDurability> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("baseline").forGetter(ForgeDurability::baseline),
        Step.CODEC.listOf().fieldOf("steps").forGetter(ForgeDurability::steps)
    ).apply(i, ForgeDurability::new));

    public static final StreamCodec<ByteBuf, ForgeDurability> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> {
            buf.writeInt(data.baseline());
            buf.writeInt(data.steps().size());
            for (Step step : data.steps()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, step.line());
                buf.writeInt(step.level());
                buf.writeDouble(step.factor());
            }
        },
        buf -> {
            int base = buf.readInt();
            int count = buf.readInt();
            if (count < 0 || count > buf.readableBytes() / 13) throw new IllegalArgumentException("Invalid durability history size");
            List<Step> steps = new ArrayList<>();
            for (int n = 0; n < count; n++) steps.add(new Step(
                ByteBufCodecs.STRING_UTF8.decode(buf), buf.readInt(), buf.readDouble()));
            return new ForgeDurability(base, List.copyOf(steps));
        });

    private static int scaled(int value, double factor) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, Math.round(value * factor)));
    }

    private int maximum() {
        int max = baseline;
        for (Step step : steps) max = scaled(max, step.factor());
        return max;
    }

    private void write(ItemStack stack) {
        stack.set(ModDataComponents.DURABILITY_HISTORY.get(), this);
        int maximum = maximum();
        stack.set(DataComponents.MAX_DAMAGE, maximum);
        markManaged(stack);
        stack.set(DataComponents.DAMAGE, Math.min(stack.getOrDefault(DataComponents.DAMAGE, 0), maximum - 1));
    }

    private static final ThreadLocal<java.util.Set<ItemStack>> NATIVE_READ = new ThreadLocal<>();

    /** 只在原生查询的调用范围内保存身份；finally 清空，嵌套查询不影响其他装备。 */
    public static int nativeMaximum(ItemStack stack) {
        var active = NATIVE_READ.get();
        if (active == null) {
            active = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            NATIVE_READ.set(active);
        }
        boolean added = active.add(stack);
        try { return stack.getItem().getMaxDamage(stack); }
        finally {
            if (added) active.remove(stack);
            if (active.isEmpty()) NATIVE_READ.remove();
        }
    }

    public static int managedMaximum(ItemStack stack) {
        if (!(stack.getItem() instanceof DynamicDurabilityItem)) return 0;
        var active = NATIVE_READ.get();
        if (active != null && active.contains(stack)) return 0;
        if (!Boolean.TRUE.equals(stack.get(ModDataComponents.DURABILITY_MANAGED.get()))
            && !stack.has(ModDataComponents.DURABILITY_HISTORY.get())) return 0;
        return stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
    }

    public static void markManaged(ItemStack stack) {
        if (stack.getItem() instanceof DynamicDurabilityItem) stack.set(ModDataComponents.DURABILITY_MANAGED.get(), true);
    }

    /** 换部件后以新的原生耐久重新计算，嵌入顺序及随机升级历史各只应用一次。 */
    public static void rebaseDynamic(ItemStack stack) {
        if (!(stack.getItem() instanceof DynamicDurabilityItem)) return;
        var history = stack.get(ModDataComponents.DURABILITY_HISTORY.get());
        var data = stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        boolean marked = Boolean.TRUE.equals(stack.get(ModDataComponents.DURABILITY_MANAGED.get()));
        if (history == null && data.embeds().isEmpty() && !marked) return;
        var frozen = dev.anvilcraft.thenextforge.api.ManagedEmbeddings.durabilityFactors(stack);
        int nativeMaximum = nativeMaximum(stack);
        int baseline = nativeMaximum;
        boolean embedded = false;
        for (int i = 0; i < data.embeds().size(); i++) {
            double factor = frozen.getOrDefault(i, EmbedMaterials.durabilityFactor(data.embeds().get(i), TargetKind.of(stack)));
            if (factor != 1) { baseline = scaled(baseline, factor); embedded = true; }
        }
        if (history != null) new ForgeDurability(baseline, history.steps()).write(stack);
        else if (embedded || marked) {
            stack.set(DataComponents.MAX_DAMAGE, baseline);
            stack.set(DataComponents.DAMAGE, Math.min(stack.getDamageValue(), Math.max(0, baseline - 1)));
            if (embedded) markManaged(stack); else stack.remove(ModDataComponents.DURABILITY_MANAGED.get());
        }
    }

    public static void upgrade(ItemStack stack, ForgingLine line, int level, double factor) {
        if (!Double.isFinite(factor) || factor <= 0 || stack.getMaxDamage() <= 0) return;
        ForgeDurability old = stack.get(ModDataComponents.DURABILITY_HISTORY.get());
        if (old == null) old = new ForgeDurability(stack.getMaxDamage(), List.of());
        List<Step> steps = new ArrayList<>(old.steps());
        steps.add(new Step(line.name(), level, factor));
        new ForgeDurability(old.baseline(), List.copyOf(steps)).write(stack);
    }

    /** 嵌入材料等外部倍率留在基底上，移除升级不能撤掉嵌入的耐久。 */
    public static boolean scaleBaseline(ItemStack stack, double factor) {
        ForgeDurability old = stack.get(ModDataComponents.DURABILITY_HISTORY.get());
        if (old == null) return false;
        new ForgeDurability(scaled(old.baseline(), factor), old.steps()).write(stack);
        return true;
    }

    public static void restoreBaseline(ItemStack stack, int baseline) {
        if (baseline <= 0) return;
        var current = stack.get(ModDataComponents.DURABILITY_HISTORY.get());
        if (current != null) new ForgeDurability(baseline, current.steps()).write(stack);
        else { stack.set(DataComponents.MAX_DAMAGE, baseline); stack.set(DataComponents.DAMAGE, Math.min(stack.getDamageValue(), baseline - 1)); }
    }

    public static void prune(ItemStack stack, ForgingData data) {
        ForgeDurability old = stack.get(ModDataComponents.DURABILITY_HISTORY.get());
        if (old == null) return;
        List<Step> kept = old.steps().stream().filter(step ->
            ForgingLine.ANVIL.name().equals(step.line()) ? step.level() <= data.anvilLevel()
                : ForgingLine.GRINDSTONE.name().equals(step.line()) && step.level() <= data.grindstoneLevel()).toList();
        if (kept.size() != old.steps().size()) new ForgeDurability(old.baseline(), kept).write(stack);
    }
}
