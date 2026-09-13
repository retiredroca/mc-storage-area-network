package com.retiredroca.craftingnetwork.fabric;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.block.TerminalProtection;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class CraftingNetwork implements ModInitializer {
    @Override
    public void onInitialize() {
        CraftingNetworkCommon.setPlatform(new FabricCraftingNetworkPlatform());
        CraftingNetworkConfig.load();
        Networking.register();
        Registration.register();

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel && !TerminalProtection.canBreak(serverLevel, pos, player)) {
                player.displayClientMessage(
                        Component.translatable("block.crafting_network.terminal_locked"), true);
                return false;
            }
            return true;
        });
    }
}
