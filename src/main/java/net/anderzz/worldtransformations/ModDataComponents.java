package net.anderzz.worldtransformations;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "worldtransformations");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IN_WORLD_ACTIVE =
            DATA_COMPONENT_TYPES.register("in_world_active", () ->
                    DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
