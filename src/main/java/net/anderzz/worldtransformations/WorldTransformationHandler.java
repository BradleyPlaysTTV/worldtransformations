package net.anderzz.worldtransformations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.anderzz.worldtransformations.recipe.ModRecipes;
import net.anderzz.worldtransformations.recipe.WeightedOutput;
import net.anderzz.worldtransformations.recipe.WorldTransformationRecipe;

import java.util.List;

@EventBusSubscriber(modid = WorldTransformations.MOD_ID)
public class WorldTransformationHandler {

    private static final String TIME_TAG = "transformTime";

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        Level level = itemEntity.getCommandSenderWorld();
        if (level.isClientSide()) return;

        ItemStack stack = itemEntity.getItem();
        BlockPos pos = itemEntity.blockPosition();
        Fluid fluidAtItem = level.getFluidState(pos).getType();
        Block blockBelowItem = level.getBlockState(pos.below()).getBlock();
        Block blockAtItem = level.getBlockState(pos).getBlock();

        if (blockAtItem != Blocks.FIRE && blockAtItem != Blocks.SOUL_FIRE) {
            Block blockAboveItem = level.getBlockState(pos.above()).getBlock();

            if (blockAboveItem == Blocks.FIRE || blockAboveItem == Blocks.SOUL_FIRE) {
                blockAtItem = blockAboveItem;
            }
        }

        // Handle input item fireproofing safely for hot environments
        boolean isCurrentBlockFire = blockAtItem == Blocks.FIRE || blockAtItem == Blocks.SOUL_FIRE;
        boolean isCurrentFluidLava = fluidAtItem.isSame(Fluids.LAVA);
        if (isCurrentBlockFire || isCurrentFluidLava) {
            itemEntity.setInvulnerable(true);
        }

