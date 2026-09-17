package com.retiredroca.mcstorageareanetwork.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/**
 * Break protection for the mod set's blocks ({@link NetworkBlock}). A block may be broken by its
 * owner, a player the owner invited, a member of the owner's scoreboard team (when
 * {@link NetworkSettings#teamSharing()} is on), or an operator. Operators breaking a block they
 * don't own must confirm first. The actual rule lives in {@link NetworkPermissions#canBreak}.
 */
public final class BreakProtection {
    private BreakProtection() {}

    /** True if the block at {@code pos} belongs to this mod set and is therefore protected. */
    public static boolean isProtected(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof NetworkBlock;
    }

    /** True if the player may break the block (non-mod blocks are always breakable). */
    public static boolean canBreak(ServerLevel level, BlockPos pos, Player player) {
        return !isProtected(level, pos) || NetworkPermissions.canBreak(level, pos, player);
    }

    /** True for an operator who isn't the owner/team/invited and must confirm before breaking. */
    public static boolean needsOpConfirmation(ServerLevel level, BlockPos pos, Player player) {
        return isProtected(level, pos) && player.hasPermissions(2)
                && !NetworkPermissions.isTrusted(level, pos, player);
    }

    /** Force-breaks a protected block (the caller must have already checked permission). */
    public static void forceBreak(ServerLevel level, BlockPos pos) {
        if (isProtected(level, pos) && level.destroyBlock(pos, true)) {
            ContainerOwnership.clearOwner(level, pos);
        }
    }
}
