package com.retiredroca.remoteaccessterminal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Carries the placing player from {@code setPlacedBy} to the block's {@code onPlace} link
 * registration. Which of the two runs first differs between loaders, so this holds a pending owner
 * keyed by colour, dimension and position until the link exists, and is consumed by the registration.
 */
public final class PlacementContext {
    private static final Map<Key, UUID> PENDING = new ConcurrentHashMap<>();

    private PlacementContext() {
    }

    /** Identifies one terminal place by dye, dimension and exact block position. */
    public record Key(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** Records the placing player for the terminal at the given location. */
    public static void put(DyeColor color, ResourceKey<Level> dimension, BlockPos pos, UUID owner) {
        PENDING.put(new Key(color, dimension, pos), owner);
    }

    /** Consumes the placing player for the terminal at the given location, or {@code null}. */
    public static UUID take(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        return PENDING.remove(new Key(color, dimension, pos));
    }

    /** Reads the placing player for the terminal at the given location without consuming it. */
    public static UUID peek(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        return PENDING.get(new Key(color, dimension, pos));
    }
}
