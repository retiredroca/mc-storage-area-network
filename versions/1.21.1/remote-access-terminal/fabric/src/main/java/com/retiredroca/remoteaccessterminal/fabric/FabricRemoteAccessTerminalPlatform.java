package com.retiredroca.remoteaccessterminal.fabric;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalPlatform;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.OpenNamePayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.RenamePayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.RequestSyncPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetChunkLoaderPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetColorPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetOpenPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetInvitesPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetSortPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TravelPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.UnlinkPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalSyncHelper;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class FabricRemoteAccessTerminalPlatform implements RemoteAccessTerminalPlatform {
    /** Ticket keeping a terminal's chunk (and lazy ring) loaded; distinct from {@code /forceload}. */
    private static final TicketType<ChunkPos> KEPT = TicketType.create("remote_access_terminal:kept",
            Comparator.comparingLong(ChunkPos::toLong));

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public MenuType<?> terminalMenuType() {
        return Registration.MENU;
    }

    @Override
    public void openTerminal(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, DyeColor color,
            boolean settings) {
        Component title = Component.translatable(settings
                ? "container.remote_access_terminal.settings"
                : "container.remote_access_terminal.picker");
        TerminalMenu.Data data = new TerminalMenu.Data(dimension, pos, color, settings, false);
        player.openMenu(new TerminalMenuOpener(data, title));
        sync(player, data);
    }

    @Override
    public void openRoutingLinker(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = player.blockPosition();
        ResourceKey<Level> dimension = level.dimension();
        Component title = Component.translatable("container.remote_access_terminal.linker");
        TerminalMenu.Data data = new TerminalMenu.Data(dimension, pos, DyeColor.WHITE, false, true);
        player.openMenu(new TerminalMenuOpener(data, title));
        sync(player, data);
    }

    @Override
    public void sendOpenName(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, DyeColor color) {
        ServerPlayNetworking.send(player, new OpenNamePayload(dimension, pos, color));
    }

    private static void sync(ServerPlayer player, TerminalMenu.Data data) {
        if (player.level() instanceof ServerLevel level) {
            ServerPlayNetworking.send(player, data.linker()
                    ? TerminalSyncHelper.buildLinker(level, player, data.dimension(), data.pos())
                    : TerminalSyncHelper.build(level, player, data.color(), data.pos()));
        }
    }

    @Override
    public void sendRequestSync() {
        ClientPlayNetworking.send(new RequestSyncPayload());
    }

    @Override
    public void sendTravel(ResourceKey<Level> targetDimension, BlockPos targetPos) {
        ClientPlayNetworking.send(new TravelPayload(targetDimension, targetPos));
    }

    @Override
    public void sendRename(ResourceKey<Level> dimension, BlockPos pos, DyeColor color, String name) {
        ClientPlayNetworking.send(new RenamePayload(dimension, pos, color, name));
    }

    @Override
    public void sendSetColor(DyeColor color) {
        ClientPlayNetworking.send(new SetColorPayload(color));
    }

    @Override
    public void sendSetSort(int sortMode) {
        ClientPlayNetworking.send(new SetSortPayload(sortMode));
    }

    @Override
    public void sendSetOpen(boolean open) {
        ClientPlayNetworking.send(new SetOpenPayload(open));
    }

    @Override
    public void sendSetChunkLoader(boolean enabled) {
        ClientPlayNetworking.send(new SetChunkLoaderPayload(enabled));
    }

    @Override
    public void addChunkTicket(ServerLevel level, ChunkPos pos, int distance) {
        level.getChunkSource().addRegionTicket(KEPT, pos, distance, pos);
    }

    @Override
    public void removeChunkTicket(ServerLevel level, ChunkPos pos, int distance) {
        level.getChunkSource().removeRegionTicket(KEPT, pos, distance, pos);
    }

    @Override
    public void sendSetInvites(List<UUID> invites) {
        ClientPlayNetworking.send(new SetInvitesPayload(invites));
    }

    @Override
    public void sendUnlink() {
        ClientPlayNetworking.send(new UnlinkPayload());
    }
}
