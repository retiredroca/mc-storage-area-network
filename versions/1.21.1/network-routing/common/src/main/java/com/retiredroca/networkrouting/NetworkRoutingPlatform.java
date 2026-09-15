package com.retiredroca.networkrouting;

import java.nio.file.Path;
import java.util.List;

import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-specific services used by the loader-neutral Network Routing code. */
public interface NetworkRoutingPlatform {
    MenuType<?> menuType();

    BlockEntityType<?> terminalBlockEntityType();

    Item terminalItem();

    Item linkerItem();

    RoutingTerminalBlock terminalBlock();

    AbstractRoutingTerminalBlockEntity createTerminalBlockEntity(BlockPos pos, BlockState state);

    /** Opens the routing screen for a routing terminal ({@code terminal}) or a labeled container. */
    void openMenu(ServerPlayer player, BlockPos pos, boolean terminal);

    /** Send the terminal snapshot (containers, toggles, bound host) to the player. */
    void sendSync(ServerPlayer player, RoutingSyncPayload payload);

    /** C2S: overwrite the filter tokens of a container. */
    void sendSetFilter(BlockPos origin, BlockPos containerPos, List<String> tokens, boolean terminal);

    /** C2S: set the terminal's toggle flags (see {@code RoutingMenu.FLAG_*}). */
    void sendToggle(BlockPos terminalPos, int flags);

    /** C2S: run an action (sort/defrag/trim now). */
    void sendAction(BlockPos terminalPos, int action);

    boolean isServerModded();

    Path configDir();
}
