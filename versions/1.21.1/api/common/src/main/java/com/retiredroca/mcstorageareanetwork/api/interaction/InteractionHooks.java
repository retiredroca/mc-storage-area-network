package com.retiredroca.mcstorageareanetwork.api.interaction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.retiredroca.mcstorageareanetwork.api.CrafterAutomation;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.NetworkBlock;
import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Registry and dispatcher for the mod set's shared world interactions.
 *
 * <p>Hooks are dispatched per {@link InteractionType} in <strong>ascending</strong>
 * {@link InteractionHook#priority()}, and the first hook that returns
 * {@link InteractionOutcome#HANDLED} wins; later hooks are not consulted. Registration is stable for
 * equal priorities (earlier registration runs first), so a mod can run before or after another by
 * choosing a lower or higher value. {@link #dispatchClient} additionally only calls hooks whose
 * {@link InteractionHook#runsOnClient()} is {@code true}; {@link #dispatchServer} calls every hook.
 *
 * <p>Slot selection for a right-click on a block: the loader offers {@link InteractionType#ITEM_ON_BLOCK}
 * first when the main hand holds an item, then {@link InteractionType#BLOCK_USE} if it was not handled.
 * {@link InteractionType#AIR_USE} is only dispatched when the interaction ray is a genuine miss.
 *
 * <p>The API's own behaviours (crafter link toggle, container-exclusion toggle) are registered last
 * by {@link #registerBuiltins()}, so a mod hook at the default priority runs before them.
 */
public final class InteractionHooks {
    /** Built-ins run after every default-priority hook. */
    private static final int BUILTIN_PRIORITY = Integer.MAX_VALUE;
    private static final Map<InteractionType, List<InteractionHook>> HOOKS = new EnumMap<>(InteractionType.class);

    private static boolean builtinsRegistered;

    static {
        for (InteractionType type : InteractionType.values()) {
            HOOKS.put(type, new CopyOnWriteArrayList<>());
        }
    }

    private InteractionHooks() {}

    /** Registers a hook for {@code type}, keeping the list ordered by ascending priority. */
    public static void register(InteractionType type, InteractionHook hook) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(hook, "hook");
        List<InteractionHook> hooks = HOOKS.get(type);
        synchronized (hooks) {
            int index = hooks.size();
            for (int i = 0; i < hooks.size(); i++) {
                if (hooks.get(i).priority() > hook.priority()) {
                    index = i;
                    break;
                }
            }
            hooks.add(index, hook);
        }
    }

    /** True when no hook is registered for {@code type} (lets a loader skip work entirely). */
    public static boolean isEmpty(InteractionType type) {
        return HOOKS.get(type).isEmpty();
    }

    /** Runs the hooks that participate on the client; true when one consumed the interaction. */
    public static boolean dispatchClient(InteractionType type, InteractionContext context) {
        return dispatch(type, context, true);
    }

    /** Runs every hook for {@code type}; true when one consumed the interaction. */
    public static boolean dispatchServer(InteractionType type, InteractionContext context) {
        return dispatch(type, context, false);
    }

    /**
     * Registers the API's built-in block-use hooks (crafter toggle, then container-exclusion toggle)
     * at {@link #BUILTIN_PRIORITY}. Safe to call from every loader initializer; the second call is a
     * no-op.
     */
    public static synchronized void registerBuiltins() {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;
        register(InteractionType.BLOCK_USE, new CrafterToggleHook());
        register(InteractionType.BLOCK_USE, new ContainerExclusionHook());
    }

    private static boolean dispatch(InteractionType type, InteractionContext context, boolean clientSide) {
        for (InteractionHook hook : HOOKS.get(type)) {
            if (clientSide && !hook.runsOnClient()) {
                continue;
            }
            if (hook.onUse(context) == InteractionOutcome.HANDLED) {
                return true;
            }
        }
        return false;
    }

    /** Crouch + right-click an empty hand on a crafter: link or unlink its network pattern. */
    private static final class CrafterToggleHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || !context.sneaking() || !context.emptyHand()
                    || context.pos() == null) {
                return InteractionOutcome.PASS;
            }
            BlockState state = context.level().getBlockState(context.pos());
            if (!(state.getBlock() instanceof CrafterBlock)) {
                return InteractionOutcome.PASS;
            }
            if (!context.clientSide() && context.player() instanceof ServerPlayer player) {
                player.displayClientMessage(
                        CrafterAutomation.message(CrafterAutomation.toggle(player, context.pos())), true);
            }
            return InteractionOutcome.HANDLED;
        }

        @Override
        public boolean runsOnClient() {
            return true;
        }

        @Override
        public int priority() {
            return BUILTIN_PRIORITY;
        }
    }

    /** Crouch + right-click an empty hand on a container: toggle its block type in/out of the network. */
    private static final class ContainerExclusionHook implements InteractionHook {
        @Override
        public InteractionOutcome onUse(InteractionContext context) {
            if (context.hand() != InteractionHand.MAIN_HAND || !context.sneaking() || !context.emptyHand()
                    || context.pos() == null) {
                return InteractionOutcome.PASS;
            }
            if (context.level().getBlockState(context.pos()).getBlock() instanceof NetworkBlock) {
                return InteractionOutcome.PASS;
            }
            if (!ItemNetworkServices.scanner().hasItemStorage(context.level(), context.pos())) {
                return InteractionOutcome.PASS;
            }
            if (!context.clientSide() && context.player() instanceof ServerPlayer player) {
                player.displayClientMessage(
                        NetworkExclusions.message(NetworkExclusions.toggle(player, context.pos())), true);
            }
            return InteractionOutcome.HANDLED;
        }

        @Override
        public boolean runsOnClient() {
            return true;
        }

        @Override
        public int priority() {
            return BUILTIN_PRIORITY;
        }
    }
}
