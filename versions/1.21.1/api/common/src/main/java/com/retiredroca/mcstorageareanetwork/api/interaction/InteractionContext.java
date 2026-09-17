package com.retiredroca.mcstorageareanetwork.api.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Everything a hook needs to answer an interaction, loader-neutral.
 *
 * <p>The context is built on whichever side dispatch runs on. On the logical server {@link #player()}
 * is a {@link ServerPlayer} and {@link #level()} a {@link ServerLevel}; on the client they are the
 * local player and the client level. Hooks branch on {@link #clientSide()} before touching
 * server-only state. The server-only types are not on the record because the same context shape is
 * used for client dispatch, where no {@link ServerPlayer} or {@link ServerLevel} exists.
 *
 * @param player     the acting player
 * @param level      the level the interaction happens in
 * @param hand       the acting hand
 * @param stack      the item held in {@code hand} (empty when the hand is empty)
 * @param pos        the targeted block position, or null for {@link InteractionType#AIR_USE}
 * @param face       the targeted block face, or null when there is no block target
 * @param location   the precise hit location, or null when there is no block target
 * @param sneaking   whether the player is using the secondary (crouch) action
 * @param clientSide whether dispatch is running on the logical client
 */
public record InteractionContext(
        Player player,
        Level level,
        InteractionHand hand,
        ItemStack stack,
        BlockPos pos,
        Direction face,
        Vec3 location,
        boolean sneaking,
        boolean clientSide) {

    /** True when the acting hand holds no item. */
    public boolean emptyHand() {
        return stack.isEmpty();
    }
}
