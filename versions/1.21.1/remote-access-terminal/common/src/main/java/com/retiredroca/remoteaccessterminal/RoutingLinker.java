package com.retiredroca.remoteaccessterminal;

import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities;
import com.retiredroca.mcstorageareanetwork.api.capability.RouteProvider;
import com.retiredroca.mcstorageareanetwork.api.capability.TerminalRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Routing Linker branch of the air-use hook: holding the {@code network_routing:routing_linker}
 * item and right-clicking air lists the destinations the routing service offers, then travels to the
 * chosen one.
 *
 * <p>{@code network-routing} is a soft dependency. The linker item is recognised only by registry id,
 * and the routing state is read from the API's {@link RouteProvider} capability, so this class never
 * links against that mod; when the capability is absent the branch stays quiet.
 *
 * <p>The link is one-way: a player already inside a network is refused, both when the list is opened
 * and again when a travel request arrives. The provider is re-queried on every action, so a
 * destination that is no longer offered (or a player who has since entered a network) is refused, and
 * travel uses the shared cross-dimension helper.
 */
public final class RoutingLinker {
    private static final ResourceLocation LINKER_ITEM =
            ResourceLocation.parse("network_routing:routing_linker");

    private RoutingLinker() {
    }

    /** True when {@code stack} is the Routing Linker from the (optional) {@code network-routing} mod. */
    public static boolean isLinker(ItemStack stack) {
        return !stack.isEmpty() && LINKER_ITEM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /**
     * Opens the linker destination picker when the player is outside every routing inherited range.
     * The picker itself reports when the provider offers no destinations, so an empty list still
     * opens the menu rather than only flashing an action-bar message.
     *
     * @return false when no {@link RouteProvider} is installed or the player is inside a range (the
     *         caller then passes the interaction on); true when the action was handled.
     */
    public static boolean open(ServerPlayer player) {
        RouteProvider provider = NetworkCapabilities.get(RouteProvider.class).orElse(null);
        if (provider == null || !(player.level() instanceof ServerLevel)) {
            return false;
        }
        if (provider.routes(player).insideNetwork()) {
            return false;
        }
        RemoteAccessTerminalCommon.platform().openRoutingLinker(player);
        return true;
    }

    /** The destinations of the current provider result, for the picker snapshot (empty when absent). */
    public static List<TerminalRegistry.Terminal> destinations(ServerPlayer player) {
        RouteProvider provider = NetworkCapabilities.get(RouteProvider.class).orElse(null);
        return provider == null ? List.of() : provider.routes(player).destinations();
    }

    /**
     * Handles a travel request for the open linker menu. Re-queries the provider and refuses when the
     * player is now inside a network or the target is no longer offered.
     */
    public static boolean travel(ServerPlayer player, ResourceKey<Level> targetDimension, BlockPos targetPos) {
        RouteProvider provider = NetworkCapabilities.get(RouteProvider.class).orElse(null);
        if (provider == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        RouteProvider.RouteResult result = provider.routes(player);
        if (result.insideNetwork()) {
            player.displayClientMessage(
                    Component.translatable("message.remote_access_terminal.inside_scan_area"), true);
            return false;
        }
        for (TerminalRegistry.Terminal terminal : result.destinations()) {
            if (!terminal.dimension().equals(targetDimension) || !terminal.pos().equals(targetPos)) {
                continue;
            }
            if (TerminalTravel.teleport(player, level, player.blockPosition(), targetDimension, targetPos)) {
                player.closeContainer();
                return true;
            }
            return false;
        }
        return false;
    }
}
