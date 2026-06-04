package net.anderzz.worldtransformations.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.anderzz.worldtransformations.recipe.ModRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.anderzz.worldtransformations.recipe.WorldTransformationRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JeiModPlugin implements IModPlugin {

    // Define a unique JEI reference registration identifier for your transformation type
    public static final RecipeType<WorldTransformationRecipe> TRANSFORM_JEI_TYPE =
            RecipeType.create("worldtransform", "world_transform", WorldTransformationRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("worldtransform", "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // Register the graphical blueprint layout interface container
        registration.addRecipeCategories(new WorldTransformRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Fetch the game client's active level to securely extract loaded data-driven JSON objects
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            RecipeManager recipeManager = minecraft.level.getRecipeManager();

            // Map our RecipeHolder array configs out into a pure raw list for JEI to index
            List<WorldTransformationRecipe> recipes = recipeManager.getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get())
                    .stream()
                    .map(RecipeHolder::value)
                    .toList();

            registration.addRecipes(TRANSFORM_JEI_TYPE, recipes);
        }
    }
}