package com.retiredroca.networkrouting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-level store of each routing terminal's bound host, keyed by the terminal's position.
 *
 * <p>A routing terminal publishes the host it is bound to (position, chunk radius and tier) here
 * whenever it re-binds in {@code refreshBinding}, and the entry is dropped when the binding is
 * cleared or the terminal block is removed. Because it is a {@link SavedData} the bound areas
 * survive chunk unloads, so {@link NetworkRoutingAreas} can still answer ranged lookups while the
 * terminal (or its host) is not loaded.
 */
public final class RoutingBindings {
    private static final String DATA_NAME = "network_routing_bindings";

    private RoutingBindings() {}

    public static final class Data extends SavedData {
        public static final SavedData.Factory<Data> FACTORY =
                new SavedData.Factory<>(Data::new, Data::load, DataFixTypes.LEVEL);

        private final Map<BlockPos, NetworkRoutingAreas.Area> bindings = new HashMap<>();

        public Data() {}

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            ListTag list = tag.getList("bindings", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos terminal = BlockPos.of(entry.getLong("pos"));
                BlockPos host = BlockPos.of(entry.getLong("host"));
                data.bindings.put(terminal,
                        new NetworkRoutingAreas.Area(host, entry.getInt("radius"), entry.getInt("tier")));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            bindings.forEach((terminal, area) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("pos", terminal.asLong());
                entry.putLong("host", area.hostPos().asLong());
                entry.putInt("radius", area.chunkRadius());
                entry.putInt("tier", area.tier());
                list.add(entry);
            });
            tag.put("bindings", list);
            return tag;
        }

        Map<BlockPos, NetworkRoutingAreas.Area> bindings() {
            return bindings;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    /** Publishes (or refreshes) the host served by the terminal at {@code terminalPos}. */
    public static void set(ServerLevel level, BlockPos terminalPos, BlockPos hostPos, int chunkRadius, int tier) {
        Data data = data(level);
        NetworkRoutingAreas.Area area =
                new NetworkRoutingAreas.Area(hostPos.immutable(), chunkRadius, tier);
        BlockPos key = terminalPos.immutable();
        if (area.equals(data.bindings().get(key))) {
            return;
        }
        data.bindings().put(key, area);
        data.setDirty();
    }

    /** Drops the host published by the terminal at {@code terminalPos}; true when one was stored. */
    public static boolean remove(ServerLevel level, BlockPos terminalPos) {
        Data data = data(level);
        if (data.bindings().remove(terminalPos) == null) {
            return false;
        }
        data.setDirty();
        return true;
    }

    /** The hosts currently stored for {@code level} (immutable; empty when none). */
    public static List<NetworkRoutingAreas.Area> all(ServerLevel level) {
        return List.copyOf(data(level).bindings().values());
    }
}
