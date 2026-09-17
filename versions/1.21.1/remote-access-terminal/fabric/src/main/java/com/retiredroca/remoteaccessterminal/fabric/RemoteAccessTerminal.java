package com.retiredroca.remoteaccessterminal.fabric;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.TerminalChunkLoader;
import com.retiredroca.remoteaccessterminal.TerminalHooks;
import com.retiredroca.remoteaccessterminal.TerminalRegistryPublisher;
import com.retiredroca.remoteaccessterminal.command.RemoteAccessCommands;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class RemoteAccessTerminal implements ModInitializer {
    @Override
    public void onInitialize() {
        RemoteAccessTerminalCommon.setPlatform(new FabricRemoteAccessTerminalPlatform());
        RemoteAccessTerminalConfig.load();
        Registration.register();
        Networking.register();
        TerminalHooks.register();
        TerminalRegistryPublisher.register();
        // Chunk tickets are not persisted; restore them from the saved links once the world is up.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TerminalRegistryPublisher.installServer(server);
            TerminalChunkLoader.reapplyAll(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> TerminalRegistryPublisher.installServer(null));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(RemoteAccessCommands.build()));

        RemoteAccessTerminalCommon.LOGGER.info("{} initialized", RemoteAccessTerminalCommon.MOD_NAME);
    }
}
