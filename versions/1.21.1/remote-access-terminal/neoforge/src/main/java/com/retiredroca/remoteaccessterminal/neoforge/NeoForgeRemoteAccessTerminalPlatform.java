package com.retiredroca.remoteaccessterminal.neoforge;

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

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgeRemoteAccessTerminalPlatform implements RemoteAccessTerminalPlatform {
    /** Ticket keeping a terminal's chunk (and lazy ring) loaded; distinct from {@code /forceload}. */
    private static final TicketType<ChunkPos> KEPT = TicketType.create("remote_access_terminal:kept",
            Comparator.comparingLong(ChunkPos::toLong));

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public MenuType<?> terminalMenuType() {
        return Registration.MENU.get();
    }

    @Override
    public void openTerminal(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, DyeColor color,
            boolean settings) {
        Component title = Component.translatable(settings
                ? "container.remote_access_terminal.settings"
                : "container.remote_access_terminal.picker");
        TerminalMenu.Data data = new TerminalMenu.Data(dimension, pos, color, settings, false);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new TerminalMenu(containerId, inventory, dimension, pos, color,
                        settings, false),
                title), buf -> TerminalMenu.Data.STREAM_CODEC.encode(buf, data));
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
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new TerminalMenu(containerId, inventory, dimension, pos,
                        DyeColor.WHITE, false, true),
                title), buf -> TerminalMenu.Data.STREAM_CODEC.encode(buf, data));
        sync(player, data);
    }

    @Override
    public void sendOpenName(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos, DyeColor color) {
        PacketDistributor.sendToPlayer(player, new OpenNamePayload(dimension, pos, color));
    }

    private static void sync(ServerPlayer player, TerminalMenu.Data data) {
        if (player.level() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayer(player, data.linker()
                    ? TerminalSyncHelper.buildLinker(level, player, data.dimension(), data.pos())
                    : TerminalSyncHelper.build(level, player, data.color(), data.pos()));
        }
    }

    @Override
    public void sendRequestSync() {
        PacketDistributor.sendToServer(new RequestSyncPayload());
    }

    @Override
    public void sendTravel(ResourceKey<Level> targetDimension, BlockPos targetPos) {
        PacketDistributor.sendToServer(new TravelPayload(targetDimension, targetPos));
    }

    @Override
    public void sendRename(ResourceKey<Level> dimension, BlockPos pos, DyeColor color, String name) {
        PacketDistributor.sendToServer(new RenamePayload(dimension, pos, color, name));
    }

    @Override
    public void sendSetColor(DyeColor color) {
        PacketDistributor.sendToServer(new SetColorPayload(color));
    }

    @Override
    public void sendSetSort(int sortMode) {
        PacketDistributor.sendToServer(new SetSortPayload(sortMode));
    }

    @Override
    public void sendSetOpen(boolean open) {
        PacketDistributor.sendToServer(new SetOpenPayload(open));
    }

    @Override
    public void sendSetChunkLoader(boolean enabled) {
        PacketDistributor.sendToServer(new SetChunkLoaderPayload(enabled));
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
        PacketDistributor.sendToServer(new SetInvitesPayload(invites));
    }

    @Override
    public void sendUnlink() {
        PacketDistributor.sendToServer(new UnlinkPayload());
    }
}
