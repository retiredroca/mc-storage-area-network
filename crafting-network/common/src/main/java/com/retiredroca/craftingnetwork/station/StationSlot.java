package com.retiredroca.craftingnetwork.station;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** One displayed slot of a processor station (icon + translatable label), synced to the client. */
public record StationSlot(String label, ItemStack item) {
    public static final StreamCodec<RegistryFriendlyByteBuf, StationSlot> STREAM_CODEC = StreamCodec.of(
            StationSlot::encode,
            StationSlot::decode);

    private static void encode(RegistryFriendlyByteBuf buf, StationSlot slot) {
        buf.writeUtf(slot.label());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, slot.item());
    }

    private static StationSlot decode(RegistryFriendlyByteBuf buf) {
        return new StationSlot(buf.readUtf(Short.MAX_VALUE),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }
}