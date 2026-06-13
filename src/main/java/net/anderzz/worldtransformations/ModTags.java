package net.anderzz.worldtransformations;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class ModTags {
    public static final TagKey<Fluid> FIREPROOFING = TagKey.create(
            Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(WorldTransformations.MOD_ID, "fireproofing")
    );
}
