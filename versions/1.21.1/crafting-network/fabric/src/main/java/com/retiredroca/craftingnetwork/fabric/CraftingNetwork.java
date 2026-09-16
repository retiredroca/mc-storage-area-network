package com.retiredroca.craftingnetwork.fabric;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;

import net.fabricmc.api.ModInitializer;

public class CraftingNetwork implements ModInitializer {
    @Override
    public void onInitialize() {
        CraftingNetworkCommon.setPlatform(new FabricCraftingNetworkPlatform());
        CraftingNetworkConfig.load();
        Networking.register();
        Registration.register();
    }
}
