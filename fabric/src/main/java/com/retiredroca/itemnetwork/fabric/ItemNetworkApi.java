package com.retiredroca.itemnetwork.fabric;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.itemnetwork.api.ItemNetworkServices;
import com.retiredroca.itemnetwork.api.ItemSource;
import com.retiredroca.itemnetwork.api.ItemSourceRegistry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric entrypoint for the Item Network API. Installs the platform scanner and loads every
 * {@code item_network_api} entrypoint as a registered {@link ItemSource}.
 */
public class ItemNetworkApi implements ModInitializer {
    public static final String MODID = "item_network_api";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String SOURCE_ENTRYPOINT = "item_network_api";

    @Override
    public void onInitialize() {
        ItemNetworkServices.setScanner(new FabricItemScanner());
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        for (ItemSource source : FabricLoader.getInstance().getEntrypoints(SOURCE_ENTRYPOINT, ItemSource.class)) {
            ItemSourceRegistry.register(source);
            LOGGER.info("Registered item source: {}", source.getSourceName());
        }
    }
}
