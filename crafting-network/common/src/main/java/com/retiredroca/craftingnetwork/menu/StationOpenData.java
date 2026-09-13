package com.retiredroca.craftingnetwork.menu;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Menu open payload for processor stations: position, station kind and the source list. */
public record StationOpenData(BlockPos pos, StationType type, List<CraftingSourceInfo> sources) {
    public static final StreamCodec<FriendlyByteBuf, StationOpenData> STREAM_CODEC = StreamCodec.of(
            StationOpenData::encode,
            StationOpenData::decode);

    private static void encode(FriendlyByteBuf buf, StationOpenData data) {
        buf.writeBlockPos(data.pos());
        buf.writeUtf(data.type().path());
        buf.writeVarInt(data.sources().size());
        for (CraftingSourceInfo info : data.sources()) {
            CraftingSourceInfo.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, info);
        }
    }

    private static StationOpenData decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        StationType type = StationType.fromPath(buf.readUtf(Short.MAX_VALUE));
        int count = buf.readVarInt();
        List<CraftingSourceInfo> sources = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sources.add(CraftingSourceInfo.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
        }
        return new StationOpenData(pos, type, sources);
    }
}