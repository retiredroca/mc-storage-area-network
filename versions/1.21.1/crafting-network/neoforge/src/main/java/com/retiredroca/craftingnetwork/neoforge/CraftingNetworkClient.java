package com.retiredroca.craftingnetwork.neoforge;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.client.CraftingStationRenderer;
import com.retiredroca.craftingnetwork.client.CraftingStationScreen;
import com.retiredroca.craftingnetwork.client.StationRenderer;
import com.retiredroca.craftingnetwork.client.StationScreen;

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

@Mod(value = CraftingNetworkCommon.MODID, dist = Dist.CLIENT)
public class CraftingNetworkClient {
    public CraftingNetworkClient(IEventBus modEventBus) {
        modEventBus.addListener(CraftingNetworkClient::registerScreens);
        modEventBus.addListener(CraftingNetworkClient::registerRenderers);
        modEventBus.addListener(CraftingNetworkClient::registerItemExtensions);
        NeoForge.EVENT_BUS.addListener(CraftingNetworkClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Networking.setServerModded(false);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.CRAFTING_STATION_MENU.get(), CraftingStationScreen::new);
        event.register(Registration.COOKING_STATION_MENU.get(), StationScreen::new);
        event.register(Registration.BREWING_STATION_MENU.get(), StationScreen::new);
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.CRAFTING_STATION_BE.get(),
                context -> new CraftingStationRenderer<>(context));
        event.registerBlockEntityRenderer(Registration.STATION_BE.get(),
                context -> new StationRenderer<>(context));
    }

    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft mc = Minecraft.getInstance();
                return new NeoForgeCraftingStationBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        }, Registration.CRAFTING_STATION_ITEM.get());
        IClientItemExtensions stationRenderer = new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft mc = Minecraft.getInstance();
                return new NeoForgeStationBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        };
        event.registerItem(stationRenderer, Registration.SMELTING_STATION_ITEM.get());
        event.registerItem(stationRenderer, Registration.BLASTING_STATION_ITEM.get());
        event.registerItem(stationRenderer, Registration.SMOKING_STATION_ITEM.get());
        event.registerItem(stationRenderer, Registration.BREWING_STATION_ITEM.get());
    }
}
