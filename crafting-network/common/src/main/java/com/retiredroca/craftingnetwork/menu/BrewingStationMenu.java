package com.retiredroca.craftingnetwork.menu;

import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationStatus;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BrewingStationMenu extends AbstractContainerMenu implements IStationMenu {
    private static final int BOTTLE_SLOT_START = 0;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int PLAYER_SLOT_START = 5;
    private static final int PLAYER_INV_COUNT = 27;
    private static final int PLAYER_HOTBAR_COUNT = 9;
    private static final int CATALOG_SIZE = 63;
    private static final int SOURCE_ALL = 0;

    private final AbstractStationBlockEntity station;
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final StationMenuSupport support;
    private final StationType type;
    private ServerPlayer owner;
    private StationState lastSentState;

    public BrewingStationMenu(int containerId, Inventory playerInventory, AbstractStationBlockEntity station) {
        super(CraftingNetworkCommon.platform().brewingStationMenuType(), containerId);
        this.station = station;
        this.pos = station.getBlockPos();
        this.playerInventory = playerInventory;
        this.type = station.type();
        this.support = new StationMenuSupport(station);
        this.support.setOwner(playerInventory.player instanceof ServerPlayer sp ? sp : null);
        addOwnSlots();
        if (playerInventory.player instanceof ServerPlayer sp) {
            this.owner = sp;
        }
    }

    public static BrewingStationMenu fromNetwork(int containerId, Inventory playerInventory,
            StationOpenData data) {
        return new BrewingStationMenu(containerId, playerInventory, data.pos(), data.type(), data.sources());
    }

    public static BrewingStationMenu fromNetwork(int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        StationOpenData data = StationOpenData.STREAM_CODEC.decode(buffer);
        return new BrewingStationMenu(containerId, playerInventory, data.pos(), data.type(), data.sources());
    }

    private BrewingStationMenu(int containerId, Inventory playerInventory, BlockPos pos,
            StationType type, List<CraftingSourceInfo> sources) {
        super(CraftingNetworkCommon.platform().brewingStationMenuType(), containerId);
        this.station = null;
        this.pos = pos;
        this.playerInventory = playerInventory;
        this.type = type;
        this.support = new StationMenuSupport(sources);
        addOwnSlots();
    }

    private void addOwnSlots() {
        this.addSlot(new Slot(new SimpleContainer(5), BOTTLE_SLOT_START + 0, 56, 51));
        this.addSlot(new Slot(new SimpleContainer(5), BOTTLE_SLOT_START + 1, 79, 58));
        this.addSlot(new Slot(new SimpleContainer(5), BOTTLE_SLOT_START + 2, 102, 51));
        this.addSlot(new Slot(new SimpleContainer(5), INGREDIENT_SLOT, 79, 17));
        this.addSlot(new Slot(new SimpleContainer(5), FUEL_SLOT, 17, 17));
        int startX = 8;
        int startY = 84;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }
        for (int i = 0; i < CATALOG_SIZE; i++) {
            this.addSlot(new Slot(support.getCatalog(), i, -10000, -10000));
        }
    }

    @Override
    public void setServerState(StationState state) {
        this.lastSentState = state;
    }

    @Override
    public StationState getState() {
        if (station != null) {
            return station.getState();
        }
        return lastSentState != null ? lastSentState
                : new StationState(StationStatus.IDLE, List.of(), 0, "", List.of());
    }

    @Override
    public StationType stationType() {
        return type;
    }

    @Override
    public BlockPos stationPos() {
        return pos;
    }

    @Override
    public AbstractStationBlockEntity getStationEntity() {
        return station;
    }

    @Override
    public void applyBrewTarget(String potionId) {
        if (station != null) {
            station.setBrewTarget(potionId);
        }
    }

    @Override
    public List<CraftingSourceInfo> getSources() {
        return support.getSources();
    }

    @Override
    public int getSelectedSource() {
        return support.getSelectedSource();
    }

    @Override
    public void selectSource(int id) {
        support.selectSource(id);
    }

    @Override
    public int getSourceCount() {
        return support.getSourceCount();
    }

    @Override
    public List<String> getSourceLabels() {
        return support.getSourceLabels();
    }

    @Override
    public ItemStack getCatalogItem(int index) {
        return support.getCatalogItem(index);
    }

    @Override
    public int getDataVersion() {
        return support.getDataVersion();
    }

    @Override
    public void broadcastChanges() {
        if (station != null) {
            if (support.syncFromStation()) {
                support.refreshCatalog();
            }
            support.refreshCatalog();
            StationState current = station.getState();
            if (!java.util.Objects.equals(current, lastSentState) && owner != null) {
                CraftingNetworkCommon.platform().sendStationState(owner, pos, current);
                lastSentState = current;
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < PLAYER_SLOT_START) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= SOURCE_ALL && id < getSourceCount()) {
            selectSource(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= PLAYER_SLOT_START && index < PLAYER_SLOT_START + PLAYER_INV_COUNT + PLAYER_HOTBAR_COUNT) {
            Slot slot = this.slots.get(index);
            if (slot == null || !slot.hasItem() || station == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            int count = stack.getCount();
            ItemStack remainder = support.depositIntoNetwork(stack);
            slot.set(remainder);
            if (remainder.getCount() < count) {
                this.broadcastChanges();
            }
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        if (station != null) {
            station.stopOpen(player);
        }
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (station == null) {
            return true;
        }
        return station.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }
}
