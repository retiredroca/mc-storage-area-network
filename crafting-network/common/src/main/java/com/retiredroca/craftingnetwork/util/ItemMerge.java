package com.retiredroca.craftingnetwork.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * Merges item stacks by value. {@code ItemStack} does not override {@code equals}/{@code hashCode} in
 * 1.21, so it cannot be used as a {@code HashMap} key — this collapses stacks that share the same item
 * and components into a single entry of the combined count.
 */
public final class ItemMerge {
    private final List<ItemStack> keys = new ArrayList<>();
    private final List<Integer> counts = new ArrayList<>();

    public void add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        for (int i = 0; i < keys.size(); i++) {
            if (ItemStack.isSameItemSameComponents(keys.get(i), stack)) {
                counts.set(i, counts.get(i) + stack.getCount());
                return;
            }
        }
        keys.add(stack.copyWithCount(1));
        counts.add(stack.getCount());
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public int size() {
        return keys.size();
    }

    /** The representative stack (count 1) at index {@code i}. */
    public ItemStack key(int i) {
        return keys.get(i);
    }

    public int count(int i) {
        return counts.get(i);
    }

    public int countOf(ItemStack item) {
        int total = 0;
        for (int i = 0; i < keys.size(); i++) {
            if (ItemStack.isSameItemSameComponents(keys.get(i), item)) {
                total += counts.get(i);
            }
        }
        return total;
    }
}
