package dev.anvilcraft.thenextforge.forging;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 一件装备的锻造状态，跟随物品本身保存。
 *
 * @param anvilLevel        铁砧线等级
 * @param grindstoneLevel   砂轮线等级
 * @param anvilTokens       基底提供的必定成功次数（-1 未使用，0 已用完）
 * @param grindTokens       锤提供的必定成功次数
 * @param embeds            已嵌入材料的 id
 * @param catStreak         猫护符连续成功计数
 * @param xpSpent           该物品累计消耗的经验
 * @param protections       已消耗的保护来源（armadillo / emerald / topaz），"locked" 表示黄玉封印
 * @param nextHitProtected  犰狳提供的「下一次锤击免损毁」是否待消耗
 */
public record ForgingData(
    int anvilLevel,
    int grindstoneLevel,
    int anvilTokens,
    int grindTokens,
    List<String> embeds,
    int catStreak,
    int xpSpent,
    List<String> protections,
    boolean nextHitProtected
) {
    public static final ForgingData EMPTY = new ForgingData(0, 0, -1, -1, List.of(), 0, 0, List.of(), false);

    public static final Codec<ForgingData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("anvil_level", 0).forGetter(ForgingData::anvilLevel),
        Codec.INT.optionalFieldOf("grindstone_level", 0).forGetter(ForgingData::grindstoneLevel),
        Codec.INT.optionalFieldOf("anvil_tokens", -1).forGetter(ForgingData::anvilTokens),
        Codec.INT.optionalFieldOf("grind_tokens", -1).forGetter(ForgingData::grindTokens),
        Codec.STRING.listOf().optionalFieldOf("embeds", List.of()).forGetter(ForgingData::embeds),
        Codec.INT.optionalFieldOf("cat_streak", 0).forGetter(ForgingData::catStreak),
        Codec.INT.optionalFieldOf("xp_spent", 0).forGetter(ForgingData::xpSpent),
        Codec.STRING.listOf().optionalFieldOf("protections", List.of()).forGetter(ForgingData::protections),
        Codec.BOOL.optionalFieldOf("next_hit_protected", false).forGetter(ForgingData::nextHitProtected)
    ).apply(instance, ForgingData::new));

    public static final StreamCodec<ByteBuf, ForgingData> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> {
            buf.writeInt(data.anvilLevel());
            buf.writeInt(data.grindstoneLevel());
            buf.writeInt(data.anvilTokens());
            buf.writeInt(data.grindTokens());
            buf.writeInt(data.embeds().size());
            for (String id : data.embeds()) ByteBufCodecs.STRING_UTF8.encode(buf, id);
            buf.writeInt(data.catStreak());
            buf.writeInt(data.xpSpent());
            buf.writeInt(data.protections().size());
            for (String id : data.protections()) ByteBufCodecs.STRING_UTF8.encode(buf, id);
            buf.writeBoolean(data.nextHitProtected());
        },
        buf -> {
            int anvil = buf.readInt();
            int grind = buf.readInt();
            int anvilTokens = buf.readInt();
            int grindTokens = buf.readInt();
            int embedCount = buf.readInt();
            List<String> embeds = new ArrayList<>();
            for (int i = 0; i < embedCount; i++) embeds.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            int cat = buf.readInt();
            int xp = buf.readInt();
            int protectionCount = buf.readInt();
            List<String> protections = new ArrayList<>();
            for (int i = 0; i < protectionCount; i++) protections.add(ByteBufCodecs.STRING_UTF8.decode(buf));
            boolean nextProtected = buf.readBoolean();
            return new ForgingData(anvil, grind, anvilTokens, grindTokens, List.copyOf(embeds), cat, xp, List.copyOf(protections), nextProtected);
        }
    );

    private ForgingData copy(int anvil, int grind, int anvilTokens, int grindTokens, List<String> embeds,
                             int catStreak, int xpSpent, List<String> protections, boolean nextHitProtected) {
        return new ForgingData(anvil, grind, anvilTokens, grindTokens, embeds, catStreak, xpSpent, protections, nextHitProtected);
    }

    public int level(ForgingLine line) {
        return line == ForgingLine.ANVIL ? this.anvilLevel : this.grindstoneLevel;
    }

    public ForgingData withLevel(ForgingLine line, int value) {
        return line == ForgingLine.ANVIL
            ? copy(value, grindstoneLevel, anvilTokens, grindTokens, embeds, catStreak, xpSpent, protections, nextHitProtected)
            : copy(anvilLevel, value, anvilTokens, grindTokens, embeds, catStreak, xpSpent, protections, nextHitProtected);
    }

    public int tokens(ForgingLine line) {
        return line == ForgingLine.ANVIL ? this.anvilTokens : this.grindTokens;
    }

    public ForgingData withTokens(ForgingLine line, int value) {
        return line == ForgingLine.ANVIL
            ? copy(anvilLevel, grindstoneLevel, value, grindTokens, embeds, catStreak, xpSpent, protections, nextHitProtected)
            : copy(anvilLevel, grindstoneLevel, anvilTokens, value, embeds, catStreak, xpSpent, protections, nextHitProtected);
    }

    /** 用给定的嵌入列表替换（取出材料时用）。 */
    public ForgingData withEmbeds(List<String> embeds) {
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, List.copyOf(embeds),
            catStreak, xpSpent, protections, nextHitProtected);
    }

    public ForgingData withEmbed(String materialId) {
        List<String> next = new ArrayList<>(this.embeds);
        next.add(materialId);
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, List.copyOf(next), catStreak, xpSpent, protections, nextHitProtected);
    }

    public boolean hasEmbed(String materialId) {
        return this.embeds.contains(materialId);
    }

    public ForgingData withCatStreak(int value) {
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, embeds, value, xpSpent, protections, nextHitProtected);
    }

    public ForgingData withXpSpent(int value) {
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, embeds, catStreak, value, protections, nextHitProtected);
    }

    public boolean protectionUsed(String source) {
        return this.protections.contains(source);
    }

    public ForgingData withProtectionUsed(String source) {
        List<String> next = new ArrayList<>(this.protections);
        if (!next.contains(source)) next.add(source);
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, embeds, catStreak, xpSpent, List.copyOf(next), nextHitProtected);
    }

    public ForgingData withProtections(List<String> value) {
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, embeds, catStreak, xpSpent, List.copyOf(value), nextHitProtected);
    }

    public ForgingData withNextHitProtected(boolean value) {
        return copy(anvilLevel, grindstoneLevel, anvilTokens, grindTokens, embeds, catStreak, xpSpent, protections, value);
    }

    public boolean locked() {
        return this.protections.contains("locked");
    }

    public ForgingData withLocked() {
        return this.withProtectionUsed("locked");
    }
}
