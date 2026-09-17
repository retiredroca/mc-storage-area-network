package com.retiredroca.storagenetwork;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionContext;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHook;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHooks;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionOutcome;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionType;
import com.retiredroca.storagenetwork.block.NetworkShareTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Registers Storage Network's world interactions on the API's shared dispatch. The Output Terminal's
 * crouch-click exposure toggle and its normal open both run through a single {@code BLOCK_USE} hook,
 * so the block no longer needs its own {@code useWithoutItem} override. Registration is idempotent.
 */
public final class StorageNetworkHooks {
    private static boolean registered;

    private StorageNetworkHooks() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        InteractionHooks.register(InteractionType.BLOCK_USE, new OutputTerminalHook());
    }

    /**
     * Output Terminal: crouch + empty hand toggles whether it is listed in the network (owner/trusted
     * only), while a plain empty-hand click opens it.
     */
    private static final class OutputTerminalHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || !context.emptyHand() || context.pos() == null) {
                return InteractionOutcome.PASS;
            }
            if (!(context.level().getBlockState(context.pos()).getBlock() instanceof NetworkShareTerminalBlock)) {
                return InteractionOutcome.PASS;
            }
            if (!context.clientSide()) {
                handle(context);
            }
            return InteractionOutcome.HANDLED;
        }

        @Override
        public boolean runsOnClient() {
            return true;
        }
    }

    private static void handle(InteractionContext context) {
        Level level = context.level();
        BlockPos pos = context.pos();
        if (!(level instanceof ServerLevel serverLevel)
                || !(level.getBlockEntity(pos) instanceof AbstractNetworkShareTerminalBlockEntity share)) {
            return;
        }
        Player player = context.player();
        if (context.sneaking()) {
            if (!NetworkPermissions.canEdit(serverLevel, pos, player)) {
                player.displayClientMessage(Component.translatable("block.storage_network.terminal_locked"), true);
                return;
            }
            boolean exposed = share.toggleExposedToNetwork();
            player.displayClientMessage(Component.translatable(exposed
                    ? "message.storage_network.output_exposed"
                    : "message.storage_network.output_hidden"), true);
            return;
        }
        if (!NetworkPermissions.canUse(serverLevel, pos, player)) {
            player.displayClientMessage(Component.translatable("block.storage_network.terminal_locked"), true);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            share.startOpen(serverPlayer);
            StorageNetworkCommon.platform().openShareTerminal(serverPlayer, share);
        }
    }
}
