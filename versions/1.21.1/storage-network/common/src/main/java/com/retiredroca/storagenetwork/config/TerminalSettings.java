package com.retiredroca.storagenetwork.config;

/** Loader-neutral Storage Network settings. The loader config writes these values. */
public final class TerminalSettings {
    public static final int MAX_SUPPORTED_TIER = 5;

    private static int maxTier = MAX_SUPPORTED_TIER;

    private TerminalSettings() {}

    public static int getMaxTier() {
        return maxTier;
    }

    public static void setMaxTier(int value) {
        maxTier = Math.max(0, Math.min(MAX_SUPPORTED_TIER, value));
    }
}
