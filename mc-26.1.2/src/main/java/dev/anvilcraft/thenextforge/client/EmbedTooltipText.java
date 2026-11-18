package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.api.ManagedEmbeddings;
import dev.anvilcraft.thenextforge.forging.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashSet;

/** 客户端按当前资源语言生成描述；旧服务端中文合同与存档中的材料 ID 保持稳定。 */
public final class EmbedTooltipText {
    private EmbedTooltipText() {}
    private static String key(String id) { return "thenextforge.embed." + id.replace("#", "").replace(':', '.'); }
    public static Component property(String id, EmbedMaterials.Definition definition) {
        return Component.translatableWithFallback(key(id) + ".property", EmbedMaterials.propertyOf(definition.label()));
    }
    public static String bonus(ItemStack stack, String id, int level) {
        String applied = ManagedEmbeddings.describeAppliedLocalized(stack, id);
        if (!applied.isBlank()) return applied;
        var texts = new LinkedHashSet<String>();
        for (var role : TargetKind.rolesOf(stack)) {
            String fallback = EmbedMaterials.bonusText(id, level, role);
            if (fallback.isBlank()) continue;
            texts.add(Component.translatableWithFallback(key(id) + "." + role.name().toLowerCase(java.util.Locale.ROOT), fallback).getString());
        }
        String joined = String.join(Component.translatable("thenextforge.tooltip.separator").getString(), texts);
        var definition = EmbedMaterials.byId(id);
        return definition != null && definition.unique() && !joined.isBlank()
            ? Component.translatable("thenextforge.tooltip.unique", joined).getString() : joined;
    }
}
