package com.retiredroca.remoteaccessterminal.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.remoteaccessterminal.RoutingLinker;
import com.retiredroca.remoteaccessterminal.SortMode;
import com.retiredroca.remoteaccessterminal.TerminalChunkLoader;
import com.retiredroca.remoteaccessterminal.TerminalLinks;
import com.retiredroca.remoteaccessterminal.TerminalLinksAccess;
import com.retiredroca.remoteaccessterminal.TerminalPermissions;
import com.retiredroca.remoteaccessterminal.TerminalTravel;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.OpenNamePayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.RenamePayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.RequestSyncPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetChunkLoaderPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetColorPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetInvitesPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetOpenPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.SetSortPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TerminalSyncPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TravelPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.UnlinkPayload;
import com.retiredroca.remoteaccessterminal.network.TerminalSyncHelper;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

/** Fabric payload registration and the validating server handlers for Remote Access Terminal. */
public final class Networking {
    private static final int MAX_INVITES = 64;

    private Networking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestSyncPayload.TYPE, RequestSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TravelPayload.TYPE, TravelPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RenamePayload.TYPE, RenamePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetColorPayload.TYPE, SetColorPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetSortPayload.TYPE, SetSortPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetOpenPayload.TYPE, SetOpenPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetChunkLoaderPayload.TYPE, SetChunkLoaderPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetInvitesPayload.TYPE, SetInvitesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UnlinkPayload.TYPE, UnlinkPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenNamePayload.TYPE, OpenNamePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestSyncPayload.TYPE, Networking::handleRequestSync);
        ServerPlayNetworking.registerGlobalReceiver(TravelPayload.TYPE, Networking::handleTravel);
        ServerPlayNetworking.registerGlobalReceiver(RenamePayload.TYPE, Networking::handleRename);
        ServerPlayNetworking.registerGlobalReceiver(SetColorPayload.TYPE, Networking::handleSetColor);
        ServerPlayNetworking.registerGlobalReceiver(SetSortPayload.TYPE, Networking::handleSetSort);
        ServerPlayNetworking.registerGlobalReceiver(SetOpenPayload.TYPE, Networking::handleSetOpen);
        ServerPlayNetworking.registerGlobalReceiver(SetChunkLoaderPayload.TYPE,
                Networking::handleSetChunkLoader);
        ServerPlayNetworking.registerGlobalReceiver(SetInvitesPayload.TYPE, Networking::handleSetInvites);
        ServerPlayNetworking.registerGlobalReceiver(UnlinkPayload.TYPE, Networking::handleUnlink);
    }

    private static void handleRequestSync(RequestSyncPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof TerminalMenu menu) {
                sync(player, menu);
            }
        });
    }

    private static void handleTravel(TravelPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (menu.isLinker()) {
                RoutingLinker.travel(player, payload.targetDimension(), payload.targetPos());
                return;
            }
            TerminalLinks links = TerminalLinksAccess.get(level).links();
            TerminalLinks.Link target = links.find(menu.getColor(), payload.targetDimension(), payload.targetPos());
            if (target == null) {
                return;
            }
            ServerLevel targetLevel = level.getServer().getLevel(payload.targetDimension());
            if (targetLevel == null || !NetworkPermissions.canUse(targetLevel, target.pos(), player)) {
                return;
            }
            if (TerminalTravel.teleport(player, level, menu.getPos(), target)) {
                player.closeContainer();
            }
        });
    }

    private static void handleRename(RenamePayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            ServerLevel level = player.getServer() == null ? null : player.getServer().getLevel(payload.dimension());
            if (level == null || !TerminalPermissions.canEdit(level, payload.color(), payload.pos(), player)) {
                return;
            }
            TerminalLinksAccess access = TerminalLinksAccess.get(level);
            if (access.links().setName(payload.color(), payload.dimension(), payload.pos(), payload.name())) {
                access.setDirty();
            }
            if (player.containerMenu instanceof TerminalMenu menu
                    && menu.getDimension().equals(payload.dimension()) && menu.getPos().equals(payload.pos())) {
                sync(player, menu);
            }
        });
    }

    private static void handleSetColor(SetColorPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            DyeColor target = payload.color();
            if (target == menu.getColor()) {
                sync(player, menu);
                return;
            }
            TerminalLinksAccess access = TerminalLinksAccess.get(level);
            TerminalLinks links = access.links();
            TerminalLinks.Link link = links.find(menu.getColor(), level.dimension(), menu.getPos());
            if (!links.changeColor(menu.getColor(), level.dimension(), menu.getPos(), target)) {
                return;
            }
            TerminalChunkLoader.deactivate(level, link);
            access.setDirty();
            TerminalBlock block = Registration.blocks().get(target);
            if (block != null) {
                level.setBlockAndUpdate(menu.getPos(), block.defaultBlockState());
            }
            menu.setColor(target);
            sync(player, menu);
        });
    }

    private static void handleSetSort(SetSortPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            TerminalLinksAccess access = TerminalLinksAccess.get(level);
            TerminalLinks.Link link = access.links().find(menu.getColor(), level.dimension(), menu.getPos());
            if (link != null) {
                link.setSortMode(SortMode.byId(payload.sortMode()));
                access.setDirty();
            }
            sync(player, menu);
        });
    }

    private static void handleSetOpen(SetOpenPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            NetworkPermissions.setOpen(level, menu.getPos(), payload.open());
            sync(player, menu);
        });
    }

    private static void handleSetChunkLoader(SetChunkLoaderPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            TerminalChunkLoader.setEnabled(level, menu.getColor(), menu.getPos(), player, payload.enabled());
            sync(player, menu);
        });
    }

    private static void handleSetInvites(SetInvitesPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            TerminalLinks.Link link = TerminalLinksAccess.get(level).links()
                    .find(menu.getColor(), level.dimension(), menu.getPos());
            if (link == null) {
                return;
            }
            List<UUID> invites = new ArrayList<>();
            for (UUID invite : payload.invites()) {
                if (invite == null || invites.size() >= MAX_INVITES) {
                    break;
                }
                invites.add(invite);
            }
            NetworkPermissions.setInvites(level, link.owner(), invites);
            sync(player, menu);
        });
    }

    private static void handleUnlink(UnlinkPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            TerminalLinksAccess access = TerminalLinksAccess.get(level);
            TerminalLinks links = access.links();
            TerminalChunkLoader.deactivate(level, links.find(menu.getColor(), level.dimension(), menu.getPos()));
            if (links.remove(menu.getColor(), level.dimension(), menu.getPos())) {
                access.setDirty();
            }
            player.closeContainer();
        });
    }

    private static void sync(ServerPlayer player, TerminalMenu menu) {
        if (player.level() instanceof ServerLevel level) {
            ServerPlayNetworking.send(player, menu.isLinker()
                    ? TerminalSyncHelper.buildLinker(level, player, menu.getDimension(), menu.getPos())
                    : TerminalSyncHelper.build(level, player, menu.getColor(), menu.getPos()));
        }
    }
}
