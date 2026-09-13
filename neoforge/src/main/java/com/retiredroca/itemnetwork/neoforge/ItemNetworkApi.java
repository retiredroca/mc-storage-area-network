package com.retiredroca.itemnetwork.neoforge;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.itemnetwork.api.ItemNetworkServices;
import com.retiredroca.itemnetwork.api.ItemSource;
import com.retiredroca.itemnetwork.api.ItemSourceRegistry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;

/**
 * NeoForge entrypoint for the Item Network API. Installs the platform scanner, registers the
 * built-in shulker flattening source, and registers every {@code register_item_source} IMC message
 * as an {@link ItemSource}.
 */
@Mod(ItemNetworkApi.MODID)
public class ItemNetworkApi {
    public static final String MODID = "item_network_api";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String SOURCE_IMC = "register_item_source";

    public ItemNetworkApi(IEventBus modEventBus, ModContainer modContainer) {
        ShulkerBoxConfig.register(modContainer);
        modEventBus.addListener(ShulkerBoxConfig::onConfigLoad);
        ItemNetworkServices.setScanner(new NeoForgeItemScanner());
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        modEventBus.addListener(ItemNetworkApi::onInterModProcess);
    }

    private static void onInterModProcess(InterModProcessEvent event) {
        event.getIMCStream(SOURCE_IMC::equals).forEach(message -> {
            try {
                Object value = message.messageSupplier().get();
                if (value instanceof ItemSource source) {
                    ItemSourceRegistry.register(source);
                    LOGGER.info("Registered item source: {}", source.getSourceName());
                }
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to process item source IMC", throwable);
            }
        });
    }
}
