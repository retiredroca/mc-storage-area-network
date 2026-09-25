package com.retiredroca.craftingnetwork.menu;

import java.util.List;

import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Shared view of a processor station's menu used by the station screen: source selection,
 * catalog reads and the live state snapshot.
 */
public interface IStationMenu {
    StationType stationType();

    BlockPos stationPos();

    AbstractStationBlockEntity getStationEntity();

    List<CraftingSourceInfo> getSources();

    /** Applies a source list pushed from the server after the menu was already open. */
    void setServerSources(List<CraftingSourceInfo> sources);

    int getSelectedSource();

    void selectSource(int id);

    int getSourceCount();

    List<String> getSourceLabels();

    ItemStack getCatalogItem(int index);

    int getDataVersion();

    StationState getState();

    void setServerState(StationState state);

    /** C2S brew target change. No-op on cooking stations. */
    void applyBrewTarget(String potionId);
}