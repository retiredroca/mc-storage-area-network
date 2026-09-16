package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.storagenetwork.StorageNetworkCommon;

import net.fabricmc.api.ModInitializer;

public class StorageNetwork implements ModInitializer {
    @Override
    public void onInitialize() {
        StorageNetworkCommon.setPlatform(new FabricStorageNetworkPlatform());
        StorageNetworkConfig.load();
        Registration.register();
        Networking.register();
    }
}
