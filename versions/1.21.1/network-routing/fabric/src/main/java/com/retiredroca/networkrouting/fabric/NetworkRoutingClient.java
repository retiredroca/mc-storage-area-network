package com.retiredroca.networkrouting.fabric;

import com.retiredroca.networkrouting.client.RoutingScreen;
import com.retiredroca.networkrouting.client.RoutingTerminalBEWLR;
import com.retiredroca.networkrouting.client.RoutingTerminalRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class NetworkRoutingClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Registration.MENU, RoutingScreen::new);
        BlockEntityRendererRegistry.register(Registration.TERMINAL_BE,
                context -> new RoutingTerminalRenderer<>(context));
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.TERMINAL_ITEM, RoutingTerminalBEWLR::renderByItem);
        Networking.registerClient();
    }
}
