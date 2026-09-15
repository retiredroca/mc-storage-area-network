package com.retiredroca.craftingnetwork.blockentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.menu.BrewingStationMenu;
import com.retiredroca.craftingnetwork.menu.CookingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.station.BrewPath;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationStatus;
import com.retiredroca.craftingnetwork.station.StationType;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.mcstorageareanetwork.api.ShulkerBoxHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/**
 * Loader-neutral processor station (smelting/blasting/smoking/brewing). Pulls ingredients and fuel
 * from the network scanned through the MC Storage Area Network API, processes them, and pushes results back.
 */
public abstract class AbstractStationBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_TIER = "tier";
    private static final String TAG_PINNED = "pinnedSource";
    private static final String TAG_BREW = "brewTarget";
    private static final String TAG_BREW_DONE = "brewDone";
    private static final String TAG_FUEL_CHARGE = "fuelCharge";
    private static final String TAG_BREW_PROGRESS = "brewProgress";
    private static final String TAG_BURN_TIME = "burnTime";
    private static final String TAG_BURN_TOTAL = "burnTimeTotal";
    private static final String TAG_COOK_PROGRESS = "cookProgress";
    private static final String TAG_RECIPE_FILTER = "recipeFilter";
    private static final String TAG_INPUT = "input";
    private static final String TAG_FUEL = "fuel";
    private static final String TAG_RESULT = "result";
    private static final String TAG_BOTTLES = "bottles";
    private static final String TAG_INGREDIENT = "ingredient";
    private static final String TAG_SHULKERS_FIRST = "shulkersFirst";
    private static final String TAG_INVENTORY_FIRST = "inventoryFirst";
    private static final String TAG_STORED_XP = "storedXp";
    private static final long[] CHUNK_RADII = { 0, 1, 2, 3, 4, 5 };

    private static final record BoxLeaf(String label, int[] slots) {}

    private int tier = 0;
    private boolean lidOpen = false;
    private long lidChangeTime = 0;

    private final List<ScannedStorage> scannedStorages = new ArrayList<>();
    private final Map<BlockPos, List<BoxLeaf>> nestedBoxes = new HashMap<>();
    private long lastScan = 0;

    private BlockPos pinnedSource = null;
    private String brewTarget = "";
    private ItemStack recipeFilter = ItemStack.EMPTY;
    private boolean shulkersFirst = false;
    private boolean inventoryFirst = false;
    private float storedExperience = 0f;

    private int fuelCharge = 0;
    private float brewProgress = 0f;
    private boolean brewDone = false;

    private int burnTime = 0;
    private int burnTimeTotal = 0;
    private float cookProgress = 0f;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private final ItemStack[] bottles = new ItemStack[3];
    private ItemStack ingredient = ItemStack.EMPTY;

    private int networkTick = 0;
    private boolean tickErrorLogged = false;
    private List<String> craftablePotions = List.of();

    private ItemStack cachedRecipeInput = ItemStack.EMPTY;
    private RecipeHolder<? extends AbstractCookingRecipe> cachedRecipe;

    private ItemStack lastSmelted = ItemStack.EMPTY;

    private final StationType type;

    protected AbstractStationBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state) {
        super(blockEntityType, pos, state);
        this.type = StationType.fromBlock(state.getBlock());
        for (int i = 0; i < 3; i++) bottles[i] = ItemStack.EMPTY;
    }

    public int getTier() {
        return tier;
    }

    public int getEffectiveMaxTier() {
        int cap = CraftingNetworkCommon.platform().getMaxTier();
        if (level instanceof ServerLevel serverLevel) {
            int viewDistance = serverLevel.getServer().getPlayerList().getViewDistance();
            int simDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();
            int radius = Math.min(viewDistance, simDistance);
            for (int t = 0; t < CHUNK_RADII.length && CHUNK_RADII[t] <= radius; t++) {
                cap = t;
            }
        }
        return cap;
    }

    public int getChunkRadius() {
        int effectiveTier = Math.min(tier, getEffectiveMaxTier());
        return (int) CHUNK_RADII[Math.min(effectiveTier, CHUNK_RADII.length - 1)];
    }

    public StationType type() {
        return type;
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
    public boolean triggerEvent(int type, int data) {
        if (type == 1) {
            lidOpen = data != 0;
            if (level != null) {
                lidChangeTime = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(type, data);
    }

    public float getOpenNess(float partialTick) {
        if (level == null || lidChangeTime == 0) {
            return 0.0F;
        }
        float elapsed = (float) (level.getGameTime() - lidChangeTime) + partialTick;
        float t = Mth.clamp(elapsed / 7.0F, 0.0F, 1.0F);
        return lidOpen ? t : 1.0F - t;
    }

    public void scanNetwork() {
        if (level instanceof ServerLevel serverLevel) {
            long now = System.currentTimeMillis();
            if (now - lastScan < 500) {
                return;
            }
            lastScan = now;
            scannedStorages.clear();
            nestedBoxes.clear();
            int radius = getChunkRadius();
            for (ScannedStorage storage : ItemNetworkServices.scanner().scan(serverLevel, worldPosition, radius)) {
                scannedStorages.add(storage);
                BlockEntity blockEntity = serverLevel.getBlockEntity(storage.pos());
                List<BoxLeaf> leaves = collectBoxLeaves(blockEntity);
                if (!leaves.isEmpty()) {
                    nestedBoxes.put(storage.pos(), leaves);
                }
            }
        }
    }

    private static List<BoxLeaf> collectBoxLeaves(BlockEntity blockEntity) {
        List<BoxLeaf> out = new ArrayList<>();
        if (!(blockEntity instanceof Container container)) {
            return out;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty() && ShulkerBoxHelper.isShulkerBox(stack)) {
                out.add(new BoxLeaf(stack.getHoverName().getString(), new int[] { slot }));
            }
        }
        return out;
    }

    public List<ScannedStorage> getScannedStorages() {
        return scannedStorages;
    }

    public BlockPos getPinnedSource() {
        return pinnedSource;
    }

    public void setPinnedSource(BlockPos pinnedSource) {
        if (java.util.Objects.equals(this.pinnedSource, pinnedSource)) {
            return;
        }
        this.pinnedSource = pinnedSource;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isShulkersFirst() {
        return shulkersFirst;
    }

    public void setShulkersFirst(boolean shulkersFirst) {
        if (this.shulkersFirst == shulkersFirst) {
            return;
        }
        this.shulkersFirst = shulkersFirst;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isInventoryFirst() {
        return inventoryFirst;
    }

    public void setInventoryFirst(boolean inventoryFirst) {
        if (this.inventoryFirst == inventoryFirst) {
            return;
        }
        this.inventoryFirst = inventoryFirst;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Experience accumulated by completed cooks, collectable by the owner via crouch + right-click. */
    public float getStoredExperience() {
        return storedExperience;
    }

    public int collectExperience() {
        int whole = Mth.floor(storedExperience);
        if (whole > 0) {
            storedExperience -= whole;
            setChanged();
        }
        return whole;
    }

    public String getBrewTarget() {
        return brewTarget;
    }

    public ItemStack getRecipeFilter() {
        return recipeFilter;
    }

    /** Sets the item the station should process, or empty to process anything smeltable. */
    public void setRecipeFilter(ItemStack filter) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ItemStack next = filter == null ? ItemStack.EMPTY : filter.copyWithCount(1);
        if (ItemStack.isSameItemSameComponents(next, recipeFilter)) {
            return;
        }
        recipeFilter = next;
        cachedRecipe = null;
        cachedRecipeInput = ItemStack.EMPTY;
        setChanged();
    }

    public void setBrewTarget(String potionId) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        boolean changed;
        if (potionId == null || potionId.isEmpty()) {
            changed = !brewTarget.isEmpty();
            brewTarget = "";
        } else {
            // Re-selecting a target starts a fresh single batch.
            changed = !potionId.equals(brewTarget) || brewDone;
            brewTarget = potionId;
            brewDone = false;
        }
        if (changed) {
            setChanged();
        }
    }

    public void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        try {
            if ((networkTick++) % 40 == 0) {
                scanNetwork();
                recomputeCraftablePotions();
            }
            if (networkTick % 10 == 0) {
                pumpNetwork();
            }
            if (type.isBrewing()) {
                processBrewing();
            } else {
                processCooking();
            }
            tickErrorLogged = false;
        } catch (Exception e) {
            // Never let one bad tick kill the block entity ticker (which would freeze the station).
            if (!tickErrorLogged) {
                tickErrorLogged = true;
                com.retiredroca.craftingnetwork.CraftingNetworkCommon.LOGGER.error(
                        "[station] tick failed at {}", worldPosition, e);
            }
        }
    }

    private void pumpNetwork() {
        if (type.isBrewing()) {
            pumpBrewing();
        } else {
            pumpCooking();
        }
    }

    private List<ScannedStorage> pumpHandlers() {
        if (pinnedSource == null) {
            return scannedStorages;
        }
        List<ScannedStorage> out = new ArrayList<>();
        for (ScannedStorage storage : scannedStorages) {
            if (pinnedSource.equals(storage.pos())) {
                out.add(storage);
            }
        }
        return out;
    }

    private int pullFromNetwork(ItemStack item, int max) {
        if (item.isEmpty() || max <= 0) return 0;
        int taken = 0;
        for (ScannedStorage storage : pumpHandlers()) {
            if (taken >= max) break;
            taken += storage.extract(item, max - taken);
        }
        return taken;
    }

    private ItemStack pushToNetwork(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        // "Inventory first": if a player is standing right here, drop the output for them to pick up.
        if (inventoryFirst && level.hasNearbyAlivePlayer(worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 2.0)) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack.copy());
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack.copy();
        // "Shulkers first": fill shulker-box contents before plain container slots.
        if (shulkersFirst) {
            remaining = insertIntoShulkers(remaining);
        }
        // Collection-only sinks (Output Terminal) receive output next.
        for (ScannedStorage storage : pumpHandlers()) {
            if (remaining.isEmpty()) break;
            if (storage.collectionOnly()) {
                remaining = storage.insert(remaining);
            }
        }
        for (ScannedStorage storage : pumpHandlers()) {
            if (remaining.isEmpty()) break;
            remaining = storage.insert(remaining);
        }
        return remaining;
    }

    private List<ItemStack> networkCatalogue() {
        com.retiredroca.craftingnetwork.util.ItemMerge merged = new com.retiredroca.craftingnetwork.util.ItemMerge();
        for (ScannedStorage storage : pumpHandlers()) {
            if (!storage.supportsExtraction()) continue;
            for (ItemStack stack : storage.enumerate()) {
                merged.add(stack);
            }
        }
        List<ItemStack> out = new ArrayList<>(merged.size());
        for (int i = 0; i < merged.size(); i++) {
            out.add(merged.key(i).copyWithCount(merged.count(i)));
        }
        return out;
    }

    /** Recomputes which effect potions can be fully brewed from the current network contents. */
    private void recomputeCraftablePotions() {
        if (!type.isBrewing() || !(level instanceof ServerLevel serverLevel)) {
            if (!craftablePotions.isEmpty()) {
                craftablePotions = List.of();
            }
            return;
        }
        List<ItemStack> catalogue = networkCatalogue();
        boolean hasWater = containsStack(catalogue, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        boolean hasBlaze = containsStack(catalogue, new ItemStack(Items.BLAZE_POWDER));
        if (!hasWater || !hasBlaze) {
            List<String> empty = List.of();
            if (!craftablePotions.equals(empty)) {
                craftablePotions = empty;
                setChanged();
            }
            return;
        }
        List<String> out = new ArrayList<>();
        java.util.Set<String> seenNames = new java.util.HashSet<>();
        for (var holder : BuiltInRegistries.POTION.holders().toList()) {
            String id = holder.key().location().toString();
            if (id.equals("minecraft:water")) {
                continue;
            }
            // Keep one entry per potion name (the long_/strong_ variants share a display name).
            String name = PotionContents.createItemStack(Items.POTION, holder).getHoverName().getString();
            if (!seenNames.add(name)) {
                continue;
            }
            if (brewable(serverLevel, catalogue, holder, Items.POTION)) {
                out.add(id);
            }
            if (brewable(serverLevel, catalogue, holder, Items.SPLASH_POTION)) {
                out.add(id + "|splash");
            }
            if (brewable(serverLevel, catalogue, holder, Items.LINGERING_POTION)) {
                out.add(id + "|linger");
            }
        }
        if (!craftablePotions.equals(out)) {
            craftablePotions = out;
            setChanged();
        }
    }

    /** True if every ingredient for the given potion/container is present in the catalogue. */
    private static boolean brewable(ServerLevel level, List<ItemStack> catalogue,
            net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> holder,
            net.minecraft.world.item.Item container) {
        BrewPath path = BrewPath.compute(level, holder, container);
        if (path == null || path.empty()) {
            return false;
        }
        for (ItemStack ingredient : path.ingredients()) {
            if (!containsStack(catalogue, ingredient)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsStack(List<ItemStack> catalogue, ItemStack needle) {
        for (ItemStack stack : catalogue) {
            if (ItemStack.isSameItemSameComponents(stack, needle)) {
                return true;
            }
        }
        return false;
    }

    private void pumpCooking() {
        if (!result.isEmpty()) {
            ItemStack rem = pushToNetwork(result.copy());
            if (rem.isEmpty()) {
                result = ItemStack.EMPTY;
            } else {
                result = rem;
            }
        }
        // Cooking stations stay idle until a recipe is selected from the recipe book.
        if (recipeFilter.isEmpty()) {
            return;
        }
        if (input.isEmpty()) {
            ItemStack c = recipeFilter.copyWithCount(1);
            if (c.isEmpty() || recipeFor(c) == null) {
                c = selectSmeltable();
            }
            if (!c.isEmpty()) {
                int got = pullFromNetwork(c, c.getMaxStackSize());
                if (got > 0) {
                    input = c.copyWithCount(got);
                    lastSmelted = c;
                }
            }
        }
        if (fuel.isEmpty() && !input.isEmpty() && burnTime == 0) {
            ItemStack f = selectFuel();
            if (!f.isEmpty()) {
                int got = pullFromNetwork(f, 1);
                if (got > 0) {
                    fuel = f.copy();
                }
            }
        }
    }

    private ItemStack selectSmeltable() {
        if (!recipeFilter.isEmpty()) {
            return recipeFor(recipeFilter) != null ? recipeFilter.copyWithCount(1) : ItemStack.EMPTY;
        }
        for (ItemStack stack : networkCatalogue()) {
            ItemStack c = stack.copyWithCount(1);
            if (recipeFor(c) != null) {
                return c;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack selectFuel() {
        ItemStack best = ItemStack.EMPTY;
        int bestTicks = -1;
        for (ItemStack stack : networkCatalogue()) {
            if (!AbstractFurnaceBlockEntity.isFuel(stack)) continue;
            Integer ticks = AbstractFurnaceBlockEntity.getFuel().get(stack.getItem());
            int t = ticks == null ? 0 : ticks;
            if (t > bestTicks) {
                bestTicks = t;
                best = stack.copyWithCount(1);
            }
        }
        return best;
    }

    private void processCooking() {
        if (recipeFilter.isEmpty()) {
            cookProgress = 0f;
            return;
        }
        if (burnTime > 0) {
            burnTime--;
        }
        if (burnTime == 0 && !fuel.isEmpty() && !input.isEmpty()) {
            int ticks = burnDuration(fuel);
            if (ticks > 0) {
                fuel.shrink(1);
                burnTime = burnTimeTotal = ticks;
                setChanged();
            }
        }
        RecipeHolder<? extends AbstractCookingRecipe> h = recipeFor(input);
        if (h == null) {
            cookProgress = 0f;
            return;
        }
        ItemStack out = h.value().getResultItem(level.registryAccess());
        if (out.isEmpty()) {
            cookProgress = 0f;
            return;
        }
        if (!result.isEmpty() && !(ItemStack.isSameItemSameComponents(result, out) && result.getCount() + 1 <= result.getMaxStackSize())) {
            return;
        }
        // Cooking only progresses while the station is actually burning fuel.
        if (burnTime <= 0) {
            return;
        }
        float speed = type.speed(getTier());
        cookProgress += speed;
        int base = type.baseCookTicks();
        if (cookProgress >= base) {
            cookProgress -= base;
            if (result.isEmpty()) {
                result = out.copy();
            } else {
                result.grow(1);
            }
            input.shrink(1);
            // Cooking recipes award experience; the owner can collect it via crouch + right-click.
            storedExperience += h.value().getExperience();
            setChanged();
        }
    }

    private int burnDuration(ItemStack stack) {
        if (stack.isEmpty() || !AbstractFurnaceBlockEntity.isFuel(stack)) return 0;
        Integer ticks = AbstractFurnaceBlockEntity.getFuel().get(stack.getItem());
        return ticks == null ? 0 : ticks;
    }

    private RecipeHolder<? extends AbstractCookingRecipe> recipeFor(ItemStack in) {
        if (in.isEmpty()) {
            cachedRecipe = null;
            return null;
        }
        if (ItemStack.isSameItemSameComponents(cachedRecipeInput, in) && cachedRecipe != null) {
            return cachedRecipe;
        }
        cachedRecipeInput = in.copyWithCount(1);
        @SuppressWarnings("unchecked")
        RecipeHolder<? extends AbstractCookingRecipe> rh = (RecipeHolder<? extends AbstractCookingRecipe>) level.getRecipeManager()
                .getRecipeFor((RecipeType) type.recipeType(), new SingleRecipeInput(in), level).orElse(null);
        cachedRecipe = rh;
        return rh;
    }

    private void pumpBrewing() {
        BrewPath path = brewPathForTarget();
        if (path != null && !path.empty()) {
            ItemStack finished = path.states().get(path.states().size() - 1);
            for (int i = 0; i < 3; i++) {
                if (!bottles[i].isEmpty() && ItemStack.isSameItemSameComponents(bottles[i], finished)) {
                    ItemStack rem = pushToNetwork(bottles[i].copy());
                    if (rem.isEmpty()) {
                        bottles[i] = ItemStack.EMPTY;
                    } else if (pumpHandlers().isEmpty()) {
                        // No network storage at all: drop the finished potion so it is never lost.
                        net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                                worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, rem);
                        bottles[i] = ItemStack.EMPTY;
                    } else {
                        bottles[i] = rem;
                    }
                    setChanged();
                }
            }
        }
        if (brewDone) {
            // One batch per target selection: wait until the target is re-selected.
            return;
        }
        if (fuelCharge < 20) {
            int got = pullFromNetwork(new ItemStack(Items.BLAZE_POWDER), 1);
            if (got > 0) {
                fuelCharge += 20;
                setChanged();
            }
        }
        if (path == null || path.empty()) return;

        // Fill empty bottle slots with water first, then pull the ingredient for the current step.
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        for (int i = 0; i < 3; i++) {
            if (bottles[i].isEmpty()) {
                int got = pullFromNetwork(water, 1);
                if (got > 0) {
                    bottles[i] = water.copy();
                    setChanged();
                }
            }
        }
        int cur = currentStep(path);
        if (cur >= 0 && cur < path.ingredients().size() && ingredient.isEmpty()) {
            ItemStack need = path.ingredients().get(cur);
            // Brewing consumes one ingredient per step; pull a single item, not a whole stack.
            int got = pullFromNetwork(need, 1);
            if (got > 0) {
                ingredient = need.copyWithCount(got);
                setChanged();
            }
        }
    }

    private BrewPath brewPathForTarget() {
        if (brewTarget.isEmpty() || level == null) return null;
        Item container = Items.POTION;
        String id = brewTarget;
        if (id.endsWith("|splash")) {
            container = Items.SPLASH_POTION;
            id = id.substring(0, id.length() - "|splash".length());
        } else if (id.endsWith("|linger")) {
            container = Items.LINGERING_POTION;
            id = id.substring(0, id.length() - "|linger".length());
        }
        Item finalContainer = container;
        return BrewPath.compute((ServerLevel) level, BuiltInRegistries.POTION.getHolder(
                net.minecraft.resources.ResourceKey.create(Registries.POTION,
                        ResourceLocation.parse(id))).orElse(null), finalContainer);
    }

    private int currentStep(BrewPath path) {
        if (bottles[0].isEmpty()) return -1;
        for (int i = 0; i < path.states().size(); i++) {
            if (ItemStack.isSameItemSameComponents(bottles[0], path.states().get(i))) {
                return i;
            }
        }
        return -1;
    }

    private void processBrewing() {
        if (brewTarget.isEmpty() || level == null) return;
        BrewPath path = brewPathForTarget();
        if (path == null || path.empty()) return;
        int cur = currentStep(path);
        if (cur < 0 || cur >= path.states().size() - 1) return;
        if (fuelCharge <= 0) return;
        boolean allFilled = !bottles[0].isEmpty() && !bottles[1].isEmpty() && !bottles[2].isEmpty();
        if (!allFilled || ingredient.isEmpty()) return;
        fuelCharge--;
        float speed = type.speed(getTier());
        brewProgress += speed;
        int base = type.baseCookTicks();
        if (brewProgress >= base) {
            brewProgress -= base;
            try {
                ItemStack next = level.potionBrewing().mix(ingredient, bottles[0].copy());
                if (!next.isEmpty() && !ItemStack.isSameItemSameComponents(next, bottles[0])) {
                    for (int i = 0; i < 3; i++) bottles[i] = next.copy();
                    ingredient.shrink(1);
                    if (cur + 1 >= path.states().size() - 1) {
                        brewDone = true; // single batch complete
                    }
                    setChanged();
                } else {
                    com.retiredroca.craftingnetwork.CraftingNetworkCommon.LOGGER.info(
                            "[brew] no mix: ingredient={} potion={} expected={} target={}",
                            BuiltInRegistries.ITEM.getKey(ingredient.getItem()), BrewPath.potionIdOf(bottles[0]),
                            BrewPath.potionIdOf(path.states().get(cur + 1)), brewTarget);
                }
            } catch (Exception ignored) {}
        }
    }

    public List<CraftingSourceInfo> getSourceInfos() {
        List<CraftingSourceInfo> out = new ArrayList<>(scannedStorages.size());
        for (ScannedStorage storage : scannedStorages) {
            BlockPos pos = storage.pos();
            List<CraftingSourceInfo> children = new ArrayList<>();
            List<BoxLeaf> leaves = nestedBoxes.get(pos);
            if (leaves != null) {
                for (BoxLeaf leaf : leaves) {
                    children.add(new CraftingSourceInfo(pos, leaf.label()));
                }
            }
            out.add(new CraftingSourceInfo(pos, storage.label(), children));
        }
        return out;
    }

    public int getBoxLeafCount(BlockPos parentPos) {
        List<BoxLeaf> leaves = nestedBoxes.get(parentPos);
        return leaves == null ? 0 : leaves.size();
    }

    public ItemStack getBoxLeafStack(BlockPos parentPos, int childIndex) {
        List<BoxLeaf> leaves = nestedBoxes.get(parentPos);
        if (leaves == null || !(level.getBlockEntity(parentPos) instanceof Container container)) {
            return ItemStack.EMPTY;
        }
        return ShulkerBoxHelper.stackAt(container, leaves.get(childIndex).slots());
    }

    public int countInBoxLeaf(BlockPos parentPos, int childIndex, ItemStack item) {
        List<BoxLeaf> leaves = nestedBoxes.get(parentPos);
        if (leaves == null || !(level.getBlockEntity(parentPos) instanceof Container container)) {
            return 0;
        }
        return ShulkerBoxHelper.countInLeaf(container, leaves.get(childIndex).slots(), item);
    }

    public int extractFromBoxLeaf(BlockPos parentPos, int childIndex, ItemStack item, int max) {
        List<BoxLeaf> leaves = nestedBoxes.get(parentPos);
        if (leaves == null || !(level.getBlockEntity(parentPos) instanceof Container container)) {
            return 0;
        }
        return ShulkerBoxHelper.extractFromLeaf(container, leaves.get(childIndex).slots(), item, max);
    }

    /**
     * Inserts into the shulker boxes found in the scanned containers (same-type slots first, then any
     * free slot), returning the remainder. Used by the "shulkers first" routing option.
     */
    public ItemStack insertIntoShulkers(ItemStack stack) {
        ItemStack remaining = insertIntoBoxLeaves(stack.copy(), true);
        return insertIntoBoxLeaves(remaining, false);
    }

    private ItemStack insertIntoBoxLeaves(ItemStack stack, boolean sameTypeOnly) {
        ItemStack remaining = stack;
        if (remaining.isEmpty() || level == null) {
            return remaining;
        }
        for (Map.Entry<BlockPos, List<BoxLeaf>> entry : nestedBoxes.entrySet()) {
            if (remaining.isEmpty()) {
                break;
            }
            if (!(level.getBlockEntity(entry.getKey()) instanceof Container container)) {
                continue;
            }
            for (BoxLeaf leaf : entry.getValue()) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = ShulkerBoxHelper.insertIntoLeaf(container, leaf.slots(), remaining, sameTypeOnly);
            }
        }
        return remaining;
    }

    public StationStatus getStatus() {
        if (type.isBrewing()) {
            if (brewTarget.isEmpty()) return StationStatus.IDLE;
            if (bottles[0].isEmpty()) return StationStatus.IDLE;
            BrewPath path = brewPathForTarget();
            if (path != null && !path.empty() && ItemStack.isSameItemSameComponents(
                    bottles[0], path.states().get(path.states().size() - 1))) {
                return StationStatus.DONE;
            }
            return StationStatus.RUNNING;
        }
        if (!result.isEmpty()) return StationStatus.DONE;
        if (recipeFilter.isEmpty()) return StationStatus.IDLE;
        return (burnTime > 0 && !input.isEmpty()) ? StationStatus.RUNNING : StationStatus.IDLE;
    }

    public StationState getState() {
        int progressPct = type.baseCookTicks() <= 0 ? 0
                : Math.min(100, (int) (type.isBrewing() ? (brewProgress * 100.0f / type.baseCookTicks())
                        : (cookProgress * 100.0f / type.baseCookTicks())));
        if (type.isBrewing()) {
            List<com.retiredroca.craftingnetwork.station.StationSlot> slots = new ArrayList<>(5);
            for (int i = 0; i < 3; i++) {
                slots.add(new com.retiredroca.craftingnetwork.station.StationSlot(
                        "gui.crafting_network.slot_bottle", bottles[i].copy()));
            }
            slots.add(new com.retiredroca.craftingnetwork.station.StationSlot(
                    "gui.crafting_network.slot_ingredient", ingredient.copy()));
            slots.add(new com.retiredroca.craftingnetwork.station.StationSlot(
                    "gui.crafting_network.slot_fuel", fuelCharge > 0 ? new ItemStack(Items.BLAZE_POWDER) : ItemStack.EMPTY));
            StationStatus status = getStatus();
            return new StationState(status, slots, progressPct, brewTarget, craftablePotions, shulkersFirst, inventoryFirst);
        }
        List<com.retiredroca.craftingnetwork.station.StationSlot> slots = List.of(
                new com.retiredroca.craftingnetwork.station.StationSlot("gui.crafting_network.slot_input", input.copy()),
                new com.retiredroca.craftingnetwork.station.StationSlot("gui.crafting_network.slot_fuel", fuel.copy()),
                new com.retiredroca.craftingnetwork.station.StationSlot("gui.crafting_network.slot_result", result.copy()));
        StationStatus status = getStatus();
        return new StationState(status, slots, progressPct, "", List.of(), shulkersFirst, inventoryFirst);
    }

    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.crafting_network." + type.path());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (type.isBrewing()) {
            return new BrewingStationMenu(containerId, playerInventory, this);
        }
        return new CookingStationMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt(TAG_TIER);
        if (tag.contains(TAG_PINNED)) {
            int[] p = tag.getIntArray(TAG_PINNED);
            if (p.length == 3) {
                pinnedSource = new BlockPos(p[0] + worldPosition.getX(), p[1] + worldPosition.getY(),
                        p[2] + worldPosition.getZ());
            }
        } else {
            pinnedSource = null;
        }
        brewTarget = tag.getString(TAG_BREW);
        brewDone = tag.getBoolean(TAG_BREW_DONE);
        fuelCharge = tag.getInt(TAG_FUEL_CHARGE);
        brewProgress = tag.getFloat(TAG_BREW_PROGRESS);
        burnTime = tag.getInt(TAG_BURN_TIME);
        burnTimeTotal = tag.getInt(TAG_BURN_TOTAL);
        cookProgress = tag.getFloat(TAG_COOK_PROGRESS);
        if (tag.contains(TAG_INPUT)) {
            input = ItemStack.parseOptional(registries, tag.getCompound(TAG_INPUT));
        }
        if (tag.contains(TAG_FUEL)) {
            fuel = ItemStack.parseOptional(registries, tag.getCompound(TAG_FUEL));
        }
        if (tag.contains(TAG_RESULT)) {
            result = ItemStack.parseOptional(registries, tag.getCompound(TAG_RESULT));
        }
        if (tag.contains(TAG_BOTTLES)) {
            ListTag list = tag.getList(TAG_BOTTLES, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(list.size(), 3); i++) {
                bottles[i] = ItemStack.parseOptional(registries, list.getCompound(i));
            }
        }
        if (tag.contains(TAG_INGREDIENT)) {
            ingredient = ItemStack.parseOptional(registries, tag.getCompound(TAG_INGREDIENT));
        }
        if (tag.contains(TAG_RECIPE_FILTER)) {
            recipeFilter = ItemStack.parseOptional(registries, tag.getCompound(TAG_RECIPE_FILTER));
        }
        shulkersFirst = tag.getBoolean(TAG_SHULKERS_FIRST);
        inventoryFirst = tag.getBoolean(TAG_INVENTORY_FIRST);
        storedExperience = tag.getFloat(TAG_STORED_XP);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, tier);
        if (pinnedSource != null) {
            tag.putIntArray(TAG_PINNED, new int[] {
                    pinnedSource.getX() - worldPosition.getX(),
                    pinnedSource.getY() - worldPosition.getY(),
                    pinnedSource.getZ() - worldPosition.getZ()
            });
        }
        tag.putString(TAG_BREW, brewTarget);
        tag.putBoolean(TAG_BREW_DONE, brewDone);
        tag.putInt(TAG_FUEL_CHARGE, fuelCharge);
        tag.putFloat(TAG_BREW_PROGRESS, brewProgress);
        tag.putInt(TAG_BURN_TIME, burnTime);
        tag.putInt(TAG_BURN_TOTAL, burnTimeTotal);
        tag.putFloat(TAG_COOK_PROGRESS, cookProgress);
        tag.put(TAG_RECIPE_FILTER, recipeFilter.saveOptional(registries));
        tag.put(TAG_INPUT, input.saveOptional(registries));
        tag.put(TAG_FUEL, fuel.saveOptional(registries));
        tag.put(TAG_RESULT, result.saveOptional(registries));
        ListTag bottleList = new ListTag();
        for (ItemStack s : bottles) {
            bottleList.add(s.saveOptional(registries));
        }
        tag.put(TAG_BOTTLES, bottleList);
        tag.put(TAG_INGREDIENT, ingredient.saveOptional(registries));
        tag.putBoolean(TAG_SHULKERS_FIRST, shulkersFirst);
        tag.putBoolean(TAG_INVENTORY_FIRST, inventoryFirst);
        tag.putFloat(TAG_STORED_XP, storedExperience);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(TAG_TIER, tier);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
