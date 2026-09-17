package com.retiredroca.mcstorageareanetwork.api.capability;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;

/**
 * Capability published by network-routing: whether the player is inside a network, plus the
 * destinations the routing linker may offer.
 *
 * <p>Publishers register an implementation through
 * {@link com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities#register} and consumers read
 * it through {@link com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities#get}. The result is
 * computed on demand from the publisher's live state, so callers should re-query it whenever they
 * act.
 */
public interface RouteProvider {
    /** The routing state for one player. */
    record RouteResult(boolean insideNetwork, List<TerminalRegistry.Terminal> destinations) {}

    /** Resolves the player's routing state; never null. */
    RouteResult routes(ServerPlayer player);
}
