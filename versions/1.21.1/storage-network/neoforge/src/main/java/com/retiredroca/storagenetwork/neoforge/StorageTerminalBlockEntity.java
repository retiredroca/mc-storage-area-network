package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;

public class StorageTerminalBlockEntity extends AbstractStorageTerminalBlockEntity implements MenuProvider {
    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_TERMINAL_BE.get(), pos, state);
    }
}
