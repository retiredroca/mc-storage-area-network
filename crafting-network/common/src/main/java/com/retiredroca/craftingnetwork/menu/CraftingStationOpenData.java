package com.retiredroca.craftingnetwork.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CraftingStationOpenData(BlockPos pos, List<CraftingSourceInfo> sources, boolean shulkersFirst) {
    public static final StreamCodec<FriendlyByteBuf, CraftingStationOpenData> STREAM_CODEC = StreamCodec.of(
            CraftingStationOpenData::encode,
            CraftingStationOpenData::decode);

    private static void encode(FriendlyByteBuf buf, CraftingStationOpenData data) {
        buf.writeBlockPos(data.pos());
        buf.writeVarInt(data.sources().size());
        for (CraftingSourceInfo info : data.sources()) {
            CraftingSourceInfo.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, info);
        }
        buf.writeBoolean(data.shulkersFirst());
    }

    private static CraftingStationOpenData decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readVarInt();
        List<CraftingSourceInfo> sources = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sources.add(CraftingSourceInfo.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
        }
        return new CraftingStationOpenData(pos, sources, buf.readBoolean());
    }
}