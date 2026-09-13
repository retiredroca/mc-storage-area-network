package com.retiredroca.craftingnetwork;

import java.nio.file.Path;
import java.util.List;

import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.menu.BrewingStationMenu;
import com.retiredroca.craftingnetwork.menu.CookingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-specific services used by the loader-neutral Crafting Network code. */
public interface CraftingNetworkPlatform {
    MenuType<CraftingStationMenu> craftingStationMenuType();

    MenuType<CookingStationMenu> cookingStationMenuType();

    MenuType<BrewingStationMenu> brewingStationMenuType();

    BlockEntityType<?> craftingStationBlockEntityType();

    BlockEntityType<?> stationBlockEntityType();

    Item craftingStationItem();

    Block stationBlock(StationType type);

    Item stationItem(StationType type);

    AbstractCraftingStationBlockEntity createCraftingStationBlockEntity(BlockPos pos, BlockState state);

    AbstractStationBlockEntity createStationBlockEntity(BlockPos pos, BlockState state);

    void openCraftingStation(ServerPlayer player, AbstractCraftingStationBlockEntity station);

    void openStation(ServerPlayer player, AbstractStationBlockEntity station);

    void sendSources(ServerPlayer player, List<CraftingSourceInfo> sources);

    void sendStationState(ServerPlayer player, BlockPos pos, StationState state);

    /** C2S: change the brew target of the station the player currently has open. */
    void sendBrewTarget(BlockPos pos, String potion);

    /** Play the recipe-book tab bounce animation. Called client-side only. */
    void bounceRecipeBookTabs(Object recipeBookComponent);

    boolean isServerModded();

    Path configDir();

    int getMaxTier();
}
