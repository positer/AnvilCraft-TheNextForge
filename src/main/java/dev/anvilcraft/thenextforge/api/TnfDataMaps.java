package dev.anvilcraft.thenextforge.api;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.ForgingMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/** NeoForge 原生 Data Map 负责标签展开、包优先级、/reload、客户端同步及移除。 */
public final class TnfDataMaps {
    private TnfDataMaps() { }
    public static final DataMapType<Item, TnfDefinitions.Target> TARGETS = DataMapType.builder(
        TheNextForge.of("targets"), Registries.ITEM, TnfDefinitions.Target.CODEC).synced(TnfDefinitions.Target.CODEC, true).build();
    public static final DataMapType<Item, TnfDefinitions.Upgrades> UPGRADES = DataMapType.builder(
        TheNextForge.of("upgrades"), Registries.ITEM, TnfDefinitions.Upgrades.CODEC).synced(TnfDefinitions.Upgrades.CODEC, true).build();
    public static final DataMapType<Block, TnfDefinitions.Base> BASES = DataMapType.builder(
        TheNextForge.of("bases"), Registries.BLOCK, TnfDefinitions.Base.CODEC).synced(TnfDefinitions.Base.CODEC, true).build();
    public static final DataMapType<Item, ForgingMaterial> HAMMERS = DataMapType.builder(
        TheNextForge.of("hammers"), Registries.ITEM, TnfDefinitions.MATERIAL).synced(TnfDefinitions.MATERIAL, true).build();
    public static final DataMapType<Item, TnfDefinitions.Embedding> EMBEDDINGS = DataMapType.builder(
        TheNextForge.of("embeddings"), Registries.ITEM, TnfDefinitions.Embedding.CODEC).synced(TnfDefinitions.Embedding.CODEC, true).build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(TARGETS); event.register(UPGRADES); event.register(BASES);
        event.register(HAMMERS); event.register(EMBEDDINGS);
    }
}
