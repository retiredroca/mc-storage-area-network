package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.storagenetwork.client.NetworkShareTerminalBEWLR;
import com.retiredroca.storagenetwork.client.NetworkShareTerminalRenderer;
import com.retiredroca.storagenetwork.client.NetworkShareTerminalScreen;
import com.retiredroca.storagenetwork.client.StorageTerminalBEWLR;
import com.retiredroca.storagenetwork.client.StorageTerminalRenderer;
import com.retiredroca.storagenetwork.client.StorageTerminalScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class StorageNetworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Registration.STORAGE_TERMINAL_MENU, StorageTerminalScreen::new);
        BlockEntityRendererRegistry.register(Registration.STORAGE_TERMINAL_BE, StorageTerminalRenderer::new);
        Networking.registerClient();

        MenuScreens.register(Registration.SHARE_TERMINAL_MENU, NetworkShareTerminalScreen::new);
        BlockEntityRendererRegistry.register(Registration.SHARE_TERMINAL_BE,
                context -> new NetworkShareTerminalRenderer<>(context));

        BuiltinItemRendererRegistry.INSTANCE.register(Registration.STORAGE_TERMINAL_ITEM,
                StorageTerminalBEWLR::renderByItem);
        BuiltinItemRendererRegistry.INSTANCE.register(Registration.SHARE_TERMINAL_ITEM,
                NetworkShareTerminalBEWLR::renderByItem);
    }
}
