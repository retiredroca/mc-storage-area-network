package com.retiredroca.mcstorageareanetwork.neoforge;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.mcstorageareanetwork.api.BreakProtection;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.CrafterAutomation;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;
import com.retiredroca.mcstorageareanetwork.api.NetworkBlock;
import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ConfirmBreakPayload;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ForceBreakPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
        ItemNetworkServices.setConfigService(ShulkerBoxConfig::setContainerExcluded);
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        modEventBus.addListener(McStorageAreaNetwork::onInterModProcess);
        modEventBus.addListener(McStorageAreaNetwork::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(McStorageAreaNetwork::onEntityPlace);
        NeoForge.EVENT_BUS.addListener(McStorageAreaNetwork::onBreak);
        NeoForge.EVENT_BUS.addListener(McStorageAreaNetwork::onRightClickBlock);

        // Drive linked crafters every tick (the automation throttles itself).
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
            if (event.getLevel() instanceof ServerLevel level) {
                CrafterAutomation.tick(level);
            }
        });
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive()
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        boolean crafter = state.getBlock() instanceof CrafterBlock;
        boolean container = !crafter && !(state.getBlock() instanceof NetworkBlock)
                && event.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
        if (!crafter && !container) {
            return;
        }
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Component message = crafter
                    ? CrafterAutomation.message(CrafterAutomation.toggle(serverPlayer, pos))
                    : NetworkExclusions.message(NetworkExclusions.toggle(serverPlayer, pos));
            serverPlayer.displayClientMessage(message, true);
        }
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1");
        if (FMLLoader.getDist() == Dist.CLIENT) {
            registrar = registrar.optional();
        }
        registrar.playToServer(ForceBreakPayload.TYPE, ForceBreakPayload.STREAM_CODEC, McStorageAreaNetwork::handleForceBreak);
        registrar.playToClient(ConfirmBreakPayload.TYPE, ConfirmBreakPayload.STREAM_CODEC, McStorageAreaNetwork::handleConfirmBreak);
    }

    private static void handleForceBreak(ForceBreakPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player
                    && player.hasPermissions(2) && player.level() instanceof ServerLevel level) {
                BreakProtection.forceBreak(level, payload.pos());
            }
        });
    }

    private static void handleConfirmBreak(ConfirmBreakPayload payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> McStorageAreaNetworkClient.openConfirm(payload.pos()));
        }
    }

    private static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof ServerPlayer player
                && level.getBlockEntity(event.getPos()) instanceof Container) {
            ContainerOwnership.setOwner(level, event.getPos(), player.getUUID(), player.getGameProfile().getName());
        }
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (BreakProtection.isProtected(level, event.getPos())) {
            Player player = event.getPlayer();
            if (BreakProtection.needsOpConfirmation(level, event.getPos(), player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, new ConfirmBreakPayload(event.getPos()));
                }
                event.setCanceled(true);
                return;
            }
            if (!BreakProtection.canBreak(level, event.getPos(), player)) {
                event.setCanceled(true);
                player.displayClientMessage(
                        Component.translatable("block.mc_storage_area_network.protected"), true);
                return;
            }
        }
        if (level.getBlockEntity(event.getPos()) instanceof Container) {
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
