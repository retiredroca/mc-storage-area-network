package com.retiredroca.mcstorageareanetwork.fabric;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.mcstorageareanetwork.api.BreakProtection;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ConfirmBreakPayload;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ForceBreakPayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        ItemNetworkServices.setConfigService(ShulkerBoxConfig::setContainerExcluded);
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel && entity instanceof Container) {
                ContainerOwnership.clearOwner(serverLevel, pos);
            }
        });

        // Break protection for the mod set: owner/team may break; operators must confirm.
        PayloadTypeRegistry.playC2S().register(ForceBreakPayload.TYPE, ForceBreakPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfirmBreakPayload.TYPE, ConfirmBreakPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ForceBreakPayload.TYPE, (payload, context) ->
                context.player().server.execute(() -> {
                    if (context.player().hasPermissions(2)
                            && context.player().level() instanceof ServerLevel serverLevel) {
                        BreakProtection.forceBreak(serverLevel, payload.pos());
                    }
                }));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (!(level instanceof ServerLevel serverLevel) || !BreakProtection.isProtected(serverLevel, pos)) {
                return true;
            }
            if (BreakProtection.needsOpConfirmation(serverLevel, pos, player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ServerPlayNetworking.send(serverPlayer, new ConfirmBreakPayload(pos));
                }
                return false;
            }
            if (!BreakProtection.canBreak(serverLevel, pos, player)) {
                player.displayClientMessage(
                        Component.translatable("block.mc_storage_area_network.protected"), true);
                return false;
            }
            return true;
        });
        for (ItemSource source : FabricLoader.getInstance().getEntrypoints(SOURCE_ENTRYPOINT, ItemSource.class)) {
            ItemSourceRegistry.register(source);
            LOGGER.info("Registered item source: {}", source.getSourceName());
        }
    }
}
