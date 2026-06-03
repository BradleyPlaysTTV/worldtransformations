package net.anderzz.worldtransform.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;

public record WeightedOutput(ItemStack itemStack, double chance) {

    // FIX: Using BuiltInRegistries.ITEM.byNameCodec() targets a raw string in JSON ("id": "minecraft:diamond")
    // and .xmap automatically turns it into a ready-to-use ItemStack behind the scenes.
    public static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec()
                    .xmap(ItemStack::new, ItemStack::getItem)
                    .fieldOf("id")
                    .forGetter(WeightedOutput::itemStack),
            Codec.DOUBLE.fieldOf("chance").forGetter(WeightedOutput::chance)
    ).apply(instance, WeightedOutput::new));

    // Network packet stream codec for server-to-client recipe syncing
    public static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM).encode(buf, val.itemStack().getItem());
                buf.writeDouble(val.chance());
            },
            buf -> {
                Item item = ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM).decode(buf);
                return new WeightedOutput(new ItemStack(item), buf.readDouble());
            }
    );
}