package com.retiredroca.networkrouting.routing;

import java.util.List;

import com.retiredroca.networkrouting.network.RoutingPackets;
import com.retiredroca.networkrouting.network.RoutingPackets.ContainerInfo;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;

/** Builds the linker-mode snapshot (a single container and its filter). */
public final class RoutingSyncHelper {
    private RoutingSyncHelper() {}

    public static RoutingSyncPayload linker(ServerLevel level, BlockPos containerPos) {
        List<String> tokens = RoutingLabels.get(level, containerPos);
        ContainerInfo info = new ContainerInfo(labelOf(level, containerPos), containerPos,
                tokens == null ? List.of() : tokens);
        return new RoutingSyncPayload(containerPos, RoutingPackets.STATE_LIST, 0, 0, List.of(info));
    }

    public static String labelOf(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Nameable nameable) {
            Component custom = nameable.getCustomName();
            if (custom != null) {
                return custom.getString();
            }
        }
        return level.getBlockState(pos).getBlock().getName().getString();
    }
}
