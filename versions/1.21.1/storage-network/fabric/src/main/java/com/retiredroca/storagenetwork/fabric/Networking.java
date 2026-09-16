package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;
import com.retiredroca.storagenetwork.menu.StorageTerminalMenu;
import com.retiredroca.storagenetwork.network.TerminalPackets.ServerPresencePayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExcludePayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExtractPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSelectPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Networking {
    private static volatile boolean serverHasMod;

    private Networking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TerminalExtractPayload.TYPE, TerminalExtractPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalSelectPayload.TYPE, TerminalSelectPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TerminalExcludePayload.TYPE, TerminalExcludePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(TerminalSyncPayload.TYPE, TerminalSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ServerPresencePayload.TYPE, ServerPresencePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TerminalExtractPayload.TYPE, Networking::handleExtract);
        ServerPlayNetworking.registerGlobalReceiver(TerminalSelectPayload.TYPE, Networking::handleSelect);
        ServerPlayNetworking.registerGlobalReceiver(TerminalExcludePayload.TYPE, Networking::handleExclude);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!ServerPlayNetworking.canSend(player, ServerPresencePayload.TYPE)) {
                player.connection.disconnect(
                        Component.translatable("disconnect.storage_network.server_requires.reason"));
                return;
            }
            ServerPlayNetworking.send(player, new ServerPresencePayload());
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ServerPresencePayload.TYPE,
                (payload, context) -> context.client().execute(() -> setServerModded(true)));
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, Networking::handleTerminalSync);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> setServerModded(false));
    }

    public static void setServerModded(boolean present) {
        serverHasMod = present;
    }

    public static boolean isServerModded() {
        return serverHasMod;
    }

    private static void handleTerminalSync(TerminalSyncPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu
                            instanceof StorageTerminalMenu menu) {
                menu.updateServerItems(payload.items(), payload.counts(), payload.chests(), payload.tier());
            }
        });
    }

    private static void handleExtract(TerminalExtractPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            if (context.player().containerMenu instanceof StorageTerminalMenu menu
                    && menu.getPos().equals(payload.pos())) {
                menu.doExtract(context.player(), payload.stack(), payload.mode());
            }
        });
    }

    private static void handleSelect(TerminalSelectPayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            if (context.player().containerMenu instanceof StorageTerminalMenu menu
                    && menu.getPos().equals(payload.pos())) {
                menu.setDepositTarget(payload.all() ? null : payload.targetPos(), payload.childName());
            }
        });
    }

    private static void handleExclude(TerminalExcludePayload payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (player.containerMenu instanceof StorageTerminalMenu menu && menu.getPos().equals(payload.pos())) {
                NetworkExclusions.Result result = NetworkExclusions.toggle(player, payload.containerPos());
                player.displayClientMessage(StorageNetworkCommon.exclusionMessage(result), true);
                if (player.level().getBlockEntity(payload.pos()) instanceof AbstractStorageTerminalBlockEntity terminal) {
                    StorageNetworkCommon.platform().sendTerminalSync(player, terminal.buildSync());
                }
            }
        });
    }
}
