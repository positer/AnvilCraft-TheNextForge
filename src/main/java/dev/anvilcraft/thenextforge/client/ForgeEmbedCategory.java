package dev.anvilcraft.thenextforge.client;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.forging.TargetKind;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * JEI 里的「嵌入材料」统合页：所有材料集中在同一个分类下，用 JEI 的前后翻页浏览。
 *
 * <p>每条配方的布局是「材料槽 + 它允许嵌入的对象槽」，槽本身就是可点击的索引：
 * 点材料看该物品，点对象看该装备，材料与对象可以互相翻。
 */
public final class ForgeEmbedCategory implements IRecipeCategory<dev.anvilcraft.thenextforge.forging.EmbedPage> {
    /** JEI 分类 id。 */
    public static final RecipeType<dev.anvilcraft.thenextforge.forging.EmbedPage> TYPE =
        RecipeType.create(TheNextForge.MOD_ID, "embed_materials", dev.anvilcraft.thenextforge.forging.EmbedPage.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ForgeEmbedCategory(IJeiHelpers helpers) {
        this.background = helpers.getGuiHelper().createBlankDrawable(176, 116);
        this.icon = helpers.getGuiHelper().createDrawableItemStack(new ItemStack(Items.ANVIL));
    }

    @Override
    public RecipeType<dev.anvilcraft.thenextforge.forging.EmbedPage> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("嵌入材料");
    }

    @Override
    public int getWidth() {
        return 176;
    }

    @Override
    public int getHeight() {
        return 116;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    /** 文字区域：左侧留出材料槽，右侧留边；槽位固定在底部一行。 */
    private static final int TEXT_X = 26;
    private static final int TEXT_Y = 6;
    private static final int TEXT_WIDTH = 146;
    private static final int LINE_HEIGHT = 9;
    private static final int SLOT_Y = 96;

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, dev.anvilcraft.thenextforge.forging.EmbedPage entry, IFocusGroup focuses) {
        // 左上是材料本身；底部是它允许嵌入的对象（槽可点击，构成互相索引）
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).addItemStack(entry.material());
        int x = 26;
        for (TargetKind kind : entry.kinds()) {
            ItemStack sample = dev.anvilcraft.thenextforge.forging.EmbedPage.sampleFor(kind);
            if (sample.isEmpty()) continue;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, SLOT_Y).addItemStack(sample);
            x += 20;
        }
    }

    @Override
    public void draw(dev.anvilcraft.thenextforge.forging.EmbedPage entry, IRecipeSlotsView slots,
                     GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int y = TEXT_Y;
        int bottom = SLOT_Y - 2;
        for (Component line : entry.lines()) {
            // 自动换行：一行放不下就折行，折行后仍然只画到槽位上沿
            for (var wrapped : font.split(line, TEXT_WIDTH)) {
                if (y + LINE_HEIGHT > bottom) return;
                gui.drawString(font, wrapped, TEXT_X, y, 0x404040, false);
                y += LINE_HEIGHT;
            }
        }
    }
    /** 每个类别的示例物品（实现放在 EmbedPage，本类只做展示）。 */
    public static ItemStack sampleFor(TargetKind kind) {
        return dev.anvilcraft.thenextforge.forging.EmbedPage.sampleFor(kind);
    }
}
