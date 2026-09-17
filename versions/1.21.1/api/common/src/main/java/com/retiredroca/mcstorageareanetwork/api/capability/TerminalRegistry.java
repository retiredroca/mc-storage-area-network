package com.retiredroca.mcstorageareanetwork.api.capability;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Capability published by remote-access-terminal: every placed terminal, independent of chunk
 * loading.
 *
 * <p>Publishers register an implementation through
 * {@link com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities#register} and consumers read
 * it through {@link com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities#get}. The listing is
 * a live view and is never filtered by the caller's position or permissions.
 */
public interface TerminalRegistry {
    /** One placed terminal: its dimension and position, plus how it presents. */
    record Terminal(ResourceKey<Level> dimension, BlockPos pos, String name, String color) {}

    /** Every registered terminal across all dimensions; empty when none are registered. */
    List<Terminal> terminals();
}
