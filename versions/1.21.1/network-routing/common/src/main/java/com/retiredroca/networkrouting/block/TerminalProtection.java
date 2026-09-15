package com.retiredroca.networkrouting.block;

import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** Restricts breaking of a Routing Terminal to the player who placed it. */
public final class TerminalProtection {
    private TerminalProtection() {}

    public static boolean canBreak(ServerLevel level, BlockPos pos, Player player) {
        if (!(level.getBlockState(pos).getBlock() instanceof RoutingTerminalBlock)) {
            return true;
        }
        return ContainerOwnership.isOwner(level, pos, player.getUUID());
    }
}
