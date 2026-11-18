package dev.anvilcraft.thenextforge.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 「嵌入材料」JEI 分类的工作方块索引。
 *
 * <p>只列**真的能嵌入**的对象，取自 {@code ForgingMaterial} 的判定范围：
 * 原版铁砧 / 砂轮（基础档）＋ 铁砧工艺本体的四档铁砧、四档砂轮与五把铁砧锤。
 *
 * <p>按 id 查找而非直接引用类：AnvilCraft 的某些变体可能不存在于旧版本，
 * 缺失时静默跳过，不会因此启动失败。
 */
public final class EmbedCatalysts {
    /** 工作方块 / 工具的注册 id，顺序即 JEI 里的展示顺序。 */
    private static final List<String> IDS = List.of(
        "minecraft:anvil",
        "minecraft:grindstone",
        "anvilcraft:royal_anvil",
        "anvilcraft:frost_anvil",
        "anvilcraft:ember_anvil",
        "anvilcraft:transcendence_anvil",
        "anvilcraft:royal_grindstone",
        "anvilcraft:frost_grindstone",
        "anvilcraft:ember_grindstone",
        "anvilcraft:transcendence_grindstone",
        "anvilcraft:anvil_hammer",
        "anvilcraft:royal_anvil_hammer",
        "anvilcraft:frost_anvil_hammer",
        "anvilcraft:ember_anvil_hammer",
        "anvilcraft:transcendence_anvil_hammer"
    );

    private EmbedCatalysts() {
    }

    /**
     * 已注册的催化剂物品栈（不存在的 id 被跳过）。
     *
     * @return 物品栈列表
     */
    public static List<ItemStack> stacks() {
        List<ItemStack> out = new java.util.ArrayList<>(IDS.size());
        for (String id : IDS) {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                .ifPresent(item -> out.add(new ItemStack(item)));
        }
        return out;
    }
}
