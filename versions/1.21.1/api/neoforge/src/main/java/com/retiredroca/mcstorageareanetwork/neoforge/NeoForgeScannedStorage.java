package com.retiredroca.mcstorageareanetwork.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** NeoForge implementation of {@link ScannedStorage} backed by an {@link IItemHandler}. */
public final class NeoForgeScannedStorage implements ScannedStorage {
    private final BlockPos pos;
    private final String label;
    private final IItemHandler handler;
    private final ResourceLocation blockId;
    private final boolean collectionOnly;

    public NeoForgeScannedStorage(BlockPos pos, String label, IItemHandler handler, ResourceLocation blockId) {
        this(pos, label, handler, blockId, false);
    }

    public NeoForgeScannedStorage(BlockPos pos, String label, IItemHandler handler, ResourceLocation blockId,
            boolean collectionOnly) {
        this.pos = pos;
        this.label = label;
        this.handler = handler;
        this.blockId = blockId;
        this.collectionOnly = collectionOnly;
    }

    @Override
    public ResourceLocation blockId() {
        return blockId;
    }

    @Override
    public boolean collectionOnly() {
        return collectionOnly;
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public List<ItemStack> enumerate() {
        List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                out.add(stack.copy());
            }
        }
        return out;
    }

    @Override
    public int count(ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public int extract(ItemStack item, int max) {
        if (item.isEmpty() || max <= 0) {
            return 0;
        }
        int taken = 0;
        for (int slot = 0; slot < handler.getSlots() && taken < max; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, item)) {
                ItemStack extracted = handler.extractItem(slot, max - taken, false);
                taken += extracted.getCount();
            }
        }
        return taken;
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemHandlerHelper.insertItem(handler, stack.copy(), false);
    }
}
