package com.retiredroca.networkrouting.neoforge;

import java.nio.file.Path;
import java.util.List;

import com.retiredroca.networkrouting.NetworkRoutingPlatform;
import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;
import com.retiredroca.networkrouting.menu.RoutingMenu;
import com.retiredroca.networkrouting.network.RoutingPackets.ActionPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.FilterPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.TogglePayload;
import com.retiredroca.networkrouting.routing.RoutingSyncHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgeNetworkRoutingPlatform implements NetworkRoutingPlatform {
    @Override
    public MenuType<?> menuType() {
        return Registration.MENU.get();
    }

    @Override
    public BlockEntityType<?> terminalBlockEntityType() {
        return Registration.TERMINAL_BE.get();
    }

    @Override
    public Item terminalItem() {
        return Registration.TERMINAL_ITEM.get();
    }

    @Override
    public Item linkerItem() {
        return Registration.LINKER_ITEM.get();
    }

    @Override
    public RoutingTerminalBlock terminalBlock() {
        return Registration.TERMINAL_BLOCK.get();
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
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new RoutingMenu(id, inventory, pos), title),
                buf -> buf.writeBlockPos(pos));
        sync(player, player.serverLevel(), pos, terminal);
    }

    private static void sync(ServerPlayer player, ServerLevel level, BlockPos pos, boolean terminal) {
        if (terminal) {
            if (level.getBlockEntity(pos) instanceof AbstractRoutingTerminalBlockEntity be) {
                PacketDistributor.sendToPlayer(player, be.buildSync());
            }
        } else {
            PacketDistributor.sendToPlayer(player, RoutingSyncHelper.linker(level, pos));
        }
    }

    @Override
    public void sendSync(ServerPlayer player, RoutingSyncPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendSetFilter(BlockPos origin, BlockPos containerPos, List<String> tokens, boolean terminal) {
        PacketDistributor.sendToServer(new FilterPayload(origin, containerPos, tokens, terminal));
    }

    @Override
    public void sendToggle(BlockPos terminalPos, int flags) {
        PacketDistributor.sendToServer(new TogglePayload(terminalPos, flags));
    }

    @Override
    public void sendAction(BlockPos terminalPos, int action) {
        PacketDistributor.sendToServer(new ActionPayload(terminalPos, action));
    }

    @Override
    public boolean isServerModded() {
        return Networking.isServerModded();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
