package com.retiredroca.mcstorageareanetwork.api;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Loader-independent helpers for navigating and mutating shulker boxes that are stored inside
 * vanilla {@link Container} slots. Uses only vanilla types so the same code works on Fabric
 * and NeoForge and can be shared between Storage Central, Crafting Central and any shulker add-on.
 */
public final class ShulkerBoxHelper {
    public static final int BOX_CAPACITY = 27;

    private ShulkerBoxHelper() {}

    /** A shulker box found in a slot of a container, described by its label and slot path. */
    public record Leaf(String label, int[] slots) {}

    public static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    /** One entry per shulker box stored directly in the block entity's container slots. */
    public static List<Leaf> collectLeaves(BlockEntity blockEntity) {
        List<Leaf> out = new java.util.ArrayList<>();
        if (!(blockEntity instanceof Container container)) {
            return out;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && isShulkerBox(stack)) {
                out.add(new Leaf(stack.getHoverName().getString(), new int[] { slot }));
            }
        }
        return out;
    }

    public static NonNullList<ItemStack> contents(ItemStack box) {
        NonNullList<ItemStack> slots = NonNullList.withSize(BOX_CAPACITY, ItemStack.EMPTY);
        box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(slots);
        return slots;
    }

    public static ItemStack withContents(ItemStack box, List<ItemStack> slots) {
        ItemStack copy = box.copy();
        copy.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return copy;
    }

    /**
     * The stack at a given depth of a slot path. Depth 0 is the box stored in the container
     * slot, depth 1 is a box nested inside that box, and so on.
     */
    public static ItemStack stackAtDepth(Container container, int[] slotPath, int depth) {
        ItemStack stack = container.getItem(slotPath[0]);
        for (int i = 1; i <= depth; i++) {
            stack = contents(stack).get(slotPath[i]);
        }
        return stack;
    }

    /** The stack at the leaf of the slot path (path[0] is the container slot). */
    public static ItemStack stackAt(Container container, int[] slotPath) {
        return stackAtDepth(container, slotPath, slotPath.length - 1);
    }

    /** Writes {@code leafSlots} back through the slot path into the container. */
    public static void writeBack(Container container, int[] slotPath, NonNullList<ItemStack> leafSlots) {
        int n = slotPath.length - 1;
        ItemStack node = withContents(stackAtDepth(container, slotPath, n), leafSlots);
        for (int i = n - 1; i >= 0; i--) {
            ItemStack parent = stackAtDepth(container, slotPath, i);
            NonNullList<ItemStack> slots = contents(parent);
            slots.set(slotPath[i + 1], node);
            node = withContents(parent, slots);
        }
        container.setItem(slotPath[0], node);
        container.setChanged();
    }

    public static int countInLeaf(Container container, int[] slotPath, ItemStack item) {
        int total = 0;
        for (ItemStack s : contents(stackAt(container, slotPath))) {
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, item)) {
                total += s.getCount();
            }
        }
        return total;
    }

    /** Removes up to {@code max} of {@code item} from the box leaf, writing back as needed. */
    public static int extractFromLeaf(Container container, int[] slotPath, ItemStack item, int max) {
        if (max <= 0) {
            return 0;
        }
        NonNullList<ItemStack> slots = contents(stackAt(container, slotPath));
        int removed = 0;
        for (int s = 0; s < slots.size() && removed < max; s++) {
            ItemStack cur = slots.get(s);
            if (!cur.isEmpty() && ItemStack.isSameItemSameComponents(cur, item)) {
                int take = Math.min(max - removed, cur.getCount());
                cur.shrink(take);
                if (cur.isEmpty()) {
                    slots.set(s, ItemStack.EMPTY);
                }
                removed += take;
            }
        }
        if (removed > 0) {
            writeBack(container, slotPath, slots);
        }
        return removed;
    }

    /** Inserts into the box leaf (same-type-first when requested), returning the remainder. */
    public static ItemStack insertIntoLeaf(Container container, int[] slotPath, ItemStack stack, boolean sameTypeOnly) {
        ItemStack remaining = stack.copy();
        NonNullList<ItemStack> slots = contents(stackAt(container, slotPath));
        boolean changed = false;
        while (!remaining.isEmpty()) {
            int slot = firstSlotFor(slots, remaining, sameTypeOnly);
            if (slot < 0) {
                break;
            }
            ItemStack cur = slots.get(slot);
            if (cur.isEmpty()) {
                int add = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                slots.set(slot, remaining.copyWithCount(add));
                remaining.shrink(add);
            } else {
                int add = Math.min(cur.getMaxStackSize() - cur.getCount(), remaining.getCount());
                if (add <= 0) {
                    break;
                }
                cur.grow(add);
                remaining.shrink(add);
            }
            changed = true;
        }
        if (changed) {
            writeBack(container, slotPath, slots);
        }
        return remaining;
    }

    private static int firstSlotFor(NonNullList<ItemStack> slots, ItemStack stack, boolean sameTypeOnly) {
        int anyFree = -1;
        for (int s = 0; s < slots.size(); s++) {
            ItemStack cur = slots.get(s);
            if (cur.isEmpty()) {
                if (anyFree < 0) {
                    anyFree = s;
                }
                continue;
            }
            if (sameTypeOnly && ItemStack.isSameItemSameComponents(cur, stack)
                    && cur.getCount() < cur.getMaxStackSize()) {
                return s;
            }
        }
        return sameTypeOnly ? -1 : anyFree;
    }
}
