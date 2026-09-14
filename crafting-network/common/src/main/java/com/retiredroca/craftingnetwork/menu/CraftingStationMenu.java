package com.retiredroca.craftingnetwork.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.mcstorageareanetwork.api.ShulkerBoxHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class CraftingStationMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> implements PlaceRecipe<Ingredient> {
    private static final int TEMPLATE_COUNT = 9;
    private static final int RESULT_SLOT_INDEX = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int PLAYER_SLOT_START = 10;
    private static final int PLAYER_INV_COUNT = 27;
    private static final int PLAYER_HOTBAR_COUNT = 9;
    private static final int CATALOG_SIZE = 63;
    private static final int SOURCE_ALL = 0;
    private static final int SOURCE_INVENTORY = 1;

    private final AbstractCraftingStationBlockEntity station;
    private final BlockPos pos;
    private final Inventory playerInventory;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots;
    private List<ScannedStorage> menuStorages;
    private List<ScannedStorage> storagesAll;
    private List<CraftingSourceInfo> sources;
    private static final record SourceTarget(BlockPos pos, int handlerIndex, int childIndex) {}
    private final Map<Integer, SourceTarget> flatTargets = new LinkedHashMap<>();
    private int flatSourceCount = SOURCE_INVENTORY + 1;
    private final SimpleContainer catalog;
    private ServerPlayer owner;
    private int dataVersion;
    private int selectedSource = SOURCE_ALL;
    private boolean clientShulkersFirst;
    private static final int SHULKERS_FIRST_BUTTON = 1000;
    private ItemStack resultOutput = ItemStack.EMPTY;
    private int resultPerCraft = 1;

    public CraftingStationMenu(int containerId, Inventory playerInventory, AbstractCraftingStationBlockEntity station) {
        super(CraftingNetworkCommon.platform().craftingStationMenuType(), containerId);
        this.station = station;
        this.pos = station.getBlockPos();
        this.playerInventory = playerInventory;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();
        this.menuStorages = new ArrayList<>(station.getScannedStorages());
        this.sources = station.getSourceInfos();
        rebuildFlatTargets();
        this.storagesAll = dedupeStorages(menuStorages);
        BlockPos pin = station.getPinnedSource();
        if (pin != null) {
            for (Map.Entry<Integer, SourceTarget> e : flatTargets.entrySet()) {
                if (e.getValue().childIndex() < 0 && pin.equals(e.getValue().pos())) {
                    this.selectedSource = e.getKey();
                    break;
                }
            }
        }
        this.catalog = new SimpleContainer(CATALOG_SIZE);
        if (playerInventory.player instanceof ServerPlayer serverPlayer) {
            this.owner = serverPlayer;
        }
        addOwnSlots();
    }

    public static CraftingStationMenu fromNetwork(int containerId, Inventory playerInventory,
            CraftingStationOpenData data) {
        return new CraftingStationMenu(containerId, playerInventory, data.pos(), data.sources(),
                data.shulkersFirst());
    }

    public static CraftingStationMenu fromNetwork(int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        List<CraftingSourceInfo> sources = readSources(buffer);
        boolean shulkersFirst = buffer.readBoolean();
        return new CraftingStationMenu(containerId, playerInventory, pos, sources, shulkersFirst);
    }

    public static void writeSources(RegistryFriendlyByteBuf buffer, List<CraftingSourceInfo> sources) {
        buffer.writeVarInt(sources.size());
        for (CraftingSourceInfo info : sources) {
            CraftingSourceInfo.STREAM_CODEC.encode(buffer, info);
        }
    }

    public static List<CraftingSourceInfo> readSources(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<CraftingSourceInfo> sources = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sources.add(CraftingSourceInfo.STREAM_CODEC.decode(buffer));
        }
        return sources;
    }

    private CraftingStationMenu(int containerId, Inventory playerInventory, BlockPos pos,
            List<CraftingSourceInfo> sources, boolean shulkersFirst) {
        super(CraftingNetworkCommon.platform().craftingStationMenuType(), containerId);
        this.station = null;
        this.pos = pos;
        this.playerInventory = playerInventory;
        this.clientShulkersFirst = shulkersFirst;
        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();
        this.menuStorages = new ArrayList<>();
        this.storagesAll = new ArrayList<>();
        this.sources = sources;
        rebuildFlatTargets();
        this.catalog = new SimpleContainer(CATALOG_SIZE);
        addOwnSlots();
    }

    private static List<ScannedStorage> dedupeStorages(List<ScannedStorage> storages) {
        List<ScannedStorage> result = new ArrayList<>();
        for (ScannedStorage storage : storages) {
            boolean dup = false;
            for (ScannedStorage existing : result) {
                if (sameInventory(existing, storage)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                result.add(storage);
            }
        }
        return result;
    }

    private static boolean sameInventory(ScannedStorage a, ScannedStorage b) {
        if (a == b) {
            return true;
        }
        return contents(a).equals(contents(b));
    }

    private static Map<ItemStack, Integer> contents(ScannedStorage storage) {
        Map<ItemStack, Integer> map = new HashMap<>();
        for (ItemStack stack : storage.enumerate()) {
            if (stack.isEmpty()) {
                continue;
            }
            map.merge(stack.copyWithCount(1), stack.getCount(), Integer::sum);
        }
        return map;
    }

    private void addOwnSlots() {
        this.addSlot(new Slot(resultSlots, 0, 124, 35));
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }
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
            this.addSlot(new Slot(catalog, i, -10000, -10000));
        }
    }

    public List<String> getSourceLabels() {
        List<String> labels = new ArrayList<>();
        labels.add("All Storage");
        labels.add("Inventory");
        for (CraftingSourceInfo info : sources) {
            BlockPos p = info.pos();
            labels.add(info.label() + " — chunk " + (p.getX() >> 4) + "," + (p.getZ() >> 4)
                    + " · " + p.getX() + "," + p.getY() + "," + p.getZ());
            for (CraftingSourceInfo child : info.children()) {
                labels.add("  " + child.label());
            }
        }
        return labels;
    }

    public int getSourceCount() {
        return flatSourceCount;
    }

    private void rebuildFlatTargets() {
        flatTargets.clear();
        int id = SOURCE_INVENTORY + 1;
        for (int i = 0; i < sources.size(); i++) {
            CraftingSourceInfo info = sources.get(i);
            flatTargets.put(id, new SourceTarget(info.pos(), i, -1));
            id++;
            for (int c = 0; c < info.children().size(); c++) {
                flatTargets.put(id, new SourceTarget(info.children().get(c).pos(), i, c));
                id++;
            }
        }
        flatSourceCount = id;
    }

    public List<CraftingSourceInfo> getSources() {
        return sources;
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public void setServerSources(List<CraftingSourceInfo> sources, boolean shulkersFirst) {
        this.clientShulkersFirst = shulkersFirst;
        if (!this.sources.equals(sources)) {
            this.sources = new ArrayList<>(sources);
            rebuildFlatTargets();
            this.dataVersion++;
        }
    }

    /** Whether the "shulkers first" output option is enabled for this station. */
    public boolean isShulkersFirst() {
        return station != null ? station.isShulkersFirst() : clientShulkersFirst;
    }

    public int getSelectedSource() {
        return selectedSource;
    }

    public void selectSource(int id) {
        if (id < 0 || id >= flatSourceCount) {
            return;
        }
        this.selectedSource = id;
        if (station != null) {
            SourceTarget target = flatTargets.get(id);
            if (id == SOURCE_ALL || id == SOURCE_INVENTORY || target == null || target.childIndex() >= 0) {
                station.setPinnedSource(null);
            } else {
                station.setPinnedSource(target.pos());
            }
        }
        refreshCatalog();
        updateResult();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == SHULKERS_FIRST_BUTTON) {
            if (station != null) {
                station.setShulkersFirst(!station.isShulkersFirst());
                if (owner != null) {
                    CraftingNetworkCommon.platform().sendSources(owner, sources, station.isShulkersFirst());
                }
            }
            return true;
        }
        selectSource(id);
        return id >= 0 && id < getSourceCount();
    }

    public ItemStack getCatalogItem(int index) {
        return index >= 0 && index < CATALOG_SIZE ? catalog.getItem(index) : ItemStack.EMPTY;
    }

    private void refreshCatalog() {
        if (station == null || station.getLevel() == null) {
            return;
        }
        Map<ItemStack, Integer> merged = new HashMap<>();
        for (SourceTarget target : activeTargets()) {
            if (target.childIndex() < 0) {
                ScannedStorage storage = handlerFor(target);
                if (storage == null || !storage.supportsExtraction()) {
                    continue;
                }
                for (ItemStack in : storage.enumerate()) {
                    if (!in.isEmpty()) {
                        ItemStack key = in.copyWithCount(1);
                        merged.merge(key, in.getCount(), Integer::sum);
                    }
                }
            } else {
                mergeBoxLeaf(merged, target);
            }
        }
        List<Map.Entry<ItemStack, Integer>> sorted = new ArrayList<>(merged.entrySet());
        sorted.sort(Comparator.comparingInt(entry -> -entry.getValue()));
        for (int i = 0; i < CATALOG_SIZE; i++) {
            ItemStack want = i < sorted.size() ? sorted.get(i).getKey().copyWithCount(sorted.get(i).getValue())
                    : ItemStack.EMPTY;
            if (!ItemStack.matches(catalog.getItem(i), want)) {
                catalog.setItem(i, want);
            }
        }
    }

    private void mergeBoxLeaf(Map<ItemStack, Integer> merged, SourceTarget target) {
        ItemStack box = station.getBoxLeafStack(target.pos(), target.childIndex());
        if (box.isEmpty()) {
            return;
        }
        for (ItemStack s : ShulkerBoxHelper.contents(box)) {
            if (!s.isEmpty()) {
                ItemStack key = s.copyWithCount(1);
                merged.merge(key, s.getCount(), Integer::sum);
            }
        }
    }

    private List<SourceTarget> activeTargets() {
        List<SourceTarget> out = new ArrayList<>();
        if (selectedSource == SOURCE_ALL) {
            out.addAll(flatTargets.values());
        } else if (selectedSource > SOURCE_INVENTORY) {
            SourceTarget target = flatTargets.get(selectedSource);
            if (target != null) {
                out.add(target);
            }
        }
        return out;
    }

    private ScannedStorage handlerFor(SourceTarget target) {
        if (target == null || target.childIndex() >= 0 || target.handlerIndex() < 0
                || target.handlerIndex() >= menuStorages.size()) {
            return null;
        }
        return menuStorages.get(target.handlerIndex());
    }

    private void refreshHandlers() {
        if (station == null) {
            return;
        }
        List<CraftingSourceInfo> freshSources = station.getSourceInfos();
        if (freshSources.equals(sources)) {
            return;
        }
        this.sources = new ArrayList<>(freshSources);
        this.menuStorages = new ArrayList<>(station.getScannedStorages());
        this.storagesAll = dedupeStorages(menuStorages);
        rebuildFlatTargets();
        this.dataVersion++;
        if (owner != null) {
            CraftingNetworkCommon.platform().sendSources(owner, sources, station.isShulkersFirst());
        }
    }

    @Override
    public void broadcastChanges() {
        if (station != null) {
            refreshHandlers();
            refreshCatalog();
        }
        super.broadcastChanges();
    }

    public BlockPos getPos() {
        return pos;
    }

    public AbstractCraftingStationBlockEntity getStation() {
        return station;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == RESULT_SLOT_INDEX) {
            if (clickType == ClickType.QUICK_MOVE) {
                craftResult(player, true); // shift-click: a stack, or as much as possible
            } else if (clickType == ClickType.PICKUP) {
                craftResult(player, false); // plain click: one craft
            }
            return;
        }
        if (slotId >= CRAFT_SLOT_START && slotId < CRAFT_SLOT_START + TEMPLATE_COUNT) {
            int templateIndex = slotId - CRAFT_SLOT_START;
            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                if (!getCarried().isEmpty()) {
                    ItemStack template = getCarried().copy();
                    template.setCount(1);
                    craftSlots.setItem(templateIndex, template);
                } else {
                    craftSlots.setItem(templateIndex, ItemStack.EMPTY);
                }
            }
            this.broadcastChanges();
            updateResult();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void updateResult() {
        if (resultSlots == null) {
            return;
        }
        this.resultOutput = ItemStack.EMPTY;
        this.resultPerCraft = 1;
        resultSlots.setItem(0, ItemStack.EMPTY);
        Level level = station == null ? null : station.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            this.broadcastChanges();
            return;
        }
        CraftingInput input = craftSlots.asCraftInput();
        RecipeHolder<CraftingRecipe> holder = serverLevel.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, serverLevel).orElse(null);
        if (holder == null) {
            this.broadcastChanges();
            return;
        }
        if (!hasIngredients()) {
            this.broadcastChanges();
            return;
        }
        ItemStack out = holder.value().assemble(input, level.registryAccess());
        if (out.isEmpty()) {
            this.broadcastChanges();
            return;
        }
        this.resultOutput = out.copy();
        this.resultPerCraft = Math.max(1, out.getCount());
        int maxCrafts = maxCraftable();
        int batch = Math.max(this.resultPerCraft,
                Math.min(maxCrafts * this.resultPerCraft, out.getMaxStackSize()));
        resultSlots.setItem(0, out.copyWithCount(batch));
        resultSlots.setChanged();
        this.broadcastChanges();
    }

    private int maxCraftable() {
        Map<ItemStack, Integer> needed = neededIngredients();
        if (needed.isEmpty()) {
            return 0;
        }
        int max = Integer.MAX_VALUE;
        for (Map.Entry<ItemStack, Integer> entry : needed.entrySet()) {
            int available = networkCount(entry.getKey()) + playerCount(entry.getKey());
            max = Math.min(max, available / entry.getValue());
        }
        return Math.max(0, max);
    }

    /** True if the network + player have everything needed for at least one craft of {@code recipe}. */
    private boolean canCraftRecipe(RecipeHolder<?> recipe) {
        Map<ItemStack, Integer> needed = new HashMap<>();
        for (Ingredient ingredient : recipe.value().getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) {
                continue;
            }
            ItemStack key = items[0].copyWithCount(1);
            needed.merge(key, 1, Integer::sum);
        }
        if (needed.isEmpty()) {
            return false;
        }
        for (Map.Entry<ItemStack, Integer> entry : needed.entrySet()) {
            if (networkCount(entry.getKey()) + playerCount(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasIngredients() {
        Map<ItemStack, Integer> needed = neededIngredients();
        for (Map.Entry<ItemStack, Integer> entry : needed.entrySet()) {
            if (networkCount(entry.getKey()) + playerCount(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private Map<ItemStack, Integer> neededIngredients() {
        Map<ItemStack, Integer> needed = new HashMap<>();
        for (int i = 0; i < TEMPLATE_COUNT; i++) {
            ItemStack template = craftSlots.getItem(i);
            if (template.isEmpty()) {
                continue;
            }
            ItemStack key = template.copyWithCount(1);
            needed.merge(key, 1, Integer::sum);
        }
        return needed;
    }

    private int networkCount(ItemStack item) {
        if (station == null) {
            return 0;
        }
        int total = 0;
        for (SourceTarget target : activeTargets()) {
            if (target.childIndex() < 0) {
                total += countHandler(handlerFor(target), item);
            } else {
                total += station.countInBoxLeaf(target.pos(), target.childIndex(), item);
            }
        }
        return total;
    }

    private static int countHandler(ScannedStorage storage, ItemStack item) {
        if (storage == null || !storage.supportsExtraction()) {
            return 0;
        }
        return storage.count(item);
    }

    private int playerCount(ItemStack item) {
        int total = 0;
        for (int s = 0; s < 36; s++) {
            ItemStack in = playerInventory.getItem(s);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, item)) {
                total += in.getCount();
            }
        }
        return total;
    }

    private void craftResult(Player player, boolean stack) {
        Level level = station == null ? null : station.getLevel();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (resultOutput.isEmpty()) {
            return;
        }
        int perCraft = Math.max(1, resultPerCraft);
        int maxItems = stack ? 64 : perCraft;
        int produced = 0;
        while (produced < maxItems) {
            if (!consumeIngredients(player)) {
                break;
            }
            routeOutput(player, resultOutput.copy());
            produced += perCraft;
        }
        updateResult();
    }

    private boolean consumeIngredients(Player player) {
        Map<ItemStack, Integer> needed = neededIngredients();
        for (Map.Entry<ItemStack, Integer> entry : needed.entrySet()) {
            if (networkCount(entry.getKey()) + playerCount(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        for (Map.Entry<ItemStack, Integer> entry : needed.entrySet()) {
            int remaining = entry.getValue();
            remaining -= consumeFromNetwork(entry.getKey(), remaining);
            if (remaining > 0) {
                remaining -= consumeFromPlayer(player, entry.getKey(), remaining);
            }
        }
        return true;
    }

    private int consumeFromNetwork(ItemStack item, int max) {
        if (station == null) {
            return 0;
        }
        int taken = 0;
        for (SourceTarget target : activeTargets()) {
            if (taken >= max) {
                break;
            }
            if (target.childIndex() < 0) {
                ScannedStorage storage = handlerFor(target);
                if (storage == null || !storage.supportsExtraction()) {
                    continue;
                }
                taken += storage.extract(item, max - taken);
            }
        }
        for (SourceTarget target : activeTargets()) {
            if (taken >= max) {
                break;
            }
            if (target.childIndex() >= 0) {
                taken += station.extractFromBoxLeaf(target.pos(), target.childIndex(), item, max - taken);
            }
        }
        return taken;
    }

    private int consumeFromPlayer(Player player, ItemStack item, int max) {
        int taken = 0;
        for (int s = 0; s < 36 && taken < max; s++) {
            ItemStack in = playerInventory.getItem(s);
            if (!in.isEmpty() && ItemStack.isSameItemSameComponents(in, item)) {
                int amount = Math.min(max - taken, in.getCount());
                in.shrink(amount);
                playerInventory.setItem(s, in);
                taken += amount;
            }
        }
        return taken;
    }

    /**
     * Routes a crafted stack: into a network container already holding the item, then the scanned
     * container nearest to the station, then the player's inventory, and finally drops at the player.
     */
    private void routeOutput(Player player, ItemStack stack) {
        if (stack.isEmpty() || station == null) {
            return;
        }
        ItemStack remaining = stack;
        // "Shulkers first": fill shulker-box contents before anything else.
        if (station.isShulkersFirst()) {
            remaining = station.insertIntoShulkers(remaining);
            if (remaining.isEmpty()) {
                return;
            }
        }
        // Collection-only sinks (Network Share Terminal) receive crafted output first.
        for (ScannedStorage storage : station.getScannedStorages()) {
            if (remaining.isEmpty()) {
                return;
            }
            if (storage.collectionOnly()) {
                remaining = storage.insert(remaining);
            }
        }
        if (remaining.isEmpty()) {
            return;
        }
        for (ScannedStorage storage : station.getScannedStorages()) {
            if (remaining.isEmpty()) {
                return;
            }
            if (storage.count(remaining) > 0) {
                remaining = storage.insert(remaining);
            }
        }
        if (remaining.isEmpty()) {
            return;
        }
        List<ScannedStorage> nearest = new ArrayList<>(station.getScannedStorages());
        BlockPos stationPos = station.getBlockPos();
        nearest.sort(Comparator.comparingDouble(s -> s.pos().distSqr(stationPos)));
        for (ScannedStorage storage : nearest) {
            if (remaining.isEmpty()) {
                return;
            }
            remaining = storage.insert(remaining);
        }
        if (remaining.isEmpty()) {
            return;
        }
        player.getInventory().add(remaining);
        if (!remaining.isEmpty()) {
            player.drop(remaining, false);
        }
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
            ItemStack remainder = depositIntoNetwork(stack);
            slot.set(remainder);
            if (remainder.getCount() < count) {
                this.broadcastChanges();
            }
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack depositIntoNetwork(ItemStack stack) {
        if (stack.isEmpty() || station == null) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (ScannedStorage storage : menuStorages.isEmpty() ? station.getScannedStorages() : menuStorages) {
            remaining = storage.insert(remaining);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
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
        return RecipeBookType.CRAFTING;
    }

    @Override
    public int getSize() {
        return TEMPLATE_COUNT + 1;
    }

    @Override
    public int getGridWidth() {
        return 3;
    }

    @Override
    public int getGridHeight() {
        return 3;
    }

    @Override
    public int getResultSlotIndex() {
        return RESULT_SLOT_INDEX;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents helper) {
        helper.clear();
        craftSlots.fillStackedContents(helper);
        for (int i = 0; i < CATALOG_SIZE; i++) {
            ItemStack stack = catalog.getItem(i);
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
        updateResult();
    }

    @Override
    public boolean recipeMatches(RecipeHolder<CraftingRecipe> recipe) {
        Level level = station == null ? null : station.getLevel();
        return level != null && recipe.value().matches(craftSlots.asCraftInput(), level);
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
        if (!(recipe.value() instanceof CraftingRecipe)) {
            return;
        }
        if (!canCraftRecipe(recipe)) {
            return; // not enough ingredients: do nothing when the recipe is clicked
        }
        clearCraftingContent();
        placeRecipe(getGridWidth(), getGridHeight(), getResultSlotIndex(), recipe,
                recipe.value().getIngredients().iterator(), 1);
        this.broadcastChanges();
        updateResult();
    }

    @Override
    public void addItemToSlot(Ingredient item, int slot, int maxAmount, int x, int y) {
        if (!item.isEmpty()) {
            int craftIndex = slot - CRAFT_SLOT_START;
            if (craftIndex >= 0 && craftIndex < TEMPLATE_COUNT) {
                craftSlots.setItem(craftIndex, item.getItems()[0].copyWithCount(1));
            }
        }
    }
}
