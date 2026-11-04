package dev.anvilcraft.thenextforge.forging;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 方块上静置装备的展示实体。
 *
 * <p>用原版 {@link Display.ItemDisplay} 承载，物品随实体持久化；归属方块按实体位置反推，
 * 不额外写自定义数据，客户端与服务端用同一套判定。
 */
public final class ForgeDisplay {
    private ForgeDisplay() {
    }

    public static Display.ItemDisplay find(EntityGetter level, BlockPos pos) {
        List<Display.ItemDisplay> list = level.getEntitiesOfClass(
            Display.ItemDisplay.class,
            new AABB(pos).inflate(0.6, 1.0, 0.6),
            entity -> belongsTo(entity, pos)
        );
        return list.isEmpty() ? null : list.getFirst();
    }

    private static boolean belongsTo(Display.ItemDisplay entity, BlockPos pos) {
        return Math.abs(entity.getX() - (pos.getX() + 0.5)) < 0.25
            && Math.abs(entity.getZ() - (pos.getZ() + 0.5)) < 0.25
            && Math.abs(entity.getY() - (pos.getY() + 1.0)) < 0.35;
    }

    public static Display.ItemDisplay place(ServerLevel level, BlockPos pos, ItemStack stack, float yRot) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        CompoundTag tag = new CompoundTag();
        tag.putString("billboard", "fixed");
        // 三叉戟在原版是 builtin/entity 的 BEWLR 三维模型：上下文为 none 时原版不会套用它自己的
        // fixed 变换（0.5 缩放 + 位移），整把会按 1:1 画出来，看起来就偏离方块中心，等级放大后更明显。
        // 只有三叉戟需要 fixed；普通物品的 fixed 带 180 度翻转，会把平面贴图翻到背面（这正是本模组当初用 none 的原因）。
        if (stack.is(net.minecraft.world.item.Items.TRIDENT)) {
            tag.putString("item_display", "fixed");
        }
        tag.put("transformation", encodeTransformation(stack));
        tag.put("item", ItemStack.CODEC.encodeStart(
            level.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack.copy()
        ).getOrThrow());
        display.load(tag);
        display.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        display.setYRot(yRot);
        display.setNoGravity(true);
        level.addFreshEntity(display);
        return display;
    }

    /**
     * 设计 L19：模型放大由数据驱动——每级倍率来自 ForgeRulesData，并支持整类关闭与黑名单。
     * 铁砧与砂轮等级加算。
     */
    /** 展示实体用的倍率：同样受实验性开关与数据驱动配置约束。 */
    public static float scaleOf(ItemStack stack) {
        return ModelScaling.scaleOf(stack);
    }

    /** 只看等级与数据驱动配置的原始倍率（实验性开关的判断在 {@link ModelScaling}）。 */
    public static float rawScaleOf(ItemStack stack) {
        ForgingData data = stack.get(dev.anvilcraft.thenextforge.init.ModDataComponents.FORGING.get());
        if (data == null || !ForgeRulesData.modelScaleEnabled(stack)) return 1.0F;
        double perLevel = ForgeRulesData.modelScalePerLevel();
        return (float) (1.0D + (data.level(ForgingLine.ANVIL) + data.level(ForgingLine.GRINDSTONE)) * perLevel);
    }
    public static ItemStack stackOf(Display.ItemDisplay display) {
        return display.getSlot(0).get();
    }

    /** 换物品并立刻按新等级刷新模型大小（放置后升级/降级都当场生效）。 */
    public static void setStack(Display.ItemDisplay display, ItemStack stack) {
        display.getSlot(0).set(stack);
        refreshTransformation(display, stack);
    }

    /**
     * 按物品当前等级重算展示变换并应用。
     *
     * <p>{@link Display} 没有公开的变换 setter，因此沿用 {@code place} 用的同一套 NBT：
     * 先保存实体自身的完整数据，只替换 {@code transformation} 再 load 回去，
     * 位置、朝向、无重力、billboard 等其余字段原样保留。
     */
    public static void refreshTransformation(Display.ItemDisplay display, ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        display.saveWithoutId(tag);
        tag.put("transformation", encodeTransformation(stack));
        display.load(tag);
    }

    /** 设计：每级触及/横扫提升 10% 模型大小，铁砧与砂轮加算。 */
    private static net.minecraft.nbt.Tag encodeTransformation(ItemStack stack) {
        float scale = scaleOf(stack);
        Transformation transformation = new Transformation(
            new Vector3f(0.0F, 0.12F, 0.0F),
            new Quaternionf(),
            new Vector3f(scale, scale, scale),
            new Quaternionf().rotateX((float) (Math.PI / 2.0))
        );
        return Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).getOrThrow();
    }
}
