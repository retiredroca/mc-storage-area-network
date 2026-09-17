package com.retiredroca.mcstorageareanetwork.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.CollectionOnlyStorage;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.ItemScanner;
import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** NeoForge {@link ItemScanner} using the item-handler block capability. Only ticking chunks are read. */
public final class NeoForgeItemScanner implements ItemScanner {
    @Override
    public boolean hasItemStorage(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
    }

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
                    BlockState state = level.getBlockState(pos);
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (NetworkSettings.isContainerExcluded(blockId)) {
                        continue;
                    }
                    // Prefer the unsided handler (full inventory). Some vanilla containers expose a
                    // side-subset when queried with a direction (e.g. a brewing stand's DOWN face omits
                    // the blaze-powder fuel slot), so fall back to per-side handlers only if unsided fails.
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    if (handler == null) {
                        for (Direction side : Direction.values()) {
                            handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
                            if (handler != null) {
                                break;
                            }
                        }
                    }
                    if (handler != null
                            && ContainerOwnership.canSee(level, ContainerOwnership.ownerOf(level, pos), host)) {
                        boolean collectionOnly = state.getBlock() instanceof CollectionOnlyStorage;
                        out.add(new NeoForgeScannedStorage(pos, labelOf(blockEntity), handler, blockId, collectionOnly));
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
