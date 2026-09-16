package com.retiredroca.storagenetwork;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;

import net.minecraft.network.chat.Component;

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

    /** Message for a container-exclusion toggle result. */
    public static Component exclusionMessage(NetworkExclusions.Result result) {
        String key = switch (result) {
            case EXCLUDED -> "message.storage_network.container_excluded";
            case INCLUDED -> "message.storage_network.container_included";
            case PROTECTED -> "message.storage_network.container_protected";
            case NOT_ALLOWED -> "message.storage_network.container_not_allowed";
        };
        return Component.translatable(key);
    }
}
