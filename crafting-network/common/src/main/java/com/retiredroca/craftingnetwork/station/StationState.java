package com.retiredroca.craftingnetwork.station;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Live snapshot of a processor station, pushed S2C whenever it changes: status, the displayed
 * slot icons, a 0..100 progress percentage, the brewing target potion id (empty otherwise), and
 * the set of potion ids that can currently be brewed from the scanned network (brewing only).
 */
public record StationState(StationStatus status, List<StationSlot> slots, int progress, String target,
        List<String> craftable, boolean shulkersFirst, boolean inventoryFirst) {
    public static final StreamCodec<RegistryFriendlyByteBuf, StationState> STREAM_CODEC = StreamCodec.of(
            StationState::encode,
            StationState::decode);

    private static void encode(RegistryFriendlyByteBuf buf, StationState state) {
        buf.writeEnum(state.status());
        StationSlot.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
                .apply(ByteBufCodecs.list())
                .encode(buf, state.slots());
        buf.writeVarInt(state.progress());
        buf.writeUtf(state.target());
        buf.writeVarInt(state.craftable().size());
        for (String id : state.craftable()) {
            buf.writeUtf(id);
        }
        buf.writeBoolean(state.shulkersFirst());
        buf.writeBoolean(state.inventoryFirst());
    }

    private static StationState decode(RegistryFriendlyByteBuf buf) {
        StationStatus status = buf.readEnum(StationStatus.class);
        List<StationSlot> slots = new ArrayList<>(
                StationSlot.STREAM_CODEC.<RegistryFriendlyByteBuf>cast()
                        .apply(ByteBufCodecs.list())
                        .decode(buf));
        int progress = buf.readVarInt();
        String target = buf.readUtf(Short.MAX_VALUE);
        int craftableCount = buf.readVarInt();
        List<String> craftable = new ArrayList<>(craftableCount);
        for (int i = 0; i < craftableCount; i++) {
            craftable.add(buf.readUtf());
        }
        boolean shulkersFirst = buf.readBoolean();
        boolean inventoryFirst = buf.readBoolean();
        return new StationState(status, slots, progress, target, craftable, shulkersFirst, inventoryFirst);
    }

    /**
     * Value-based comparison. {@code ItemStack} does not override {@code equals}/{@code hashCode} in
     * 1.21, so the generated record {@code equals} compares stack identities and can both suppress real
     * changes (mutable shared stacks) and report false changes (fresh stacks).
     */
    public boolean sameAs(StationState other) {
        if (other == null) {
            return false;
        }
        if (status != other.status || progress != other.progress) {
            return false;
        }
        if (shulkersFirst != other.shulkersFirst || inventoryFirst != other.inventoryFirst) {
            return false;
        }
        if (!target.equals(other.target) || !craftable.equals(other.craftable)) {
            return false;
        }
        if (slots.size() != other.slots.size()) {
            return false;
        }
        for (int i = 0; i < slots.size(); i++) {
            StationSlot a = slots.get(i);
            StationSlot b = other.slots.get(i);
            if (!a.label().equals(b.label()) || !ItemStack.matches(a.item(), b.item())) {
                return false;
            }
        }
        return true;
    }
}
