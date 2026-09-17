package com.retiredroca.craftingnetwork;

import com.retiredroca.craftingnetwork.block.StationBlock;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionContext;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHook;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHooks;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionOutcome;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

/**
 * Registers Crafting Network's station interactions on the API's shared dispatch. A processor
 * station's crouch-click XP collect and its normal open both run through a single {@code BLOCK_USE}
 * hook, so {@link StationBlock} no longer needs its own {@code useWithoutItem} override. Registration
 * is idempotent.
 */
public final class CraftingNetworkHooks {
    private static boolean registered;

    private CraftingNetworkHooks() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        InteractionHooks.register(InteractionType.BLOCK_USE, new StationHook());
    }

    /**
     * Processor station: crouch + empty hand collects accumulated cooking experience (owner/trusted
     * only; brewing stations do not earn any), while a plain empty-hand click opens the station.
     */
    private static final class StationHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || !context.emptyHand() || context.pos() == null) {
                return InteractionOutcome.PASS;
            }
            if (!(context.level().getBlockState(context.pos()).getBlock() instanceof StationBlock)) {
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
                || !(level.getBlockEntity(pos) instanceof AbstractStationBlockEntity station)
                || !(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (context.sneaking() && !station.type().isBrewing()
                && NetworkPermissions.canEdit(serverLevel, pos, serverPlayer)) {
            int xp = station.collectExperience();
            if (xp > 0) {
                serverPlayer.giveExperiencePoints(xp);
                return;
            }
        }
        if (!NetworkPermissions.canUse(serverLevel, pos, serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable("block.crafting_network.terminal_locked"), true);
            return;
        }
        station.scanNetwork();
        station.startOpen(serverPlayer);
        CraftingNetworkCommon.platform().openStation(serverPlayer, station);
    }
}
