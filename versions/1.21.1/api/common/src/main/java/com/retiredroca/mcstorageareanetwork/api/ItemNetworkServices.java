package com.retiredroca.mcstorageareanetwork.api;

/**
 * Holds the loader-specific platform services. The loader initializer installs the {@link ItemScanner}
 * before any host scans run, so common code can call {@link #scanner()} without referencing a loader.
 */
public final class ItemNetworkServices {
    private static volatile ItemScanner scanner;

    private ItemNetworkServices() {}

    public static void setScanner(ItemScanner scanner) {
        ItemNetworkServices.scanner = scanner;
    }

    public static ItemScanner scanner() {
        ItemScanner current = scanner;
        if (current == null) {
            throw new IllegalStateException("MC Storage Area Network API platform has not been initialized");
        }
        return current;
    }
}
