package com.retiredroca.mcstorageareanetwork.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * In-game toggling of the container-exclusion list (see {@link NetworkSettings#isContainerExcluded}).
 *
 * <p>The exclusion list itself lives in the loader config; this store only records <em>who</em>
 * excluded each block type, so removal is restricted to that player (or their scoreboard team), or
 * an operator. Config entries with no attribution are treated as server-managed (ops only).
 */
public final class NetworkExclusions {
    private static final String DATA_NAME = "mc_storage_area_network_exclusions";

    /** Outcome of a toggle attempt, for the caller to translate into a message. */
    public enum Result { EXCLUDED, INCLUDED, PROTECTED, NOT_ALLOWED }

    private NetworkExclusions() {}

    public static final class Data extends SavedData {
        public static final SavedData.Factory<Data> FACTORY =
                new SavedData.Factory<>(Data::new, Data::load, DataFixTypes.LEVEL);

        private final Map<ResourceLocation, ContainerOwnership.Entry> owners = new HashMap<>();

        public Data() {}

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            ListTag list = tag.getList("owners", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("block"));
                if (id == null) {
                    continue;
                }
                try {
                    data.owners.put(id, new ContainerOwnership.Entry(UUID.fromString(entry.getString("uuid")),
                            entry.getString("name")));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed entries
                }
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            owners.forEach((id, entry) -> {
                CompoundTag e = new CompoundTag();
                e.putString("block", id.toString());
                e.putString("uuid", entry.id().toString());
                e.putString("name", entry.name());
                list.add(e);
            });
            tag.put("owners", list);
            return tag;
        }

        Map<ResourceLocation, ContainerOwnership.Entry> owners() {
            return owners;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    /** Toggles the block type at {@code pos} in/out of the network, enforcing ownership. */
    public static Result toggle(ServerPlayer player, BlockPos pos) {        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (NetworkSettings.isProtectedContainer(blockId)) {
            return Result.PROTECTED;
        }

        ContainerOwnership.Entry viewer = new ContainerOwnership.Entry(player.getUUID(),
                player.getGameProfile().getName());
        boolean op = player.hasPermissions(2);
        Data data = data(level);

        if (NetworkSettings.isContainerExcluded(blockId)) {
            ContainerOwnership.Entry owner = data.owners().get(blockId);
            boolean allowed = op || (owner != null && ContainerOwnership.canSee(level, owner, viewer));
            if (!allowed) {
                return Result.NOT_ALLOWED;
            }
            ItemNetworkServices.configService().setContainerExcluded(blockId, false);
            if (data.owners().remove(blockId) != null) {
                data.setDirty();
            }
            return Result.INCLUDED;
        }

        boolean allowed = op || ContainerOwnership.canSee(level, ContainerOwnership.ownerOf(level, pos), viewer);
        if (!allowed) {
            return Result.NOT_ALLOWED;
        }
        ItemNetworkServices.configService().setContainerExcluded(blockId, true);
        data.owners().put(blockId, viewer);
        data.setDirty();
        return Result.EXCLUDED;
    }

    public static Component message(Result result) {
        String key = switch (result) {
            case EXCLUDED -> "message.mc_storage_area_network.container_excluded";
            case INCLUDED -> "message.mc_storage_area_network.container_included";
            case PROTECTED -> "message.mc_storage_area_network.container_protected";
            case NOT_ALLOWED -> "message.mc_storage_area_network.container_not_allowed";
        };
        return Component.translatable(key);
    }
}
