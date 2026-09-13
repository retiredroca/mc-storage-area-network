package com.retiredroca.itemnetwork.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * A virtual item source that can be attached to an item network (a Storage Terminal or a Crafting
 * Station network).
 *
 * <p>Implementations are loader-independent at the interface level: the loader-specific storage
 * access lives inside the implementation, so the same source works on Fabric and NeoForge.
 *
 * <p>Every operation receives the {@code hostPos} of the block whose network is being queried, so a
 * single registered source can safely serve multiple hosts at once by keying its state by host
 * position.
 */
public interface ItemSource {
    /** Human readable name shown in the host sidebar (e.g. "Shulker Boxes"). */
    String getSourceName();

    /**
     * Called whenever a host rescans its network. The source should discard any stale state for
     * {@code hostPos} and re-read containers in that host's loaded-chunk radius.
     */
    void refresh(ServerLevel level, BlockPos hostPos, int chunkRadius);

    /** Items currently reachable through this source for the given host, as stack entries. */
    List<ItemStack> enumerate(BlockPos hostPos);

    /** Sub-sources that live inside a container row, so the host can nest them under their parent. */
    default List<NestedSource> nestedSources(BlockPos hostPos) {
        return List.of();
    }

    /**
     * Remove up to {@code maxCount} of {@code item} from this source.
     *
     * @param shulkerFirst when true prefer pulling from shulker-box content over plain slots
     * @return the number of items actually removed
     */
    int extract(BlockPos hostPos, ItemStack item, int maxCount, boolean shulkerFirst);

    /**
     * Insert {@code stack} into this source.
     *
     * @param shulkerFirst when true prefer filling shulker boxes before empty container slots
     * @return the remainder that could not be stored
     */
    ItemStack insert(BlockPos hostPos, ItemStack stack, boolean shulkerFirst);

    /**
     * Insert {@code stack} into a specific nested source previously reported by
     * {@link #nestedSources(BlockPos)}.
     */
    default ItemStack insertIntoChild(BlockPos hostPos, BlockPos containerPos, String childLabel,
            ItemStack stack, boolean shulkerFirst) {
        return stack;
    }
}
