package com.retiredroca.networkrouting;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities;
import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.mcstorageareanetwork.api.capability.RouteProvider;
import com.retiredroca.mcstorageareanetwork.api.capability.TerminalRegistry;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Publishes Network Routing's {@link RouteProvider} capability.
 *
 * <p>The routing state is derived from the persisted {@link RoutingBindings} areas and the
 * terminals published by the access mod's {@link TerminalRegistry}; Network Routing only talks to
 * the API and never links against that mod directly. Because the areas are stored per level, a
 * destination is offered even when its terminal's chunk is unloaded.
 */
public final class NetworkRoutingRoutes implements RouteProvider {
    private static boolean registered;

    private NetworkRoutingRoutes() {}

    /** Registers the capability once; safe to call from both loaders. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkCapabilities.register(RouteProvider.class, new NetworkRoutingRoutes());
    }

    @Override
    public RouteResult routes(ServerPlayer player) {
        ServerLevel playerLevel = player.serverLevel();
        boolean insideNetwork = NetworkRoutingAreas.isInside(
                player.blockPosition(), NetworkRoutingAreas.activeAreas(playerLevel));
        TerminalRegistry terminals = NetworkCapabilities.get(TerminalRegistry.class).orElse(null);
        if (terminals == null) {
            return new RouteResult(insideNetwork, List.of());
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new RouteResult(insideNetwork, List.of());
        }
        List<TerminalRegistry.Terminal> destinations = new ArrayList<>();
        for (TerminalRegistry.Terminal terminal : terminals.terminals()) {
            ServerLevel level = server.getLevel(terminal.dimension());
            if (level == null
                    || !NetworkRoutingAreas.isInside(terminal.pos(), NetworkRoutingAreas.activeAreas(level))
                    || !NetworkPermissions.canUse(level, terminal.pos(), player)) {
                continue;
            }
            destinations.add(terminal);
        }
        return new RouteResult(insideNetwork, destinations);
    }
}
