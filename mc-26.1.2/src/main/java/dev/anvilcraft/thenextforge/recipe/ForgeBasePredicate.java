package dev.anvilcraft.thenextforge.recipe;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.thenextforge.api.TnfDefinitions;
import dev.anvilcraft.thenextforge.forging.ForgingLine;
import dev.anvilcraft.thenextforge.forging.ForgingMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** 在每次匹配时查当前基底映射，/reload 无需重建一份方块清单。 */
public record ForgeBasePredicate(ForgingLine line) implements IRecipePredicate<ForgeBasePredicate> {
    @Override public boolean test(InWorldRecipeContext context) {
        var state = context.getLevel().getBlockState(BlockPos.containing(context.getPos()).below(2));
        return line == ForgingLine.ANVIL ? ForgingMaterial.ofAnvilBlock(state) != null : ForgingMaterial.ofGrindstoneBlock(state) != null;
    }
    @Override public int priority() { return 0; }
    @Override public Type getType() { return ModRecipeOutcomes.FORGE_BASE.get(); }
    public static class Type implements IRecipePredicate.Type<ForgeBasePredicate> {
        private static final MapCodec<ForgeBasePredicate> CODEC = TnfDefinitions.LINE.fieldOf("line").xmap(ForgeBasePredicate::new, ForgeBasePredicate::line);
        private static final StreamCodec<RegistryFriendlyByteBuf, ForgeBasePredicate> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeEnum(value.line()), buffer -> new ForgeBasePredicate(buffer.readEnum(ForgingLine.class)));
        @Override public MapCodec<ForgeBasePredicate> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, ForgeBasePredicate> streamCodec() { return STREAM_CODEC; }
    }
}
