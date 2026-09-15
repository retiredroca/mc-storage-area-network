package com.retiredroca.networkrouting;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/** Loader-neutral entry points for Network Routing. The loader module installs the platform. */
public final class NetworkRoutingCommon {
    public static final String MODID = "network_routing";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static volatile NetworkRoutingPlatform platform;

    private NetworkRoutingCommon() {}

    public static void setPlatform(NetworkRoutingPlatform value) {
        platform = value;
    }

    public static NetworkRoutingPlatform platform() {
        NetworkRoutingPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Network Routing platform has not been initialized");
        }
        return current;
    }
}
