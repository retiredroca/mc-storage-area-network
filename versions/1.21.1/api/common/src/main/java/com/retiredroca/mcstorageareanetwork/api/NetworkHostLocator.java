package com.retiredroca.mcstorageareanetwork.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/** Finds the nearest {@link NetworkHost} (e.g. a Storage Terminal) around a position. */
public final class NetworkHostLocator {
    private NetworkHostLocator() {}

    public static NetworkHost findNearest(ServerLevel level, BlockPos origin, int chunkRadius) {
        NetworkHost best = null;
        double bestDist = Double.MAX_VALUE;
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof NetworkHost host) {
                        double dist = blockEntity.getBlockPos().distSqr(origin);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = host;
                        }
                    }
                }
            }
        }
        return best;
    }
}
