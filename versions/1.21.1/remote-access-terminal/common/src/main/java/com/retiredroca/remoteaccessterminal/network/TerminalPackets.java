package com.retiredroca.remoteaccessterminal.network;

import java.util.List;
import java.util.UUID;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/** Loader-neutral custom payloads for Remote Access Terminal. Each loader registers/handles them. */
public final class TerminalPackets {
    public static final ResourceLocation SYNC = id("terminal_sync");
    public static final ResourceLocation OPEN_NAME = id("terminal_open_name");
    public static final ResourceLocation REQUEST_SYNC = id("terminal_request_sync");
    public static final ResourceLocation TRAVEL = id("terminal_travel");
    public static final ResourceLocation RENAME = id("terminal_rename");
    public static final ResourceLocation SET_COLOR = id("terminal_set_color");
    public static final ResourceLocation SET_SORT = id("terminal_set_sort");
    public static final ResourceLocation SET_OPEN = id("terminal_set_open");
    public static final ResourceLocation SET_CHUNK_LOADER = id("terminal_set_chunk_loader");
    public static final ResourceLocation SET_INVITES = id("terminal_set_invites");
    public static final ResourceLocation UNLINK = id("terminal_unlink");

    private static final StreamCodec<ByteBuf, ResourceKey<Level>> DIMENSION_CODEC =
            ResourceKey.streamCodec(Registries.DIMENSION);

