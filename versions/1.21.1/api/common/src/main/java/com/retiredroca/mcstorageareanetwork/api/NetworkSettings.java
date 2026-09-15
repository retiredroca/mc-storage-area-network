package com.retiredroca.mcstorageareanetwork.api;

/**
 * Server-side policy flags for the item network. Set by the loader config on load/reload; read by
 * the scanners and sources when deciding which containers a terminal may see.
 */
public final class NetworkSettings {
    private static volatile boolean ownershipEnabled = true;
    private static volatile boolean teamSharing = true;

    private NetworkSettings() {}

    public static void configure(boolean ownership, boolean teams) {
        ownershipEnabled = ownership;
        teamSharing = teams;
    }

    /** When true, terminals only see global (unowned) storage plus storage their owner placed. */
    public static boolean ownershipEnabled() {
        return ownershipEnabled;
    }

    /** When true, players on the same scoreboard team share their placed storage. */
    public static boolean teamSharing() {
        return teamSharing;
    }
}