        List<RecipeHolder<WorldTransformationRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get());

        for (RecipeHolder<WorldTransformationRecipe> holder : recipes) {
            WorldTransformationRecipe recipe = holder.value();

            // 1. Verify Item Rule Match
            if (recipe.item().isEmpty() || !recipe.item().test(stack)) continue;

            // 2. Strict Fire Check: If it's a fire recipe, you MUST be in fire.
            if (recipe.isFire()) {
                boolean insideFire = blockAtItem == Blocks.FIRE || blockAtItem == Blocks.SOUL_FIRE;
                if (!insideFire) continue;
            }

            // 3. Strict Fluid Check: If the recipe asks for a fluid, you MUST be in it.
            if (recipe.fluid().isPresent()) {
                if (!fluidAtItem.isSame(recipe.fluid().get())) continue;
            } else {
                // If it is NOT a fire recipe AND it has NO fluid requirement (Anywhere Recipe),
                // make sure it isn't accidentally drowning inside water or lava!
                if (!recipe.isFire() && !fluidAtItem.isSame(Fluids.EMPTY)) continue;
            }

            // 4. Verify the block underneath if defined
            if (recipe.block().isPresent() && blockBelowItem != recipe.block().get()) continue;

            // --- ALL ENVIRONMENT FILTERS PASSED SUCCESSFULLY ---
            // If the code reaches this line, the recipe is completely valid for this environment!

            // 5. Check up on timer NBT keys
            CompoundTag nbt = itemEntity.getPersistentData();

            int requiredTimeInTicks = 0;
            if (recipe.transformTime().isPresent()) {
                int requiredTimeInSeconds = recipe.transformTime().orElse(0);
                requiredTimeInTicks = requiredTimeInSeconds * 20;
            }

            // 6. Run the 0-second check now that we are 100% sure this environment is valid
            if (requiredTimeInTicks <= 0) {
                executeTransformation(itemEntity, level, pos, stack, recipe);
                return;
            }

            // 7. Tick Timer Management
            int currentTicks = nbt.getInt(TIME_TAG) + 1;
            nbt.putInt(TIME_TAG, currentTicks);

            spawnTickingParticles(level, itemEntity, fluidAtItem, recipe.isFire());

            if (currentTicks >= requiredTimeInTicks) {
                executeTransformation(itemEntity, level, pos, stack, recipe);
                nbt.remove(TIME_TAG);
            }
            return; // Clean exit out of the tick segment for this active item entity instance
        }
    }

    private static void spawnTickingParticles(Level level, ItemEntity itemEntity, Fluid fluidAtItem, boolean isFireRecipe) {
        if (!(level instanceof ServerLevel serverlevel)) return;

        net.minecraft.util.RandomSource random = level.getRandom();

        if (random.nextInt(3) != 0) return;

        double offsetX = itemEntity.getX() + (random.nextDouble() - 0.5) * 0.4;
        double offsetY = itemEntity.getY() + random.nextDouble() * 0.4;
        double offsetZ = itemEntity.getZ() + (random.nextDouble() - 0.5) * 0.4;

        if (isFireRecipe) {
            serverlevel.sendParticles(ParticleTypes.FLAME,
                    offsetX, offsetY, offsetZ,
                    1,
                    0, 0.02, 0,
                    0.01);
        } else if (!fluidAtItem.isSame(Fluids.EMPTY)) {
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

    private static void executeTransformation(ItemEntity itemEntity, Level level, BlockPos pos, ItemStack originalStack, WorldTransformationRecipe recipe) {
        int count = originalStack.getCount();
        RandomSource random = level.getRandom();

        boolean isFireProofNeeded = (recipe.fluid().isPresent() && recipe.fluid().get().isSame(Fluids.LAVA)) || recipe.isFire();

        // Process Single 'result' field
        if (recipe.result().isPresent()) {
            ItemStack output = recipe.result().get().copy();
            output.setCount(output.getCount() * count);
            ItemEntity singleOutputEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output);

            if (isFireProofNeeded) singleOutputEntity.setInvulnerable(true);
            level.addFreshEntity(singleOutputEntity);
        }

        // Process Weighted Multi-Drop Array
        if (recipe.results().isPresent()) {
            for (WeightedOutput outputConfig : recipe.results().get()) {
                int successfulDrops = 0;
                for (int i = 0; i < count; i++) {
                    if (random.nextDouble() * 100.0 <= outputConfig.chance()) {
                        int minCount = outputConfig.min();
                        int maxCount = outputConfig.max();

                        if (maxCount < minCount) {
                            maxCount = minCount;
                        }

                        int rolledAmount = minCount + random.nextInt((maxCount - minCount) + 1);

                        successfulDrops += rolledAmount;
                    }
                }
                if (successfulDrops > 0) {
                    ItemStack output = outputConfig.itemStack().copy();
                    output.setCount(successfulDrops);
                    ItemEntity multiOutputEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output);

                    if (isFireProofNeeded) multiOutputEntity.setInvulnerable(true);
                    level.addFreshEntity(multiOutputEntity);
                }
            }
        }

        if (recipe.replaceFluid().isPresent()) {
            BlockState blockAtPos = level.getBlockState(pos);
            if (!blockAtPos.getFluidState().isEmpty()) {
                Block targetBlock = recipe.replaceFluid().get();
                BlockState targetState = targetBlock.defaultBlockState();

                level.setBlockAndUpdate(pos, targetState);
            }
        }

        SoundEvent completionSound = SoundEvents.ITEM_PICKUP;
        if (isFireProofNeeded) {
            completionSound = SoundEvents.FIRE_EXTINGUISH;
        } else if (recipe.fluid().isPresent() && recipe.fluid().get().isSame(Fluids.WATER)) {
            completionSound = SoundEvents.FISHING_BOBBER_SPLASH;
        }

        level.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                completionSound, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);

        itemEntity.discard();
    }

    @SubscribeEvent
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        DamageSource source = event.getSource();

        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.CAMPFIRE) || source.is(DamageTypes.LAVA)) {
            Level level = itemEntity.getCommandSenderWorld();
            if (level.isClientSide()) return;

            ItemStack stack = itemEntity.getItem();

            List<RecipeHolder<WorldTransformationRecipe>> recipes = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get());

            for (RecipeHolder<WorldTransformationRecipe> holder : recipes) {
                WorldTransformationRecipe recipe = holder.value();

                if (!recipe.item().isEmpty() && recipe.item().test(stack)) {
                    boolean isLavaRecipe = recipe.fluid().isPresent() && recipe.fluid().get().isSame(Fluids.LAVA);

                    if (recipe.isFire() || isLavaRecipe) {
                        event.setInvulnerable(true);
                        return;
                    }
                }
            }
        }
    }
}