package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.block.TerminalProtection;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

@Mod(StorageNetworkCommon.MODID)
public class StorageNetwork {
    public StorageNetwork(IEventBus modEventBus, ModContainer modContainer) {
        StorageNetworkCommon.setPlatform(new NeoForgeStorageNetworkPlatform());
        StorageNetworkConfig.register(modContainer);
        modEventBus.addListener(StorageNetworkConfig::onConfigLoad);

        Registration.register(modEventBus);
        Networking.register(modEventBus);

        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level
                    && !TerminalProtection.canBreak(level, event.getPos(), event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(
                        Component.translatable("block.storage_network.terminal_locked"), true);
            }
        });
    }
}
