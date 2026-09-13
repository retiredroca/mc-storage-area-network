package com.retiredroca.craftingnetwork.neoforge;

import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingStationBlockEntity extends AbstractCraftingStationBlockEntity {
    public CraftingStationBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.CRAFTING_STATION_BE.get(), pos, state);
    }
}
