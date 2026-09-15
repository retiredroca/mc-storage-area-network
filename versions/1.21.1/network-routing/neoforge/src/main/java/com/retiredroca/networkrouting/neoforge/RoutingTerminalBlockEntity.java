package com.retiredroca.networkrouting.neoforge;

import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;

public class RoutingTerminalBlockEntity extends AbstractRoutingTerminalBlockEntity implements MenuProvider {
    public RoutingTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.TERMINAL_BE.get(), pos, state);
    }
}
