package com.retiredroca.mcstorageareanetwork.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Tracks which player placed each storage/terminal block. Terminals use this to show only global
 * (player-independent, e.g. worldgen) storage plus storage their owner placed, and to restrict
 * breaking of terminals to their owner.
 */
public final class ContainerOwnership {
    private static final String DATA_NAME = "mc_storage_area_network_owners";

    /** Owner of a placed block: player UUID plus their last-known name (for scoreboard teams). */
    public record Entry(UUID id, String name) {}

    private ContainerOwnership() {}

    public static final class Data extends SavedData {
        public static final SavedData.Factory<Data> FACTORY =
                new SavedData.Factory<>(Data::new, Data::load, DataFixTypes.LEVEL);

        private final Map<BlockPos, Entry> owners = new HashMap<>();

        public Data() {}

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            ListTag list = tag.getList("owners", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos pos = BlockPos.of(entry.getLong("pos"));
                try {
                    UUID id = UUID.fromString(entry.getString("uuid"));
                    String name = entry.getString("name");
                    data.owners.put(pos, new Entry(id, name));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed entries
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            owners.forEach((pos, entry) -> {
                CompoundTag e = new CompoundTag();
                e.putLong("pos", pos.asLong());
                e.putString("uuid", entry.id().toString());
                e.putString("name", entry.name());
                list.add(e);
            });
            tag.put("owners", list);
            return tag;
        }

        Map<BlockPos, Entry> owners() {
            return owners;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    public static void setOwner(ServerLevel level, BlockPos pos, UUID id, String name) {
        if (id == null) {
            return;
        }
        Data data = data(level);
        data.owners().put(pos.immutable(), new Entry(id, name == null ? "" : name));
        data.setDirty();
    }

    public static void clearOwner(ServerLevel level, BlockPos pos) {
        Data data = data(level);
        if (data.owners().remove(pos) != null) {
            data.setDirty();
        }
    }

    public static Entry ownerOf(ServerLevel level, BlockPos pos) {
        return data(level).owners().get(pos);
    }

    /** True if the terminal owner may see a container with the given owner. */
    public static boolean canSee(ServerLevel level, Entry container, Entry viewer) {
        if (!NetworkSettings.ownershipEnabled() || container == null) {
            return true; // global / unowned storage
        }
        if (viewer == null) {
            return false;
        }
        if (container.id().equals(viewer.id())) {
            return true;
        }
        if (!NetworkSettings.teamSharing() || container.name().isEmpty() || viewer.name().isEmpty()) {
            return false;
        }
        PlayerTeam a = level.getScoreboard().getPlayersTeam(container.name());
        PlayerTeam b = level.getScoreboard().getPlayersTeam(viewer.name());
        return a != null && a == b;
    }

    /** True if the player may break the block at {@code pos} (unowned blocks are free to break). */
    public static boolean isOwner(ServerLevel level, BlockPos pos, UUID player) {
        Entry owner = ownerOf(level, pos);
        return owner == null || owner.id().equals(player);
    }
}
