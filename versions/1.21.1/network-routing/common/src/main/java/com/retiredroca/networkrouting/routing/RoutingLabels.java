package com.retiredroca.networkrouting.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-container filter tokens, keyed by block position (mirrors the API's ContainerOwnership store). */
public final class RoutingLabels {
    private static final String DATA_NAME = "network_routing_labels";

    private RoutingLabels() {}

    public static final class Data extends SavedData {
        public static final SavedData.Factory<Data> FACTORY =
                new SavedData.Factory<>(Data::new, Data::load, DataFixTypes.LEVEL);

        private final Map<BlockPos, List<String>> labels = new HashMap<>();

        public Data() {}

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            ListTag list = tag.getList("labels", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos pos = BlockPos.of(entry.getLong("pos"));
                ListTag tokens = entry.getList("tokens", Tag.TAG_STRING);
                List<String> out = new ArrayList<>();
                for (int j = 0; j < tokens.size(); j++) {
                    out.add(tokens.getString(j));
                }
                if (!out.isEmpty()) {
                    data.labels.put(pos, out);
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            labels.forEach((pos, tokens) -> {
                if (tokens.isEmpty()) {
                    return;
                }
                CompoundTag entry = new CompoundTag();
                entry.putLong("pos", pos.asLong());
                ListTag out = new ListTag();
                tokens.forEach(t -> out.add(StringTag.valueOf(t)));
                entry.put("tokens", out);
                list.add(entry);
            });
            tag.put("labels", list);
            return tag;
        }

        Map<BlockPos, List<String>> labels() {
            return labels;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    public static List<String> get(ServerLevel level, BlockPos pos) {
        return data(level).labels().get(pos);
    }

    public static ContainerFilter filter(ServerLevel level, BlockPos pos) {
        List<String> tokens = get(level, pos);
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        ContainerFilter filter = new ContainerFilter(tokens);
        return filter.isEmpty() ? null : filter;
    }

    public static boolean isLabeled(ServerLevel level, BlockPos pos) {
        return filter(level, pos) != null;
    }

    public static void set(ServerLevel level, BlockPos pos, List<String> tokens) {
        Data data = data(level);
        List<String> clean = tokens == null ? List.of() : tokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (clean.isEmpty()) {
            if (data.labels().remove(pos) != null) {
                data.setDirty();
            }
            return;
        }
        data.labels().put(pos.immutable(), clean);
        data.setDirty();
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        set(level, pos, List.of());
    }

    /**
     * Clears the container's filter when the player may reconfigure it (owner/trusted or operator);
     * returns false and leaves the filter untouched otherwise.
     */
    public static boolean clear(ServerLevel level, BlockPos pos, Player player) {
        if (!NetworkPermissions.canEdit(level, pos, player)) {
            return false;
        }
        clear(level, pos);
        return true;
    }
}
