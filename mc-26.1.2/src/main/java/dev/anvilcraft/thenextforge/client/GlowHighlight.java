package dev.anvilcraft.thenextforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.thenextforge.TheNextForge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 发光墨囊：主手工具带嵌入时，高亮 13 格内与副手方块同类型的方块。
 *
 * <p>26.1.2 的渲染管线整体更换（没有 {@code RenderType.create} 与 {@code RenderStateShard}），
 * 这里改用原版**现成**的线段渲染类型 {@code RenderTypes.lines()} 画方框，
 * 不再自定义管线——代价是方框不再穿透方块显示（原版该类型带深度测试）。
 */
@EventBusSubscriber(modid = TheNextForge.MOD_ID, value = Dist.CLIENT)
public final class GlowHighlight {
    private static final int RADIUS = 13;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final double BOX_GROW = 0.002D;
    private static final int COLOR = 0xFFE6FF33;

    /**
     * 穿透性线段：基于原版 {@code LINES_SNIPPET}，但显式关掉深度测试，因此方框能穿过方块显示
     * （与 1.21.1 的 {@code NO_DEPTH_TEST} 等价）。
     *
     * <p>26.1.2 要求自建管线在 {@link net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent}
     * 里注册，否则运行期可能被拒绝——所以这里分成「管线注册」与「RenderType 惰性构建」两步。
     */
    private static final com.mojang.blaze3d.pipeline.RenderPipeline PIPELINE;
    private static net.minecraft.client.renderer.rendertype.RenderType glowLines;

    static {
        PIPELINE = com.mojang.blaze3d.pipeline.RenderPipeline
            .builder(net.minecraft.client.renderer.RenderPipelines.LINES_SNIPPET)
            .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                TheNextForge.MOD_ID, "pipeline/glow_highlight"))
            .withDepthStencilState(java.util.Optional.empty())
            .build();
    }

    /** 在 mod bus 上注册自建管线（客户端）。 */
    public static void registerPipelines(
        net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event
    ) {
        event.registerPipeline(PIPELINE);
    }

    private static net.minecraft.client.renderer.rendertype.RenderType glowLines() {
        if (glowLines == null) {
            glowLines = net.minecraft.client.renderer.rendertype.RenderType.create(
                "thenextforge_glow_highlight",
                net.minecraft.client.renderer.rendertype.RenderSetup.builder(PIPELINE)
                    .bufferSize(1536)
                    .createRenderSetup());
        }
        return glowLines;
    }

    private static List<BlockPos> highlighted = List.of();
    private static long lastScanTick = Long.MIN_VALUE;

    private GlowHighlight() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            highlighted = List.of();
            return;
        }
        rescanIfDue(minecraft);
        if (highlighted.isEmpty()) return;

        // 26.1.2 的事件不再给 Camera，相机位置在 LevelRenderState 里
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poses = event.getPoseStack();
        var renderType = glowLines();
        VertexConsumer vertices = minecraft.renderBuffers().bufferSource().getBuffer(renderType);
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos pos : highlighted) {
            net.minecraft.client.renderer.ShapeRenderer.renderShape(
                poses, vertices, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState().getShape(
                    minecraft.level, pos),
                pos.getX() - BOX_GROW, pos.getY() - BOX_GROW, pos.getZ() - BOX_GROW,
                COLOR, 1.0F);
        }
        poses.popPose();
        minecraft.renderBuffers().bufferSource().endBatch(renderType);
    }

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
