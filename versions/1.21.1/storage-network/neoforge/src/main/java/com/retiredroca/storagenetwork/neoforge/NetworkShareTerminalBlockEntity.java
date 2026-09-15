package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkShareTerminalBlockEntity extends AbstractNetworkShareTerminalBlockEntity {
    public NetworkShareTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SHARE_TERMINAL_BE.get(), pos, state);
    }
}
