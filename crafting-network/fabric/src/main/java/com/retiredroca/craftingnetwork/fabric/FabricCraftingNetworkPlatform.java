package com.retiredroca.craftingnetwork.fabric;

import java.nio.file.Path;
import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkPlatform;
import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.menu.BrewingStationMenu;
import com.retiredroca.craftingnetwork.menu.CookingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.mixin.RecipeBookComponentAccessor;
import com.retiredroca.craftingnetwork.mixin.RecipeBookTabButtonAccessor;
import com.retiredroca.craftingnetwork.network.StationPackets;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationType;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
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

public final class FabricCraftingNetworkPlatform implements CraftingNetworkPlatform {
    @Override
    public MenuType<CraftingStationMenu> craftingStationMenuType() {
        return Registration.CRAFTING_STATION_MENU;
    }

    @Override
    public MenuType<CookingStationMenu> cookingStationMenuType() {
        return Registration.COOKING_STATION_MENU;
    }

    @Override
    public MenuType<BrewingStationMenu> brewingStationMenuType() {
        return Registration.BREWING_STATION_MENU;
    }

    @Override
    public BlockEntityType<?> craftingStationBlockEntityType() {
        return Registration.CRAFTING_STATION_BE;
    }

    @Override
    public BlockEntityType<?> stationBlockEntityType() {
        return Registration.STATION_BE;
    }

    @Override
    public Item craftingStationItem() {
        return Registration.CRAFTING_STATION_ITEM;
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
        player.openMenu((CraftingStationBlockEntity) station);
    }

    @Override
    public void openStation(ServerPlayer player, AbstractStationBlockEntity station) {
        player.openMenu((StationBlockEntity) station);
    }

    @Override
    public void sendSources(ServerPlayer player, List<CraftingSourceInfo> sources) {
        Networking.sendSources(player, sources);
    }

    @Override
    public void sendStationState(ServerPlayer player, BlockPos pos, StationState state) {
        Networking.sendStationState(player, pos, state);
    }

    @Override
    public void sendBrewTarget(BlockPos pos, String potion) {
        ClientPlayNetworking.send(new StationPackets.StationBrewTargetPayload(pos, potion));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void bounceRecipeBookTabs(Object recipeBookComponent) {
        if (!(recipeBookComponent instanceof RecipeBookComponent component)) {
            return;
        }
        List<RecipeBookTabButton> tabs = ((RecipeBookComponentAccessor) component).craftingnetwork$getTabButtons();
        for (RecipeBookTabButton tab : tabs) {
            if (isSearchTab(tab)) {
                continue;
            }
            ((RecipeBookTabButtonAccessor) tab).craftingnetwork$setAnimationTime(15.0F);
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
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public int getMaxTier() {
        return CraftingNetworkConfig.getMaxTier();
    }
}
