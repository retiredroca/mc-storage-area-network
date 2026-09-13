package com.retiredroca.craftingnetwork.neoforge;

import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class StationBlockEntity extends AbstractStationBlockEntity {
    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STATION_BE.get(), pos, state);
    }
}
