package com.retiredroca.itemnetwork.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.itemnetwork.api.ItemScanner;
import com.retiredroca.itemnetwork.api.ScannedStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** NeoForge {@link ItemScanner} using the item-handler block capability. Only ticking chunks are read. */
public final class NeoForgeItemScanner implements ItemScanner {
    @Override
    public List<ScannedStorage> scan(ServerLevel level, BlockPos center, int chunkRadius) {
        List<ScannedStorage> out = new ArrayList<>();
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
                    for (Direction side : Direction.values()) {
                        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
                        if (handler != null) {
                            out.add(new NeoForgeScannedStorage(pos, labelOf(blockEntity), handler));
                            break;
                        }
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
