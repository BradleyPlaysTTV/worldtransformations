package net.anderzz.worldtransformations.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.anderzz.worldtransformations.WorldTransformations;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public record WorldTransformationRecipe(
        Ingredient item,
        Optional<Integer> transformTime,
        Optional<Fluid> fluid,
        Optional<Block> block,
        Optional<ItemStack> result,
        Optional<List<WeightedOutput>> results,
        Optional<Block> replaceFluid,
        boolean isFire
) implements Recipe<RecipeInput> {

    @Override
    public boolean matches(@NotNull RecipeInput input, @NotNull Level level) {
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput input, HolderLookup.@NotNull Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        if (result.isPresent()) return result.get();
        if (results.isPresent() && !results.get().isEmpty()) return results.get().getFirst().itemStack();
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.WORLD_TRANSFORM_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.WORLD_TRANSFORM_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<WorldTransformationRecipe> {

        private static final Codec<Ingredient> INPUT_CODEC = Codec.either(
                BuiltInRegistries.ITEM.byNameCodec(),
                Ingredient.CODEC
        ).xmap(
                either -> either.map(Ingredient::of, ingredient -> ingredient),
                Either::right
        );

        private static final MapCodec<WorldTransformationRecipe> BASE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                INPUT_CODEC.fieldOf("item").forGetter(WorldTransformationRecipe::item),
                Codec.INT.optionalFieldOf("transformTime").forGetter(WorldTransformationRecipe::transformTime),
                BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(WorldTransformationRecipe::fluid),
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(WorldTransformationRecipe::block),
                ItemStack.CODEC.optionalFieldOf("result").forGetter(WorldTransformationRecipe::result),
                WeightedOutput.CODEC.listOf().optionalFieldOf("results").forGetter(WorldTransformationRecipe::results),
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("replaceFluid").forGetter(WorldTransformationRecipe::replaceFluid),
                Codec.BOOL.optionalFieldOf("isFire", false).forGetter(WorldTransformationRecipe::isFire)
        ).apply(instance, WorldTransformationRecipe::new));

        private static final MapCodec<WorldTransformationRecipe> CODEC = BASE_CODEC.flatXmap(
                recipe -> {
                    ResourceLocation unboxedReplaceFluidId = recipe.replaceFluid()
                            .map(BuiltInRegistries.BLOCK::getKey)
                            .orElse(null);

                    boolean isValid = RecipeValidator.validateRecipe(
                            ResourceLocation.fromNamespaceAndPath("worldtransformations", "recipe_validation"),
                            recipe.item(),
                            unboxedReplaceFluidId,
                            recipe.result().orElse(null),
                            recipe.results().orElse(null)
                    );

                    if (!isValid) {
                        BASE_CODEC.codec().encodeStart(JsonOps.INSTANCE, recipe)
                                .resultOrPartial(err -> WorldTransformations.error("Failed to dump raw JSON content: " + err))
                                .ifPresent(jsonElement -> {
                                    // Spits out the raw formatted JSON block into your console log window
                                    WorldTransformations.error("Failing JSON Recipe: {}", jsonElement.toString());
                                });

                        return DataResult.error(() -> "WorldTransformation recipe structural validation gate failed! Skipping entry.");
                    }

                    return DataResult.success(recipe);
                },
                DataResult::success
        );

        @Override
        public @NotNull MapCodec<WorldTransformationRecipe> codec() { return CODEC; }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, WorldTransformationRecipe> streamCodec() {
            return StreamCodec.of(
                    (buf, recipe) -> {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.item());
                        ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).encode(buf, recipe.transformTime());
                        ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.FLUID)).encode(buf, recipe.fluid());
                        ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)).encode(buf, recipe.block());
                        ByteBufCodecs.optional(ItemStack.STREAM_CODEC).encode(buf, recipe.result());
                        ByteBufCodecs.optional(
                                ByteBufCodecs.collection(java.util.ArrayList::new, WeightedOutput.STREAM_CODEC)
                                        .map(list -> (java.util.List<WeightedOutput>) list, java.util.ArrayList::new)
                        ).encode(buf, recipe.results());
                        ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)).encode(buf, recipe.replaceFluid());
                        ByteBufCodecs.BOOL.encode(buf, recipe.isFire());
                    },
                    buf -> new WorldTransformationRecipe(
                            Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.FLUID)).decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)).decode(buf),
                            ByteBufCodecs.optional(ItemStack.STREAM_CODEC).decode(buf),
                            ByteBufCodecs.optional(
                                    ByteBufCodecs.collection(java.util.ArrayList::new, WeightedOutput.STREAM_CODEC)
                                            .map(list -> (java.util.List<WeightedOutput>) list, java.util.ArrayList::new)
                            ).decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)).decode(buf),
                            ByteBufCodecs.BOOL.decode(buf)
                    )
            );
        }
    }
}