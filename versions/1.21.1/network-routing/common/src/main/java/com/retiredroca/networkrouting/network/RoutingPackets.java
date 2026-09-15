package com.retiredroca.networkrouting.network;

import java.util.List;

import com.retiredroca.networkrouting.NetworkRoutingCommon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Loader-neutral custom payloads for Network Routing. Each loader registers/handles them. */
public final class RoutingPackets {
    /** {@code state & 1} = routing terminal; {@code state & 2} = a host is bound; {@code state & 4} = show the container list. */
    public static final int STATE_TERMINAL = 1;
    public static final int STATE_BOUND = 2;
    public static final int STATE_LIST = 4;

    public static final ResourceLocation SYNC = id("routing_sync");
    public static final ResourceLocation SET_FILTER = id("routing_set_filter");
    public static final ResourceLocation TOGGLE = id("routing_toggle");
    public static final ResourceLocation ACTION = id("routing_action");
    public static final ResourceLocation SERVER_PRESENCE = id("server_presence");

    private RoutingPackets() {}

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(NetworkRoutingCommon.MODID, path);
    }

    /** One container row: display label, position, and its filter tokens. */
    public record ContainerInfo(String label, BlockPos pos, List<String> tokens) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ContainerInfo> STREAM_CODEC = StreamCodec.of(
                ContainerInfo::encode, ContainerInfo::decode);

        private static void encode(RegistryFriendlyByteBuf buf, ContainerInfo info) {
            buf.writeUtf(info.label());
            buf.writeBlockPos(info.pos());
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, info.tokens());
        }

        private static ContainerInfo decode(RegistryFriendlyByteBuf buf) {
            String label = buf.readUtf(Short.MAX_VALUE);
            BlockPos pos = buf.readBlockPos();
            List<String> tokens = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf);
            return new ContainerInfo(label, pos, tokens);
        }
    }

    public record RoutingSyncPayload(BlockPos pos, int state, int tier, int flags, List<ContainerInfo> containers)
            implements CustomPacketPayload {
        public static final Type<RoutingSyncPayload> TYPE = new Type<>(SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, RoutingSyncPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, RoutingSyncPayload::pos,
                ByteBufCodecs.VAR_INT, RoutingSyncPayload::state,
                ByteBufCodecs.VAR_INT, RoutingSyncPayload::tier,
                ByteBufCodecs.VAR_INT, RoutingSyncPayload::flags,
                ContainerInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), RoutingSyncPayload::containers,
                RoutingSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record FilterPayload(BlockPos origin, BlockPos container, List<String> tokens, boolean terminal)
            implements CustomPacketPayload {
        public static final Type<FilterPayload> TYPE = new Type<>(SET_FILTER);
        public static final StreamCodec<RegistryFriendlyByteBuf, FilterPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, FilterPayload::origin,
                BlockPos.STREAM_CODEC, FilterPayload::container,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), FilterPayload::tokens,
                ByteBufCodecs.BOOL, FilterPayload::terminal,
                FilterPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TogglePayload(BlockPos pos, int flags) implements CustomPacketPayload {
        public static final Type<TogglePayload> TYPE = new Type<>(TOGGLE);
        public static final StreamCodec<RegistryFriendlyByteBuf, TogglePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TogglePayload::pos,
                ByteBufCodecs.VAR_INT, TogglePayload::flags,
                TogglePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ActionPayload(BlockPos pos, int action) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(ACTION);
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ActionPayload::pos,
                ByteBufCodecs.VAR_INT, ActionPayload::action,
                ActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerPresencePayload() implements CustomPacketPayload {
        public static final Type<ServerPresencePayload> TYPE = new Type<>(SERVER_PRESENCE);
        public static final StreamCodec<FriendlyByteBuf, ServerPresencePayload> STREAM_CODEC =
                StreamCodec.unit(new ServerPresencePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
