package com.retiredroca.mcstorageareanetwork.api.interaction;

/**
 * The kind of world interaction offered to {@link InteractionHook}s.
 *
 * <p>A right-click on a block is offered as {@link #ITEM_ON_BLOCK} first when the main hand holds an
 * item, then as {@link #BLOCK_USE} if still unhandled. Using an item when the interaction ray is a
 * genuine miss (no block within reach) is {@link #AIR_USE}.
 */
public enum InteractionType {
    /** Right-clicking a block (empty hand, or after {@link #ITEM_ON_BLOCK} passed). */
    BLOCK_USE,
    /** Using an item when the interaction ray is a genuine miss (no block within reach). */
    AIR_USE,
    /** Right-clicking a block while the main hand holds an item. */
    ITEM_ON_BLOCK
}
