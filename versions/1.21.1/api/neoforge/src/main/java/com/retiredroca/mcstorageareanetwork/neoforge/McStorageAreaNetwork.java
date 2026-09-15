package com.retiredroca.mcstorageareanetwork.neoforge;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * NeoForge entrypoint for the MC Storage Area Network API. Installs the platform scanner, registers the
 * built-in shulker flattening source, and registers every {@code register_item_source} IMC message
 * as an {@link ItemSource}.
 */
@Mod(McStorageAreaNetwork.MODID)
public class McStorageAreaNetwork {
    public static final String MODID = "mc_storage_area_network";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String SOURCE_IMC = "register_item_source";

    public McStorageAreaNetwork(IEventBus modEventBus, ModContainer modContainer) {
        ShulkerBoxConfig.register(modContainer);
        modEventBus.addListener(ShulkerBoxConfig::onConfigLoad);
        ItemNetworkServices.setScanner(new NeoForgeItemScanner());
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        modEventBus.addListener(McStorageAreaNetwork::onInterModProcess);
        NeoForge.EVENT_BUS.addListener(McStorageAreaNetwork::onEntityPlace);
        NeoForge.EVENT_BUS.addListener(McStorageAreaNetwork::onBreak);
    }

    private static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player
                && level.getBlockEntity(event.getPos()) instanceof Container) {
            ContainerOwnership.setOwner(level, event.getPos(), player.getUUID(), player.getGameProfile().getName());
        }
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.getBlockEntity(event.getPos()) instanceof Container) {
            ContainerOwnership.clearOwner(level, event.getPos());
        }
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
