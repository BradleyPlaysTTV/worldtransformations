package net.anderzz.worldtransformations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
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
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.anderzz.worldtransformations.recipe.ModRecipes;
import net.anderzz.worldtransformations.recipe.WeightedOutput;
import net.anderzz.worldtransformations.recipe.WorldTransformationRecipe;

import java.util.List;

@EventBusSubscriber(modid = WorldTransformations.MOD_ID)
public class WorldTransformationHandler {

    @SubscribeEvent
    public static void onItemTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        if (itemEntity.tickCount % Config.TICK_PROCESSING_RATE.get() != 0) return;

        if (!Config.ALLOW_CONTINUOUS_RECIPES.get() && itemEntity.getPersistentData().getBoolean("is_recipe_output")) return;

        Level level = itemEntity.getCommandSenderWorld();
        if (level.isClientSide()) return;

        if (!Config.ALLOW_FAKE_PLAYER.get() && itemEntity.getOwner() != null) return;

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

        boolean isCurrentBlockFire = blockAtItem == Blocks.FIRE || blockAtItem == Blocks.SOUL_FIRE;
        boolean isCurrentFluidFireproofNeeded = BuiltInRegistries.FLUID.wrapAsHolder(fluidAtItem).is(ModTags.FIREPROOFING);
        if (isCurrentBlockFire || isCurrentFluidFireproofNeeded) {
            itemEntity.setInvulnerable(true);
        }

