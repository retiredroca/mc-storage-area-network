package com.retiredroca.craftingnetwork.network;

import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.station.StationState;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Loader-neutral packet payloads shared by the loader networking implementations. */
public final class StationPackets {
    public static final ResourceLocation CRAFTING_SOURCES = ResourceLocation
            .fromNamespaceAndPath(CraftingNetworkCommon.MODID, "crafting_sources");
    public static final ResourceLocation SERVER_PRESENCE = ResourceLocation
            .fromNamespaceAndPath(CraftingNetworkCommon.MODID, "server_presence");
    public static final ResourceLocation STATION_STATE = ResourceLocation
            .fromNamespaceAndPath(CraftingNetworkCommon.MODID, "station_state");
    public static final ResourceLocation STATION_BREW_TARGET = ResourceLocation
            .fromNamespaceAndPath(CraftingNetworkCommon.MODID, "station_brew_target");

    private StationPackets() {}

    public record ServerPresencePayload() implements CustomPacketPayload {
        public static final Type<ServerPresencePayload> TYPE = new Type<>(SERVER_PRESENCE);
        public static final StreamCodec<FriendlyByteBuf, ServerPresencePayload> STREAM_CODEC = StreamCodec
                .unit(new ServerPresencePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CraftingSourcesPayload(List<CraftingSourceInfo> sources, boolean shulkersFirst,
            boolean inventoryFirst) implements CustomPacketPayload {
        public static final Type<CraftingSourcesPayload> TYPE = new Type<>(CRAFTING_SOURCES);
        public static final StreamCodec<RegistryFriendlyByteBuf, CraftingSourcesPayload> STREAM_CODEC = StreamCodec.of(
                CraftingSourcesPayload::encode,
                CraftingSourcesPayload::decode);

        private static void encode(RegistryFriendlyByteBuf buf, CraftingSourcesPayload payload) {
            CraftingSourceInfo.STREAM_CODEC.<RegistryFriendlyByteBuf>cast().apply(ByteBufCodecs.list())
                    .encode(buf, payload.sources());
            buf.writeBoolean(payload.shulkersFirst());
            buf.writeBoolean(payload.inventoryFirst());
        }

        private static CraftingSourcesPayload decode(RegistryFriendlyByteBuf buf) {
            List<CraftingSourceInfo> sources = CraftingSourceInfo.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
                    .apply(ByteBufCodecs.list()).decode(buf);
            boolean shulkersFirst = buf.readBoolean();
            boolean inventoryFirst = buf.readBoolean();
            return new CraftingSourcesPayload(sources, shulkersFirst, inventoryFirst);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StationStatePayload(BlockPos pos, StationState state) implements CustomPacketPayload {
        public static final Type<StationStatePayload> TYPE = new Type<>(STATION_STATE);
        public static final StreamCodec<RegistryFriendlyByteBuf, StationStatePayload> STREAM_CODEC = StreamCodec.of(
                StationStatePayload::encode,
                StationStatePayload::decode);

        private static void encode(RegistryFriendlyByteBuf buf, StationStatePayload payload) {
            buf.writeBlockPos(payload.pos());
            StationState.STREAM_CODEC.encode(buf, payload.state());
        }

        private static StationStatePayload decode(RegistryFriendlyByteBuf buf) {
            return new StationStatePayload(buf.readBlockPos(), StationState.STREAM_CODEC.decode(buf));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record StationBrewTargetPayload(BlockPos pos, String potion) implements CustomPacketPayload {
        public static final Type<StationBrewTargetPayload> TYPE = new Type<>(STATION_BREW_TARGET);
        public static final StreamCodec<FriendlyByteBuf, StationBrewTargetPayload> STREAM_CODEC = StreamCodec.of(
                StationBrewTargetPayload::encode,
                StationBrewTargetPayload::decode);

        private static void encode(FriendlyByteBuf buf, StationBrewTargetPayload payload) {
            buf.writeBlockPos(payload.pos());
            buf.writeUtf(payload.potion());
        }

        private static StationBrewTargetPayload decode(FriendlyByteBuf buf) {
            return new StationBrewTargetPayload(buf.readBlockPos(), buf.readUtf(Short.MAX_VALUE));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
