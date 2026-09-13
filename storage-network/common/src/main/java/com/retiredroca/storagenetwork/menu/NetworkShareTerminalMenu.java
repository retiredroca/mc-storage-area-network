package com.retiredroca.storagenetwork.menu;

import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class NetworkShareTerminalMenu extends AbstractContainerMenu {
    private static final int ROWS = 3;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int PLAYER_START = SIZE;

    private final Container container;
    private final BlockPos pos;

    public NetworkShareTerminalMenu(int containerId, Inventory playerInventory, Container container, BlockPos pos) {
        super(StorageNetworkCommon.platform().shareTerminalMenuType(), containerId);
        this.container = container;
        this.pos = pos;
        addSlots(playerInventory);
    }

    public static NetworkShareTerminalMenu fromNetwork(int containerId, Inventory playerInventory, BlockPos pos) {
        return new NetworkShareTerminalMenu(containerId, playerInventory, clientContainer(playerInventory, pos), pos);
    }

    public static NetworkShareTerminalMenu fromNetwork(int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        return new NetworkShareTerminalMenu(containerId, playerInventory, clientContainer(playerInventory, pos), pos);
    }

    private static Container clientContainer(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof AbstractNetworkShareTerminalBlockEntity be) {
            return be;
        }
        return new SimpleContainer(SIZE);
    }

    private void addSlots(Inventory playerInventory) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                addSlot(new Slot(container, col + row * COLS, 8 + col * 18, 18 + row * 18));
            }
        }
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, startY + 58));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
