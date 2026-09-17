package com.retiredroca.remoteaccessterminal.neoforge;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.TerminalChunkLoader;
import com.retiredroca.remoteaccessterminal.TerminalHooks;
import com.retiredroca.remoteaccessterminal.TerminalRegistryPublisher;
import com.retiredroca.remoteaccessterminal.command.RemoteAccessCommands;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(RemoteAccessTerminalCommon.MODID)
public class RemoteAccessTerminal {
    public RemoteAccessTerminal(IEventBus modEventBus, ModContainer modContainer) {
        RemoteAccessTerminalCommon.setPlatform(new NeoForgeRemoteAccessTerminalPlatform());
        RemoteAccessTerminalConfig.register(modContainer);
        modEventBus.addListener(RemoteAccessTerminalConfig::onConfigLoad);
        Registration.register(modEventBus);
        Networking.register(modEventBus);
        TerminalHooks.register();
        TerminalRegistryPublisher.register();
        // Chunk tickets are not persisted; restore them from the saved links once the world is up.
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            TerminalRegistryPublisher.installServer(event.getServer());
            TerminalChunkLoader.reapplyAll(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> TerminalRegistryPublisher.installServer(null));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                event.getDispatcher().register(RemoteAccessCommands.build()));

        RemoteAccessTerminalCommon.LOGGER.info("{} initialized", RemoteAccessTerminalCommon.MOD_NAME);
    }
}
