package dev.anvilcraft.thenextforge.init;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 本模组的附着数据。
 *
 * <p>只保存「琥珀标记」这一层辨识状态，本体的怨恨值仍然由 AnvilCraft 的 ResentmentUtil 维护。
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TheNextForge.MOD_ID);

    /**
     * 命中时打上的琥珀标记，值为该武器上琥珀嵌入的数量，0 表示无标记。
     * 重复命中只保留较大的值，不累加。
     */
    public static final Supplier<AttachmentType<Integer>> AMBER_MARK = REGISTER.register(
        "amber_mark",
        () -> AttachmentType.<Integer>builder(() -> 0)
            .serialize(com.mojang.serialization.Codec.INT.fieldOf("value"))
            .build()
    );

    /** 负物质粒的易伤到期 tick，0 表示无。 */
    public static final Supplier<AttachmentType<Long>> VULNERABLE_UNTIL = REGISTER.register(
        "vulnerable_until",
        () -> AttachmentType.<Long>builder(() -> 0L)
            .serialize(com.mojang.serialization.Codec.LONG.fieldOf("value"))
            .build()
    );

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
