package com.retiredroca.mcstorageareanetwork.api;

import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/**
 * Server-side policy flags for the item network. Set by the loader config on load/reload; read by
 * the scanners and sources when deciding which containers a terminal may see.
 */
public final class NetworkSettings {
    private static volatile boolean ownershipEnabled = true;
    private static volatile boolean teamSharing = true;
    private static volatile Set<ResourceLocation> excludedContainers = Set.of();

    /** Container block types that always stay in the network, even if listed as excluded. */
    private static final Set<ResourceLocation> PROTECTED_CONTAINERS = Set.of(
            ResourceLocation.withDefaultNamespace("chest"),
            ResourceLocation.withDefaultNamespace("trapped_chest"),
            ResourceLocation.withDefaultNamespace("barrel"));

    private NetworkSettings() {}

    public static void configure(boolean ownership, boolean teams) {
        ownershipEnabled = ownership;
        teamSharing = teams;
    }

    /** Replaces the set of container block types excluded from every network. */
    public static void configureExcludedContainers(Set<ResourceLocation> excluded) {
        excludedContainers = excluded == null ? Set.of() : Set.copyOf(excluded);
    }

    /** When true, terminals only see global (unowned) storage plus storage their owner placed. */
    public static boolean ownershipEnabled() {
        return ownershipEnabled;
    }

    /** When true, players on the same scoreboard team share their placed storage. */
    public static boolean teamSharing() {
        return teamSharing;
    }

    /** True if the container block type is excluded from the network (protected types never are). */
    public static boolean isContainerExcluded(ResourceLocation blockId) {
        return blockId != null && !isProtectedContainer(blockId) && excludedContainers.contains(blockId);
    }

    /** Chests, trapped chests and barrels always stay in the network. */
    public static boolean isProtectedContainer(ResourceLocation blockId) {
        return blockId != null && PROTECTED_CONTAINERS.contains(blockId);
    }

    /** The current excluded set (immutable). */
    public static Set<ResourceLocation> excludedContainers() {
        return excludedContainers;
    }
}