        List<RecipeHolder<WorldTransformationRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get());

        for (RecipeHolder<WorldTransformationRecipe> holder : recipes) {
            WorldTransformationRecipe recipe = holder.value();

            if (recipe.item().isEmpty() || !recipe.item().test(stack)) continue;

            if (recipe.isFire()) {
                boolean insideFire = blockAtItem == Blocks.FIRE || blockAtItem == Blocks.SOUL_FIRE;
                if (!insideFire) continue;
            }

            if (recipe.fluid().isPresent()) {
                if (!fluidAtItem.isSame(recipe.fluid().get())) continue;
            } else {
                if (!recipe.isFire() && !fluidAtItem.isSame(Fluids.EMPTY)) continue;
            }

            if (recipe.block().isPresent() && blockBelowItem != recipe.block().get()) continue;

            CompoundTag nbt = itemEntity.getPersistentData();

            int requiredTimeInTicks = 0;
            if (recipe.transformTime().isPresent()) {
                int requiredTimeInSeconds = recipe.transformTime().orElse(0);
                requiredTimeInTicks = requiredTimeInSeconds * 20;
            }

            if (requiredTimeInTicks <= 0) {
                executeTransformation(itemEntity, level, pos, stack, recipe);
                break;
            }

            int currentTicks = nbt.getInt("transformTime") + Config.TICK_PROCESSING_RATE.get();
            nbt.putInt("transformTime", currentTicks);

            spawnTickingParticles(level, itemEntity, fluidAtItem, recipe.isFire());

            if (currentTicks >= requiredTimeInTicks) {
                executeTransformation(itemEntity, level, pos, stack, recipe);
                nbt.remove("transformTime");
            }
            break;
        }
    }

    private static void spawnTickingParticles(Level level, ItemEntity itemEntity, Fluid fluidAtItem, boolean isFireRecipe) {
        if (!Config.ENABLE_PARTICLES.get()) return;

        if (!(level instanceof ServerLevel serverlevel)) return;

        RandomSource random = level.getRandom();
        if (random.nextInt(3) != 0) return;

        double offsetX = itemEntity.getX() + (random.nextDouble() - 0.5) * 0.4;
        double offsetY = itemEntity.getY() + random.nextDouble() * 0.4;
        double offsetZ = itemEntity.getZ() + (random.nextDouble() - 0.5) * 0.4;

        if (isFireRecipe) {
            serverlevel.sendParticles(ParticleTypes.FLAME, offsetX, offsetY, offsetZ, 1, 0, 0.02, 0, 0.01);
        } else if (!fluidAtItem.isSame(Fluids.EMPTY)) {
            serverlevel.sendParticles(ParticleTypes.BUBBLE, offsetX, offsetY, offsetZ, 1, 0, 0.05, 0.01, 0.01);
        } else {
            serverlevel.sendParticles(ParticleTypes.SMOKE, offsetX, offsetY, offsetZ, 1, 0, 0.05, 0.01, 0.01);
        }
    }

    private static void executeTransformation(ItemEntity itemEntity, Level level, BlockPos pos, ItemStack originalStack, WorldTransformationRecipe recipe) {
        int count = originalStack.getCount();
        RandomSource random = level.getRandom();

        if (recipe.result().isPresent()) {
            ItemStack output = recipe.result().get().copy();
            output.setCount(output.getCount() * count);
            ItemEntity singleOutputEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output);

            if (isFireProofNeeded(recipe, level)) singleOutputEntity.setInvulnerable(true);

            singleOutputEntity.getPersistentData().putBoolean("is_recipe_output", true);

            level.addFreshEntity(singleOutputEntity);
        }

        if (recipe.results().isPresent()) {
            for (WeightedOutput outputConfig : recipe.results().get()) {
                int successfulDrops = 0;
                for (int i = 0; i < count; i++) {
                    if (random.nextDouble() * 100.0 <= outputConfig.chance()) {
                        int minCount = outputConfig.min();
                        int maxCount = outputConfig.max();
                        if (maxCount < minCount) maxCount = minCount;

                        int rolledAmount = minCount + random.nextInt((maxCount - minCount) + 1);
                        successfulDrops += rolledAmount;
                    }
                }
                if (successfulDrops > 0) {
                    ItemStack output = outputConfig.itemStack().copy();
                    output.setCount(successfulDrops);
                    ItemEntity multiOutputEntity = new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), output);

                    if (isFireProofNeeded(recipe, level)) multiOutputEntity.setInvulnerable(true);

                    multiOutputEntity.getPersistentData().putBoolean("is_recipe_output", true);

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
        if (isFireProofNeeded(recipe, level)) {
            completionSound = SoundEvents.FIRE_EXTINGUISH;
        } else if (recipe.fluid().isPresent() && recipe.fluid().get().isSame(Fluids.WATER)) {
            completionSound = SoundEvents.FISHING_BOBBER_SPLASH;
        }

        if (Config.ENABLE_SOUNDS.get()) {
            level.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                    completionSound, SoundSource.BLOCKS, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        }

        itemEntity.discard();
    }

    @SubscribeEvent
    public static void onIncomingDamage(EntityInvulnerabilityCheckEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        DamageSource source = event.getSource();

        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.CAMPFIRE) || source.is(DamageTypes.LAVA)) {
            Level level = itemEntity.getCommandSenderWorld();
            if (level.isClientSide()) return;

            if (itemEntity.getOwner() == null) return;

            ItemStack stack = itemEntity.getItem();

            List<RecipeHolder<WorldTransformationRecipe>> recipes = level.getRecipeManager()
                    .getAllRecipesFor(ModRecipes.WORLD_TRANSFORM_TYPE.get());

            for (RecipeHolder<WorldTransformationRecipe> holder : recipes) {
                WorldTransformationRecipe recipe = holder.value();

                if (!recipe.item().isEmpty() && recipe.item().test(stack)) {
                    if (isFireProofNeeded(recipe, level)) {
                        event.setInvulnerable(true);
                        return;
                    }
                }
            }
        }
    }

    public static boolean isFireProofNeeded(WorldTransformationRecipe recipe, Level level) {
        return recipe.isFire() || (recipe.fluid().isPresent() &&
                level.registryAccess().lookupOrThrow(Registries.FLUID)
                        .get(ResourceKey.create(Registries.FLUID,
                                BuiltInRegistries.FLUID.getKey(recipe.fluid().get())))
                        .map(fluidHolder -> fluidHolder.is(ModTags.FIREPROOFING)).orElse(false));
    }
}