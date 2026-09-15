package com.retiredroca.craftingnetwork.neoforge;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.block.TerminalProtection;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

@Mod(CraftingNetworkCommon.MODID)
public class CraftingNetwork {
    public CraftingNetwork(IEventBus modEventBus, ModContainer modContainer) {
        CraftingNetworkCommon.setPlatform(new NeoForgeCraftingNetworkPlatform());
        CraftingNetworkConfig.register(modContainer);
        modEventBus.addListener(CraftingNetworkConfig::onConfigLoad);

        Registration.register(modEventBus);
        Networking.register(modEventBus);

        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level
                    && !TerminalProtection.canBreak(level, event.getPos(), event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(
                        Component.translatable("block.crafting_network.terminal_locked"), true);
            }
        });
    }
}
