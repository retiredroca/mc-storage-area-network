package com.retiredroca.craftingnetwork.fabric;

import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;
import com.retiredroca.craftingnetwork.menu.CraftingStationOpenData;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingStationBlockEntity extends AbstractCraftingStationBlockEntity
        implements ExtendedScreenHandlerFactory<CraftingStationOpenData> {
    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.CRAFTING_STATION_BE, pos, state);
    }

    @Override
    public CraftingStationOpenData getScreenOpeningData(ServerPlayer player) {
        return new CraftingStationOpenData(getBlockPos(), getSourceInfos(), isShulkersFirst(), isInventoryFirst());
    }
}
