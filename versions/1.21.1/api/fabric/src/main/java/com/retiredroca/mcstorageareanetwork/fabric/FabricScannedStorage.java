package com.retiredroca.mcstorageareanetwork.fabric;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Fabric implementation of {@link ScannedStorage} backed by a transfer {@link Storage}. */
public final class FabricScannedStorage implements ScannedStorage {
    private final BlockPos pos;
    private final String label;
    private final Storage<ItemVariant> storage;
    private final ResourceLocation blockId;
    private final boolean collectionOnly;

    public FabricScannedStorage(BlockPos pos, String label, Storage<ItemVariant> storage, ResourceLocation blockId) {
        this(pos, label, storage, blockId, false);
    }

    public FabricScannedStorage(BlockPos pos, String label, Storage<ItemVariant> storage, ResourceLocation blockId,
            boolean collectionOnly) {
        this.pos = pos;
        this.label = label;
        this.storage = storage;
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
    public boolean supportsExtraction() {
        return storage.supportsExtraction();
    }

    @Override
    public List<ItemStack> enumerate() {
        List<ItemStack> out = new ArrayList<>();
        if (!storage.supportsExtraction()) {
            return out;
        }
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            out.add(view.getResource().toStack((int) view.getAmount()));
        }
        return out;
    }

    @Override
    public int count(ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }
        int total = 0;
        ItemVariant variant = ItemVariant.of(item);
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            if (view.getResource().equals(variant)) {
                total += (int) view.getAmount();
            }
        }
        return total;
    }

    @Override
    public int extract(ItemStack item, int max) {
        if (item.isEmpty() || max <= 0 || !storage.supportsExtraction()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long amount = storage.extract(ItemVariant.of(item), max, transaction);
            transaction.commit();
            return (int) amount;
        }
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
            if (inserted <= 0) {
                return stack;
            }
            ItemStack remaining = stack.copy();
            remaining.shrink((int) inserted);
            return remaining;
        }
    }
}
