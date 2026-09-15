package com.retiredroca.mcstorageareanetwork.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * A routing rule that biases which container receives an inserted stack. Rules are consulted by
 * {@link StorageRouter} on every network insertion, so a companion mod (for example a routing mod
 * that labels containers) can make matching containers win priority without the host mods
 * depending on it.
 *
 * <p>{@code refPos} is the position doing the routing (terminal/station), so a rule can break ties
 * by distance. Higher scores are tried first; equal scores keep the caller's existing order (the
 * sort is stable). Return {@code 0} for "no opinion".
 */
@FunctionalInterface
public interface StoragePriority {
    int priority(ServerLevel level, BlockPos refPos, BlockPos pos, ItemStack stack);
}
