package com.retiredroca.mcstorageareanetwork.api;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Server-side policy flags for the item network. Set by the loader config on load/reload; read by
 * the scanners and sources when deciding which containers a terminal may see.
 */
public final class NetworkSettings {
    private static volatile boolean ownershipEnabled = true;
    private static volatile boolean teamSharing = true;
    private static volatile Set<ResourceLocation> excludedContainers = Set.of();

    /**
     * Primary storage: chest (and double chest), trapped chest, barrel and every shulker box.
     * Protected from exclusion and the only blocks the routing linker may label.
     */
    private static final Set<ResourceLocation> PRIMARY_STORAGE = buildPrimaryStorage();

    private static Set<ResourceLocation> buildPrimaryStorage() {
        Set<ResourceLocation> ids = new HashSet<>();
        ids.add(ResourceLocation.withDefaultNamespace("chest"));
        ids.add(ResourceLocation.withDefaultNamespace("trapped_chest"));
        ids.add(ResourceLocation.withDefaultNamespace("barrel"));
        ids.add(ResourceLocation.withDefaultNamespace("shulker_box"));
        for (DyeColor color : DyeColor.values()) {
            ids.add(ResourceLocation.withDefaultNamespace(color.getName() + "_shulker_box"));
        }
        return Set.copyOf(ids);
    }

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

    /** True for the primary storage blocks: chest, trapped chest, barrel and any shulker box. */
    public static boolean isStorageContainer(ResourceLocation blockId) {
        return blockId != null && PRIMARY_STORAGE.contains(blockId);
    }

    /** Chests, trapped chests and barrels always stay in the network. */
    public static boolean isProtectedContainer(ResourceLocation blockId) {
        return isStorageContainer(blockId);
    }

    /** The current excluded set (immutable). */
    public static Set<ResourceLocation> excludedContainers() {
        return excludedContainers;
    }
}
