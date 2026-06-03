package net.anderzz.worldtransformations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.anderzz.worldtransformations.recipe.ModRecipes;
import net.anderzz.worldtransformations.recipe.WeightedOutput;
import net.anderzz.worldtransformations.recipe.WorldTransformRecipe;

import java.util.List;

@EventBusSubscriber(modid = "worldtransformations")
public class WorldTransformationHandler {

    private static final String TIME_TAG = "transformTime";

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        Level level = itemEntity.getCommandSenderWorld();
        // Always execute modifications exclusively on the server thread
        if (level.isClientSide()) return;

        ItemStack stack = itemEntity.getItem();
        BlockPos pos = itemEntity.blockPosition();
        Fluid fluidAtItem = level.getFluidState(pos).getType();
        Block blockBelowItem = level.getBlockState(pos.below()).getBlock();

        // Safely pull all registered custom data blueprints from the game engine
        List<RecipeHolder<WorldTransformRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get());

        for (RecipeHolder<WorldTransformRecipe> holder : recipes) {
            WorldTransformRecipe recipe = holder.value();

            // 1. Verify Item Rule Match
            if (recipe.item().isEmpty() || !recipe.item().test(stack)) continue;

            if (recipe.fluid().isPresent() && !fluidAtItem.isSame(recipe.fluid().get())) continue;
            if (recipe.block().isPresent() && blockBelowItem != recipe.block().get()) continue;

            // FIX: Convert the JSON transformTime value (Seconds) directly into game ticks
            // Default to 0 seconds if the tag is completely missing from the file
            int requiredTimeInSeconds = recipe.transformTime().orElse(0);
            int requiredTimeInTicks = requiredTimeInSeconds * 20;

            // If time is set to 0, execute the swap instantly
            if (requiredTimeInTicks <= 0) {
                executeTransformation(itemEntity, level, pos, stack, recipe);
                return;
            }

            // 4. Tick Timer Management
            CompoundTag nbt = itemEntity.getPersistentData();
            int currentTicks = nbt.getInt(TIME_TAG) + 1;
            nbt.putInt(TIME_TAG, currentTicks);
            
            spawnTickingParticles(level, itemEntity, fluidAtItem);

            // Execute transformation once the item accumulates enough ticks
            if (currentTicks >= requiredTimeInTicks) {
                executeTransformation(itemEntity, level, pos, stack, recipe);
                nbt.remove(TIME_TAG);
            }
            return; // Exit loop early once a valid recipe match begins processing
        }
    }

    private static void spawnTickingParticles(Level level, ItemEntity itemEntity, Fluid fluidAtItem) {
        if (!(level instanceof ServerLevel serverlevel)) return;

        net.minecraft.util.RandomSource random = level.getRandom();

        if (random.nextInt(3) != 0) return;
        
        double offsetX = itemEntity.getX() + (random.nextDouble() - 0.5) * 0.4;
        double offsetY = itemEntity.getY() + random.nextDouble() * 0.4;
        double offsetZ = itemEntity.getZ() + (random.nextDouble() - 0.5) * 0.4;
        
        if (!fluidAtItem.isSame(Fluids.EMPTY)) {
            serverlevel.sendParticles(ParticleTypes.BUBBLE,
                    offsetX, offsetY, offsetZ,
                    1,
                    0, 0.05, 0.01,
                    0.01);
        } else {
            serverlevel.sendParticles(ParticleTypes.SMOKE,
                    offsetX, offsetY, offsetZ,
                    1,
                    0, 0.05, 0.01,
                    0.01);
        }
    }

    private static void executeTransformation(ItemEntity itemEntity, Level level, BlockPos pos, ItemStack originalStack, WorldTransformRecipe recipe) {
        int count = originalStack.getCount();
        RandomSource random = level.getRandom();

        // Process Single 'result' field
        if (recipe.result().isPresent()) {
            ItemStack output = recipe.result().get().copy();
            output.setCount(output.getCount() * count);
            level.addFreshEntity(new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output));
        }

        // Process Weighted Multi-Drop Array
        if (recipe.results().isPresent()) {
            for (WeightedOutput outputConfig : recipe.results().get()) {
                int successfulDrops = 0;
                for (int i = 0; i < count; i++) {
                    if (random.nextDouble() * 100.0 <= outputConfig.chance()) {
                        successfulDrops += outputConfig.itemStack().getCount();
                    }
                }
                if (successfulDrops > 0) {
                    ItemStack output = outputConfig.itemStack().copy();
                    output.setCount(successfulDrops);
                    level.addFreshEntity(new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output));
                }
            }
        }

        // Fluid replacement handling
        if (recipe.replaceFluid().isPresent()) {
            BlockState blockAtPos = level.getBlockState(pos);
            if (!blockAtPos.getFluidState().isEmpty()) {
                level.setBlockAndUpdate(pos, recipe.replaceFluid().get().defaultBlockState());
            }
        }

        level.playSound(
                null,
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                SoundEvents.ITEM_PICKUP,
                SoundSource.BLOCKS,
                1.0F,
                (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
        );

        itemEntity.discard(); // Safely eliminate old parent item from world grid
    }
}