package com.retiredroca.storagenetwork.fabric;

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

        BuiltinItemRendererRegistry.INSTANCE.register(Registration.STORAGE_TERMINAL_ITEM,
                StorageTerminalBEWLR::renderByItem);
    }
}
