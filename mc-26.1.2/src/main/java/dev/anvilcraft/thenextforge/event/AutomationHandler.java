package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.EmbedRemoval;
import dev.anvilcraft.thenextforge.forging.ForgingData;
import dev.anvilcraft.thenextforge.forging.ForgingLine;
import dev.anvilcraft.thenextforge.forging.ForgingMaterial;
import dev.anvilcraft.thenextforge.forging.ForgingRules;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 自动化锤击：铁砧掉落在「基座砧上方的鱼缸」上时，把鱼缸里的物品当作被锤击的对象。
 *
 * <p>经验由鱼缸内的经验流体等价替代，不显示文字但保留特效与音效。
 */
public final class AutomationHandler {
    private AutomationHandler() {
    }

    /**
     * 对指定鱼缸执行一次自动锤击，返回是否发生了有效结算。
     *
     * <p>抽出给事件与 GameTest 共用。
     */
    public static boolean applyAutomatedHit(ServerLevel level, BlockPos pos, BlockState falling) {
        return applyAutomatedHit(level, pos, falling, level.getRandom());
    }

    /** 带随机源的版本：测试可注入固定种子，生产走 {@code level.getRandom()}。 */
    public static boolean applyAutomatedHit(ServerLevel level, BlockPos pos, BlockState falling,
                                            net.minecraft.util.RandomSource random) {
        TheNextForge.LOGGER.info("[TNF] automation enter pos={} below={} falling={}", pos,
            level.getBlockState(pos.below()).getBlock(), falling.getBlock());
        if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity tank)) return false;

        BlockState below = level.getBlockState(pos.below());
        ForgingMaterial base = ForgingMaterial.ofAnvilBlock(below);
        ForgingLine line = ForgingLine.ANVIL;
        if (base == null) {
            base = ForgingMaterial.ofGrindstoneBlock(below);
            line = ForgingLine.GRINDSTONE;
        }
        if (base == null) {
            TheNextForge.LOGGER.info("[TNF] automation exit: below is not an anvil/grindstone");
            return false;
        }

        ForgingMaterial hammer = ForgingMaterial.ofAnvilBlock(falling);
        if (hammer == null) {
            TheNextForge.LOGGER.info("[TNF] automation exit: falling block is not an anvil");
            return false;
        }

        java.util.Set<String> amulets = dev.anvilcraft.thenextforge.forging.Amulets.collect(level, pos.below());
        boolean silent = amulets.contains("silence");
        // 自动锤结构只接受经验流体
        var tankFluid = FishTankFluids.get(tank);
        if (!tankFluid.isEmpty() && !tankFluid.getFluidType()
            .equals(dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID_TYPE.get())) {
            TheNextForge.LOGGER.info("[TNF] automation rejected: fish tank holds a non-experience fluid");
            return false;
        }
        // 约束：鱼缸内只允许「单个可升级对象」与「单个属于它白名单的嵌入材料」；空缸不接受材料
        var handler = tank.getItemHandler();
        java.util.List<Integer> targetSlots = new java.util.ArrayList<>();
        java.util.List<Integer> otherSlots = new java.util.ArrayList<>();
        for (int slot = 8; slot < 16; slot++) {
            ItemStack candidate = FishTankSlots.get(tank, slot);
            if (candidate.isEmpty()) continue;
            if (TargetKind.of(candidate) != null) {
                targetSlots.add(slot);
            } else {
                otherSlots.add(slot);
            }
        }
        if (targetSlots.isEmpty()) {
            TheNextForge.LOGGER.info("[TNF] automation exit: tank holds no forging target (materials are not accepted without one)");
            return false;
        }
        if (targetSlots.size() > 1) {
            // 自愈：保留等级最高的一个，其余移到 output，避免重复对象导致永久卡死
            StringBuilder preHeal = new StringBuilder();
            for (int s : targetSlots) {
                ItemStack it = FishTankSlots.get(tank, s);
                preHeal.append(s).append('=').append(it.isEmpty() ? "air" : it.getItem() + "x" + it.getCount()).append(' ');
            }
            TheNextForge.LOGGER.info("[TNF] self-heal inputs: [{}]", preHeal.toString().trim());
            int keep = targetSlots.getFirst();
            int keepLevel = -1;
            for (int slot : targetSlots) {
                ForgingData d = FishTankSlots.get(tank, slot).getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
                int lv = d.level(line);
                if (lv > keepLevel) { keepLevel = lv; keep = slot; }
            }
            for (int slot : targetSlots) {
                if (slot == keep) continue;
                ItemStack extra = FishTankSlots.extract(handler, slot);
                if (!extra.isEmpty()) FishTankSlots.insertOutput(tank, extra.copy());
            }
            StringBuilder healDump = new StringBuilder();
            for (int s = 8; s < 16; s++) {
                ItemStack it = FishTankSlots.get(tank, s);
                if (!it.isEmpty()) healDump.append(s).append('=').append(it.getItem()).append('x').append(it.getCount()).append(' ');
            }
            TheNextForge.LOGGER.info("[TNF] automation self-healed: kept slot {} level {}, moved {} duplicate(s); slots=[{}]",
                keep, keepLevel, targetSlots.size() - 1, healDump.toString().trim());
            targetSlots.clear();
            targetSlots.add(keep);
        }
        if (otherSlots.size() > 1) {
            TheNextForge.LOGGER.info("[TNF] automation rejected: tank must hold at most one material, found {}", otherSlots.size());
            return false;
        }
        int targetSlot = targetSlots.getFirst();
        ItemStack stack = FishTankSlots.get(tank, targetSlot);
        TargetKind kind = TargetKind.of(stack);
        int materialSlotIndex = otherSlots.isEmpty() ? -1 : otherSlots.getFirst();
        ItemStack materialSlot = materialSlotIndex >= 0 ? FishTankSlots.get(tank, materialSlotIndex) : ItemStack.EMPTY;

        // 新增：鱼缸里放海绵芽 → 不判定成功与否，消耗 1 个海绵芽，逆序取出最近嵌入的材料（只在砂轮上有意义）
        if (EmbedRemoval.isSpongeGemmule(materialSlot)) {
            if (line != ForgingLine.GRINDSTONE) {
                TheNextForge.LOGGER.info("[TNF] automation rejected: sponge gemmule only works above a grindstone");
                return false;
            }
            return automatedSpongeRemove(level, pos, tank, stack, targetSlot, materialSlotIndex, materialSlot);
        }

        // 设计 L23/L36：工具与护符不能用砂轮升级（放置允许，结算时拒绝）
        if (line == ForgingLine.GRINDSTONE && !TargetKind.rolesOf(stack).contains(TargetKind.WEAPON) && kind != TargetKind.ARMOR) {
            TheNextForge.LOGGER.info("[TNF] automation refused: {} cannot be upgraded on a grindstone", kind);
            return false;
        }

        if (!materialSlot.isEmpty()) {
            if (materialSlot.getCount() > 1) {
                TheNextForge.LOGGER.info("[TNF] automation rejected: embed slot must hold a single material");
                return false;
            }
            var def = dev.anvilcraft.thenextforge.forging.EmbedMaterials.of(materialSlot);
            if (def == null || !dev.anvilcraft.thenextforge.forging.EmbedMaterials.allowsFor(def, stack)) {
                TheNextForge.LOGGER.info("[TNF] automation rejected: {} is not an allowed embed for this target", materialSlot.getItem());
                return false;
            }
        }
        TheNextForge.LOGGER.info("[TNF] automation target slot={} item={} material={}", targetSlot, stack.getItem(), materialSlot.getItem());
        {
            StringBuilder before = new StringBuilder();
            for (int s = 8; s < 16; s++) {
                ItemStack it = FishTankSlots.get(tank, s);
                if (!it.isEmpty()) before.append(s).append('=').append(it.getItem()).append('x').append(it.getCount()).append(' ');
            }
            TheNextForge.LOGGER.info("[TNF] tank snapshot before: [{}]", before.toString().trim());
        }

        ForgingData data = stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
        int current = data.level(line);
        if (current >= ForgingRules.MAX_AUTOMATION_LEVEL) {
            TheNextForge.LOGGER.info("[TNF] automation stops at the level cap {}/{}", current, ForgingRules.MAX_AUTOMATION_LEVEL);
            return false;
        }
        final int selectedSlot = targetSlot;
        final int embedSlot = materialSlotIndex;
        final boolean[] paid = {false};
        dev.anvilcraft.thenextforge.forging.ForgingService.settle(
            new dev.anvilcraft.thenextforge.forging.ForgingService.StrikeAccess() {
                public ItemStack material() { return materialSlot; }
                public net.minecraft.util.RandomSource random() { return random; }
                public boolean pay(int cost) {
                    paid[0] = consumeExperience(level, pos, cost);
                    return paid[0];
                }
                public void refund(int xp) { fillRefund(tank, xp); }
                // 自动化没有操作者，不虚构玩家着火和扣血。
                public boolean onFire() { return false; }
                public void rubyCost() { }
                public void save(ItemStack result) {
                    FishTankSlots.set(tank, selectedSlot, result);
                    if (embedSlot >= 0) FishTankSlots.set(tank, embedSlot, materialSlot);
                }
                public void destroy() { FishTankSlots.set(tank, selectedSlot, ItemStack.EMPTY); }
                public void message(net.minecraft.network.chat.Component message) { }
            }, stack.copy(), level, pos.below(), line, base, hammer);
        if (!paid[0]) return false;
        ItemStack after = FishTankSlots.get(tank, selectedSlot);
        if (!after.isEmpty()) {
            ForgingData afterData = after.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY);
            tryOutput(level, pos, tank, afterData.level(line), targetSlot);
        }
        return true;
    }

    /**
     * 自动锤的海绵芽取出：消耗 1 个海绵芽，把最近嵌入的材料放进鱼缸输出槽（放不下的掉落）。
     */
    private static boolean automatedSpongeRemove(ServerLevel level, BlockPos pos,
                                                 FishTankBlockEntity tank, ItemStack stack,
                                                 int targetSlot, int materialSlotIndex, ItemStack materialSlot) {
        ItemStack removed = EmbedRemoval.removeLatest(stack, level);
        if (removed.isEmpty()) {
            TheNextForge.LOGGER.info("[TNF] automated sponge gemmule: nothing to remove");
            return false;
        }
        materialSlot.shrink(1);
        FishTankSlots.set(tank, materialSlotIndex,
            materialSlot.isEmpty() ? ItemStack.EMPTY : materialSlot);
        FishTankSlots.set(tank, targetSlot, stack);
        ItemStack leftover = FishTankSlots.insertOutput(tank, removed.copy());
        if (!leftover.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                leftover);
        }
        TheNextForge.LOGGER.info("[TNF] automated sponge gemmule returned {} (embeds left {})",
            removed.getItem(), stack.getOrDefault(ModDataComponents.FORGING.get(), ForgingData.EMPTY).embeds().size());
        return true;
    }

    /**
     * 自动输出阈值：开口且无红石信号时产物到 1 级即输出，信号强度 s 时到 2s+1 级输出。
     *
     * <p>是否输出由鱼缸开口（{@code FishTankBlock.OUTLET}）决定；不开口时永远不输出，只持续升级。
     */
    public static int outputThreshold(int redstoneSignal) {
        return redstoneSignal * 2 + 1;
    }
    public static void fillRefund(FishTankBlockEntity tank, int xp) {
        int mb = xp * MB_PER_XP;
        if (mb <= 0) return;
        FishTankFluids.insert(tank, new net.neoforged.neoforge.fluids.FluidStack(
            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), mb));
    }

    private static void tryOutput(ServerLevel level, BlockPos pos, FishTankBlockEntity tank, int itemLevel, int sourceSlot) {
        // 鱼缸没有开口就不输出：那是「持续升级」结构，红石信号只在开口后决定阈值
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof dev.dubhe.anvilcraft.block.workstation.FishTankBlock)
            || !state.getValue(dev.dubhe.anvilcraft.block.workstation.FishTankBlock.OUTLET)) {
            return;
        }
        int threshold = outputThreshold(level.getBestNeighborSignal(pos));
        if (itemLevel < threshold) return;
        int inputSlot = sourceSlot - 8;
        if (inputSlot < 0 || inputSlot >= 8) return;
        // 直接从 input 提取（proxy 8..15 对应 input 0..7），避免 proxy 双写造成复制
        ItemStack out = FishTankSlots.extract(tank.getInputHandler(), inputSlot);
        // 提取后强制清空该槽：引用共享时 extractItem 可能没真正移除，导致 input 与 output 各挂一份（渲染两个）
        FishTankSlots.set(tank.getInputHandler(), inputSlot, ItemStack.EMPTY);
        if (out.isEmpty()) return;
        ItemStack leftover = FishTankSlots.insertOutput(tank, out);
        {
            StringBuilder o = new StringBuilder();
            for (int s = 0; s < 16; s++) {
                ItemStack it = FishTankSlots.get(tank, s);
                if (!it.isEmpty()) o.append(s).append('=').append(it.getItem()).append('x').append(it.getCount()).append(' ');
            }
            TheNextForge.LOGGER.info("[TNF] tank after output: extracted={} leftover={} slots=[{}]", out.getItem(), leftover.getItem(), o.toString().trim());
        }
        if (!leftover.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, leftover
            );
        }
        TheNextForge.LOGGER.info("[TNF] automation output level {} (threshold {}, signal {}, outlet open)",
            itemLevel, threshold, level.getBestNeighborSignal(pos));
    }

    /** 每点经验折算的经验流体量：铁砧工艺 1 瓶（约 7 点经验）→ 250 mB，折合 35 mB/点。 */
    public static final int MB_PER_XP = 35;

    /**
     * 从鱼缸的经验流体里扣掉等价经验。
     *
     * @return 是否扣成功
     */
    /** 固定 124 个候选偏移：先六邻接，再半径 2 方盒，不在每锤重建和去重。 */
    private static final java.util.List<BlockPos> EXPERIENCE_OFFSETS = experienceOffsets();

    private static java.util.List<BlockPos> experienceOffsets() {
        java.util.List<BlockPos> result = new java.util.ArrayList<>();
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) result.add(BlockPos.ZERO.relative(direction));
        for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) result.add(new BlockPos(dx, dy, dz));
        }
        return java.util.List.copyOf(result);
    }

    public static boolean consumeExperience(net.minecraft.world.level.Level level, BlockPos pos, int xpCost) {
        if (xpCost <= 0 || xpCost > Integer.MAX_VALUE / MB_PER_XP) return false;
        if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity tank)) return false;
        int needed = xpCost * MB_PER_XP;
        java.util.Set<net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        var resource = net.neoforged.neoforge.transfer.fluid.FluidResource.of(new net.neoforged.neoforge.fluids.FluidStack(
            dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int remaining = needed;
            for (int i = -1; i < EXPERIENCE_OFFSETS.size() && remaining > 0; i++) {
                net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler;
                if (i < 0) handler = tank.getFluidHandler();
                else {
                    BlockPos target = pos.offset(EXPERIENCE_OFFSETS.get(i));
                    if (!level.hasChunkAt(target)) continue;
                    handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, target, null);
                    if (handler == null && level.getBlockEntity(target) instanceof dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder holder) handler = holder.getFluidHandler();
                }
                if (handler == null || !seen.add(handler)) continue;
                for (int slot = 0; slot < handler.size() && remaining > 0; slot++) {
                    remaining -= handler.extract(slot, resource, remaining, transaction);
                }
            }
            if (remaining > 0) return false;
            transaction.commit();
            return true;
        }
    }
}
