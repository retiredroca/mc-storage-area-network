package com.retiredroca.storagenetwork.block;

import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/** Restricts breaking of a Storage Network terminal to the player who placed it. */
public final class TerminalProtection {
    private TerminalProtection() {}

    public static boolean canBreak(ServerLevel level, BlockPos pos, Player player) {
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof StorageTerminalBlock) && !(block instanceof NetworkShareTerminalBlock)) {
            return true;
        }
        return ContainerOwnership.isOwner(level, pos, player.getUUID());
    }
}
