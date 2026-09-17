package com.retiredroca.remoteaccessterminal.fabric;

import com.retiredroca.remoteaccessterminal.client.TerminalNameScreen;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.OpenNamePayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TerminalSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-only payload handling for Remote Access Terminal. Loaded solely from
 * {@link RemoteAccessTerminalClient}, so no dedicated-server path ever resolves the client types it
 * touches.
 */
public final class NetworkingClient {
    private NetworkingClient() {
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(TerminalSyncPayload.TYPE, NetworkingClient::handleSync);
        ClientPlayNetworking.registerGlobalReceiver(OpenNamePayload.TYPE, NetworkingClient::handleOpenName);
    }

    private static void handleSync(TerminalSyncPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (context.client().player != null
                    && context.client().player.containerMenu instanceof TerminalMenu menu
                    && menu.getPos().equals(payload.pos())
                    && menu.getDimension().equals(payload.dimension())) {
                menu.updateSync(payload);
            }
        });
    }

    private static void handleOpenName(OpenNamePayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> context.client().setScreen(
                new TerminalNameScreen(payload.dimension(), payload.pos(), payload.color())));
    }
}
