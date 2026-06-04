package net.anderzz.worldtransformations.compat;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.anderzz.worldtransformations.recipe.WeightedOutput;
import net.anderzz.worldtransformations.recipe.WorldTransformationRecipe;

import java.util.List;

public class WorldTransformRecipeCategory implements IRecipeCategory<WorldTransformationRecipe> {

    private final IDrawable background;
    private final IDrawable icon;
    IGuiHelper helper;
    private final Component localizedName;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("worldtransform", "textures/gui/slot.png");

    public WorldTransformRecipeCategory(IGuiHelper helper) {
        this.helper = helper;
        // Build an empty grey container background frame (Width: 160, Height: 60)
        this.background = helper.createBlankDrawable(160, 60);
        // Use a Cobblestone block icon to represent this custom category tab inside the JEI index panel
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.GRASS_BLOCK));
        this.localizedName = Component.literal("In-World Transformation");
    }

    @Override
    public RecipeType<WorldTransformationRecipe> getRecipeType() {
        return JeiModPlugin.TRANSFORM_JEI_TYPE;
    }

    @Override
    public Component getTitle() {
        return this.localizedName;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    // 2. Specify the structural height of your JEI category layout frame
    @Override
    public int getHeight() {
        return 150;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WorldTransformationRecipe recipe, IFocusGroup focuses) {
        // Slot 1: Input Item/Tag (Left Side)
        builder.addSlot(RecipeIngredientRole.INPUT, (getWidth() / 2) - 9, 10)
                .addIngredients(recipe.item());

        // Slot 2: Fluid Condition (Middle Top - Rendered via Bucket)
        if (recipe.fluid().isPresent()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, (getWidth() / 2) - 9, 50)
                    .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.fluid().get().getBucket()))
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                        tooltip.add(Component.literal("§eItem must be placed in liquid"));
                    });

        }

        if (recipe.isFire()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, (getWidth() / 2) - 9, 50)
                    .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CAMPFIRE))
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                        tooltip.add(Component.literal("§eItem must be thrown into fire"));
                    });
        }

        // Slot 3: Below Block Condition (Middle Bottom)
        if (recipe.block().isPresent()) {
            int startX = 0;

            if (recipe.fluid().isPresent()) {
                startX = 20;
            }

            builder.addSlot(RecipeIngredientRole.CATALYST, ((getWidth() / 2) - 9) + startX, 50)
                    .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.block().get().asItem()))
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                        tooltip.add(Component.literal("§eItem must be placed with this block below"));
                    });
        }

        // Output Processing System (Right Side)
        // Scenario A: Standard Singular Output 'result' key (Always 100% chance)
        if (recipe.result().isPresent()) {
            int startY = 50;

            if (recipe.fluid().isPresent() || recipe.block().isPresent() || recipe.isFire()) {
                startY = 70;
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, (getWidth() / 2) - 9, startY)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.result().get())
                    // FIX: Replaced with addRichTooltipCallback
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                        tooltip.add(Component.literal("§a100% Chance"));
                    });
        }

        // Scenario B: Handle Flat 'results' list arrays (Dynamic Chance)
        if (recipe.results().isPresent()) {
            List<WeightedOutput> outputs = recipe.results().get();
            int startX = 10;
            int startY = 70;

            for (int i = 0; i < outputs.size(); i++) {
                WeightedOutput outputConfig = outputs.get(i);
                double itemChance = outputConfig.chance();

                // Arrange them in rows of 10 items max across the bottom
                int col = i % 10;
                int row = i / 10;

                int slotX = startX + (col * 20);
                int slotY = startY + (row * 18);

                builder.addSlot(RecipeIngredientRole.OUTPUT, slotX, slotY)
                        .addIngredient(VanillaTypes.ITEM_STACK, outputConfig.itemStack())
                        .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                            String formatString = String.format("Drop Chance: %.1f%%", itemChance);
                            tooltip.add(Component.literal("§6" + formatString));
                        })
                        .setBackground(this.helper.getSlotDrawable(), -1, -1);
            }
        }
    }

    @Override
    public void draw(WorldTransformationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 1. Fetch the font renderer instance from Minecraft's main layout layer
        Minecraft minecraft = Minecraft.getInstance();

        // 2. Safely parse the transformation timer value in seconds
        int seconds = recipe.transformTime().orElse(0);
        String timeString = seconds + "s";

        // 3. Render the progress strings
        guiGraphics.drawString(minecraft.font, timeString, 82, 35, 0x404040, false);
        guiGraphics.drawString(minecraft.font, "↓", 75, 35, 0x8B8B8B, false);
    }
}