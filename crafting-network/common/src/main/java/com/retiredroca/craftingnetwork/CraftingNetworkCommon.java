package com.retiredroca.craftingnetwork;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Loader-neutral entry point for Crafting Network. Holds the mod id/logger and the loader-specific
 * {@link CraftingNetworkPlatform}, which the loader initializer installs before any station code runs.
 */
public final class CraftingNetworkCommon {
    public static final String MODID = "crafting_network";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static volatile CraftingNetworkPlatform platform;

    private CraftingNetworkCommon() {}

    public static void setPlatform(CraftingNetworkPlatform platform) {
        CraftingNetworkCommon.platform = platform;
    }

    public static CraftingNetworkPlatform platform() {
        CraftingNetworkPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Crafting Network platform has not been initialized");
        }
        return current;
    }
}
