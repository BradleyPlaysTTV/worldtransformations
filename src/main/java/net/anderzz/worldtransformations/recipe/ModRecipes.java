package net.anderzz.worldtransformations.recipe;

import net.anderzz.worldtransformations.WorldTransformations;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, WorldTransformations.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, WorldTransformations.MOD_ID);

    public static final Supplier<RecipeSerializer<WorldTransformRecipe>> WORLD_TRANSFORM_SERIALIZER =
            SERIALIZERS.register("world_transform", net.anderzz.worldtransformations.recipe.WorldTransformRecipe.Serializer::new);

    public static final Supplier<RecipeType<WorldTransformRecipe>> WORLD_TRANSFORM_TYPE =
            TYPES.register("world_transform", () -> new RecipeType<>() {
                @Override
                public String toString() { return "world_transform"; }
            });

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}