package dev.anvilcraft.thenextforge.gametest;

import com.google.gson.*;
import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.api.*;
import dev.anvilcraft.thenextforge.forging.*;
import dev.anvilcraft.thenextforge.init.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.util.*;

/** 独立运行的真实 Data Map 重载探针；不与普通测试共享活动数据包。 */
public final class ApiReloadProbe {
    private static boolean attemptedLoad;
    private static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException(reason);
    }
    public static void nativeDatapackReload(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        if (!attemptedLoad && TnfApi.target(new ItemStack(Items.CARROT)) == null) {
            attemptedLoad = true;
            try {
                Path source = Path.of(System.getProperty("tnf.apiProbeSource")).toAbsolutePath().normalize();
                Path directory = Path.of(System.getProperty("tnf.apiProbePack")).toAbsolutePath().normalize();
                require(directory.equals(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.DATAPACK_DIR)
                    .resolve("tnf_api_probe").toAbsolutePath().normalize()), "探针路径不是当前测试世界的数据包目录");
                require(!Files.exists(directory), "探针包路径已存在，拒绝覆盖");
                try (var paths = Files.walk(source)) {
                    for (Path path : paths.toList()) {
                        Path destination = directory.resolve(source.relativize(path));
                        if (Files.isDirectory(path)) Files.createDirectories(destination);
                        else Files.copy(path, destination);
                    }
                }
                Files.writeString(directory.resolve(".tnf-api-probe-owned"), "TNF API probe temporary pack v1");
            } catch (Exception failure) { throw new IllegalStateException("Cannot install probe pack after GameTest world creation", failure); }
            server.getPackRepository().reload();
            var packId = server.getPackRepository().getAvailableIds().stream().filter(v -> v.contains("tnf_api_probe")).findFirst().orElseThrow();
            var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
            if (!selected.contains(packId)) selected.add(packId);
            server.reloadResources(selected).whenCompleteAsync((ignored, error) -> {
                try {
                    if (error != null) throw new IllegalStateException("Initial pack load failed", error);
                    nativeDatapackReload(helper);
                } catch (Throwable failure) {
                    TheNextForge.LOGGER.error("[TNF-API-PROBE] FAIL", failure);
                    helper.runAfterDelay(1, () -> helper.assertTrue(false, failure.toString()));
                }
            }, server);
            return;
        }
        List<TnfApi.Registration> registrations = new ArrayList<>();
        registrations.add(TnfApi.registerTarget("minecraft:carrot", new TnfDefinitions.Target(true, List.of(TargetKind.TOOL), List.of(ForgingLine.ANVIL))));
        registrations.add(TnfApi.registerBase("minecraft:dirt", new TnfDefinitions.Base(true, ForgingLine.GRINDSTONE, ForgingMaterial.BASE)));
        try {
            require(TargetKind.of(new ItemStack(Items.CARROT)) == TargetKind.WEAPON, "原生 targets Data Map 未加载或没有覆盖 Java 默认值");
            require(TargetKind.of(new ItemStack(Items.OAK_LOG)) == TargetKind.TOOL, "物品标签未展开");
            require(ForgingMaterial.ofAnvilBlock(Blocks.DIRT.defaultBlockState()) == ForgingMaterial.FROST, "方块标签或基底定义未加载");
            require(ForgingMaterial.ofHammer(new ItemStack(Items.STICK)) == ForgingMaterial.ROYAL, "锤映射未加载");
            require(TnfApi.upgrades(new ItemStack(Items.CARROT)) != null, "升级 Data Map 未加载");
            require(EmbedMaterials.of(new ItemStack(Items.OAK_PLANKS)) != null, "材料标签未展开");
            ItemStack target = new ItemStack(Items.DIAMOND_AXE);
            int original = target.getMaxDamage();
            require(TnfApi.tryEmbed(target, new ItemStack(Items.BRICK), helper.getLevel()).applied(), "真实 Data Map 材料未进入效果执行器");
            require(target.getMaxDamage() == Math.round(original * 1.3), "真实 Data Map 耐久未生效");
            helper.setBlock(1, 1, 1, Blocks.DIRT);
            BlockPos basePos = helper.absolutePos(new BlockPos(1, 1, 1));
            var context = new dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext(helper.getLevel(), net.minecraft.world.phys.Vec3.atCenterOf(basePos.above(2)), null);
            var anvil = new dev.anvilcraft.thenextforge.recipe.ForgeBasePredicate(ForgingLine.ANVIL);
            var grind = new dev.anvilcraft.thenextforge.recipe.ForgeBasePredicate(ForgingLine.GRINDSTONE);
            require(anvil.test(context) && !grind.test(context), "自动锤未匹配 Data Map 基底");
            List<String> selected = List.copyOf(server.getPackRepository().getSelectedIds());
            String packId = selected.stream().filter(v -> v.contains("tnf_api_probe")).findFirst().orElseThrow(() -> new IllegalStateException("探针包不在已启用清单：" + selected));
            Path directory = Path.of(System.getProperty("tnf.apiProbePack")).toAbsolutePath().normalize();
            Path materialFile = directory.resolve("data/thenextforge/data_maps/item/embeddings.json");
            JsonObject json = JsonParser.parseString(Files.readString(materialFile)).getAsJsonObject();
            json.getAsJsonObject("values").getAsJsonObject("minecraft:brick").addProperty("durability_factor", 2);
            Files.writeString(materialFile, json.toString());
            TheNextForge.LOGGER.info("[TNF-API-PROBE] loaded targets/upgrades/bases/hammers/embeddings; tags expanded; selected={}", packId);
            server.reloadResources(selected).thenRunAsync(() -> {
                require(TnfApi.embedding("minecraft:brick").durabilityFactor() == 2, "/reload 没有更新材料定义");
                ItemStack fresh = new ItemStack(Items.DIAMOND_AXE);
                require(TnfApi.tryEmbed(fresh, new ItemStack(Items.BRICK), helper.getLevel()).applied() && fresh.getMaxDamage() == original * 2,
                    "/reload 后新物品未用新效果");
                TnfApi.removeLatestEmbedding(target, helper.getLevel());
                require(target.getMaxDamage() == original && !target.has(ModDataComponents.EMBED_JOURNAL.get()), "/reload 后旧物品没有按快照撤销");
                TheNextForge.LOGGER.info("[TNF-API-PROBE] reload changed=2.0 old_snapshot_restored=true");
            }, server).thenCompose(ignored -> server.reloadResources(selected.stream().filter(v -> !v.equals(packId)).toList()))
                .whenCompleteAsync((ignored, error) -> {
                    try {
                        if (error != null) throw new IllegalStateException("reload failed", error);
                        require(TargetKind.of(new ItemStack(Items.CARROT)) == TargetKind.TOOL, "移除包后没有恢复 Java 对象默认映射");
                        require(!anvil.test(context) && grind.test(context), "移除包后动态基底谓词没有刷新");
                        require(TnfApi.embedding("minecraft:brick") == null && EmbedMaterials.of(new ItemStack(Items.BRICK)) == null,
                            "移除包后材料定义残留");
                        TheNextForge.LOGGER.info("[TNF-API-PROBE] PASS native_maps=5 reloads=2 removal_fallback=true snapshot=true");
                        registrations.forEach(TnfApi.Registration::close);
                        helper.succeed();
                        // /reload 重建 26.x 测试注册表后，旧 runner 可能继续等待环境；断言完成即关闭隔离服务器。
                        server.halt(false);
                    } catch (Throwable failure) {
                        registrations.forEach(TnfApi.Registration::close);
                        TheNextForge.LOGGER.error("[TNF-API-PROBE] FAIL", failure);
                        helper.runAfterDelay(1, () -> helper.assertTrue(false, failure.toString()));
                    }
                }, server);
        } catch (Throwable failure) {
            registrations.forEach(TnfApi.Registration::close);
            throw new IllegalStateException("API probe setup failed", failure);
        }
    }
}
