package com.retiredroca.mcstorageareanetwork.api;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Loader-specific scanner that finds inventory-bearing blocks in a chunk radius. The platform
 * implementation is installed by the loader initializer via {@link ItemNetworkServices}.
 */
public interface ItemScanner {
    List<ScannedStorage> scan(ServerLevel level, BlockPos center, int chunkRadius);

    /**
     * True if the block at {@code pos} exposes an item storage on the loader's item capability. The
     * single-block counterpart of {@link #scan}, used by the built-in interaction hooks (which run on
     * both sides, so the level is not narrowed to {@link ServerLevel}).
     */
    default boolean hasItemStorage(Level level, BlockPos pos) {
        return false;
    }
}
