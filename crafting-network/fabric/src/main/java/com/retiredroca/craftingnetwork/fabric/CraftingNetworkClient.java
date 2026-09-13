package com.retiredroca.craftingnetwork.fabric;

import com.retiredroca.craftingnetwork.client.CraftingStationBEWLR;
import com.retiredroca.craftingnetwork.client.CraftingStationRenderer;
import com.retiredroca.craftingnetwork.client.CraftingStationScreen;
import com.retiredroca.craftingnetwork.client.StationBEWLR;
import com.retiredroca.craftingnetwork.client.StationRenderer;
import com.retiredroca.craftingnetwork.client.StationScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class CraftingNetworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Networking.registerClient();
        MenuScreens.register(Registration.CRAFTING_STATION_MENU, CraftingStationScreen::new);
        MenuScreens.register(Registration.COOKING_STATION_MENU, StationScreen::new);
        MenuScreens.register(Registration.BREWING_STATION_MENU, StationScreen::new);
        BlockEntityRendererRegistry.register(Registration.CRAFTING_STATION_BE,
                context -> new CraftingStationRenderer<>(context));
        BlockEntityRendererRegistry.register(Registration.STATION_BE,
                context -> new StationRenderer<>(context));
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.CRAFTING_STATION_ITEM,
                CraftingStationBEWLR::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.SMELTING_STATION_ITEM,
                StationBEWLR::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.BLASTING_STATION_ITEM,
                StationBEWLR::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.SMOKING_STATION_ITEM,
                StationBEWLR::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.BREWING_STATION_ITEM,
                StationBEWLR::renderByItem);
    }
}
