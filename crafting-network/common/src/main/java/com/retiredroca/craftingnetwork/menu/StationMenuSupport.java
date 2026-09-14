package com.retiredroca.craftingnetwork.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.util.ItemMerge;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.mcstorageareanetwork.api.ShulkerBoxHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/**
 * Shared source/catalog plumbing for the cooking and brewing station menus: mirrors the station's
 * scanned storages and box-leaf list into the menu, keeps the hidden catalog slots filled (server
 * side, syncs to the client via normal slot tracking), and routes shift-click deposits back into
 * the station network.
 */
public class StationMenuSupport {
    public static final int SOURCE_ALL = 0;
    public static final int SOURCE_INVENTORY = 1;
    public static final int CATALOG_SIZE = 63;

    private static final record SourceTarget(BlockPos pos, int handlerIndex, int childIndex) {}

    private final AbstractStationBlockEntity station;
    private final SimpleContainer catalog;
    private final List<CraftingSourceInfo> sources = new ArrayList<>();
    private final Map<Integer, SourceTarget> flatTargets = new LinkedHashMap<>();
    private int flatSourceCount = SOURCE_INVENTORY + 1;
    private int selectedSource = SOURCE_ALL;
    private int dataVersion;
    private ServerPlayer owner;

    public StationMenuSupport(AbstractStationBlockEntity station) {
        this.station = station;
        this.catalog = new SimpleContainer(CATALOG_SIZE);
        if (station != null) {
            this.sources.addAll(station.getSourceInfos());
            rebuildFlatTargets();
        }
    }

    public SimpleContainer getCatalog() {
        return catalog;
    }

    public StationMenuSupport(List<CraftingSourceInfo> sources) {
        this.station = null;
        this.catalog = new SimpleContainer(CATALOG_SIZE);
        this.sources.addAll(sources);
        rebuildFlatTargets();
    }

    public void setOwner(ServerPlayer owner) {
        this.owner = owner;
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

    public int getSelectedSource() {
        return selectedSource;
    }

    public int getSourceCount() {
        return flatSourceCount;
    }

    public ItemStack getCatalogItem(int index) {
        return index >= 0 && index < CATALOG_SIZE ? catalog.getItem(index) : ItemStack.EMPTY;
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

    public void setServerSources(List<CraftingSourceInfo> fresh) {
        if (!sources.equals(fresh)) {
            sources.clear();
            sources.addAll(fresh);
            rebuildFlatTargets();
            dataVersion++;
        }
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
    }

    /** Pull the freshly scanned handlers/sources from the station; returns true if changed. */
    public boolean syncFromStation() {
        if (station == null) {
            return false;
        }
        List<CraftingSourceInfo> freshSources = station.getSourceInfos();
        if (freshSources.equals(sources)) {
            return false;
        }
        sources.clear();
        sources.addAll(freshSources);
        rebuildFlatTargets();
        dataVersion++;
        if (owner != null) {
            CraftingNetworkCommon.platform().sendSources(owner, sources, false, false);
        }
        return true;
    }

    public void refreshCatalog() {
        if (station == null || station.getLevel() == null) {
            return;
        }
        ItemMerge merged = new ItemMerge();
        for (SourceTarget target : activeTargets()) {
            if (target.childIndex() < 0) {
                ScannedStorage storage = handlerFor(target);
                if (storage == null || !storage.supportsExtraction()) {
                    continue;
                }
                for (ItemStack in : storage.enumerate()) {
                    merged.add(in);
                }
            } else {
                mergeBoxLeaf(merged, target);
            }
        }
        List<Integer> order = new ArrayList<>(merged.size());
        for (int i = 0; i < merged.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> {
            int byCount = Integer.compare(merged.count(b), merged.count(a));
            if (byCount != 0) {
                return byCount;
            }
            return sortKey(merged.key(a)).compareTo(sortKey(merged.key(b)));
        });
        for (int i = 0; i < CATALOG_SIZE; i++) {
            ItemStack want = i < order.size()
                    ? merged.key(order.get(i)).copyWithCount(merged.count(order.get(i)))
                    : ItemStack.EMPTY;
            if (!ItemStack.matches(catalog.getItem(i), want)) {
                catalog.setItem(i, want);
            }
        }
    }

    private static String sortKey(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private void mergeBoxLeaf(ItemMerge merged, SourceTarget target) {
        ItemStack box = station.getBoxLeafStack(target.pos(), target.childIndex());
        if (box.isEmpty()) {
            return;
        }
        for (ItemStack s : ShulkerBoxHelper.contents(box)) {
            merged.add(s);
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
                || target.handlerIndex() >= station.getScannedStorages().size()) {
            return null;
        }
        return station.getScannedStorages().get(target.handlerIndex());
    }

    /** Count of {@code item} available across the selected source(s), boxes included. */
    public int countInSources(ItemStack item) {
        if (station == null || item.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (SourceTarget target : activeTargets()) {
            if (target.childIndex() < 0) {
                ScannedStorage storage = handlerFor(target);
                if (storage != null && storage.supportsExtraction()) {
                    total += storage.count(item);
                }
            } else {
                total += station.countInBoxLeaf(target.pos(), target.childIndex(), item);
            }
        }
        return total;
    }

    /** True if a valid fuel item is available across the selected source(s). */
    public boolean hasFuelInSources() {
        if (station == null) {
            return false;
        }
        for (SourceTarget target : activeTargets()) {
            if (target.childIndex() >= 0) {
                continue;
            }
            ScannedStorage storage = handlerFor(target);
            if (storage == null || !storage.supportsExtraction()) {
                continue;
            }
            for (ItemStack s : storage.enumerate()) {
                if (!s.isEmpty() && AbstractFurnaceBlockEntity.isFuel(s)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ItemStack depositIntoNetwork(ItemStack stack) {
        if (stack.isEmpty() || station == null) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (ScannedStorage storage : station.getScannedStorages()) {
            remaining = insertBestFit(storage, remaining);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    private static ItemStack insertBestFit(ScannedStorage storage, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        return storage.insert(stack);
    }
}
