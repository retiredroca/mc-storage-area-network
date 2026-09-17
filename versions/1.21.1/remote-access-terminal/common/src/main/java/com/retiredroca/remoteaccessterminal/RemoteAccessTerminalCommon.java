package com.retiredroca.remoteaccessterminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-neutral entry point and platform seam for Remote Access Terminal.
 *
 * <p>Each loader's initializer installs a {@link RemoteAccessTerminalPlatform} before anything
 * else, so the shared sources can reach loader-only services through {@link #platform()}.
 */
public final class RemoteAccessTerminalCommon {
    public static final String MODID = "remote_access_terminal";
    public static final String MOD_NAME = "Remote Access Terminal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static volatile RemoteAccessTerminalPlatform platform;

    private RemoteAccessTerminalCommon() {
    }

    public static void setPlatform(RemoteAccessTerminalPlatform value) {
        platform = value;
    }

    public static RemoteAccessTerminalPlatform platform() {
        RemoteAccessTerminalPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Remote Access Terminal platform has not been installed");
        }
        return current;
    }
}
