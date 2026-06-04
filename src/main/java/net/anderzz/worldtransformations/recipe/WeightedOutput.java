package net.anderzz.worldtransformations.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;

public record WeightedOutput(ItemStack itemStack, double chance, int min, int max) {

    public static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec()
                    .xmap(ItemStack::new, ItemStack::getItem)
                    .fieldOf("id")
                    .forGetter(WeightedOutput::itemStack),
            Codec.DOUBLE.fieldOf("chance").forGetter(WeightedOutput::chance),
            Codec.INT.optionalFieldOf("min", 0).forGetter(WeightedOutput::min),
            Codec.INT.optionalFieldOf("max", 1).forGetter(WeightedOutput::max)
    ).apply(instance, WeightedOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.of(
            (buf, val) -> {
                ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM).encode(buf, val.itemStack().getItem());
                buf.writeDouble(val.chance());
                buf.writeInt(val.min());
                buf.writeInt(val.max());
            },
            buf -> {
                Item item = ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM).decode(buf);
                double chance = buf.readDouble();
                int min = buf.readInt();
                int max = buf.readInt();

                return new WeightedOutput(new ItemStack(item), chance, min, max);
            }
    );
}