package com.retiredroca.mcstorageareanetwork.fabric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.NestedSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Built-in shulker-box flattening source. Scans a host's chunk radius for containers, finds shulker
 * boxes within them (up to the configured flatten depth) and exposes the box contents as ordinary
 * items. Extraction and insertion write straight back through the box {@link ItemContainerContents}.
 */
public class ShulkerItemSource implements ItemSource {
    private static final int BOX_CAPACITY = 27;

    private final java.util.Map<BlockPos, List<BoxRef>> boxRefsByHost = new java.util.HashMap<>();

    private static final class BoxRef {
        final Container container;
        final BlockPos containerPos;
        final int[] slots;

        BoxRef(Container container, BlockPos containerPos, int[] slots) {
            this.container = container;
            this.containerPos = containerPos;
            this.slots = slots;
        }
    }

    public ShulkerItemSource() {
        ShulkerBoxConfig.load();
    }

    @Override
    public String getSourceName() {
        return "Shulker Boxes";
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    @Override
    public void refresh(ServerLevel level, BlockPos hostPos, int chunkRadius) {
        if (level == null || hostPos == null) {
            return;
        }
        List<BoxRef> boxRefs = new ArrayList<>();
        ContainerOwnership.Entry host = ContainerOwnership.ownerOf(level, hostPos);
        int centerX = hostPos.getX() >> 4;
        int centerZ = hostPos.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                if (!level.getChunkSource().isPositionTicking(ChunkPos.asLong(centerX + dx, centerZ + dz))) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(centerX + dx, centerZ + dz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof Container container
                            && ContainerOwnership.canSee(level, ContainerOwnership.ownerOf(level, blockEntity.getBlockPos()), host)) {
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack stack = container.getItem(slot);
                            if (isShulkerBox(stack)) {
                                collectBoxes(boxRefs, container, stack, new int[] { slot }, 1, blockEntity.getBlockPos());
                            }
                        }
                    }
                }
            }
        }
        boxRefsByHost.put(hostPos.immutable(), boxRefs);
    }

    private void collectBoxes(List<BoxRef> boxRefs, Container container, ItemStack boxStack, int[] slots, int depth,
            BlockPos containerPos) {
        boxRefs.add(new BoxRef(container, containerPos, slots));
        if (depth >= ShulkerBoxConfig.getFlattenDepth()) {
            return;
        }
        NonNullList<ItemStack> contents = toSlots(boxContents(boxStack));
        for (int s = 0; s < contents.size(); s++) {
            ItemStack inner = contents.get(s);
            if (isShulkerBox(inner)) {
                int[] next = Arrays.copyOf(slots, slots.length + 1);
                next[slots.length] = s;
                collectBoxes(boxRefs, container, inner, next, depth + 1, containerPos);
            }
        }
    }

    private static ItemContainerContents boxContents(ItemStack box) {
        return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    private ItemStack stackAt(BoxRef ref, int depth) {
        ItemStack stack = ref.container.getItem(ref.slots[0]);
        for (int i = 1; i <= depth; i++) {
            stack = toSlots(boxContents(stack)).get(ref.slots[i]);
        }
        return stack;
    }

    private static NonNullList<ItemStack> toSlots(ItemContainerContents contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(BOX_CAPACITY, ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }

    private static ItemStack boxWithContents(ItemStack box, List<ItemStack> slots) {
        ItemStack copy = box.copy();
        copy.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return copy;
    }

    private void writeBack(BoxRef ref, NonNullList<ItemStack> leafSlots) {
        int n = ref.slots.length - 1;
        ItemStack node = boxWithContents(stackAt(ref, n), leafSlots);
        for (int i = n - 1; i >= 0; i--) {
            ItemStack parent = stackAt(ref, i);
            NonNullList<ItemStack> parentSlots = toSlots(boxContents(parent));
            parentSlots.set(ref.slots[i + 1], node);
            node = boxWithContents(parent, parentSlots);
        }
        ref.container.setItem(ref.slots[0], node);
        ref.container.setChanged();
    }

    @Override
    public List<ItemStack> enumerate(BlockPos hostPos) {
        List<ItemStack> out = new ArrayList<>();
        List<BoxRef> boxRefs = boxRefsByHost.getOrDefault(hostPos, List.of());
        for (BoxRef ref : boxRefs) {
            NonNullList<ItemStack> contents = toSlots(boxContents(stackAt(ref, ref.slots.length - 1)));
            for (int s = 0; s < contents.size(); s++) {
                ItemStack item = contents.get(s);
                if (!item.isEmpty()) {
                    out.add(item.copy());
                }
            }
        }
        return out;
    }

    @Override
    public List<NestedSource> nestedSources(BlockPos hostPos) {
        List<NestedSource> out = new ArrayList<>();
        List<BoxRef> boxRefs = boxRefsByHost.getOrDefault(hostPos, List.of());
        for (BoxRef ref : boxRefs) {
            List<ItemStack> items = new ArrayList<>();
            List<Integer> counts = new ArrayList<>();
            for (ItemStack s : toSlots(boxContents(stackAt(ref, ref.slots.length - 1)))) {
                if (!s.isEmpty()) {
                    boolean matched = false;
                    for (int j = 0; j < items.size(); j++) {
                        if (ItemStack.isSameItemSameComponents(items.get(j), s)) {
                            counts.set(j, counts.get(j) + s.getCount());
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        items.add(s.copy());
                        counts.add(s.getCount());
                    }
                }
            }
            String label = stackAt(ref, ref.slots.length - 1).getHoverName().getString();
            out.add(new NestedSource(label, ref.containerPos, items, counts));
        }
        return out;
    }

    @Override
    public int extract(BlockPos hostPos, ItemStack item, int maxCount, boolean shulkerFirst) {
        if (item.isEmpty() || maxCount <= 0) {
            return 0;
        }
        List<BoxRef> boxRefs = boxRefsByHost.getOrDefault(hostPos, List.of());
        int taken = 0;
        for (BoxRef ref : boxRefs) {
            if (taken >= maxCount) {
                break;
            }
            taken += extractFromBox(ref, item, maxCount - taken);
        }
        return taken;
    }

    private int extractFromBox(BoxRef ref, ItemStack item, int max) {
        NonNullList<ItemStack> slots = toSlots(boxContents(stackAt(ref, ref.slots.length - 1)));
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
            writeBack(ref, slots);
        }
        return removed;
    }

    @Override
    public ItemStack insert(BlockPos hostPos, ItemStack stack, boolean shulkerFirst) {
        if (stack.isEmpty() || isShulkerBox(stack)) {
            return stack;
        }
        List<BoxRef> boxRefs = boxRefsByHost.getOrDefault(hostPos, List.of());
        boolean sameTypeFirst = shulkerFirst || ShulkerBoxConfig.isSameTypeFirst();
        return insertIntoBoxes(boxRefs, stack.copy(), sameTypeFirst);
    }

    private ItemStack insertIntoBoxes(List<BoxRef> boxRefs, ItemStack stack, boolean sameTypeFirst) {
        ItemStack remaining = stack.copy();
        if (sameTypeFirst) {
            for (BoxRef ref : boxRefs) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = insertIntoBox(ref, remaining, true);
            }
        }
        for (BoxRef ref : boxRefs) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = insertIntoBox(ref, remaining, false);
        }
        return remaining;
    }

    private ItemStack insertIntoBox(BoxRef ref, ItemStack stack, boolean sameTypeOnly) {
        ItemStack remaining = stack.copy();
        NonNullList<ItemStack> slots = toSlots(boxContents(stackAt(ref, ref.slots.length - 1)));
        boolean changed = false;
        while (!remaining.isEmpty()) {
            int slot = -1;
            if (sameTypeOnly) {
                slot = firstSlotFor(slots, remaining, true);
            }
            if (slot < 0) {
                slot = firstSlotFor(slots, remaining, false);
            }
            if (slot < 0) {
                break;
            }
            ItemStack cur = slots.get(slot);
            if (cur.isEmpty()) {
                int add = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                slots.set(slot, remaining.copyWithCount(add));
                remaining.shrink(add);
            } else {
                int space = cur.getMaxStackSize() - cur.getCount();
                int add = Math.min(space, remaining.getCount());
                if (add <= 0) {
                    break;
                }
                cur.grow(add);
                remaining.shrink(add);
            }
            changed = true;
        }
        if (changed) {
            writeBack(ref, slots);
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
            if (sameTypeOnly && ItemStack.isSameItemSameComponents(cur, stack) && cur.getCount() < cur.getMaxStackSize()) {
                return s;
            }
        }
        return sameTypeOnly ? -1 : anyFree;
    }

    @Override
    public ItemStack insertIntoChild(BlockPos hostPos, BlockPos containerPos, String childLabel, ItemStack stack,
            boolean shulkerFirst) {
        if (stack.isEmpty() || isShulkerBox(stack) || containerPos == null || childLabel == null) {
            return stack;
        }
        List<BoxRef> boxRefs = boxRefsByHost.getOrDefault(hostPos, List.of());
        BoxRef target = null;
        for (BoxRef ref : boxRefs) {
            if (containerPos.equals(ref.containerPos)
                    && stackAt(ref, ref.slots.length - 1).getHoverName().getString().equals(childLabel)) {
                target = ref;
                break;
            }
        }
        if (target == null) {
            return stack;
        }
        boolean sameTypeFirst = shulkerFirst || ShulkerBoxConfig.isSameTypeFirst();
        return insertIntoBox(target, stack.copy(), sameTypeFirst);
    }
}
