package com.retiredroca.storagenetwork.network;

import java.util.List;

import com.retiredroca.storagenetwork.StorageNetworkCommon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral custom payloads for Storage Network. Each loader registers/handles them. */
public final class TerminalPackets {
    public static final ResourceLocation TERMINAL_SYNC = ResourceLocation.fromNamespaceAndPath(
            StorageNetworkCommon.MODID, "terminal_sync");
    public static final ResourceLocation TERMINAL_EXTRACT = ResourceLocation.fromNamespaceAndPath(
            StorageNetworkCommon.MODID, "terminal_extract");
    public static final ResourceLocation TERMINAL_SELECT = ResourceLocation.fromNamespaceAndPath(
            StorageNetworkCommon.MODID, "terminal_select");
    public static final ResourceLocation SERVER_PRESENCE = ResourceLocation.fromNamespaceAndPath(
            StorageNetworkCommon.MODID, "server_presence");

    private TerminalPackets() {}

    public record ServerPresencePayload() implements CustomPacketPayload {
        public static final Type<ServerPresencePayload> TYPE = new Type<>(SERVER_PRESENCE);
        public static final StreamCodec<FriendlyByteBuf, ServerPresencePayload> STREAM_CODEC = StreamCodec
                .unit(new ServerPresencePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChestSync(String name, BlockPos pos, List<ItemStack> items, List<Integer> counts,
            List<ChestSync> children) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestSync> STREAM_CODEC = StreamCodec.of(
                ChestSync::encode,
                ChestSync::decode);

        private static final StreamCodec<RegistryFriendlyByteBuf, List<ChestSync>> CHILDREN_CODEC = STREAM_CODEC
                .apply(ByteBufCodecs.list());

        private static void encode(RegistryFriendlyByteBuf buf, ChestSync sync) {
            buf.writeUtf(sync.name());
            buf.writeBlockPos(sync.pos());
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, sync.items());
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).encode(buf, sync.counts());
            CHILDREN_CODEC.encode(buf, sync.children());
        }

        private static ChestSync decode(RegistryFriendlyByteBuf buf) {
            String name = buf.readUtf(Short.MAX_VALUE);
            BlockPos pos = buf.readBlockPos();
            List<ItemStack> items = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
            List<Integer> counts = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()).decode(buf);
            List<ChestSync> children = CHILDREN_CODEC.decode(buf);
            return new ChestSync(name, pos, items, counts, children);
        }
    }

    public record TerminalSyncPayload(List<ItemStack> items, List<Integer> counts, List<ChestSync> chests, int tier)
            implements CustomPacketPayload {
        public static final Type<TerminalSyncPayload> TYPE = new Type<>(TERMINAL_SYNC);
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::items,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), TerminalSyncPayload::counts,
                ChestSync.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSyncPayload::chests,
                ByteBufCodecs.VAR_INT, TerminalSyncPayload::tier,
                TerminalSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TerminalExtractPayload(BlockPos pos, ItemStack stack, int mode) implements CustomPacketPayload {
        public static final Type<TerminalExtractPayload> TYPE = new Type<>(TERMINAL_EXTRACT);
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalExtractPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TerminalExtractPayload::pos,
                ItemStack.OPTIONAL_STREAM_CODEC, TerminalExtractPayload::stack,
                ByteBufCodecs.VAR_INT, TerminalExtractPayload::mode,
                TerminalExtractPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TerminalSelectPayload(BlockPos pos, boolean all, BlockPos targetPos, String childName)
            implements CustomPacketPayload {
        public static final Type<TerminalSelectPayload> TYPE = new Type<>(TERMINAL_SELECT);
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSelectPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TerminalSelectPayload::pos,
                ByteBufCodecs.BOOL, TerminalSelectPayload::all,
                BlockPos.STREAM_CODEC, TerminalSelectPayload::targetPos,
                ByteBufCodecs.STRING_UTF8, TerminalSelectPayload::childName,
                TerminalSelectPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
