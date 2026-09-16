package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.storagenetwork.StorageNetworkCommon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(StorageNetworkCommon.MODID)
public class StorageNetwork {
    public StorageNetwork(IEventBus modEventBus, ModContainer modContainer) {
        StorageNetworkCommon.setPlatform(new NeoForgeStorageNetworkPlatform());
        StorageNetworkConfig.register(modContainer);
        modEventBus.addListener(StorageNetworkConfig::onConfigLoad);

        Registration.register(modEventBus);
        Networking.register(modEventBus);
    }
}
