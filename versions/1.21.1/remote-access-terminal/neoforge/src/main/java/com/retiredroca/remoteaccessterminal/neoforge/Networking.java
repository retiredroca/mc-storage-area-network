package com.retiredroca.remoteaccessterminal.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** NeoForge payload registration and the validating server handlers for Remote Access Terminal. */
public final class Networking {
    private static final int MAX_INVITES = 64;

    /** Client-only handling of the clientbound payloads; installed by the client initializer. */
    private static volatile Consumer<TerminalSyncPayload> clientSync;
    private static volatile Consumer<OpenNamePayload> clientName;

    private Networking() {
    }

    /** Installs the client-only payload handlers; called only from {@link RemoteAccessTerminalClient}. */
    static void installClientHandlers(Consumer<TerminalSyncPayload> sync, Consumer<OpenNamePayload> name) {
        clientSync = sync;
        clientName = name;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(RemoteAccessTerminalCommon.MODID).versioned("1");
        if (FMLLoader.getDist() == Dist.CLIENT) {
            registrar = registrar.optional();
        }
        registrar.playToClient(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC, Networking::handleSync);
        registrar.playToClient(OpenNamePayload.TYPE, OpenNamePayload.STREAM_CODEC, Networking::handleOpenName);
        registrar.playToServer(RequestSyncPayload.TYPE, RequestSyncPayload.STREAM_CODEC,
                Networking::handleRequestSync);
        registrar.playToServer(TravelPayload.TYPE, TravelPayload.STREAM_CODEC, Networking::handleTravel);
        registrar.playToServer(RenamePayload.TYPE, RenamePayload.STREAM_CODEC, Networking::handleRename);
        registrar.playToServer(SetColorPayload.TYPE, SetColorPayload.STREAM_CODEC, Networking::handleSetColor);
        registrar.playToServer(SetSortPayload.TYPE, SetSortPayload.STREAM_CODEC, Networking::handleSetSort);
        registrar.playToServer(SetOpenPayload.TYPE, SetOpenPayload.STREAM_CODEC, Networking::handleSetOpen);
        registrar.playToServer(SetChunkLoaderPayload.TYPE, SetChunkLoaderPayload.STREAM_CODEC,
                Networking::handleSetChunkLoader);
        registrar.playToServer(SetInvitesPayload.TYPE, SetInvitesPayload.STREAM_CODEC, Networking::handleSetInvites);
        registrar.playToServer(UnlinkPayload.TYPE, UnlinkPayload.STREAM_CODEC, Networking::handleUnlink);
    }

    private static void handleSync(TerminalSyncPayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        Consumer<TerminalSyncPayload> handler = clientSync;
        if (handler != null) {
            context.enqueueWork(() -> handler.accept(payload));
        }
    }

    private static void handleOpenName(OpenNamePayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        Consumer<OpenNamePayload> handler = clientName;
        if (handler != null) {
            context.enqueueWork(() -> handler.accept(payload));
        }
    }

    private static void handleRequestSync(RequestSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof TerminalMenu menu) {
                sync(player, menu);
            }
        });
    }

    private static void handleTravel(TravelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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

    private static void handleRename(RenamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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

    private static void handleSetColor(SetColorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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
            TerminalBlock block = Registration.terminalBlocks().get(target).get();
            level.setBlockAndUpdate(menu.getPos(), block.defaultBlockState());
            menu.setColor(target);
            sync(player, menu);
        });
    }

    private static void handleSetSort(SetSortPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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

    private static void handleSetOpen(SetOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            NetworkPermissions.setOpen(level, menu.getPos(), payload.open());
            sync(player, menu);
        });
    }

    private static void handleSetChunkLoader(SetChunkLoaderPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof TerminalMenu menu)
                    || !(player.level() instanceof ServerLevel level)
                    || !TerminalPermissions.canEdit(level, menu.getColor(), menu.getPos(), player)) {
                return;
            }
            TerminalChunkLoader.setEnabled(level, menu.getColor(), menu.getPos(), player, payload.enabled());
            sync(player, menu);
        });
    }

    private static void handleSetInvites(SetInvitesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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

    private static void handleUnlink(UnlinkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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
            PacketDistributor.sendToPlayer(player, menu.isLinker()
                    ? TerminalSyncHelper.buildLinker(level, player, menu.getDimension(), menu.getPos())
                    : TerminalSyncHelper.build(level, player, menu.getColor(), menu.getPos()));
        }
    }
}
