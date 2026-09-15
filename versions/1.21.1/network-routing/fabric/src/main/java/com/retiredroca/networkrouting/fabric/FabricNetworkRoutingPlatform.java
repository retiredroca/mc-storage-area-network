package com.retiredroca.networkrouting.fabric;

import java.nio.file.Path;
import java.util.List;

import com.retiredroca.networkrouting.NetworkRoutingPlatform;
import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;
import com.retiredroca.networkrouting.network.RoutingPackets.ActionPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.FilterPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.TogglePayload;
import com.retiredroca.networkrouting.routing.RoutingSyncHelper;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricNetworkRoutingPlatform implements NetworkRoutingPlatform {
    @Override
    public MenuType<?> menuType() {
        return Registration.MENU;
    }

    @Override
    public BlockEntityType<?> terminalBlockEntityType() {
        return Registration.TERMINAL_BE;
    }

    @Override
    public Item terminalItem() {
        return Registration.TERMINAL_ITEM;
    }

    @Override
    public Item linkerItem() {
        return Registration.LINKER_ITEM;
    }

    @Override
    public RoutingTerminalBlock terminalBlock() {
        return Registration.TERMINAL_BLOCK;
    }

    @Override
    public AbstractRoutingTerminalBlockEntity createTerminalBlockEntity(BlockPos pos, BlockState state) {
        return new RoutingTerminalBlockEntity(pos, state);
    }

    @Override
    public void openMenu(ServerPlayer player, BlockPos pos, boolean terminal) {
        Component title = Component.translatable(terminal
                ? "container.network_routing.routing_terminal"
                : "container.network_routing.routing_label");
        player.openMenu(new RoutingMenuOpener(pos, title));
        sync(player, player.serverLevel(), pos, terminal);
    }

    private static void sync(ServerPlayer player, ServerLevel level, BlockPos pos, boolean terminal) {
        if (terminal) {
            if (level.getBlockEntity(pos) instanceof AbstractRoutingTerminalBlockEntity be) {
                ServerPlayNetworking.send(player, be.buildSync());
            }
        } else {
            ServerPlayNetworking.send(player, RoutingSyncHelper.linker(level, pos));
        }
    }

    @Override
    public void sendSync(ServerPlayer player, RoutingSyncPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendSetFilter(BlockPos origin, BlockPos containerPos, List<String> tokens, boolean terminal) {
        ClientPlayNetworking.send(new FilterPayload(origin, containerPos, tokens, terminal));
    }

    @Override
    public void sendToggle(BlockPos terminalPos, int flags) {
        ClientPlayNetworking.send(new TogglePayload(terminalPos, flags));
    }

    @Override
    public void sendAction(BlockPos terminalPos, int action) {
        ClientPlayNetworking.send(new ActionPayload(terminalPos, action));
    }

    @Override
    public boolean isServerModded() {
        return Networking.isServerModded();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
