package com.retiredroca.itemnetwork.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * A sub-source nested inside a container row in a storage listing, for example a shulker box that
 * lives inside a chest. The position is the position of the parent container so the host can nest
 * the row under the matching container.
 */
public record NestedSource(String label, BlockPos pos, List<ItemStack> items, List<Integer> counts) {
}
