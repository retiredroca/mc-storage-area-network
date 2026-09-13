package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkShareTerminalBlockEntity extends AbstractNetworkShareTerminalBlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos> {
    public NetworkShareTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SHARE_TERMINAL_BE, pos, state);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }
}
