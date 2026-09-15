package com.retiredroca.networkrouting.config;

/** Tunables for Network Routing. Kept as static state so both loaders share one source. */
public final class RoutingSettings {
    /** Chunk radius searched for a Storage Terminal to bind to. */
    public static int searchChunks = 2;
    /** Ticks between host re-binds / network re-scans. */
    public static int scanIntervalTicks = 20;
    /** Ticks between automatic maintenance passes while a toggle is enabled. */
    public static int maintenanceIntervalTicks = 40;
    /** Maximum filter tokens accepted per container. */
    public static int maxTokens = 32;

    private RoutingSettings() {}
}
