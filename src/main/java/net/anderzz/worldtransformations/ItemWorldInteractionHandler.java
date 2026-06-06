package net.anderzz.worldtransformations;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

@EventBusSubscriber(modid = WorldTransformations.MOD_ID)
public class ItemWorldInteractionHandler {

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity entity = event.getEntity();
        ItemStack stack = entity.getItem();
        stack.set(ModDataComponents.IN_WORLD_ACTIVE.get(), true);
        entity.setItem(stack);
        entity.hasImpulse = true;
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (Boolean.TRUE.equals(stack.get(ModDataComponents.IN_WORLD_ACTIVE.get()))) {
            stack.remove(ModDataComponents.IN_WORLD_ACTIVE.get());
            entity.setItem(stack);
        }
    }
}