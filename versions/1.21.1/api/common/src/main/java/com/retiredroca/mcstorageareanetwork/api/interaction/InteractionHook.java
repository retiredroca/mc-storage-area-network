package com.retiredroca.mcstorageareanetwork.api.interaction;

/**
 * A single world-interaction handler. Hooks are dispatched by {@link InteractionHooks} in ascending
 * {@link #priority()}, and the first hook returning {@link InteractionOutcome#HANDLED} wins.
 */
@FunctionalInterface
public interface InteractionHook {
    /** Handles the interaction, returning {@link InteractionOutcome#HANDLED} to consume it. */
    InteractionOutcome onUse(InteractionContext context);

    /**
     * Whether this hook also runs during client dispatch (prediction). Defaults to {@code false}, i.e.
     * the hook only runs on the logical server.
     */
    default boolean runsOnClient() {
        return false;
    }

    /** Ordering key; lower values run first. Defaults to {@code 0}; the API built-ins run last. */
    default int priority() {
        return 0;
    }
}
