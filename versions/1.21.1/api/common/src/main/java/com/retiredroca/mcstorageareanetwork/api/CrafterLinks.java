package com.retiredroca.mcstorageareanetwork.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Remembers the crafting pattern of each network-linked Crafter. The vanilla crafter consumes its
 * grid on every craft, so the pattern has to be stored to refill it from the network afterwards.
 */
public final class CrafterLinks {
    private static final String DATA_NAME = "mc_storage_area_network_crafter_links";
    public static final int SIZE = 9;

    private CrafterLinks() {}

    public static final class Data extends SavedData {
        public static final SavedData.Factory<Data> FACTORY =
                new SavedData.Factory<>(Data::new, Data::load, DataFixTypes.LEVEL);

        private final Map<BlockPos, List<ItemStack>> patterns = new HashMap<>();

        public Data() {}

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            ListTag list = tag.getList("links", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                BlockPos pos = BlockPos.of(entry.getLong("pos"));
                ListTag items = entry.getList("pattern", Tag.TAG_COMPOUND);
                List<ItemStack> pattern = new ArrayList<>();
                for (int j = 0; j < items.size(); j++) {
                    pattern.add(ItemStack.CODEC.parse(NbtOps.INSTANCE, items.getCompound(j))
                            .result().orElse(ItemStack.EMPTY));
                }
                data.patterns.put(pos, pattern);
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            patterns.forEach((pos, pattern) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("pos", pos.asLong());
                ListTag items = new ListTag();
                for (ItemStack stack : pattern) {
                    items.add(ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack)
                            .result().orElse(new CompoundTag()));
                }
                entry.put("pattern", items);
                list.add(entry);
            });
            tag.put("links", list);
            return tag;
        }

        Map<BlockPos, List<ItemStack>> patterns() {
            return patterns;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    public static List<ItemStack> pattern(ServerLevel level, BlockPos pos) {
        return data(level).patterns().get(pos);
    }

    public static void set(ServerLevel level, BlockPos pos, List<ItemStack> pattern) {
        Data data = data(level);
        data.patterns().put(pos.immutable(), List.copyOf(pattern));
        data.setDirty();
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        Data data = data(level);
        if (data.patterns().remove(pos) != null) {
            data.setDirty();
        }
    }

    /** Snapshot of the current links (position to pattern), for the server tick. */
    public static Map<BlockPos, List<ItemStack>> all(ServerLevel level) {
        return Map.copyOf(data(level).patterns());
    }
}
