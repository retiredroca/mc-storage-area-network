package com.retiredroca.mcstorageareanetwork.api;

import net.minecraft.core.BlockPos;

/**
 * A block entity that hosts a storage network (for example the Storage Terminal). Companion mods
 * that manage the network (for example a routing terminal) bind to the nearest host and reuse its
 * scan radius and container list instead of scanning independently.
 */
public interface NetworkHost {
    BlockPos pos();

    /** Chunk radius this host scans for containers. */
    int chunkRadius();

    /** Upgrade tier of this host (used by companion hardware to tint itself). */
    default int tier() {
        return 0;
    }
}
