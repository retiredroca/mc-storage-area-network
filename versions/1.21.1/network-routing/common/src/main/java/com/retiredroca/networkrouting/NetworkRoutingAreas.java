package com.retiredroca.networkrouting;

import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.NetworkHost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Ranged lookups for the areas currently served by Network Routing terminals.
 *
 * <p>Each routing terminal publishes the host it is bound to (position, chunk radius and tier) under
 * its own position whenever it re-binds, and clears it when the binding is dropped. The data is
 * persisted per level by {@link RoutingBindings}, so the areas are known even while the terminal (or
 * its host) is unloaded; this class is just the typed view over that store.
 */
public final class NetworkRoutingAreas {
    private NetworkRoutingAreas() {}

    /** Publishes (or refreshes) the area served by the terminal at {@code terminalPos}. */
    public static void update(ServerLevel level, BlockPos terminalPos, NetworkHost host) {
        RoutingBindings.set(level, terminalPos, host.pos(), host.chunkRadius(), host.tier());
    }

    /** Clears the area published by the terminal at {@code terminalPos}. */
    public static void remove(ServerLevel level, BlockPos terminalPos) {
        RoutingBindings.remove(level, terminalPos);
    }

    /** The areas stored for {@code level} (immutable; empty when none). */
    public static List<Area> activeAreas(ServerLevel level) {
        return RoutingBindings.all(level);
    }

    /** True when {@code pos}'s chunk lies within {@code chunkRadius} chunks of any area's host. */
    public static boolean isInside(BlockPos pos, List<Area> areas) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (Area area : areas) {
            if (Math.abs(cx - (area.hostPos().getX() >> 4)) <= area.chunkRadius()
                    && Math.abs(cz - (area.hostPos().getZ() >> 4)) <= area.chunkRadius()) {
                return true;
            }
        }
        return false;
    }

    /** The host an area mirrors: its position, scan radius and tier. */
    public record Area(BlockPos hostPos, int chunkRadius, int tier) {}
}
