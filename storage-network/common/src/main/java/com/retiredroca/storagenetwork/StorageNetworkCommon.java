package com.retiredroca.storagenetwork;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/** Loader-neutral entry points for Storage Network. The loader module installs the platform. */
public final class StorageNetworkCommon {
    public static final String MODID = "storage_network";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static volatile StorageNetworkPlatform platform;

    private StorageNetworkCommon() {}

    public static void setPlatform(StorageNetworkPlatform value) {
        platform = value;
    }

    public static StorageNetworkPlatform platform() {
        StorageNetworkPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Storage Network platform has not been initialized");
        }
        return current;
    }
}
