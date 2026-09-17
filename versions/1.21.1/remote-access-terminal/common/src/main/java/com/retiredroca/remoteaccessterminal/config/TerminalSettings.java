package com.retiredroca.remoteaccessterminal.config;

/**
 * Loader-neutral Remote Access Terminal settings. The loader config writes these values; the
 * gameplay code reads them. Kept as static state so both loaders share one source.
 */
public final class TerminalSettings {
    /** Default total number of terminals allowed across every colour and dimension. */
    public static final int DEFAULT_MAX_TERMINALS = 80;
    /** Upper bound the global terminal cap is clamped to. */
    public static final int MAX_CONFIGURABLE_TERMINALS = 4096;
    /** Default total number of chunk-loading terminals across every colour and dimension. */
    public static final int DEFAULT_MAX_CHUNKLOADER_TERMINALS = 2;
    /** Default number of chunk-loading terminals a single player may hold at once. */
    public static final int DEFAULT_MAX_CHUNKLOADERS_PER_PLAYER = 2;
    /** Default lease length of a chunk loader, in minutes; 0 means the lease never expires. */
    public static final int DEFAULT_CHUNKLOADER_TIMEOUT_MINUTES = 60;
    /** Upper bound the configured lease length is clamped to (one week). */
    public static final int MAX_CONFIGURABLE_CHUNKLOADER_TIMEOUT_MINUTES = 10080;
    /** Default: a blocked request waits in a queue instead of being refused outright. */
    public static final boolean DEFAULT_CHUNKLOADER_QUEUE = true;
    /** Default lazy ring: 1 keeps the eight neighbouring chunks loaded, 0 disables the ring. */
    public static final int DEFAULT_LAZY_CHUNK_RING = 1;
    /** Upper bound the configured lazy ring is clamped to (vanilla's maximum simulation distance). */
    public static final int MAX_CONFIGURABLE_LAZY_CHUNK_RING = 32;
    /** Upper bound the total chunk-loader cap is clamped to. */
    public static final int MAX_CONFIGURABLE_CHUNKLOADER_TERMINALS = 4096;
    /** Upper bound the per-player chunk-loader cap is clamped to. */
    public static final int MAX_CONFIGURABLE_CHUNKLOADERS_PER_PLAYER = 4096;
    /** Total chunk-loader cap above which the config logs a lag warning. */
    public static final int WARNING_MAX_CHUNKLOADER_TERMINALS = 256;
    /** Per-player chunk-loader cap above which the config logs a lag warning. */
    public static final int WARNING_MAX_CHUNKLOADERS_PER_PLAYER = 8;

    private static volatile int maxTerminals = DEFAULT_MAX_TERMINALS;
    private static volatile int invitePermissionLevel = 0;
    private static volatile boolean allowCrossDimension = true;
    private static volatile int maxChunkloaderTerminals = DEFAULT_MAX_CHUNKLOADER_TERMINALS;
    private static volatile int maxChunkloadersPerPlayer = DEFAULT_MAX_CHUNKLOADERS_PER_PLAYER;
    private static volatile int chunkLoaderTimeoutMinutes = DEFAULT_CHUNKLOADER_TIMEOUT_MINUTES;
    private static volatile boolean chunkLoaderQueue = DEFAULT_CHUNKLOADER_QUEUE;
    private static volatile int lazyChunkRing = DEFAULT_LAZY_CHUNK_RING;

    private TerminalSettings() {
    }

    public static int getMaxTerminals() {
        return maxTerminals;
    }

    public static void setMaxTerminals(int value) {
        maxTerminals = Math.max(1, Math.min(MAX_CONFIGURABLE_TERMINALS, value));
    }

    public static int getInvitePermissionLevel() {
        return invitePermissionLevel;
    }

    public static void setInvitePermissionLevel(int value) {
        invitePermissionLevel = Math.max(0, Math.min(4, value));
    }

    public static boolean isAllowCrossDimension() {
        return allowCrossDimension;
    }

    public static void setAllowCrossDimension(boolean value) {
        allowCrossDimension = value;
    }

    public static int getMaxChunkloaderTerminals() {
        return maxChunkloaderTerminals;
    }

    public static void setMaxChunkloaderTerminals(int value) {
        maxChunkloaderTerminals = Math.max(0, Math.min(MAX_CONFIGURABLE_CHUNKLOADER_TERMINALS, value));
    }

    public static int getMaxChunkloadersPerPlayer() {
        return maxChunkloadersPerPlayer;
    }

    public static void setMaxChunkloadersPerPlayer(int value) {
        maxChunkloadersPerPlayer = Math.max(0, Math.min(MAX_CONFIGURABLE_CHUNKLOADERS_PER_PLAYER, value));
    }

    /** Lease length of a newly enabled chunk loader, in minutes; 0 means leases never expire. */
    public static int getChunkLoaderTimeoutMinutes() {
        return chunkLoaderTimeoutMinutes;
    }

    public static void setChunkLoaderTimeoutMinutes(int value) {
        chunkLoaderTimeoutMinutes = Math.max(0,
                Math.min(MAX_CONFIGURABLE_CHUNKLOADER_TIMEOUT_MINUTES, value));
    }

    /** The lease length in milliseconds, or 0 when leases never expire. */
    public static long chunkLoaderTimeoutMillis() {
        return chunkLoaderTimeoutMinutes <= 0 ? 0L : chunkLoaderTimeoutMinutes * 60_000L;
    }

    /** Whether a request blocked by the caps waits in a queue instead of being refused. */
    public static boolean isChunkLoaderQueue() {
        return chunkLoaderQueue;
    }

    public static void setChunkLoaderQueue(boolean value) {
        chunkLoaderQueue = value;
    }

    public static int getLazyChunkRing() {
        return lazyChunkRing;
    }

    public static void setLazyChunkRing(int value) {
        lazyChunkRing = Math.max(0, Math.min(MAX_CONFIGURABLE_LAZY_CHUNK_RING, value));
    }

    /** True when either chunk-loader cap is high enough to risk lag; the config logs a warning. */
    public static boolean chunkLoaderCapsAreHigh() {
        return maxChunkloaderTerminals > WARNING_MAX_CHUNKLOADER_TERMINALS
                || maxChunkloadersPerPlayer > WARNING_MAX_CHUNKLOADERS_PER_PLAYER;
    }
}
