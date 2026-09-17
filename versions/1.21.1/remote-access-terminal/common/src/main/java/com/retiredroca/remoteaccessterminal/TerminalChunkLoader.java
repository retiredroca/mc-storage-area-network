package com.retiredroca.remoteaccessterminal;

import java.util.UUID;

import com.retiredroca.mcstorageareanetwork.api.NetworkAwareness;
import com.retiredroca.mcstorageareanetwork.api.SisterMods;
import com.retiredroca.remoteaccessterminal.config.TerminalSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;

/**
 * Applies and releases the chunk tickets backing a terminal's "chunk loader" mode, and enforces the
 * configured caps.
 *
 * <p>Tickets are not persisted, so {@link #reapplyAll} restores them from the saved link store on
 * server start. A terminal's own chunk is kept fully ticking, and the lazy ring around it is kept
 * loaded without ticking. The ring defaults to the 3x3 footprint (the 8 neighbours); it may only grow
 * when the crafting sister mod is present, and is then capped at the server's simulation distance.
 * The configured value is stored intact; {@link #effectiveRing} applies the clamp where the tickets
 * are computed, so application and release always use the same radius.</p>
 *
 * <p>A lease also carries a holder and an expiry ({@code chunkLoaderTimeoutMinutes}, 0 = never).
 * {@link #serverTick} expires leases once a second, and requests blocked by the caps wait in a queue
 * (oldest first, one request per player) when {@code chunkLoaderQueue} is enabled; releasing or
 * losing a lease promotes the queue head instead of leaving the slot idle.</p>
 */
public final class TerminalChunkLoader {
    /** Ticket distance that keeps the terminal's own chunk fully ticking. */
    private static final int TICKING_DISTANCE = 2;
    /** Ticket distance that keeps a lazy-ring chunk loaded without ticking it. */
    private static final int LAZY_DISTANCE = 0;
    /** Ticks between lease checks, so the scan runs once a second instead of once a tick. */
    private static final int TICK_INTERVAL = 20;

    private TerminalChunkLoader() {
    }

    /**
     * The radius actually used for the lazy ring: the configured value, but never more than 1 without
     * the crafting sister mod, and never more than the server's simulation distance. Application and
     * release both call this, so they always cover exactly the same chunks.
     */
    private static int effectiveRing(ServerLevel level) {
        int cap = NetworkAwareness.isPresent(SisterMods.CRAFTING) ? simulationDistance(level) : 1;
        return Math.max(0, Math.min(TerminalSettings.getLazyChunkRing(), cap));
    }

    private static int simulationDistance(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server == null ? 1 : server.getPlayerList().getSimulationDistance();
    }

