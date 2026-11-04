package dev.anvilcraft.thenextforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * 发光墨囊：主手工具带嵌入时，穿透性高亮 13 格内与副手方块同类型的方块。
 *
 * <p>渲染方式对齐仙藏的定位器：在 {@link RenderLevelStageEvent.Stage#AFTER_PARTICLES} 里把姿态平移到相机原点，
 * 用「无深度测试」的线段渲染类型画方框并当场 {@code endBatch}，因此方框能穿过方块显示；
 * 框架列表按 5 tick 的节奏扫描并缓存，避免每帧遍历 2 万个方块。
 */
@EventBusSubscriber(modid = TheNextForge.MOD_ID, value = Dist.CLIENT)
public final class GlowHighlight {
    private static final int RADIUS = 13;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final double BOX_GROW = 0.002D;
    private static final float RED = 1.0F;
    private static final float GREEN = 0.9F;
    private static final float BLUE = 0.2F;
    private static final float ALPHA = 0.8F;

    /** 穿透性线段：不写深度、不做深度测试，因此能被真实方块挡住之外的地方看见。 */
    private static final RenderType LINES = RenderType.create(
        "thenextforge_glow_highlight",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.DEBUG_LINES,
        1536,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setCullState(RenderStateShard.NO_CULL)
            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0D)))
            .createCompositeState(false)
    );

    private static List<BlockPos> highlighted = List.of();
    private static long lastScanTick = Long.MIN_VALUE;

    private GlowHighlight() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            highlighted = List.of();
            return;
        }
        rescanIfDue(minecraft);
        if (highlighted.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        VertexConsumer vertices = minecraft.renderBuffers().bufferSource().getBuffer(LINES);
        poses.pushPose();
        // 世界坐标 → 相机相对坐标，与仙藏 ToolLocatorRenderer 一致
        poses.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : highlighted) {
            LevelRenderer.renderLineBox(poses, vertices, new AABB(pos).inflate(BOX_GROW), RED, GREEN, BLUE, ALPHA);
        }
        poses.popPose();
        minecraft.renderBuffers().bufferSource().endBatch(LINES);
    }

    /** 每 {@link #SCAN_INTERVAL_TICKS} 个游戏刻重扫一次，条件不满足就清空缓存。 */
    private static void rescanIfDue(Minecraft minecraft) {
        Level level = minecraft.level;
        long now = level.getGameTime();
        if (now == lastScanTick || now % SCAN_INTERVAL_TICKS != 0) return;
        lastScanTick = now;

        ItemStack tool = minecraft.player.getMainHandItem();
        ItemStack offhand = minecraft.player.getOffhandItem();
        if (!dev.anvilcraft.thenextforge.forging.EmbedMaterials.glowHighlightApplies(tool, offhand)
            || !(offhand.getItem() instanceof BlockItem blockItem)) {
            highlighted = List.of();
            return;
        }
        Block target = blockItem.getBlock();
        BlockPos center = minecraft.player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
            center.offset(-RADIUS, -RADIUS, -RADIUS),
            center.offset(RADIUS, RADIUS, RADIUS)
        )) {
            if (level.getBlockState(pos).is(target)) found.add(pos.immutable());
        }
        highlighted = List.copyOf(found);
    }
}
