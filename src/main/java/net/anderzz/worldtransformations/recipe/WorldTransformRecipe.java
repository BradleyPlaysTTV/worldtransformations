package net.anderzz.worldtransformations.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Optional;

public record WorldTransformRecipe(
        Ingredient item,
        Optional<Integer> transformTime,
        Optional<Fluid> fluid,
        Optional<Block> block,
        Optional<ItemStack> result,
        Optional<List<WeightedOutput>> results,
        Optional<Block> replaceFluid
) implements Recipe<RecipeInput> {

    @Override
    public boolean matches(RecipeInput input, Level level) {
        // Vanilla internal logic requires implementing Recipe, but matching will happen manually inside our Item Entity loop
        return true;
    }

    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (result.isPresent()) return result.get();
        if (results.isPresent() && !results.get().isEmpty()) return results.get().get(0).itemStack();
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WORLD_TRANSFORM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WORLD_TRANSFORM_TYPE.get();
    }

    // --- CODEC & NETWORK SERIALIZATION ---
    public static class Serializer implements RecipeSerializer<WorldTransformRecipe> {

        // FIX: Custom lenient input codec that accepts a plain string ("minecraft:cobblestone") OR an ingredient object
        private static final Codec<Ingredient> INPUT_CODEC = Codec.either(
                BuiltInRegistries.ITEM.byNameCodec(),
                Ingredient.CODEC
        ).xmap(
                either -> either.map(Ingredient::of, ingredient -> ingredient),
                ingredient -> com.mojang.datafixers.util.Either.right(ingredient)
        );

        private static final MapCodec<WorldTransformRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                INPUT_CODEC.fieldOf("item").forGetter(WorldTransformRecipe::item), // Uses our new flexible codec
                Codec.INT.optionalFieldOf("transformTime").forGetter(WorldTransformRecipe::transformTime),
                BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("fluid").forGetter(WorldTransformRecipe::fluid),
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("block").forGetter(WorldTransformRecipe::block),
                ItemStack.CODEC.optionalFieldOf("result").forGetter(WorldTransformRecipe::result),
                WeightedOutput.CODEC.listOf().optionalFieldOf("results").forGetter(WorldTransformRecipe::results),
                BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("replaceFluid").forGetter(WorldTransformRecipe::replaceFluid)
        ).apply(instance, WorldTransformRecipe::new));

        @Override
        public MapCodec<WorldTransformRecipe> codec() { return CODEC; }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WorldTransformRecipe> streamCodec() {
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
                    },
                    buf -> new WorldTransformRecipe(
                            Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.FLUID)).decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)).decode(buf),
                            ByteBufCodecs.optional(ItemStack.STREAM_CODEC).decode(buf),
                            ByteBufCodecs.optional(
                                    ByteBufCodecs.collection(java.util.ArrayList::new, WeightedOutput.STREAM_CODEC)
                                            .map(list -> (java.util.List<WeightedOutput>) list, java.util.ArrayList::new)
                            ).decode(buf),
                            ByteBufCodecs.optional(ByteBufCodecs.registry(net.minecraft.core.registries.Registries.BLOCK)).decode(buf)
                    )
            );
        }
    }
}