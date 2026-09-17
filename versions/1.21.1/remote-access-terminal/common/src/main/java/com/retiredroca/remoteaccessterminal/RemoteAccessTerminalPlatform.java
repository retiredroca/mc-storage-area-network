package com.retiredroca.remoteaccessterminal;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * The loader services the shared sources need.
 *
 * <p>Implementations are installed by the loader initializers through
 * {@link RemoteAccessTerminalCommon#setPlatform(RemoteAccessTerminalPlatform)}.
 */
public interface RemoteAccessTerminalPlatform {
    /** Directory the running loader keeps config files in. */
    Path configDir();

    /** The registered menu type for {@code TerminalMenu}. */
    MenuType<?> terminalMenuType();

    /** Opens the terminal menu (picker, or settings when {@code settings}) and pushes its snapshot. */
    void openTerminal(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, DyeColor color,
            boolean settings);

    /** Opens the Routing Linker destination picker (linker mode) and pushes its snapshot. */
    void openRoutingLinker(ServerPlayer player);

    /** Asks the client to open the naming popup for a just-placed terminal. */
    void sendOpenName(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, DyeColor color);

    /** C2S: re-request the currently open terminal menu's snapshot. */
    void sendRequestSync();

    /** C2S: travel to a destination of the currently open terminal menu. */
    void sendTravel(ResourceKey<Level> targetDimension, BlockPos targetPos);

    /** C2S: rename the terminal at the given dimension, position and dye. */
    void sendRename(ResourceKey<Level> dimension, BlockPos pos, DyeColor color, String name);

    /** C2S: change the dye of the currently open terminal menu. */
    void sendSetColor(DyeColor color);

    /** C2S: set the sort mode of the currently open terminal menu. */
    void sendSetSort(int sortMode);

    /** C2S: set the open/private flag of the currently open terminal menu. */
    void sendSetOpen(boolean open);

    /** C2S: set the chunk-loader flag of the currently open terminal menu. */
    void sendSetChunkLoader(boolean enabled);

    /**
     * Adds a region ticket keeping {@code pos}'s chunk loaded at {@code distance} (2 = entity-ticking,
     * 1 = block-ticking, 0 = loaded but not ticking).
     */
    void addChunkTicket(ServerLevel level, ChunkPos pos, int distance);

    /** Releases a region ticket previously added by {@link #addChunkTicket}. */
    void removeChunkTicket(ServerLevel level, ChunkPos pos, int distance);

    /** C2S: replace the invitees of the terminal owner of the currently open terminal menu. */
    void sendSetInvites(List<UUID> invites);

    /** C2S: remove the link record of the currently open terminal menu, leaving the block in place. */
    void sendUnlink();
}
