package net.anderzz.worldtransformations.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.anderzz.worldtransformations.WorldTransformations;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.anderzz.worldtransformations.recipe.WeightedOutput;
import net.anderzz.worldtransformations.recipe.WorldTransformationRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;

public class WorldTransformRecipeCategory implements IRecipeCategory<WorldTransformationRecipe> {

    private final IDrawable icon;
    IGuiHelper guiHelper;
    private final Component localizedName;

    public WorldTransformRecipeCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.localizedName = Component.literal("In-World Transformation");

        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(WorldTransformations.MOD_ID, "textures/gui/jei_icon.png");

        this.icon = new IDrawable() {
            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public int getHeight() {
                return 16;
            }

            @Override
            public void draw(@NotNull GuiGraphics guiGraphics, int xOffset, int yOffset) {
                guiGraphics.blit(texture, xOffset, yOffset, 16, 16, 0.0f, 0.0f, 32, 32, 32, 32);
            }
        };
    }

    @Override
    public @NotNull RecipeType<WorldTransformationRecipe> getRecipeType() {
        return JeiModPlugin.TRANSFORM_JEI_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return this.localizedName;
    }

    @Override
    public int getWidth() {
        return 166;
    }

    @Override
    public int getHeight() {
        return 126;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WorldTransformationRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, (getWidth() / 2) - 9, 10)
                .addIngredients(recipe.item());

        if (recipe.fluid().isPresent()) {

            final Component tooltipComponent;
            if (recipe.replaceFluid().isPresent()) {
                tooltipComponent = Component.literal("Will be consumed!")
                        .withStyle(ChatFormatting.RED);
            } else {
                tooltipComponent = Component.literal("Acts as a reusable catalyst")
                        .withStyle(ChatFormatting.YELLOW).withStyle(ChatFormatting.ITALIC);
            }

            builder.addSlot(RecipeIngredientRole.CATALYST, (getWidth() / 2) - 9, 50)
                    .addFluidStack(recipe.fluid().get(), 1000)
                    .setFluidRenderer(1000, false, 16, 16)
                    .addRichTooltipCallback(((recipeSlotView, tooltip) -> tooltip.add(tooltipComponent)));

            if (recipe.replaceFluid().isPresent()) {
                Block targetBlock = recipe.replaceFluid().get();

                if (targetBlock instanceof LiquidBlock) {
                    Fluid extractedFluid = targetBlock.defaultBlockState().getFluidState().getType();

                    builder.addSlot(RecipeIngredientRole.CATALYST, ((getWidth() / 2) - 9) + 20, 50)
                            .addFluidStack(extractedFluid, 1000)
                            .setFluidRenderer(1000, false, 16, 16)
                            .addRichTooltipCallback(((recipeSlotView, tooltip) -> tooltip.add(Component.literal("Fluid transformed into another fluid!")
                                    .withStyle(ChatFormatting.GREEN))));

                } else if (targetBlock == Blocks.AIR){
                    builder.addSlot(RecipeIngredientRole.CATALYST, ((getWidth() / 2) - 9) + 20, 50)
                            .addItemStack(new ItemStack(Blocks.BARRIER))
                            .addRichTooltipCallback(((recipeSlotView, tooltip) -> tooltip.add(Component.literal("Fluid will be destroyed!")
                                    .withStyle(ChatFormatting.RED))));
                } else {
                    builder.addSlot(RecipeIngredientRole.CATALYST, ((getWidth() / 2) - 9) + 20, 50)
                            .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.block().get().asItem()))
                            .addRichTooltipCallback(((recipeSlotView, tooltip) -> tooltip.add(Component.literal("Fluid transformed into a block!")
                                    .withStyle(ChatFormatting.GREEN))));
                }
            }
        }

        if (recipe.block().isPresent()) {
            int startX = 0;

            if (recipe.fluid().isPresent()) {
                startX = 20;
            }

            builder.addSlot(RecipeIngredientRole.CATALYST, ((getWidth() / 2) - 9) + startX, 50)
                    .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.block().get().asItem()));
        }

        if (recipe.result().isPresent()) {
            int startY = 50;

            if (recipe.fluid().isPresent() || recipe.block().isPresent() || recipe.isFire()) {
                startY = 70;
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, (getWidth() / 2) - 9, startY)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.result().get())
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(Component.literal("§a100% Chance")));
        }

        if (recipe.results().isPresent()) {
            List<WeightedOutput> outputs = recipe.results().get();
            int listIndex =0;
            int startX = 2;
            int startY = 70;

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 9; x++) {
                    int posX = (18 * x) + startX;
                    int posY = (18 * y) + startY;

                    var slotBuilder = builder.addSlot(RecipeIngredientRole.OUTPUT, posX, posY)
                            .setBackground(this.guiHelper.getSlotDrawable(), -1, -1);

                    if (listIndex < outputs.size()) {
                        WeightedOutput outputConfig = outputs.get(listIndex);
                        double itemChance = outputConfig.chance();

                        slotBuilder.addIngredient(VanillaTypes.ITEM_STACK, outputConfig.itemStack())
                                .addRichTooltipCallback(((recipeSlotView, tooltip) -> {
                                    DecimalFormat df = new DecimalFormat("#.#");
                                    String formattedChance = df.format(itemChance);
                                    String formatString = String.format("Drop Chance: %s%%", formattedChance);
                                    tooltip.add(Component.literal("§6" + formatString));

                                    int minCount = outputConfig.min();
                                    int maxCount = outputConfig.max();
                                    tooltip.add(Component.literal("§7Min: " + minCount));
                                    tooltip.add(Component.literal("§7Max: " + maxCount));
                                }));

                        listIndex++;
                    }
                }
            }
        }
    }

    @Override
    public void draw(WorldTransformationRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();

        int seconds = recipe.transformTime().orElse(0);
        String timeString = seconds + "s";

        guiGraphics.drawString(minecraft.font, timeString, 82, 35, 0x707070, false);
        guiGraphics.drawString(minecraft.font, "↓", 75, 35, 0x707070, false);

        guiGraphics.drawString(minecraft.font, "Throw Item:", 10, 14, 0x707070, false);

        String actionText;
        int offsetY = 0;

        if (recipe.block().isPresent() && !recipe.isFire() && recipe.fluid().isEmpty()) {
            actionText = "On Block:";
        } else if (!recipe.isFire() && recipe.fluid().isEmpty()) {
            actionText = "On Ground!";
            offsetY = -20;
        } else if (recipe.isFire()) {
            actionText = "Into Fire!";
        } else {
            actionText = "Into Fluid:";
        }

        guiGraphics.drawString(minecraft.font, actionText, 10, 54 + offsetY, 0x707070, false);

        if (recipe.isFire()) {
            ResourceLocation blocksAtlas = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png");

            net.minecraft.client.renderer.texture.TextureAtlasSprite fireSprite = minecraft.getTextureAtlas(blocksAtlas)
                    .apply(ResourceLocation.fromNamespaceAndPath("minecraft", "block/fire_0"));

            if (fireSprite != null) {
                guiGraphics.blit((getWidth() / 2) - 8, 48, 0, 16, 16, fireSprite);
            }
        }
    }
}