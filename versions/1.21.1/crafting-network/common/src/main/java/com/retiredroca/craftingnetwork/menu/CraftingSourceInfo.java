package com.retiredroca.craftingnetwork.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CraftingSourceInfo(BlockPos pos, String label, List<CraftingSourceInfo> children) {
    public CraftingSourceInfo(BlockPos pos, String label) {
        this(pos, label, List.of());
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingSourceInfo> STREAM_CODEC = StreamCodec.of(
            CraftingSourceInfo::encode,
            CraftingSourceInfo::decode);

    private static void encode(RegistryFriendlyByteBuf buf, CraftingSourceInfo info) {
        buf.writeBlockPos(info.pos());
        buf.writeUtf(info.label());
        buf.writeVarInt(info.children().size());
        for (CraftingSourceInfo child : info.children()) {
            encode(buf, child);
        }
    }

    private static CraftingSourceInfo decode(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String label = buf.readUtf(Short.MAX_VALUE);
        int count = buf.readVarInt();
        List<CraftingSourceInfo> children = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            children.add(decode(buf));
        }
        return new CraftingSourceInfo(pos, label, children);
    }
}