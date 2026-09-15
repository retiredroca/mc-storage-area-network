package com.retiredroca.storagenetwork.menu;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;
import com.retiredroca.mcstorageareanetwork.api.NestedSource;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.mcstorageareanetwork.api.StorageRouter;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;
import com.retiredroca.storagenetwork.network.TerminalPackets.ChestSync;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class StorageTerminalMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_COUNT = 27;
    private static final int PLAYER_HOTBAR_COUNT = 9;
    private static final int SLOT_COUNT = PLAYER_INV_COUNT + PLAYER_HOTBAR_COUNT;

    private final AbstractStorageTerminalBlockEntity terminal;
    private final BlockPos pos;

    private List<ItemStack> serverItems = List.of();
    private List<Integer> serverCounts = List.of();
    private List<ChestSync> serverChests = List.of();
    private int serverTier = 0;
    private int dataVersion = 0;

    private BlockPos depositTargetPos;
    private String depositTargetChild;

    public StorageTerminalMenu(int containerId, Inventory playerInventory, AbstractStorageTerminalBlockEntity terminal) {
        super(StorageNetworkCommon.platform().terminalMenuType(), containerId);
        this.terminal = terminal;
        this.pos = terminal.getBlockPos();
        addPlayerInventory(playerInventory);
    }

    public static StorageTerminalMenu fromNetwork(int containerId, Inventory playerInventory, BlockPos pos) {
        return new StorageTerminalMenu(containerId, playerInventory, pos, null);
    }

    public static StorageTerminalMenu fromNetwork(int containerId, Inventory playerInventory,
            net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        return new StorageTerminalMenu(containerId, playerInventory, buffer.readBlockPos(), null);
    }

    private StorageTerminalMenu(int containerId, Inventory playerInventory, BlockPos pos,
            AbstractStorageTerminalBlockEntity terminal) {
        super(StorageNetworkCommon.platform().terminalMenuType(), containerId);
        this.terminal = terminal;
        this.pos = pos;
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int startX = 8;
        int startY = 140;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<ItemStack> getServerItems() {
        return serverItems;
    }

    public List<Integer> getServerCounts() {
        return serverCounts;
    }

    public List<ChestSync> getServerChests() {
        return serverChests;
    }

    public int getServerTier() {
        return serverTier;
    }

    public void updateServerItems(List<ItemStack> items, List<Integer> counts, List<ChestSync> chests, int tier) {
        this.serverItems = items;
        this.serverCounts = counts;
        this.serverChests = chests;
        this.serverTier = tier;
        this.dataVersion++;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public AbstractStorageTerminalBlockEntity getTerminal() {
        return terminal;
    }

    public void setDepositTarget(BlockPos pos, String child) {
        this.depositTargetPos = pos;
        this.depositTargetChild = child;
    }

    @Override
    public void removed(Player player) {
        if (terminal != null) {
            terminal.stopOpen(player);
        }
        super.removed(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= SLOT_COUNT || terminal == null) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        int count = stack.getCount();
        ItemStack remainder = depositIntoNetwork(stack);
        slot.set(remainder);
        if (remainder.getCount() < count) {
            this.broadcastChanges();
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                StorageNetworkCommon.platform().sendTerminalSync(serverPlayer, terminal.buildSync());
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack depositIntoNetwork(ItemStack stack) {
        if (terminal == null) {
            return stack;
        }
        if (depositTargetPos == null) {
            ItemStack remaining = stack.copy();
            List<ScannedStorage> routed = terminal.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel
                    ? StorageRouter.order(serverLevel, terminal.getBlockPos(), terminal.getStorages(), remaining)
                    : terminal.getStorages();
            for (ScannedStorage storage : routed) {
                remaining = storage.insert(remaining);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                for (ItemSource source : ItemSourceRegistry.getSources()) {
                    remaining = source.insert(terminal.getBlockPos(), remaining, false);
                    if (remaining.isEmpty()) {
                        break;
                    }
                }
            }
            return remaining;
        }
        ItemStack remaining = stack.copy();
        if (depositTargetChild != null && !depositTargetChild.isEmpty()) {
            for (NestedSource nested : sourcesAt(depositTargetPos)) {
                if (nested.label().equals(depositTargetChild)) {
                    for (ItemSource source : ItemSourceRegistry.getSources()) {
                        remaining = source.insertIntoChild(terminal.getBlockPos(), depositTargetPos, depositTargetChild,
                                remaining, true);
                        if (remaining.isEmpty()) {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (!remaining.isEmpty()) {
            for (ScannedStorage storage : terminal.getStorages()) {
                if (depositTargetPos.equals(storage.pos())) {
                    remaining = storage.insert(remaining);
                    break;
                }
            }
        }
        if (!remaining.isEmpty()) {
            for (NestedSource nested : sourcesAt(depositTargetPos)) {
                for (ItemSource source : ItemSourceRegistry.getSources()) {
                    remaining = source.insertIntoChild(terminal.getBlockPos(), depositTargetPos, nested.label(),
                            remaining, false);
                    if (remaining.isEmpty()) {
                        break;
                    }
                }
                if (remaining.isEmpty()) {
                    break;
                }
            }
        }
        return remaining;
    }

    private List<NestedSource> sourcesAt(BlockPos pos) {
        List<NestedSource> out = new ArrayList<>();
        for (ItemSource source : ItemSourceRegistry.getSources()) {
            for (NestedSource nested : source.nestedSources(terminal.getBlockPos())) {
                if (pos.equals(nested.pos())) {
                    out.add(nested);
                }
            }
        }
        return out;
    }

    public void doExtract(Player player, ItemStack requested, int mode) {
        if (terminal == null) {
            return;
        }
        int desired = switch (mode) {
            case 0 -> 1;
            default -> requested.getMaxStackSize();
        };
        int extracted = 0;
        for (ScannedStorage storage : terminal.getStorages()) {
            if (extracted >= desired) {
                break;
            }
            extracted += storage.extract(requested, desired - extracted);
        }
        if (extracted < desired) {
            int needed = desired - extracted;
            for (ItemSource source : ItemSourceRegistry.getSources()) {
                if (needed <= 0) {
                    break;
                }
                int taken = source.extract(terminal.getBlockPos(), requested, needed, false);
                extracted += taken;
                needed -= taken;
            }
        }
        if (extracted > 0 && player != null) {
            ItemStack output = requested.copyWithCount(extracted);
            if (!player.getInventory().add(output)) {
                player.drop(output, false);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (terminal == null) {
            return true;
        }
        return terminal.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }
}
