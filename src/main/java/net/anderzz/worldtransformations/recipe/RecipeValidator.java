package net.anderzz.worldtransformations.recipe;

import net.anderzz.worldtransformations.WorldTransformations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecipeValidator {
    public static boolean validateRecipe(
            Ingredient inputItem,
            @Nullable ResourceLocation replaceFluidId,
            @Nullable ItemStack result,
            @Nullable List<WeightedOutput> results
    ) {
        if (inputItem == null || inputItem.isEmpty()) {
            WorldTransformations.error("BROKEN RECIPE DETECTED");
            WorldTransformations.error(" -> Reason: The input 'item' ingredient is empty or could not be found in the game registries.");
            return false;
        }

        boolean hasReplaceFluid = replaceFluidId != null;
        boolean hasResult = result != null && !result.isEmpty();
        boolean hasResults = results != null && !results.isEmpty();

        if (!hasReplaceFluid && !hasResult && !hasResults) {
            WorldTransformations.error("BROKEN RECIPE DETECTED");
            WorldTransformations.error(" -> Reason: Recipe does nothing! It must define at least one outcome: 'replaceFluid', 'result', or 'results'.");
            return false;
        }

        if (replaceFluidId != null) {
            if (!BuiltInRegistries.BLOCK.containsKey(replaceFluidId)) {
                WorldTransformations.error("BROKEN RECIPE DETECTED");
                WorldTransformations.error(" -> Reason: The block/fluid ID '{}' defined in 'replaceFluid' does not exist in the game registries!", replaceFluidId);
                return false;
            }
        }

        if (result != null && !result.isEmpty()) {
            if (result.is(Items.AIR)) {
                WorldTransformations.error("BROKEN RECIPE DETECTED");
                WorldTransformations.error(" -> Reason: The single 'result' item target is invalid or not found in the game registries.");
                return false;
            }
        }

        if (results != null && !results.isEmpty()) {
            for (int i = 0; i < results.size(); i++) {
                WeightedOutput output = results.get(i);
                ItemStack stack = output.itemStack();

                if (stack == null || stack.isEmpty() || stack.is(Items.AIR)) {
                    WorldTransformations.error("BROKEN RECIPE DETECTED");
                    WorldTransformations.error(" -> Reason: Multi-drop 'results' array at index [{}] contains an unrecognised item ID", i);
                    return false;
                }

                if (output.min() > output.max()) {
                    WorldTransformations.warn("RECIPE WARNING:");
                    WorldTransformations.warn(" -> Output index [{}] ({}) has min ({}) greater than max ({}). Inverting bounds safely.",
                            i, stack.getItem(), output.min(), output.max());
                }
            }
        }

        return true;
    }
}