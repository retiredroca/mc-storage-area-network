package com.retiredroca.networkrouting.fabric;

import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class RoutingTerminalBlockEntity extends AbstractRoutingTerminalBlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos> {
    public RoutingTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.TERMINAL_BE, pos, state);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return getBlockPos();
    }
}
