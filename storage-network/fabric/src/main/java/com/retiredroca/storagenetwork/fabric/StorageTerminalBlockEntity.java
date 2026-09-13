package com.retiredroca.storagenetwork.fabric;


import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class StorageTerminalBlockEntity extends AbstractStorageTerminalBlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos> {
    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_TERMINAL_BE, pos, state);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }
}