    private TerminalPackets() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RemoteAccessTerminalCommon.MODID, path);
    }

    /** One picker row: display name, dye, dimension and position of a destination terminal. */
    public record Destination(String name, DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Destination> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Destination::name,
                DyeColor.STREAM_CODEC, Destination::color,
                DIMENSION_CODEC, Destination::dimension,
                BlockPos.STREAM_CODEC, Destination::pos,
                Destination::new);
    }

    /** Full server-to-client snapshot of one terminal plus the menu's target. */
    public record TerminalSyncPayload(ResourceKey<Level> dimension, BlockPos pos, DyeColor color, String name,
            boolean open, boolean chunkLoader, long chunkLoaderUntil, int chunkLoaderQueuePosition,
            boolean canEdit, int sortMode, List<Destination> destinations,
            List<Integer> counts, int total, List<UUID> invites) implements CustomPacketPayload {
        public static final Type<TerminalSyncPayload> TYPE = new Type<>(SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> STREAM_CODEC =
                StreamCodec.of(TerminalSyncPayload::encode, TerminalSyncPayload::decode);

        private static void encode(RegistryFriendlyByteBuf buf, TerminalSyncPayload payload) {
            DIMENSION_CODEC.encode(buf, payload.dimension());
            BlockPos.STREAM_CODEC.encode(buf, payload.pos());
            DyeColor.STREAM_CODEC.encode(buf, payload.color());
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.name() == null ? "" : payload.name());
            ByteBufCodecs.BOOL.encode(buf, payload.open());
            ByteBufCodecs.BOOL.encode(buf, payload.chunkLoader());
            ByteBufCodecs.VAR_LONG.encode(buf, payload.chunkLoaderUntil());
            ByteBufCodecs.VAR_INT.encode(buf, payload.chunkLoaderQueuePosition());
            ByteBufCodecs.BOOL.encode(buf, payload.canEdit());
            ByteBufCodecs.VAR_INT.encode(buf, payload.sortMode());
            Destination.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.destinations());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, payload.counts());
            ByteBufCodecs.VAR_INT.encode(buf, payload.total());
            UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, payload.invites());
        }

        private static TerminalSyncPayload decode(RegistryFriendlyByteBuf buf) {
            ResourceKey<Level> dimension = DIMENSION_CODEC.decode(buf);
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            DyeColor color = DyeColor.STREAM_CODEC.decode(buf);
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            boolean open = ByteBufCodecs.BOOL.decode(buf);
            boolean chunkLoader = ByteBufCodecs.BOOL.decode(buf);
            long chunkLoaderUntil = ByteBufCodecs.VAR_LONG.decode(buf);
            int chunkLoaderQueuePosition = ByteBufCodecs.VAR_INT.decode(buf);
            boolean canEdit = ByteBufCodecs.BOOL.decode(buf);
            int sortMode = ByteBufCodecs.VAR_INT.decode(buf);
            List<Destination> destinations = Destination.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<Integer> counts = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf);
            int total = ByteBufCodecs.VAR_INT.decode(buf);
            List<UUID> invites = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            return new TerminalSyncPayload(dimension, pos, color, name.isEmpty() ? null : name, open, chunkLoader,
                    chunkLoaderUntil, chunkLoaderQueuePosition, canEdit, sortMode, destinations, counts, total,
                    invites);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Asks the client to open the naming popup for the terminal it was just prompted for. */
    public record OpenNamePayload(ResourceKey<Level> dimension, BlockPos pos, DyeColor color)
            implements CustomPacketPayload {
        public static final Type<OpenNamePayload> TYPE = new Type<>(OPEN_NAME);
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenNamePayload> STREAM_CODEC =
                StreamCodec.composite(
                        DIMENSION_CODEC, OpenNamePayload::dimension,
                        BlockPos.STREAM_CODEC, OpenNamePayload::pos,
                        DyeColor.STREAM_CODEC, OpenNamePayload::color,
                        OpenNamePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: re-send the snapshot of the currently open terminal menu. */
    public record RequestSyncPayload() implements CustomPacketPayload {
        public static final Type<RequestSyncPayload> TYPE = new Type<>(REQUEST_SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestSyncPayload> STREAM_CODEC =
                StreamCodec.unit(new RequestSyncPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: travel to a destination of the currently open terminal menu. */
    public record TravelPayload(ResourceKey<Level> targetDimension, BlockPos targetPos) implements CustomPacketPayload {
        public static final Type<TravelPayload> TYPE = new Type<>(TRAVEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, TravelPayload> STREAM_CODEC =
                StreamCodec.composite(
                        DIMENSION_CODEC, TravelPayload::targetDimension,
                        BlockPos.STREAM_CODEC, TravelPayload::targetPos,
                        TravelPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: rename a terminal identified by dimension, position and dye (settings or placement popup). */
    public record RenamePayload(ResourceKey<Level> dimension, BlockPos pos, DyeColor color, String name)
            implements CustomPacketPayload {
        public static final Type<RenamePayload> TYPE = new Type<>(RENAME);
        public static final StreamCodec<RegistryFriendlyByteBuf, RenamePayload> STREAM_CODEC =
                StreamCodec.composite(
                        DIMENSION_CODEC, RenamePayload::dimension,
                        BlockPos.STREAM_CODEC, RenamePayload::pos,
                        DyeColor.STREAM_CODEC, RenamePayload::color,
                        ByteBufCodecs.STRING_UTF8, RenamePayload::name,
                        RenamePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: change the dye of the currently open terminal menu. */
    public record SetColorPayload(DyeColor color) implements CustomPacketPayload {
        public static final Type<SetColorPayload> TYPE = new Type<>(SET_COLOR);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetColorPayload> STREAM_CODEC =
                StreamCodec.composite(DyeColor.STREAM_CODEC, SetColorPayload::color, SetColorPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: set the destination sort mode of the currently open terminal menu. */
    public record SetSortPayload(int sortMode) implements CustomPacketPayload {
        public static final Type<SetSortPayload> TYPE = new Type<>(SET_SORT);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetSortPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, SetSortPayload::sortMode, SetSortPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: set the open/private flag of the currently open terminal menu. */
    public record SetOpenPayload(boolean open) implements CustomPacketPayload {
        public static final Type<SetOpenPayload> TYPE = new Type<>(SET_OPEN);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetOpenPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, SetOpenPayload::open, SetOpenPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: set the chunk-loader flag of the currently open terminal menu. */
    public record SetChunkLoaderPayload(boolean enabled) implements CustomPacketPayload {
        public static final Type<SetChunkLoaderPayload> TYPE = new Type<>(SET_CHUNK_LOADER);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetChunkLoaderPayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, SetChunkLoaderPayload::enabled,
                        SetChunkLoaderPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: replace the invitees (link members) of the currently open terminal menu. */
    public record SetInvitesPayload(List<UUID> invites) implements CustomPacketPayload {
        public static final Type<SetInvitesPayload> TYPE = new Type<>(SET_INVITES);
        public static final StreamCodec<RegistryFriendlyByteBuf, SetInvitesPayload> STREAM_CODEC =
                StreamCodec.composite(
                        UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), SetInvitesPayload::invites,
                        SetInvitesPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: remove the link record of the currently open terminal menu, leaving the block in place. */
    public record UnlinkPayload() implements CustomPacketPayload {
        public static final Type<UnlinkPayload> TYPE = new Type<>(UNLINK);
        public static final StreamCodec<RegistryFriendlyByteBuf, UnlinkPayload> STREAM_CODEC =
                StreamCodec.unit(new UnlinkPayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
