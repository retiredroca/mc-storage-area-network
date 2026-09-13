package com.retiredroca.mcstorageareanetwork.fabric;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.CollectionOnlyStorage;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.ItemScanner;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Fabric {@link ItemScanner} using the transfer API lookups. Only ticking chunks are read. */
public final class FabricItemScanner implements ItemScanner {
    @Override
    public List<ScannedStorage> scan(ServerLevel level, BlockPos center, int chunkRadius) {
        List<ScannedStorage> out = new ArrayList<>();
        ContainerOwnership.Entry host = ContainerOwnership.ownerOf(level, center);
        int centerX = center.getX() >> 4;
        int centerZ = center.getZ() >> 4;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (!level.getChunkSource().isPositionTicking(ChunkPos.asLong(centerX + dx, centerZ + dz))) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.getY() < minY || pos.getY() >= maxY) {
                        continue;
                    }
                    Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, null);
                    if (storage != null
                            && ContainerOwnership.canSee(level, ContainerOwnership.ownerOf(level, pos), host)) {
                        boolean collectionOnly = level.getBlockState(pos).getBlock() instanceof CollectionOnlyStorage;
                        out.add(new FabricScannedStorage(pos, labelOf(blockEntity), storage, collectionOnly));
                    }
                }
            }
        }
        return out;
    }

    static String labelOf(BlockEntity blockEntity) {
        if (blockEntity instanceof Nameable nameable && nameable.hasCustomName() && nameable.getCustomName() != null) {
            return nameable.getCustomName().getString();
        }
        return blockEntity.getBlockState().getBlock().getName().getString();
    }
}
