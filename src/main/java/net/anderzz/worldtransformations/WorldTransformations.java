package net.anderzz.worldtransformations;

import com.mojang.logging.LogUtils;
import net.anderzz.worldtransformations.recipe.ModRecipes;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(WorldTransformations.MOD_ID)
public class WorldTransformations {
    public static final String MOD_ID = "worldtransformations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public WorldTransformations(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModRecipes.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    public static void error(String message, Object... args) {
        String format = "[WorldTransformations] " + message;
        LOGGER.error(format, args);
    }

    public static void warn(String message, Object... args) {
        String format = "[WorldTransformations] " + message;
        LOGGER.warn(format, args);
    }

    public static void info(String message, Object... args) {
        String format = "[WorldTransformations] " + message;
        LOGGER.info(format, args);
    }
}
