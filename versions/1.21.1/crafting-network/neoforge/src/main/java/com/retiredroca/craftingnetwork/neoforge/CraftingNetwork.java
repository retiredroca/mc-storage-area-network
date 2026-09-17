package com.retiredroca.craftingnetwork.neoforge;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.CraftingNetworkHooks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CraftingNetworkCommon.MODID)
public class CraftingNetwork {
    public CraftingNetwork(IEventBus modEventBus, ModContainer modContainer) {
        CraftingNetworkCommon.setPlatform(new NeoForgeCraftingNetworkPlatform());
        CraftingNetworkConfig.register(modContainer);
        modEventBus.addListener(CraftingNetworkConfig::onConfigLoad);

        Registration.register(modEventBus);
        Networking.register(modEventBus);
        CraftingNetworkHooks.register();
    }
}