    /** Applies the tickets for an enabled terminal at {@code pos}. */
    public static void apply(ServerLevel level, BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        RemoteAccessTerminalCommon.platform().addChunkTicket(level, center, TICKING_DISTANCE);
        int radius = effectiveRing(level);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                RemoteAccessTerminalCommon.platform()
                        .addChunkTicket(level, new ChunkPos(center.x + dx, center.z + dz), LAZY_DISTANCE);
            }
        }
    }

    /** Releases the tickets applied by {@link #apply} for the terminal at {@code pos}. */
    public static void release(ServerLevel level, BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        RemoteAccessTerminalCommon.platform().removeChunkTicket(level, center, TICKING_DISTANCE);
        int radius = effectiveRing(level);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                RemoteAccessTerminalCommon.platform()
                        .removeChunkTicket(level, new ChunkPos(center.x + dx, center.z + dz), LAZY_DISTANCE);
            }
        }
    }

    /** Clears the lease and queue state and releases tickets while a link is being removed. */
    public static void deactivate(ServerLevel level, TerminalLinks.Link link) {
        if (link == null) {
            return;
        }
        if (link.isChunkLoader()) {
            release(level, link.pos());
        }
        clearState(link);
    }

    /**
     * Drops the lease (or queued request) of {@code link} and hands the slot to the queue head.
     * Used by the admin {@code chunkloader clear} command.
     */
    public static void clear(ServerLevel level, TerminalLinks.Link link) {
        if (link == null) {
            return;
        }
        if (link.isChunkLoader()) {
            release(level, link.pos());
        }
        clearState(link);
        MinecraftServer server = level.getServer();
        if (server != null) {
            promoteQueued(server);
        }
    }

    private static void clearState(TerminalLinks.Link link) {
        link.setChunkLoader(false);
        link.setChunkLoaderHolder(null);
        link.setChunkLoaderUntil(0);
        link.setChunkLoaderQueuedSince(0);
    }

    /**
     * Toggles chunk loading for the terminal, enforcing the configured caps. A refusal is reported to
     * {@code player} on the action bar and leaves the flag unchanged.
     */
    public static void setEnabled(ServerLevel level, DyeColor color, BlockPos pos, ServerPlayer player,
            boolean enabled) {
        TerminalLinksAccess access = TerminalLinksAccess.get(level);
        TerminalLinks links = access.links();
        TerminalLinks.Link link = links.find(color, level.dimension(), pos);
        if (link == null) {
            return;
        }
        if (enabled) {
            request(level, links, access, link, player);
        } else {
            cancel(level, links, access, link, player);
        }
    }

    /** Grants the lease when a slot is free, otherwise queues the request or explains the refusal. */
    private static void request(ServerLevel level, TerminalLinks links, TerminalLinksAccess access,
            TerminalLinks.Link link, ServerPlayer player) {
        if (link.holdsChunkLoader()) {
            notify(player, onMessage(link));
            return;
        }
        if (link.isChunkLoaderQueued()) {
            notify(player, Component.translatable(
                    "message.remote_access_terminal.chunkloader_queued_already", links.queuePosition(link)));
            return;
        }
        UUID playerId = player.getUUID();
        int totalCap = TerminalSettings.getMaxChunkloaderTerminals();
        int playerCap = TerminalSettings.getMaxChunkloadersPerPlayer();
        boolean globalFree = links.chunkLoaderCount() < totalCap;
        boolean playerFree = links.chunkLoaderCount(playerId) < playerCap;
        if (globalFree && playerFree) {
            grant(level, link, playerId);
            access.setDirty();
            notify(player, onMessage(link));
            return;
        }
        if (TerminalSettings.isChunkLoaderQueue()) {
            TerminalLinks.Link existing = queuedRequest(links, playerId);
            if (existing != null) {
                notify(player, Component.translatable(
                        "message.remote_access_terminal.chunkloader_queued_already",
                        links.queuePosition(existing)));
                return;
            }
            link.setChunkLoaderHolder(playerId);
            link.setChunkLoaderQueuedSince(System.currentTimeMillis());
            access.setDirty();
            notify(player, Component.translatable(
                    "message.remote_access_terminal.chunkloader_queued", links.queuePosition(link)));
            return;
        }
        if (!globalFree) {
            notify(player, Component.translatable(
                    "message.remote_access_terminal.chunkloader_limit_global", totalCap));
        } else {
            notify(player, Component.translatable(
                    "message.remote_access_terminal.chunkloader_limit_player", playerCap));
        }
    }

    /** Releases a held lease (promoting the queue head) or drops a queued request. */
    private static void cancel(ServerLevel level, TerminalLinks links, TerminalLinksAccess access,
            TerminalLinks.Link link, ServerPlayer player) {
        if (link.isChunkLoaderQueued()) {
            clearState(link);
            access.setDirty();
            notify(player, Component.translatable(
                    "message.remote_access_terminal.chunkloader_queue_cancelled"));
            return;
        }
        if (!link.isChunkLoader()) {
            return;
        }
        release(level, link.pos());
        clearState(link);
        access.setDirty();
        notify(player, Component.translatable("message.remote_access_terminal.chunkloader_off"));
        MinecraftServer server = level.getServer();
        if (server != null) {
            promoteQueued(server);
        }
    }

    /** Activates a lease for {@code holder}, applying the configured expiry. */
    private static void grant(ServerLevel level, TerminalLinks.Link link, UUID holder) {
        link.setChunkLoader(true);
        link.setChunkLoaderHolder(holder);
        link.setChunkLoaderUntil(expiry(System.currentTimeMillis()));
        link.setChunkLoaderQueuedSince(0);
        apply(level, link.pos());
    }

    private static long expiry(long now) {
        long timeout = TerminalSettings.chunkLoaderTimeoutMillis();
        return timeout <= 0 ? 0 : now + timeout;
    }

    /** The player's other queued request, or {@code null} when they have none. */
    private static TerminalLinks.Link queuedRequest(TerminalLinks links, UUID playerId) {
        for (TerminalLinks.Link candidate : links.queuedLinks()) {
            if (playerId.equals(TerminalLinks.holderOf(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Expires finished leases and promotes queued requests; runs once a second from the server tick.
     */
    public static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % TICK_INTERVAL != 0) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        TerminalLinksAccess access = TerminalLinksAccess.get(overworld);
        TerminalLinks links = access.links();
        long now = System.currentTimeMillis();
        long timeout = TerminalSettings.chunkLoaderTimeoutMillis();
        boolean changed = false;
        if (timeout > 0) {
            for (TerminalLinks.Link link : links.allLinks()) {
                if (!link.holdsChunkLoader() || link.chunkLoaderUntil() <= 0
                        || now < link.chunkLoaderUntil()) {
                    continue;
                }
                UUID holder = link.chunkLoaderHolder();
                ServerLevel level = server.getLevel(link.dimension());
                if (level != null) {
                    release(level, link.pos());
                }
                clearState(link);
                changed = true;
                notifyHolder(server, holder, Component.translatable(
                        "message.remote_access_terminal.chunkloader_expired"));
            }
        }
        if (promoteQueued(server, links, now)) {
            changed = true;
        }
        if (changed) {
            access.setDirty();
        }
    }

    /** Grants the oldest queued requests whose dimension is available; see {@link #serverTick}. */
    private static boolean promoteQueued(MinecraftServer server, TerminalLinks links, long now) {
        int totalCap = TerminalSettings.getMaxChunkloaderTerminals();
        int playerCap = TerminalSettings.getMaxChunkloadersPerPlayer();
        boolean changed = false;
        while (links.chunkLoaderCount() < totalCap) {
            TerminalLinks.Link target = null;
            UUID holder = null;
            ServerLevel level = null;
            for (TerminalLinks.Link candidate : links.queuedLinks()) {
                UUID requester = TerminalLinks.holderOf(candidate);
                if (requester == null || links.chunkLoaderCount(requester) >= playerCap) {
                    continue;
                }
                ServerLevel candidateLevel = server.getLevel(candidate.dimension());
                if (candidateLevel == null) {
                    continue;
                }
                target = candidate;
                holder = requester;
                level = candidateLevel;
                break;
            }
            if (target == null) {
                break;
            }
            grant(level, target, holder);
            changed = true;
            notifyHolder(server, holder, Component.translatable(
                    "message.remote_access_terminal.chunkloader_promoted",
                    remainingLabel(target.chunkLoaderUntil())));
        }
        return changed;
    }

    /** Promotes queued requests after a slot was released outside the tick loop. */
    public static void promoteQueued(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        TerminalLinksAccess access = TerminalLinksAccess.get(overworld);
        if (promoteQueued(server, access.links(), System.currentTimeMillis())) {
            access.setDirty();
        }
    }

    /** Reports the player's lease or queue position when they log in. */
    public static void playerJoined(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || server.overworld() == null) {
            return;
        }
        TerminalLinks links = TerminalLinksAccess.get(server.overworld()).links();
        UUID playerId = player.getUUID();
        for (TerminalLinks.Link link : links.allLinks()) {
            if (!playerId.equals(TerminalLinks.holderOf(link))) {
                continue;
            }
            if (link.holdsChunkLoader()) {
                notify(player, onMessage(link));
            } else if (link.isChunkLoaderQueued()) {
                notify(player, Component.translatable(
                        "message.remote_access_terminal.chunkloader_queued", links.queuePosition(link)));
            }
        }
    }

    /** Restores tickets for every flagged link after a restart; tickets themselves are not persisted. */
    public static void reapplyAll(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        TerminalLinksAccess access = TerminalLinksAccess.get(overworld);
        TerminalLinks links = access.links();
        long now = System.currentTimeMillis();
        long timeout = TerminalSettings.chunkLoaderTimeoutMillis();
        boolean changed = false;
        for (TerminalLinks.Link link : links.allLinks()) {
            if (!link.isChunkLoader()) {
                continue;
            }
            if (timeout > 0 && link.chunkLoaderUntil() > 0 && now >= link.chunkLoaderUntil()) {
                clearState(link);
                changed = true;
                continue;
            }
            ServerLevel level = server.getLevel(link.dimension());
            if (level != null) {
                apply(level, link.pos());
            }
        }
        if (changed) {
            access.setDirty();
        }
        promoteQueued(server);
    }

    /** The action-bar message describing an active lease. */
    private static Component onMessage(TerminalLinks.Link link) {
        long until = link.chunkLoaderUntil();
        if (until <= 0) {
            return Component.translatable("message.remote_access_terminal.chunkloader_on");
        }
        return Component.translatable("message.remote_access_terminal.chunkloader_on_time",
                remainingLabel(until));
    }

    /** Formats the time left on a lease as {@code 42m} or {@code 1h 5m}. */
    public static String remainingLabel(long untilMillis) {
        long minutes = Math.max(1L, (untilMillis - System.currentTimeMillis() + 59_999L) / 60_000L);
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours <= 0) {
            return minutes + "m";
        }
        return rest == 0 ? hours + "h" : hours + "h " + rest + "m";
    }

    private static void notifyHolder(MinecraftServer server, UUID holder, Component message) {
        if (holder == null) {
            return;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(holder);
        if (online != null) {
            notify(online, message);
        }
    }

    private static void notify(ServerPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }
}
