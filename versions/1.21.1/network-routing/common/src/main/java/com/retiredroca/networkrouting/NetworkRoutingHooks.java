package com.retiredroca.networkrouting;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionContext;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHook;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHooks;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionOutcome;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionType;
import com.retiredroca.networkrouting.routing.RoutingLabels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

/**
 * Registers Network Routing's linker interaction on the API's shared dispatch, replacing the mod's own
 * loader events. Right-clicking a primary storage container with the Routing Linker while standing
 * inside one of this mod's inherited ranges clears its filter (crouch) or opens the routing menu
 * (normal), both owner/trusted only. Outside a range the interaction is left alone, so the access mod
 * may offer its own menu. Registration is idempotent.
 */
public final class NetworkRoutingHooks {
    private static boolean registered;

    private NetworkRoutingHooks() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        InteractionHooks.register(InteractionType.ITEM_ON_BLOCK, new LinkerOnBlockHook());
    }

    /** The Routing Linker in the main hand, right-clicking a primary storage container in range. */
    private static final class LinkerOnBlockHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || context.pos() == null) {
                return InteractionOutcome.PASS;
            }
            if (context.stack().getItem() != NetworkRoutingCommon.platform().linkerItem()) {
                return InteractionOutcome.PASS;
            }
            // Only primary storage (chest/double chest, trapped chest, barrel); leave other blocks alone.
            if (!NetworkSettings.isStorageContainer(
                    BuiltInRegistries.BLOCK.getKey(context.level().getBlockState(context.pos()).getBlock()))) {
                return InteractionOutcome.PASS;
            }
            // Only handle while the player stands inside one of this mod's inherited ranges; outside a
            // range the interaction falls through so the access mod can offer its own menu.
            if (!(context.player() instanceof ServerPlayer player)
                    || !(context.level() instanceof ServerLevel level)
                    || !NetworkRoutingAreas.isInside(player.blockPosition(),
                            NetworkRoutingAreas.activeAreas(level))) {
                return InteractionOutcome.PASS;
            }
            handle(context);
            return InteractionOutcome.HANDLED;
        }
    }

    private static void handle(InteractionContext context) {
        Level level = context.level();
        BlockPos pos = context.pos();
        if (!(level instanceof ServerLevel serverLevel) || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (context.sneaking()) {
            if (!RoutingLabels.clear(serverLevel, pos, player)) {
                player.displayClientMessage(Component.translatable("block.network_routing.terminal_locked"), true);
                return;
            }
            player.displayClientMessage(Component.translatable("message.network_routing.filter_cleared"), true);
            return;
        }
        if (!NetworkPermissions.canEdit(serverLevel, pos, player)) {
            player.displayClientMessage(Component.translatable("block.network_routing.terminal_locked"), true);
            return;
        }
        NetworkRoutingCommon.platform().openMenu(player, pos, false);
    }
}
