package com.retiredroca.networkrouting.routing;

import com.retiredroca.mcstorageareanetwork.api.StoragePriority;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Makes a labeled container win insertion priority when the stack matches its filter. The score is
 * {@code BASE - distance} so, among matching containers, the one nearest the routing hardware is
 * tried first.
 */
public final class NetworkRoutingRule implements StoragePriority {
    public static final NetworkRoutingRule INSTANCE = new NetworkRoutingRule();

    private static final int BASE = 1_000_000;

    private NetworkRoutingRule() {}

    @Override
    public int priority(ServerLevel level, BlockPos refPos, BlockPos pos, ItemStack stack) {
        ContainerFilter filter = RoutingLabels.filter(level, pos);
        if (filter == null || !filter.matches(stack)) {
            return 0;
        }
        int distance = (int) Math.sqrt(refPos.distSqr(pos));
        return BASE - Math.min(distance, BASE - 1);
    }
}
