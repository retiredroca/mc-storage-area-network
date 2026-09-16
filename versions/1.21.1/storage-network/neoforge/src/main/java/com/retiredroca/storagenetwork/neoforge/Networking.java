package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;
import com.retiredroca.storagenetwork.menu.StorageTerminalMenu;
import com.retiredroca.storagenetwork.network.TerminalPackets.ServerPresencePayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExcludePayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExtractPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSelectPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Networking {
    private static volatile boolean serverHasMod;

    private Networking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(Networking::onPlayerLoggedIn);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(StorageNetworkCommon.MODID).versioned("1");
        if (FMLLoader.getDist() == Dist.CLIENT) {
            registrar = registrar.optional();
        }
        registrar.playToClient(ServerPresencePayload.TYPE, ServerPresencePayload.STREAM_CODEC, Networking::handlePresence);
        registrar.playToClient(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC, Networking::handleSync);
        registrar.playToServer(TerminalExtractPayload.TYPE, TerminalExtractPayload.STREAM_CODEC, Networking::handleExtract);
        registrar.playToServer(TerminalSelectPayload.TYPE, TerminalSelectPayload.STREAM_CODEC, Networking::handleSelect);
        registrar.playToServer(TerminalExcludePayload.TYPE, TerminalExcludePayload.STREAM_CODEC, Networking::handleExclude);
    }

    private static void handlePresence(ServerPresencePayload payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> setServerModded(true));
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ServerPresencePayload());
        }
    }

    public static void setServerModded(boolean present) {
        serverHasMod = present;
    }

    public static boolean isServerModded() {
        return serverHasMod;
    }

    private static void handleSync(TerminalSyncPayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu
                            instanceof StorageTerminalMenu menu) {
                menu.updateServerItems(payload.items(), payload.counts(), payload.chests(), payload.tier());
            }
        });
    }

    private static void handleExtract(TerminalExtractPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof StorageTerminalMenu menu
                        && menu.getPos().equals(payload.pos())) {
                    menu.doExtract(player, payload.stack(), payload.mode());
                    if (player.level().getBlockEntity(payload.pos())
                            instanceof AbstractStorageTerminalBlockEntity terminal) {
                        PacketDistributor.sendToPlayer(player, terminal.buildSync());
                    }
                }
            }
        });
    }

    private static void handleSelect(TerminalSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof StorageTerminalMenu menu
                        && menu.getPos().equals(payload.pos())) {
                    menu.setDepositTarget(payload.all() ? null : payload.targetPos(), payload.childName());
                }
            }
        });
    }

    private static void handleExclude(TerminalExcludePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof StorageTerminalMenu menu
                        && menu.getPos().equals(payload.pos())) {
                    NetworkExclusions.Result result = NetworkExclusions.toggle(player, payload.containerPos());
                    player.displayClientMessage(StorageNetworkCommon.exclusionMessage(result), true);
                    if (player.level().getBlockEntity(payload.pos()) instanceof AbstractStorageTerminalBlockEntity terminal) {
                        PacketDistributor.sendToPlayer(player, terminal.buildSync());
                    }
                }
            }
        });
    }
}
