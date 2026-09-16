package com.retiredroca.mcstorageareanetwork.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Loader-neutral payloads for the operator break-confirmation flow (see {@link BreakProtection}). */
public final class ProtectionPackets {
    private static final ResourceLocation CONFIRM_BREAK = ResourceLocation.fromNamespaceAndPath(
            "mc_storage_area_network", "confirm_break");
    private static final ResourceLocation FORCE_BREAK = ResourceLocation.fromNamespaceAndPath(
            "mc_storage_area_network", "force_break");

    private ProtectionPackets() {}

    /** S2C: ask the client to confirm breaking a protected block they don't own. */
    public record ConfirmBreakPayload(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ConfirmBreakPayload> TYPE = new Type<>(CONFIRM_BREAK);
        public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmBreakPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ConfirmBreakPayload::pos, ConfirmBreakPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** C2S: the player confirmed; break the protected block. */
    public record ForceBreakPayload(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ForceBreakPayload> TYPE = new Type<>(FORCE_BREAK);
        public static final StreamCodec<RegistryFriendlyByteBuf, ForceBreakPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, ForceBreakPayload::pos, ForceBreakPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
