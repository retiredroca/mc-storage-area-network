package com.retiredroca.remoteaccessterminal;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

/**
 * Thin wrappers over the API permission layer for a registered terminal. Use is governed by
 * {@link NetworkPermissions#canUse} (open, or owner/trusted) and editing by
 * {@link NetworkPermissions#canEdit} (owner/trusted or operator). Both are limited to positions that
 * actually hold a link of the given dye.
 */
public final class TerminalPermissions {
    private TerminalPermissions() {
    }

    /** True when the player may open the picker or travel to this terminal. */
    public static boolean canUse(ServerLevel level, DyeColor color, BlockPos pos, Player player) {
        TerminalLinks links = TerminalLinksAccess.get(level).links();
        return links.find(color, level.dimension(), pos) != null
                && NetworkPermissions.canUse(level, pos, player);
    }

    /** True when the player may open the settings screen and mutate this terminal. */
    public static boolean canEdit(ServerLevel level, DyeColor color, BlockPos pos, Player player) {
        TerminalLinks links = TerminalLinksAccess.get(level).links();
        return links.find(color, level.dimension(), pos) != null
                && NetworkPermissions.canEdit(level, pos, player);
    }
}
