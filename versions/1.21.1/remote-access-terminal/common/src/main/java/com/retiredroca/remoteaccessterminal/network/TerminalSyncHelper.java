package com.retiredroca.remoteaccessterminal.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.mcstorageareanetwork.api.capability.TerminalRegistry;
import com.retiredroca.remoteaccessterminal.RoutingLinker;
import com.retiredroca.remoteaccessterminal.SortMode;
import com.retiredroca.remoteaccessterminal.TerminalLinks;
import com.retiredroca.remoteaccessterminal.TerminalLinksAccess;
import com.retiredroca.remoteaccessterminal.TerminalPermissions;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.Destination;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/** Builds the terminal snapshot and the ordered destination list for the picker. */
public final class TerminalSyncHelper {
    private TerminalSyncHelper() {
    }

    /** The full snapshot for the terminal at {@code pos}, as seen by {@code player}. */
    public static TerminalSyncPayload build(ServerLevel level, ServerPlayer player, DyeColor color, BlockPos pos) {
        TerminalLinks links = TerminalLinksAccess.get(level).links();
        TerminalLinks.Link link = links.find(color, level.dimension(), pos);
        if (link == null) {
            return new TerminalSyncPayload(level.dimension(), pos, color, null, false, false, 0, 0, false,
                    SortMode.NEAREST.ordinal(), List.of(), counts(links), links.totalCount(), List.of());
        }
        List<Destination> destinations = new ArrayList<>();
        for (TerminalLinks.Link other : links.usableFor(level, player, color)) {
            if (other == link) {
                continue;
            }
            destinations.add(new Destination(other.name() == null ? "" : other.name(), color, other.dimension(),
                    other.pos()));
        }
        destinations.sort(comparator(link, level.dimension()));
        Set<UUID> invites = link.owner() == null ? Set.of() : NetworkPermissions.invitesOf(level, link.owner());
        long until = link.isChunkLoader() ? link.chunkLoaderUntil() : 0L;
        return new TerminalSyncPayload(level.dimension(), pos, color, link.name(),
                NetworkPermissions.isOpen(level, pos), link.isChunkLoader(), until, links.queuePosition(link),
                TerminalPermissions.canEdit(level, color, pos, player), link.sortMode().ordinal(), destinations,
                counts(links), links.totalCount(), List.copyOf(invites));
    }

    /**
     * The snapshot for the Routing Linker picker: the destinations the routing provider offers, with
     * no terminal block backing it.
     */
    public static TerminalSyncPayload buildLinker(ServerLevel level, ServerPlayer player,
            ResourceKey<Level> dimension, BlockPos pos) {
        List<Destination> destinations = new ArrayList<>();
        for (TerminalRegistry.Terminal terminal : RoutingLinker.destinations(player)) {
            destinations.add(new Destination(terminal.name() == null ? "" : terminal.name(),
                    DyeColor.byName(terminal.color(), DyeColor.WHITE), terminal.dimension(), terminal.pos()));
        }
        return new TerminalSyncPayload(dimension, pos, DyeColor.WHITE, null, false, false, 0, 0, false,
                SortMode.NEAREST.ordinal(), destinations, List.of(), 0, List.of());
    }

    private static List<Integer> counts(TerminalLinks links) {
        List<Integer> counts = new ArrayList<>(DyeColor.values().length);
        for (DyeColor color : DyeColor.values()) {
            counts.add(links.usedCount(color));
        }
        return counts;
    }

    private static Comparator<Destination> comparator(TerminalLinks.Link link, ResourceKey<Level> dimension) {
        return switch (link.sortMode()) {
            case NEAREST -> Comparator
                    .comparingDouble((Destination destination) -> distanceSq(link, dimension, destination))
                    .thenComparing(destination -> destination.name(), String.CASE_INSENSITIVE_ORDER);
            case COLOUR -> Comparator
                    .comparingInt((Destination destination) -> destination.color().getId())
                    .thenComparing(destination -> destination.name(), String.CASE_INSENSITIVE_ORDER);
            case NAME -> Comparator.comparing(destination -> destination.name(), String.CASE_INSENSITIVE_ORDER);
        };
    }

    private static double distanceSq(TerminalLinks.Link link, ResourceKey<Level> dimension, Destination destination) {
        if (!destination.dimension().equals(dimension)) {
            return Double.MAX_VALUE;
        }
        return link.pos().distSqr(destination.pos());
    }
}
