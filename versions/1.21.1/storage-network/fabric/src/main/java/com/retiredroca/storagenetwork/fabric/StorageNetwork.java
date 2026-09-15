package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.block.TerminalProtection;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class StorageNetwork implements ModInitializer {
    @Override
    public void onInitialize() {
        StorageNetworkCommon.setPlatform(new FabricStorageNetworkPlatform());
        StorageNetworkConfig.load();
        Registration.register();
        Networking.register();

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel && !TerminalProtection.canBreak(serverLevel, pos, player)) {
                player.displayClientMessage(
                        Component.translatable("block.storage_network.terminal_locked"), true);
                return false;
            }
            return true;
        });
    }
}
