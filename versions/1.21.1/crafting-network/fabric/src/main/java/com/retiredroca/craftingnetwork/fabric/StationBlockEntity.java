package com.retiredroca.craftingnetwork.fabric;

import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.menu.StationOpenData;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class StationBlockEntity extends AbstractStationBlockEntity
        implements ExtendedScreenHandlerFactory<StationOpenData> {
    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.STATION_BE, pos, state);
    }

    @Override
    public StationOpenData getScreenOpeningData(ServerPlayer player) {
        return new StationOpenData(getBlockPos(), type(), getSourceInfos());
    }
}
