package com.retiredroca.storagenetwork.blockentity;

import java.util.List;

import com.retiredroca.storagenetwork.menu.NetworkShareTerminalMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-neutral Output Terminal: a collection-only sink that receives crafted output. It is
 * hidden from terminal listings but crafted results are routed into it first.
 */
public abstract class AbstractNetworkShareTerminalBlockEntity extends BlockEntity implements Container, MenuProvider {
    /** Double-chest capacity (6 rows x 9 columns). */
    public static final int SIZE = 54;
    private static final String TAG_ITEMS = "Items";

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    private boolean lidOpen = false;
    private long lidChangeTime = 0;

    protected AbstractNetworkShareTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void startOpen(Player player) {
        if (level == null || level.isClientSide || lidOpen) {
            return;
        }
        lidOpen = true;
        lidChangeTime = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.1F + 0.9F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 1);
    }

    public void stopOpen(Player player) {
        if (level == null || level.isClientSide || !lidOpen) {
            return;
        }
        lidOpen = false;
        lidChangeTime = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 0.5F,
                level.random.nextFloat() * 0.1F + 0.9F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
    }

    @Override
    public boolean triggerEvent(int id, int data) {
        if (id == 1) {
            lidOpen = data != 0;
            if (level != null) {
                lidChangeTime = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(id, data);
    }

    public float getOpenNess(float partialTick) {
        if (level == null || lidChangeTime == 0) {
            return 0.0F;
        }
        float elapsed = (float) (level.getGameTime() - lidChangeTime) + partialTick;
        float t = Mth.clamp(elapsed / 7.0F, 0.0F, 1.0F);
        return lidOpen ? t : 1.0F - t;
    }

    /** Best-fit insert used for crafted output routing. Returns the remainder. */
    public ItemStack insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        boolean changed = false;
        for (int i = 0; i < SIZE && !remaining.isEmpty(); i++) {
            ItemStack current = items.get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, remaining)
                    && current.getCount() < current.getMaxStackSize()) {
                int move = Math.min(current.getMaxStackSize() - current.getCount(), remaining.getCount());
                current.grow(move);
                remaining.shrink(move);
                changed = true;
            }
        }
        for (int i = 0; i < SIZE && !remaining.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                int move = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                items.set(i, remaining.copyWithCount(move));
                remaining.shrink(move);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
        return remaining;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.storage_network.network_share_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NetworkShareTerminalMenu(containerId, playerInventory, this, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack stack : items) {
            list.add(stack.saveOptional(registries));
        }
        tag.put(TAG_ITEMS, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ListTag list = tag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), SIZE); i++) {
            items.set(i, ItemStack.parseOptional(registries, list.getCompound(i)));
        }
    }
}
