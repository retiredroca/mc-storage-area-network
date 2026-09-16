package com.retiredroca.mcstorageareanetwork.api;

/**
 * Marker for a block that belongs to this mod set (Storage / Crafting / Routing hardware). Such
 * blocks have their own crouch-click behaviour (Output Terminal exposure, station XP collection)
 * and are protected from being broken by non-owners, so the general container-exclusion toggle
 * skips them.
 */
public interface NetworkBlock {
}
