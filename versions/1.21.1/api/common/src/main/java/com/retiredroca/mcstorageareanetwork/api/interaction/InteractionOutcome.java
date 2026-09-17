package com.retiredroca.mcstorageareanetwork.api.interaction;

/** Whether an {@link InteractionHook} consumed an interaction or lets later hooks (and vanilla) run. */
public enum InteractionOutcome {
    /** Not handled: dispatch continues to the next hook. */
    PASS,
    /** Handled: dispatch stops and the interaction is consumed. */
    HANDLED
}
