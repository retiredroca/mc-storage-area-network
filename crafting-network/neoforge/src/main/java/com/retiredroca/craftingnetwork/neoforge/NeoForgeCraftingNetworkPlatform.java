package com.retiredroca.craftingnetwork.neoforge;

import java.nio.file.Path;
import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkPlatform;
import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.menu.BrewingStationMenu;
import com.retiredroca.craftingnetwork.menu.CookingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.menu.StationOpenData;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;

public final class NeoForgeCraftingNetworkPlatform implements CraftingNetworkPlatform {
    @Override
    public MenuType<CraftingStationMenu> craftingStationMenuType() {
        return Registration.CRAFTING_STATION_MENU.get();
    }

    @Override
    public MenuType<CookingStationMenu> cookingStationMenuType() {
        return Registration.COOKING_STATION_MENU.get();
    }

    @Override
    public MenuType<BrewingStationMenu> brewingStationMenuType() {
        return Registration.BREWING_STATION_MENU.get();
    }

    @Override
    public BlockEntityType<?> craftingStationBlockEntityType() {
        return Registration.CRAFTING_STATION_BE.get();
    }

    @Override
    public BlockEntityType<?> stationBlockEntityType() {
        return Registration.STATION_BE.get();
    }

    @Override
    public Item craftingStationItem() {
        return Registration.CRAFTING_STATION_BLOCK.get().asItem();
    }

    @Override
    public Block stationBlock(StationType type) {
        return Registration.getStationBlock(type);
    }

    @Override
    public Item stationItem(StationType type) {
        return Registration.getStationItem(type);
    }

    @Override
    public AbstractCraftingStationBlockEntity createCraftingStationBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingStationBlockEntity(pos, state);
    }

    @Override
    public AbstractStationBlockEntity createStationBlockEntity(BlockPos pos, BlockState state) {
        return new StationBlockEntity(pos, state);
    }

    @Override
    public void openCraftingStation(ServerPlayer player, AbstractCraftingStationBlockEntity station) {
        player.openMenu(station, buffer -> {
            buffer.writeBlockPos(station.getBlockPos());
            CraftingStationMenu.writeSources(buffer, station.getSourceInfos());
            buffer.writeBoolean(station.isShulkersFirst());
            buffer.writeBoolean(station.isInventoryFirst());
        });
    }

    @Override
    public void openStation(ServerPlayer player, AbstractStationBlockEntity station) {
        player.openMenu(station, buffer -> StationOpenData.STREAM_CODEC.encode(buffer,
                new StationOpenData(station.getBlockPos(), station.type(), station.getSourceInfos())));
    }

    @Override
    public void sendSources(ServerPlayer player, List<CraftingSourceInfo> sources, boolean shulkersFirst,
            boolean inventoryFirst) {
        Networking.sendSources(player, sources, shulkersFirst, inventoryFirst);
    }

    @Override
    public void sendStationState(ServerPlayer player, BlockPos pos, StationState state) {
        Networking.sendStationState(player, pos, state);
    }

    @Override
    public void sendBrewTarget(BlockPos pos, String potion) {
        Networking.sendBrewTarget(pos, potion);
    }

    @Override
    public void bounceRecipeBookTabs(Object recipeBookComponent) {
        if (!(recipeBookComponent instanceof RecipeBookComponent component)) {
            return;
        }
        for (RecipeBookTabButton tab : component.tabButtons) {
            if (isSearchTab(tab)) {
                continue;
            }
            tab.animationTime = 15.0F;
        }
    }

    private static boolean isSearchTab(RecipeBookTabButton tab) {
        RecipeBookCategories category = tab.getCategory();
        return category == RecipeBookCategories.CRAFTING_SEARCH
                || category == RecipeBookCategories.FURNACE_SEARCH
                || category == RecipeBookCategories.BLAST_FURNACE_SEARCH
                || category == RecipeBookCategories.SMOKER_SEARCH;
    }

    @Override
    public boolean isServerModded() {
        return Networking.isServerModded();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public int getMaxTier() {
        return CraftingNetworkConfig.getMaxTier();
    }
}
