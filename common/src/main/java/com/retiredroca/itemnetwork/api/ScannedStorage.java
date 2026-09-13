package com.retiredroca.itemnetwork.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A storage block discovered by an {@link ItemScanner}. The loader-specific handler is hidden behind
 * this loader-agnostic view so host mods (and the universal jar) can use it on any loader.
 */
public interface ScannedStorage {
    BlockPos pos();

    String label();

    /** Whether items can be pulled from this storage. Storages that only accept input return false. */
    default boolean supportsExtraction() {
        return true;
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
