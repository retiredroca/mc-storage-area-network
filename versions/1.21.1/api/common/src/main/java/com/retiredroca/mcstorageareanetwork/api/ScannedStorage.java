package com.retiredroca.mcstorageareanetwork.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A storage block discovered by an {@link ItemScanner}. The loader-specific handler is hidden behind
 * this loader-agnostic view so host mods (and the universal jar) can use it on any loader.
 */
public interface ScannedStorage {
    BlockPos pos();

    /** Registry id of the block backing this storage (e.g. {@code minecraft:chest}), or null. */
    default ResourceLocation blockId() {
        return null;
    }

    String label();

    /** Whether items can be pulled from this storage. Storages that only accept input return false. */
    default boolean supportsExtraction() {
        return true;
    }

    /** Collection-only storages (output sinks) are hidden from listings but receive crafted output. */
    default boolean collectionOnly() {
        return false;
    }

    /** All non-empty stacks in this storage, as stack entries (count = amount). */
    List<ItemStack> enumerate();

    /** Total count of {@code item} across this storage's slots. */
    int count(ItemStack item);

    /** Remove up to {@code max} of {@code item}; returns the number removed. */
    int extract(ItemStack item, int max);

    /** Insert {@code stack}; returns the remainder that could not be stored. */
    ItemStack insert(ItemStack stack);
}
