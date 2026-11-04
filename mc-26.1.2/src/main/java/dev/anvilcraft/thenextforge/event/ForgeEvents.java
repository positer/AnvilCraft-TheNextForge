package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgeDisplay;
import dev.anvilcraft.thenextforge.forging.ForgingLine;
import dev.anvilcraft.thenextforge.forging.ForgingMaterial;
import dev.anvilcraft.thenextforge.forging.ForgingService;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 铁砧与砂轮上的放置、取下与锤击交互。
 *
 * <p>三者共用右键：主手持铁砧锤锤击，Shift 右键放置或取下装备。原版与铁砧工艺的右键行为在命中这些
 * 情形时被取消，其余情况放行。
 */
public final class ForgeEvents {
    private ForgeEvents() {
    }

    /**
     * 鱼缸插入拦截：先有升级对象，再按可嵌入白名单匹配材料，才允许放入。
     *
     * <p>规则见 {@link #checkInsertion}；被拒绝时把原因显示给玩家（否则玩家只能看到「放不进去」）。
     */
    @SubscribeEvent
    public static void onRightClickFishTank(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"anvilcraft".equals(id.getNamespace()) || !"fish_tank".equals(id.getPath())) return;
        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;
        if (!(level.getBlockEntity(pos) instanceof dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity tank)) return;
        var handler = tank.getItemHandler();
        int targetSlot = -1;
        boolean hasMaterial = false;
        for (int slot = 8; slot < 16; slot++) {
            ItemStack s = FishTankSlots.get(tank, slot);
            if (s.isEmpty()) continue;
            if (TargetKind.of(s) != null) {
                if (targetSlot < 0) targetSlot = slot;
            } else {
                hasMaterial = true;
            }
        }
        TargetKind targetKind = targetSlot < 0 ? null : TargetKind.of(FishTankSlots.get(tank, targetSlot));
        TankInsertion verdict = checkInsertion(held, targetSlot < 0 ? ItemStack.EMPTY : FishTankSlots.get(tank, targetSlot), targetSlot >= 0, hasMaterial);
        if (verdict == TankInsertion.ALLOW) {
            // 放行后由 AnvilCraft 自己插入：同一条日志能区分「我们拦了」与「鱼缸没接收（点击位置/标签）」
            TheNextForge.LOGGER.info("[TNF] fish tank insert allowed: {} target={} material={} hit={}",
                held.getItem(), targetKind, hasMaterial, event.getHitVec());
            return;
        }        TheNextForge.LOGGER.info("[TNF] fish tank refused {}: {} (target={} material={})",
            verdict, held.getItem(), targetKind, hasMaterial);
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player && !level.isClientSide()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(verdict.message()));
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
    }

    /** 鱼缸插入判定结果，连同给玩家看的说明。 */
    public enum TankInsertion {
        ALLOW(""),
        NO_TARGET("需要先把升级对象放进鱼缸"),
        SECOND_TARGET("鱼缸里已经有一个升级对象"),
        SECOND_MATERIAL("鱼缸里已经有一个嵌入材料"),
        NOT_ALLOWED_FOR_TARGET("这个材料不能嵌入该对象"),
        UNRELATED_ITEM("鱼缸里已有材料，只能再放升级对象");

        private final String message;

        TankInsertion(String message) {
            this.message = message;
        }

        public String message() {
            return this.message;
        }
    }

    /**
     * 鱼缸插入判定：升级对象 → 可嵌入白名单 → 才放材料。
     *
     * <ol>
     *   <li>手持升级对象：缸里已有对象就拒绝，否则放行；</li>
     *   <li>手持嵌入材料：必须已经有对象、还没有材料，且该材料对对象类别在白名单内；</li>
     *   <li>其它物品：缸里已有材料时拒绝，其余交给鱼缸本身判定。</li>
     * </ol>
     */
    /** 重载：直接用缸里的对象栈判定白名单（多重角色任一命中即可）。 */
    public static TankInsertion checkInsertion(ItemStack held, ItemStack target, boolean hasTarget,
                                               boolean hasMaterial) {
        java.util.List<TargetKind> roles = hasTarget && target != null && !target.isEmpty()
            ? TargetKind.rolesOf(target) : java.util.List.of();
        return checkInsertion(held, roles, hasTarget, hasMaterial);
    }

    public static TankInsertion checkInsertion(ItemStack held, TargetKind targetKind, boolean hasTarget,
                                               boolean hasMaterial) {
        return checkInsertion(held, targetKind == null ? java.util.List.of() : java.util.List.of(targetKind),
            hasTarget, hasMaterial);
    }

    private static TankInsertion checkInsertion(ItemStack held, java.util.List<TargetKind> roles,
                                                boolean hasTarget, boolean hasMaterial) {
        TargetKind targetKind = roles.isEmpty() ? null : roles.getFirst();
        boolean heldIsTarget = TargetKind.of(held) != null;
        dev.anvilcraft.thenextforge.forging.EmbedMaterials.Definition definition =
            dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(held);
        boolean heldIsMaterial = definition != null;
        // 海绵芽：砂轮上的「取出材料」道具，走材料槽位，但不参与可嵌入白名单
        boolean heldIsRemoval = dev.anvilcraft.thenextforge.forging.EmbedRemoval.isSpongeGemmule(held);
        if (heldIsTarget) return hasTarget ? TankInsertion.SECOND_TARGET : TankInsertion.ALLOW;
        if (heldIsRemoval) {
            if (!hasTarget || targetKind == null) return TankInsertion.NO_TARGET;
            if (hasMaterial) return TankInsertion.SECOND_MATERIAL;
            return TankInsertion.ALLOW;
        }
        if (heldIsMaterial) {
            if (!hasTarget || targetKind == null) return TankInsertion.NO_TARGET;
            if (hasMaterial) return TankInsertion.SECOND_MATERIAL;
            boolean anyRole = false;
            for (TargetKind role : roles) {
                if (definition.allows(role)) anyRole = true;
            }
            if (!anyRole) return TankInsertion.NOT_ALLOWED_FOR_TARGET;
            return TankInsertion.ALLOW;
        }
        return hasMaterial ? TankInsertion.UNRELATED_ITEM : TankInsertion.ALLOW;
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ForgingMaterial anvilMaterial = ForgingMaterial.ofAnvilBlock(state);
        ForgingMaterial grindstoneMaterial = ForgingMaterial.ofGrindstoneBlock(state);
        ForgingMaterial baseMaterial = anvilMaterial != null ? anvilMaterial : grindstoneMaterial;
        if (baseMaterial == null) return;
        ForgingLine line = anvilMaterial != null ? ForgingLine.ANVIL : ForgingLine.GRINDSTONE;

        ItemStack mainHand = event.getItemStack();
        ForgingMaterial hammerMaterial = ForgingMaterial.ofHammer(mainHand);
        boolean shift = event.getEntity().isShiftKeyDown();

        boolean hasDisplay = ForgeDisplay.find(level, pos) != null;

        TheNextForge.LOGGER.info(
            "[TNF] forge interact at {} mainHand={} display={} hammer={} shift={}",
            pos, mainHand.getItem(), hasDisplay, hammerMaterial != null, shift
        );
        if (level.isClientSide()) {
            boolean place = !hasDisplay && hammerMaterial == null && TargetKind.of(mainHand) != null;
            boolean take = shift && hasDisplay;
            boolean hit = !shift && hasDisplay && hammerMaterial != null;
            if (place || take || hit) cancel(event);
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();
        // 26.1.2：Player#serverLevel() 已移除，改从 level() 转型
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        Display.ItemDisplay display = ForgeDisplay.find(serverLevel, pos);

        if (hammerMaterial != null && display != null && !shift) {
            ForgingService.hammer(player, serverLevel, pos, line, baseMaterial, hammerMaterial);
            cancel(event);
            return;
        }

        if (display == null && hammerMaterial == null) {
            TargetKind kind = TargetKind.of(mainHand);
            TheNextForge.LOGGER.info("[TNF] forge place attempt kind={} stack={}", kind, mainHand);
            if (kind == null) return;
            // 砂轮的限制不再挡放置：允许放上去（这样才能用海绵芽取出嵌入），到真正锤击升级时才提示
            if (line == ForgingLine.GRINDSTONE && !kind.canUseGrindstone()) {
                TheNextForge.LOGGER.info("[TNF] placed a non-grindstone target on a grindstone (upgrade will be refused)");
            }
            String name = mainHand.getHoverName().getString();
            ForgeDisplay.place(serverLevel, pos, mainHand, player.getYRot());
            TheNextForge.LOGGER.info("[TNF] forge placed, display now={}", ForgeDisplay.find(serverLevel, pos) != null);
            player.getMainHandItem().shrink(1);
            player.sendOverlayMessage(Component.literal("已放置 " + name));
            cancel(event);
            return;
        }

        // 空手也能取回（MCP/自动化场景没有潜行输入）
        if ((shift || mainHand.isEmpty()) && display != null) {
            ItemStack stack = ForgeDisplay.stackOf(display);
            display.discard();
            player.getInventory().placeItemBackInInventory(stack);
            player.sendOverlayMessage(Component.literal("已取下装备"));
            cancel(event);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
