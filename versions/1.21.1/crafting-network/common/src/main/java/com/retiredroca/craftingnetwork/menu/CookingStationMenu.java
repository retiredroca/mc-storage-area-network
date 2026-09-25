package com.retiredroca.craftingnetwork.menu;

import java.util.List;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationStatus;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class CookingStationMenu extends RecipeBookMenu<SingleRecipeInput, AbstractCookingRecipe>
        implements PlaceRecipe<Ingredient>, IStationMenu {
    private static final int INPUT_SLOT = 0;
    private static final int FUEL_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int PLAYER_SLOT_START = 3;
    private static final int PLAYER_INV_COUNT = 27;
    private static final int PLAYER_HOTBAR_COUNT = 9;
    private static final int CATALOG_SIZE = 63;
    private static final int SOURCE_ALL = 0;

    private final AbstractStationBlockEntity station;
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private final StationMenuSupport support;
    private final StationType type;
    private ServerPlayer owner;
    private StationState lastSentState;

    public CookingStationMenu(int containerId, Inventory playerInventory, AbstractStationBlockEntity station) {
        super(CraftingNetworkCommon.platform().cookingStationMenuType(), containerId);
        this.station = station;
        this.pos = station.getBlockPos();
        this.playerInventory = playerInventory;
        this.type = station.type();
        this.craftSlots = new TransientCraftingContainer(this, 1, 1);
        this.resultSlots = new ResultContainer();
        this.support = new StationMenuSupport(station);
        this.support.setOwner(playerInventory.player instanceof ServerPlayer sp ? sp : null);
        addOwnSlots();
        if (playerInventory.player instanceof ServerPlayer sp) {
            this.owner = sp;
        }
    }

    public static CookingStationMenu fromNetwork(int containerId, Inventory playerInventory,
            StationOpenData data) {
        return new CookingStationMenu(containerId, playerInventory, data.pos(), data.type(), data.sources());
    }

    public static CookingStationMenu fromNetwork(int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        StationOpenData data = StationOpenData.STREAM_CODEC.decode(buffer);
        return new CookingStationMenu(containerId, playerInventory, data.pos(), data.type(), data.sources());
    }

    private CookingStationMenu(int containerId, Inventory playerInventory, BlockPos pos,
            StationType type, List<CraftingSourceInfo> sources) {
        super(CraftingNetworkCommon.platform().cookingStationMenuType(), containerId);
        this.station = null;
        this.pos = pos;
        this.playerInventory = playerInventory;
        this.type = type;
        this.craftSlots = new TransientCraftingContainer(this, 1, 1);
        this.resultSlots = new ResultContainer();
        this.support = new StationMenuSupport(sources);
        addOwnSlots();
    }

    private void addOwnSlots() {
        SimpleContainer machineSlots = new SimpleContainer(3);
        this.addSlot(new DisplaySlot(machineSlots, INPUT_SLOT, 56, 17));
        this.addSlot(new DisplaySlot(machineSlots, FUEL_SLOT, 56, 53));
        this.addSlot(new DisplaySlot(machineSlots, RESULT_SLOT, 116, 35));
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
                : new StationState(StationStatus.IDLE, List.of(), 0, 0, "", List.of(), false, false);
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
    }

    @Override
    public List<CraftingSourceInfo> getSources() {
        return support.getSources();
    }

    @Override
    public void setServerSources(List<CraftingSourceInfo> fresh) {
        support.setServerSources(fresh);
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
            if (owner != null && !current.sameAs(lastSentState)) {
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
        if (id == 1000) {
            if (station != null) {
                station.setShulkersFirst(!station.isShulkersFirst());
            }
            return true;
        }
        if (id == 1001) {
            if (station != null) {
                station.setInventoryFirst(!station.isInventoryFirst());
            }
            return true;
        }
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

    @Override
    public RecipeBookType getRecipeBookType() {
        return type.recipeBookType();
    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public int getGridWidth() {
        return 1;
    }

    @Override
    public int getGridHeight() {
        return 1;
    }

    @Override
    public int getResultSlotIndex() {
        return RESULT_SLOT;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents helper) {
        helper.clear();
        if (!craftSlots.getItem(INPUT_SLOT).isEmpty()) {
            helper.accountStack(craftSlots.getItem(INPUT_SLOT));
        }
        for (int i = 0; i < CATALOG_SIZE; i++) {
            ItemStack stack = support.getCatalogItem(i);
            if (!stack.isEmpty()) {
                helper.accountStack(stack);
            }
        }
    }

    @Override
    public void clearCraftingContent() {
        craftSlots.clearContent();
        resultSlots.clearContent();
        this.broadcastChanges();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<AbstractCookingRecipe> recipe) {
        Level level = station == null ? null : station.getLevel();
        return level != null && recipe.value().matches(new SingleRecipeInput(craftSlots.getItem(INPUT_SLOT)), level);
    }

    @Override
    public boolean shouldMoveToInventory(int slotIndex) {
        return slotIndex != getResultSlotIndex();
    }

    @Override
    public void handlePlacement(boolean placeAll, RecipeHolder<?> recipe, ServerPlayer player) {
        if (!player.getRecipeBook().contains(recipe)) {
            return;
        }
        if (!(recipe.value() instanceof AbstractCookingRecipe)) {
            return;
        }
        List<Ingredient> ingredients = recipe.value().getIngredients();
        if (ingredients.isEmpty()) {
            return;
        }
        ItemStack[] items = ingredients.get(0).getItems();
        if (items.length == 0) {
            return;
        }
        // Only start if the input and a valid fuel are available in the selected source; otherwise do nothing.
        if (support.countInSources(items[0]) <= 0 || !support.hasFuelInSources()) {
            return;
        }
        clearCraftingContent();
        this.placeRecipe(getGridWidth(), getGridHeight(), getResultSlotIndex(), recipe,
                recipe.value().getIngredients().iterator(), 1);
        if (station != null) {
            station.setRecipeFilter(items[0]);
        }
        this.broadcastChanges();
    }

    @Override
    public void addItemToSlot(Ingredient item, int slot, int maxAmount, int x, int y) {
        if (!item.isEmpty()) {
            craftSlots.setItem(INPUT_SLOT, item.getItems()[0].copyWithCount(1));
        }
    }
}
