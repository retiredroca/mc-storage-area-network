package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.client.NetworkShareTerminalRenderer;
import com.retiredroca.storagenetwork.client.NetworkShareTerminalScreen;
import com.retiredroca.storagenetwork.client.StorageTerminalBEWLR;
import com.retiredroca.storagenetwork.client.StorageTerminalRenderer;
import com.retiredroca.storagenetwork.client.StorageTerminalScreen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = StorageNetworkCommon.MODID, dist = Dist.CLIENT)
public class StorageNetworkClient {
    public StorageNetworkClient(IEventBus modEventBus) {
        modEventBus.addListener(StorageNetworkClient::registerScreens);
        modEventBus.addListener(StorageNetworkClient::registerRenderers);
        modEventBus.addListener(StorageNetworkClient::registerItemExtensions);
        NeoForge.EVENT_BUS.addListener(StorageNetworkClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Networking.setServerModded(false);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.STORAGE_TERMINAL_MENU.get(), StorageTerminalScreen::new);
        event.register(Registration.SHARE_TERMINAL_MENU.get(), NetworkShareTerminalScreen::new);
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.STORAGE_TERMINAL_BE.get(), StorageTerminalRenderer::new);
        event.registerBlockEntityRenderer(Registration.SHARE_TERMINAL_BE.get(),
                context -> new NetworkShareTerminalRenderer<>(context));
    }

    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft mc = Minecraft.getInstance();
                return new NeoForgeStorageTerminalBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        }, Registration.STORAGE_TERMINAL_ITEM.get());
        event.registerItem(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft mc = Minecraft.getInstance();
                return new NeoForgeNetworkShareTerminalBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        }, Registration.SHARE_TERMINAL_ITEM.get());
    }
}
