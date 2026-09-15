package com.retiredroca.mcstorageareanetwork.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Loader-specific scanner that finds inventory-bearing blocks in a chunk radius. The platform
 * implementation is installed by the loader initializer via {@link ItemNetworkServices}.
 */
public interface ItemScanner {
    List<ScannedStorage> scan(ServerLevel level, BlockPos center, int chunkRadius);
}
