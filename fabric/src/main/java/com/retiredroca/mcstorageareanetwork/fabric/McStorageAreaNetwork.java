package com.retiredroca.mcstorageareanetwork.fabric;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

/**
 * Fabric entrypoint for the MC Storage Area Network API. Installs the platform scanner and loads every
 * {@code mc_storage_area_network} entrypoint as a registered {@link ItemSource}.
 */
public class McStorageAreaNetwork implements ModInitializer {
    public static final String MODID = "mc_storage_area_network";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String SOURCE_ENTRYPOINT = "mc_storage_area_network";

    @Override
    public void onInitialize() {
        ShulkerBoxConfig.load();
        ItemNetworkServices.setScanner(new FabricItemScanner());
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel && entity instanceof Container) {
                ContainerOwnership.clearOwner(serverLevel, pos);
            }
        });
        for (ItemSource source : FabricLoader.getInstance().getEntrypoints(SOURCE_ENTRYPOINT, ItemSource.class)) {
            ItemSourceRegistry.register(source);
            LOGGER.info("Registered item source: {}", source.getSourceName());
        }
    }
}
