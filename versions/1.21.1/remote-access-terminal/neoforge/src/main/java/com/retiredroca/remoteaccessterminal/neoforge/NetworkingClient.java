package com.retiredroca.remoteaccessterminal.neoforge;

import com.retiredroca.remoteaccessterminal.client.TerminalNameScreen;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.OpenNamePayload;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.client.Minecraft;

/**
 * Client-only payload handling for Remote Access Terminal. Loaded solely from
 * {@link RemoteAccessTerminalClient}, so no dedicated-server path ever resolves the client types it
 * touches.
 */
public final class NetworkingClient {
    private NetworkingClient() {
    }

    /** Installs the client-side handling of the clientbound payloads. */
    public static void register() {
        Networking.installClientHandlers(NetworkingClient::handleSync, NetworkingClient::handleOpenName);
    }

    private static void handleSync(TerminalSyncPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof TerminalMenu menu
                && menu.getPos().equals(payload.pos()) && menu.getDimension().equals(payload.dimension())) {
            menu.updateSync(payload);
        }
    }

    private static void handleOpenName(OpenNamePayload payload) {
        Minecraft.getInstance().setScreen(
                new TerminalNameScreen(payload.dimension(), payload.pos(), payload.color()));
    }
}
