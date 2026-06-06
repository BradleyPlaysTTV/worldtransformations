package net.anderzz.worldtransformations;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ALLOW_FAKE_PLAYER;
    public static final ModConfigSpec.BooleanValue ALLOW_CONTINUOUS_RECIPES;
    public static final ModConfigSpec.IntValue TICK_PROCESSING_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLES;
    public static final ModConfigSpec.BooleanValue ENABLE_SOUNDS;

    static {
        BUILDER.push("general");

        ALLOW_FAKE_PLAYER = BUILDER
                .comment("If true, then fake players can also transform items by throwing them.")
                .define("allowFakePlayer", true);

        ENABLE_PARTICLES = BUILDER
                .comment("Should items emit smoke, flame, or bubbles while ticking through their conversion timers?")
                .define("enableParticles", true);

        ENABLE_SOUNDS = BUILDER
                .comment("Should a sound effect play when an item successfully finishes its world transformation?")
                .define("enableSounds", true);
        BUILDER.pop();

        BUILDER.push("recipe");
        ALLOW_CONTINUOUS_RECIPES = BUILDER
                .comment("If true, then if another recipe is valid after the first recipe has finished. It will continue to process the next recipe.")
                .define("allowContinuousRecipes", false);

        TICK_PROCESSING_RATE = BUILDER
                .comment("How often (in ticks) items on the ground look for valid recipes. 1 = every single tick (heavy performance), 20 = once every second (highly optimized). Default: 4.")
                .defineInRange("tickProcessingRate", 4, 1, 100);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
