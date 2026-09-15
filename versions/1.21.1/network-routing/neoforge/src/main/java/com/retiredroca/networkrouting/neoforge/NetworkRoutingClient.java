package com.retiredroca.networkrouting.neoforge;

import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.client.RoutingScreen;
import com.retiredroca.networkrouting.client.RoutingTerminalRenderer;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NetworkRoutingCommon.MODID, dist = Dist.CLIENT)
public class NetworkRoutingClient {
    public NetworkRoutingClient(IEventBus modEventBus) {
        modEventBus.addListener(NetworkRoutingClient::registerScreens);
        modEventBus.addListener(NetworkRoutingClient::registerRenderers);
        modEventBus.addListener(NetworkRoutingClient::registerItemExtensions);
        NeoForge.EVENT_BUS.addListener(NetworkRoutingClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Networking.setServerModded(false);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.MENU.get(), RoutingScreen::new);
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.TERMINAL_BE.get(),
                context -> new RoutingTerminalRenderer<>(context));
    }

    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft mc = Minecraft.getInstance();
                return new NeoForgeRoutingTerminalBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        }, Registration.TERMINAL_ITEM.get());
    }
}
