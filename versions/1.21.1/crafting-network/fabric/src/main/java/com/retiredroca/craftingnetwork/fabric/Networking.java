package com.retiredroca.craftingnetwork.fabric;

import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.menu.IStationMenu;
import com.retiredroca.craftingnetwork.network.StationPackets;
import com.retiredroca.craftingnetwork.station.StationState;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Networking {
    private static volatile boolean serverHasMod;

    private Networking() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(StationPackets.CraftingSourcesPayload.TYPE,
                StationPackets.CraftingSourcesPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(StationPackets.ServerPresencePayload.TYPE,
                StationPackets.ServerPresencePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(StationPackets.StationStatePayload.TYPE,
                StationPackets.StationStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(StationPackets.StationBrewTargetPayload.TYPE,
                StationPackets.StationBrewTargetPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StationPackets.StationBrewTargetPayload.TYPE, Networking::handleBrewTarget);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (!ServerPlayNetworking.canSend(player, StationPackets.ServerPresencePayload.TYPE)) {
                player.connection.disconnect(
                        Component.translatable("disconnect." + CraftingNetworkCommon.MODID + ".server_requires.reason"));
                return;
            }
            ServerPlayNetworking.send(player, new StationPackets.ServerPresencePayload());
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(StationPackets.CraftingSourcesPayload.TYPE, Networking::handleSync);
        ClientPlayNetworking.registerGlobalReceiver(StationPackets.StationStatePayload.TYPE, Networking::handleStationState);
        ClientPlayNetworking.registerGlobalReceiver(StationPackets.ServerPresencePayload.TYPE, (payload, context) ->
                context.client().execute(() -> setServerModded(true)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> setServerModded(false));
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

    private static void handleSync(StationPackets.CraftingSourcesPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof CraftingStationMenu menu) {
                menu.setServerSources(payload.sources(), payload.shulkersFirst(), payload.inventoryFirst());
            }
        });
    }

    private static void handleStationState(StationPackets.StationStatePayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (net.minecraft.client.Minecraft.getInstance().player != null
                    && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof IStationMenu menu
                    && menu.stationPos().equals(payload.pos())) {
                menu.setServerState(payload.state());
            }
        });
    }

    private static void handleBrewTarget(StationPackets.StationBrewTargetPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            if (context.player().containerMenu instanceof IStationMenu menu
                    && menu.stationPos().equals(payload.pos())) {
                menu.applyBrewTarget(payload.potion());
            }
        });
    }

    public static void sendSources(ServerPlayer player, List<CraftingSourceInfo> sources, boolean shulkersFirst,
            boolean inventoryFirst) {
        ServerPlayNetworking.send(player,
                new StationPackets.CraftingSourcesPayload(sources, shulkersFirst, inventoryFirst));
    }

    public static void sendStationState(ServerPlayer player, BlockPos pos, StationState state) {
        ServerPlayNetworking.send(player, new StationPackets.StationStatePayload(pos, state));
    }
}
