package com.retiredroca.networkrouting.neoforge;

import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;
import com.retiredroca.networkrouting.menu.RoutingMenu;
import com.retiredroca.networkrouting.network.RoutingPackets.ActionPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.FilterPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;
import com.retiredroca.networkrouting.network.RoutingPackets.ServerPresencePayload;
import com.retiredroca.networkrouting.network.RoutingPackets.TogglePayload;
import com.retiredroca.networkrouting.routing.RoutingLabels;
import com.retiredroca.networkrouting.routing.RoutingSyncHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
        PayloadRegistrar registrar = event.registrar(NetworkRoutingCommon.MODID).versioned("1");
        if (FMLLoader.getDist() == Dist.CLIENT) {
            registrar = registrar.optional();
        }
        registrar.playToClient(ServerPresencePayload.TYPE, ServerPresencePayload.STREAM_CODEC, Networking::handlePresence);
        registrar.playToClient(RoutingSyncPayload.TYPE, RoutingSyncPayload.STREAM_CODEC, Networking::handleSync);
        registrar.playToServer(FilterPayload.TYPE, FilterPayload.STREAM_CODEC, Networking::handleFilter);
        registrar.playToServer(TogglePayload.TYPE, TogglePayload.STREAM_CODEC, Networking::handleToggle);
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC, Networking::handleAction);
    }

    private static void handlePresence(ServerPresencePayload payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> serverHasMod = true);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ServerPresencePayload());
        }
    }

    public static boolean isServerModded() {
        return serverHasMod;
    }

    public static void setServerModded(boolean present) {
        serverHasMod = present;
    }

    private static void handleSync(RoutingSyncPayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.containerMenu instanceof RoutingMenu menu
                    && menu.getPos().equals(payload.pos())) {
                menu.updateSync(payload.state(), payload.tier(), payload.flags(), payload.containers());
            }
        });
    }

    private static void handleFilter(FilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof RoutingMenu menu) || !menu.getPos().equals(payload.origin())) {
                return;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            RoutingLabels.set(level, payload.container(), payload.tokens());
            sync(player, level, payload.origin(), payload.terminal());
        });
    }

    private static void handleToggle(TogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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

    private static void handleAction(ActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!context.flow().isServerbound() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
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
                PacketDistributor.sendToPlayer(player, be.buildSync());
            }
        } else {
            PacketDistributor.sendToPlayer(player, RoutingSyncHelper.linker(level, pos));
        }
    }
}
