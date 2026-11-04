package dev.anvilcraft.thenextforge.event;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 自动化测试泵：只在 run 目录存在 .autotest 标记时启用。
 *
 * <p>负责准备鱼缸（经验流体、升级对象、嵌入材料）并制造落砧；
 * 真正的锤击由落砧事件（{@link AutomationHandler}）完成。
 */
public final class AutomationTestPump {
    private static final boolean ENABLED =
        java.nio.file.Files.exists(java.nio.file.Path.of(".autotest"))
            || java.nio.file.Files.exists(java.nio.file.Path.of("run/.autotest"))
            || System.getenv("TNF_AUTOTEST") != null;

    private static boolean reported;

    static {
        TheNextForge.LOGGER.info("[TNF] autotest pump enabled={}", ENABLED);
    }

    private AutomationTestPump() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!ENABLED) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getGameTime() % 10 != 0) return;
        if (!reported) {
            reported = true;
            TheNextForge.LOGGER.info("[TNF] autotest pump first tick, players={}", level.players().size());
        }

        // 测试战斗事件：附近没有敌对生物时补一只僵尸
        boolean hasHostile = false;
        for (var entity : level.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                hasHostile = true;
                break;
            }
        }
        if (!hasHostile) {
            for (ServerPlayer player : level.players()) {
                var zombie = new net.minecraft.world.entity.monster.Zombie(
                    net.minecraft.world.entity.EntityType.ZOMBIE, level
                );
                zombie.setPos(player.getX() + 3, player.getY(), player.getZ());
                level.addFreshEntity(zombie);
                TheNextForge.LOGGER.info("[TNF] autotest spawned test zombie at {}", zombie.blockPosition());
            }
        }

        for (ServerPlayer player : level.players()) {
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -8, -8), center.offset(8, 8, 8))) {
                if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity tank)) continue;
                prepare(level, pos, tank);
                return;
            }
        }
    }

    /** 在鱼缸旁放一台创造流体储罐并灌满经验流体，给自动锤提供无限供液。 */
    private static void ensureInfiniteTank(ServerLevel level, BlockPos pos) {
        BlockPos side = pos.east();
        if (level.getBlockEntity(side) instanceof dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity tank) {
            if (tank.getFluidHandler().getFluidInTank(0).getFluidType()
                .equals(dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID_TYPE.get())) {
                return;
            }
        }
        level.setBlockAndUpdate(side, dev.dubhe.anvilcraft.init.block.ModBlocks.CREATIVE_FLUID_TANK.getDefaultState());
        if (level.getBlockEntity(side) instanceof dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity tank) {
            var handler = tank.getFluidHandler();
            if (handler instanceof net.neoforged.neoforge.fluids.capability.templates.FluidTank fluidTank) {
                fluidTank.setFluid(new net.neoforged.neoforge.fluids.FluidStack(
                    dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), Integer.MAX_VALUE
                ));
            }
            TheNextForge.LOGGER.info("[TNF] autotest placed infinite experience tank at {} fluid={}",
                side, handler.getFluidInTank(0).getFluidType().getDescriptionId());
        }
    }

    private static void prepare(ServerLevel level, BlockPos pos, FishTankBlockEntity tank) {
        ensureInfiniteTank(level, pos);
        if (!level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.ANVIL)
            && !level.getBlockState(pos.below()).is(Blocks.GRINDSTONE)) {
            return;
        }
        var tankFluid = tank.getFluidHandler();
        var currentFluid = tankFluid.getFluidInTank(0);
        boolean wrongFluid = !currentFluid.getFluidType()
            .equals(dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID_TYPE.get());
        if (wrongFluid || currentFluid.getAmount() < 1000) {
            tankFluid.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            tankFluid.fill(
                new net.neoforged.neoforge.fluids.FluidStack(
                    dev.dubhe.anvilcraft.init.block.ModFluids.EXP_FLUID.get(), 1000
                ),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE
            );
            TheNextForge.LOGGER.info("[TNF] autotest topped up experience fluid at {}", pos);
        }
        if (tank.getItemHandler().getStackInSlot(0).isEmpty()) {
            tank.getItemHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.DIAMOND_SWORD
            ));
            TheNextForge.LOGGER.info("[TNF] autotest seeded upgrade target at {}", pos);
        }
        var material = tank.getItemHandler().getStackInSlot(1);
        if (material.isEmpty()) {
            tank.getItemHandler().setStackInSlot(1, new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.QUARTZ
            ));
            TheNextForge.LOGGER.info("[TNF] autotest seeded embed material at {}", pos);
        }
    }

    /**
     * 生成一个落在鱼缸正上方的下落实体，并补发落砧事件。
     *
     * <p>鱼缸的碰撞形状不足以让下落实体停住，所以这里直接生成实体并手动 post 事件。
     */
    private static void dropAnvil(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        if (!level.getBlockState(above).isAir()) return;
        FallingBlockEntity falling = FallingBlockEntity.fall(level, above, Blocks.ANVIL.defaultBlockState());
        falling.setPos(pos.getX() + 0.5, above.getY(), pos.getZ() + 0.5);
        NeoForge.EVENT_BUS.post(new AnvilEvent.OnLand(level, above, falling, 1.0F));
        TheNextForge.LOGGER.info("[TNF] autotest dropped anvil onto {}", pos);
    }
}
