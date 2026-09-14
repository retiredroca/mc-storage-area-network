package com.retiredroca.craftingnetwork.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A non-interactive slot used behind a station's machine slots. The real machine contents are drawn
 * on top from the synced station state, so players must not be able to insert or remove items here
 * (which caused items to vanish).
 */
public class DisplaySlot extends Slot {
    public DisplaySlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }
}
