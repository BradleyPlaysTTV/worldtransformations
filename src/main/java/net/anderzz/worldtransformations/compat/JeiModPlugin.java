package net.anderzz.worldtransformations.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.anderzz.worldtransformations.WorldTransformations;
import net.anderzz.worldtransformations.recipe.ModRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.anderzz.worldtransformations.recipe.WorldTransformationRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class JeiModPlugin implements IModPlugin {

    public static final RecipeType<WorldTransformationRecipe> TRANSFORM_JEI_TYPE =
            RecipeType.create(WorldTransformations.MOD_ID, "world_transform", WorldTransformationRecipe.class);

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(WorldTransformations.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new WorldTransformRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            RecipeManager recipeManager = minecraft.level.getRecipeManager();

            List<WorldTransformationRecipe> recipes = recipeManager.getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get())
                    .stream()
                    .map(RecipeHolder::value)
                    .toList();

            registration.addRecipes(TRANSFORM_JEI_TYPE, recipes);
        }
    }
}