package com.retiredroca.craftingnetwork.neoforge;

import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.menu.IStationMenu;
import com.retiredroca.craftingnetwork.network.StationPackets;
import com.retiredroca.craftingnetwork.station.StationState;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Networking {
    private static volatile boolean serverHasMod;

    private Networking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(Networking::onPlayerLoggedIn);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CraftingNetworkCommon.MODID).versioned("1");
        if (FMLLoader.getDist() == Dist.CLIENT) {
            registrar = registrar.optional();
        }
        registrar.playToClient(StationPackets.ServerPresencePayload.TYPE, StationPackets.ServerPresencePayload.STREAM_CODEC, Networking::handlePresence)
                .playToClient(StationPackets.CraftingSourcesPayload.TYPE, StationPackets.CraftingSourcesPayload.STREAM_CODEC, Networking::handleSync)
                .playToClient(StationPackets.StationStatePayload.TYPE, StationPackets.StationStatePayload.STREAM_CODEC, Networking::handleStationState)
                .playToServer(StationPackets.StationBrewTargetPayload.TYPE, StationPackets.StationBrewTargetPayload.STREAM_CODEC, Networking::handleBrewTarget);
    }

    private static void handlePresence(StationPackets.ServerPresencePayload payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> setServerModded(true));
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new StationPackets.ServerPresencePayload());
        }
    }

    public static void setServerModded(boolean present) {
        if (present && !serverHasMod) {
            CraftingNetworkCommon.LOGGER.info("Server has {} installed - enabling.", CraftingNetworkCommon.MODID);
        }
        serverHasMod = present;
    }

    public static boolean isServerModded() {
        return serverHasMod;
    }

    private static void handleSync(StationPackets.CraftingSourcesPayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof CraftingStationMenu menu) {
                menu.setServerSources(payload.sources(), payload.shulkersFirst(), payload.inventoryFirst());
            }
        });
    }

    private static void handleStationState(StationPackets.StationStatePayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof IStationMenu menu
                    && menu.stationPos().equals(payload.pos())) {
                menu.setServerState(payload.state());
            }
        });
    }

    private static void handleBrewTarget(StationPackets.StationBrewTargetPayload payload, IPayloadContext context) {
        if (!context.flow().isServerbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof IStationMenu menu
                    && menu.stationPos().equals(payload.pos())) {
                menu.applyBrewTarget(payload.potion());
            }
        });
    }

    public static void sendSources(ServerPlayer player, List<CraftingSourceInfo> sources, boolean shulkersFirst,
            boolean inventoryFirst) {
        PacketDistributor.sendToPlayer(player,
                new StationPackets.CraftingSourcesPayload(sources, shulkersFirst, inventoryFirst));
    }

    public static void sendStationState(ServerPlayer player, BlockPos pos, StationState state) {
        PacketDistributor.sendToPlayer(player, new StationPackets.StationStatePayload(pos, state));
    }

    public static void sendBrewTarget(BlockPos pos, String potion) {
        if (!isServerModded()) {
            return;
        }
        PacketDistributor.sendToServer(new StationPackets.StationBrewTargetPayload(pos, potion));
    }
}
