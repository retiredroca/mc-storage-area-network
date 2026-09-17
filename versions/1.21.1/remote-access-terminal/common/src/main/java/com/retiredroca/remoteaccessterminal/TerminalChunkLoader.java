package com.retiredroca.remoteaccessterminal;

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
 */
public final class TerminalChunkLoader {
    /** Ticket distance that keeps the terminal's own chunk fully ticking. */
    private static final int TICKING_DISTANCE = 2;
    /** Ticket distance that keeps a lazy-ring chunk loaded without ticking it. */
    private static final int LAZY_DISTANCE = 0;

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

    /** Clears the flag and releases tickets for a link that is being removed or moved. */
    public static void deactivate(ServerLevel level, TerminalLinks.Link link) {
        if (link == null || !link.isChunkLoader()) {
            return;
        }
        link.setChunkLoader(false);
        release(level, link.pos());
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
        if (link == null || link.isChunkLoader() == enabled) {
            return;
        }
        if (enabled) {
            int totalCap = TerminalSettings.getMaxChunkloaderTerminals();
            if (links.chunkLoaderCount() >= totalCap) {
                player.displayClientMessage(Component.translatable(
                        "message.remote_access_terminal.chunkloader_limit_global", totalCap), true);
                return;
            }
            int playerCap = TerminalSettings.getMaxChunkloadersPerPlayer();
            if (link.owner() != null && links.chunkLoaderCount(link.owner()) >= playerCap) {
                player.displayClientMessage(Component.translatable(
                        "message.remote_access_terminal.chunkloader_limit_player", playerCap), true);
                return;
            }
        }
        link.setChunkLoader(enabled);
        access.setDirty();
        if (enabled) {
            apply(level, pos);
        } else {
            release(level, pos);
        }
    }

    /** Restores tickets for every flagged link after a restart; tickets themselves are not persisted. */
    public static void reapplyAll(MinecraftServer server) {
        TerminalLinks links = TerminalLinksAccess.get(server.overworld()).links();
        for (TerminalLinks.Link link : links.allLinks()) {
            if (!link.isChunkLoader()) {
                continue;
            }
            ServerLevel level = server.getLevel(link.dimension());
            if (level != null) {
                apply(level, link.pos());
            }
        }
    }
}
