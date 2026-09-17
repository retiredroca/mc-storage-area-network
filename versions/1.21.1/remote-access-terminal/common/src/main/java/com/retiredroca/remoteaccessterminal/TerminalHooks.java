package com.retiredroca.remoteaccessterminal;

import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionContext;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHook;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHooks;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionOutcome;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionType;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Registers Remote Access Terminal's world interactions on the API's shared dispatch.
 *
 * <p>Precedence, within every slot:
 * <ol>
 *   <li>The Routing Linker in the main hand, aimed at a block or air, while the player is not inside
 *       a routing inherited range: opens the linker destination picker. Without a
 *       {@code RouteProvider} the branch passes, and inside a range it passes so
 *       {@code network-routing} can handle its own storage containers.</li>
 *   <li>A terminal block: opens the destination picker, with an empty hand or any item in the main
 *       hand and with no sneak special case. Registered on both {@code ITEM_ON_BLOCK} and
 *       {@code BLOCK_USE} so a held item's own use cannot bypass it.</li>
 *   <li>Anything else passes to the next hook or to vanilla.</li>
 * </ol>
 * The linker branch is registered at a lower priority than the terminal branch so it runs first.
 * {@code AIR_USE} is registered only for the linker, so right-clicking air stays vanilla.
 * Registration is idempotent.
 */
public final class TerminalHooks {
    private static final int LINKER_PRIORITY = -100;
    private static final int TERMINAL_PRIORITY = 0;

    private static boolean registered;

    private TerminalHooks() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        LinkerHook linker = new LinkerHook();
        TerminalBlockHook terminal = new TerminalBlockHook();
        InteractionHooks.register(InteractionType.AIR_USE, linker);
        InteractionHooks.register(InteractionType.ITEM_ON_BLOCK, linker);
        InteractionHooks.register(InteractionType.ITEM_ON_BLOCK, terminal);
        InteractionHooks.register(InteractionType.BLOCK_USE, linker);
        InteractionHooks.register(InteractionType.BLOCK_USE, terminal);
    }

    /**
     * The Routing Linker: opens the destination picker for any target while the player is outside
     * every routing inherited range. Inside a range, or without a routing provider, it passes so the
     * routing mod and vanilla keep the interaction.
     */
    private static final class LinkerHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || !RoutingLinker.isLinker(context.stack())) {
                return InteractionOutcome.PASS;
            }
            if (context.player() instanceof ServerPlayer player && RoutingLinker.open(player)) {
                return InteractionOutcome.HANDLED;
            }
            return InteractionOutcome.PASS;
        }

        @Override
        public int priority() {
            return LINKER_PRIORITY;
        }
    }

    /** A terminal block: a plain click opens the destination picker, whatever the main hand holds. */
    private static final class TerminalBlockHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || context.pos() == null) {
                return InteractionOutcome.PASS;
            }
            if (!(context.level().getBlockState(context.pos()).getBlock() instanceof TerminalBlock terminal)) {
                return InteractionOutcome.PASS;
            }
            if (!context.clientSide()) {
                handle(context, terminal.getColor());
            }
            return InteractionOutcome.HANDLED;
        }

        @Override
        public boolean runsOnClient() {
            return true;
        }

        @Override
        public int priority() {
            return TERMINAL_PRIORITY;
        }
    }

    private static void handle(InteractionContext context, DyeColor color) {
        Level level = context.level();
        BlockPos pos = context.pos();
        if (!(level instanceof ServerLevel serverLevel) || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        TerminalLinks links = TerminalLinksAccess.get(serverLevel).links();
        if (links.find(color, serverLevel.dimension(), pos) == null) {
            return;
        }
        if (!TerminalPermissions.canUse(serverLevel, color, pos, player)) {
            player.displayClientMessage(Component.translatable("message.remote_access_terminal.no_access"), true);
            return;
        }
        RemoteAccessTerminalCommon.platform().openTerminal(player, serverLevel.dimension(), pos, color, false);
    }
}
