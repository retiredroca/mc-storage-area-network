package com.retiredroca.networkrouting.fabric;

import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;
import com.retiredroca.networkrouting.menu.RoutingMenu;
import com.retiredroca.networkrouting.network.RoutingPackets.ActionPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.FilterPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.ServerPresencePayload;
import com.retiredroca.networkrouting.network.RoutingPackets.TogglePayload;
import com.retiredroca.networkrouting.routing.RoutingLabels;
import com.retiredroca.networkrouting.routing.RoutingSyncHelper;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Networking {
    private static volatile boolean serverHasMod;

    private Networking() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(FilterPayload.TYPE, FilterPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TogglePayload.TYPE, TogglePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(RoutingSyncPayload.TYPE, RoutingSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ServerPresencePayload.TYPE, ServerPresencePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FilterPayload.TYPE, Networking::handleFilter);
        ServerPlayNetworking.registerGlobalReceiver(TogglePayload.TYPE, Networking::handleToggle);
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, Networking::handleAction);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.getPlayer(), new ServerPresencePayload()));
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ServerPresencePayload.TYPE,
                (payload, context) -> context.client().execute(() -> serverHasMod = true));
        ClientPlayNetworking.registerGlobalReceiver(RoutingSyncPayload.TYPE, Networking::handleSync);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> serverHasMod = false);
    }

    public static boolean isServerModded() {
        return serverHasMod;
    }

    private static void handleSync(RoutingSyncPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            if (context.client().player != null
                    && context.client().player.containerMenu instanceof RoutingMenu menu
                    && menu.getPos().equals(payload.pos())) {
                menu.updateSync(payload.state(), payload.tier(), payload.flags(), payload.containers());
            }
        });
    }

    private static void handleFilter(FilterPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof RoutingMenu menu) || !menu.getPos().equals(payload.origin())) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (!NetworkSettings.isStorageContainer(
                    BuiltInRegistries.BLOCK.getKey(level.getBlockState(payload.container()).getBlock()))) {
                return;
            }
            RoutingLabels.set(level, payload.container(), payload.tokens());
            sync(player, level, payload.origin(), payload.terminal());
        });
    }

    private static void handleToggle(TogglePayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof RoutingMenu menu) || !menu.getPos().equals(payload.pos())) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof AbstractRoutingTerminalBlockEntity terminal) {
                terminal.setFlags(payload.flags());
                if (player.level() instanceof ServerLevel level) {
                    sync(player, level, payload.pos(), true);
                }
            }
        });
    }

    private static void handleAction(ActionPayload payload, ServerPlayNetworking.Context context) {
        context.player().server.execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof RoutingMenu menu) || !menu.getPos().equals(payload.pos())) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof AbstractRoutingTerminalBlockEntity terminal) {
                terminal.refreshBinding();
                terminal.runAction(payload.action());
                if (player.level() instanceof ServerLevel level) {
                    sync(player, level, payload.pos(), true);
                }
            }
        });
    }

    private static void sync(ServerPlayer player, ServerLevel level, BlockPos pos, boolean terminal) {
        if (terminal) {
            if (level.getBlockEntity(pos) instanceof AbstractRoutingTerminalBlockEntity be) {
                ServerPlayNetworking.send(player, be.buildSync());
            }
        } else {
            ServerPlayNetworking.send(player, RoutingSyncHelper.linker(level, pos));
        }
    }
}
