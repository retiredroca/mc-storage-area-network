package com.retiredroca.storagenetwork.blockentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;
import com.retiredroca.mcstorageareanetwork.api.NestedSource;
import com.retiredroca.mcstorageareanetwork.api.NetworkHost;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.config.TerminalSettings;
import com.retiredroca.storagenetwork.menu.StorageTerminalMenu;
import com.retiredroca.storagenetwork.network.TerminalPackets.ChestSync;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-neutral Storage Terminal logic. The loader subclass supplies the block entity type. */
public abstract class AbstractStorageTerminalBlockEntity extends BlockEntity implements MenuProvider, NetworkHost {
    private static final String TAG_TIER = "tier";
    private static final long[] CHUNK_RADII = { 0, 1, 2, 3, 4, 5 };

    private int tier = 0;

    private boolean lidOpen = false;
    private long lidChangeTime = 0;

    private List<ScannedStorage> storages = new ArrayList<>();
    private long lastScan = 0;

    protected AbstractStorageTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public int getTier() {
        return tier;
    }

    public int getEffectiveMaxTier() {
        int cap = TerminalSettings.getMaxTier();
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

    @Override
    public BlockPos pos() {
        return worldPosition;
    }

    @Override
    public int chunkRadius() {
        return getChunkRadius();
    }

    @Override
    public int tier() {
        return tier;
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
            int radius = getChunkRadius();
            storages = new ArrayList<>();
            for (ScannedStorage storage : ItemNetworkServices.scanner().scan(serverLevel, worldPosition, radius)) {
                if (!storage.collectionOnly() || isExposedSink(serverLevel, storage.pos())) {
                    storages.add(storage);
                }
            }
            ItemSourceRegistry.refreshAll(serverLevel, worldPosition, radius);
        }
    }

    /**
     * A collection-only sink (Output Terminal) is normally hidden from the network listing, but its
     * owner can expose it with crouch + right-click so its contents show up here too.
     */
    private static boolean isExposedSink(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AbstractNetworkShareTerminalBlockEntity sink
                && sink.isExposedToNetwork();
    }

    public List<ScannedStorage> getStorages() {
        return storages;
    }

    /** Builds the full terminal snapshot (aggregate items + per-container rows with nested children). */
    public TerminalSyncPayload buildSync() {
        List<ItemStack> items = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (ScannedStorage storage : storages) {
            mergeInto(items, counts, storage.enumerate());
        }
        for (ItemSource source : ItemSourceRegistry.getSources()) {
            mergeInto(items, counts, source.enumerate(worldPosition));
        }

        Map<BlockPos, List<NestedSource>> nestedByPos = new HashMap<>();
        for (ItemSource source : ItemSourceRegistry.getSources()) {
            for (NestedSource nested : source.nestedSources(worldPosition)) {
                nestedByPos.computeIfAbsent(nested.pos(), p -> new ArrayList<>()).add(nested);
            }
        }

        List<ChestSync> chests = new ArrayList<>();
        for (ScannedStorage storage : storages) {
            List<ItemStack> chestItems = new ArrayList<>();
            List<Integer> chestCounts = new ArrayList<>();
            mergeInto(chestItems, chestCounts, storage.enumerate());
            List<ChestSync> children = toChildRows(nestedByPos.remove(storage.pos()));
            chests.add(new ChestSync(storage.label(), storage.pos(), chestItems, chestCounts, children));
        }
        for (ItemSource source : ItemSourceRegistry.getSources()) {
            List<ItemStack> sourceItems = new ArrayList<>();
            List<Integer> sourceCounts = new ArrayList<>();
            mergeInto(sourceItems, sourceCounts, source.enumerate(worldPosition));
            List<ChestSync> children = toChildRows(nestedByPos.remove(worldPosition));
            chests.add(new ChestSync(source.getSourceName(), worldPosition, sourceItems, sourceCounts, children));
        }
        for (Map.Entry<BlockPos, List<NestedSource>> orphan : nestedByPos.entrySet()) {
            chests.addAll(toChildRows(orphan.getValue()));
        }
        return new TerminalSyncPayload(items, counts, chests, tier);
    }

    private static void mergeInto(List<ItemStack> items, List<Integer> counts, List<ItemStack> incoming) {
        for (ItemStack stack : incoming) {
            if (stack.isEmpty() || ItemSourceRegistry.isHidden(stack)) {
                continue;
            }
            boolean matched = false;
            for (int i = 0; i < items.size(); i++) {
                if (ItemStack.isSameItemSameComponents(items.get(i), stack)) {
                    counts.set(i, counts.get(i) + stack.getCount());
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                items.add(stack.copy());
                counts.add(stack.getCount());
            }
        }
    }

    private static List<ChestSync> toChildRows(List<NestedSource> nested) {
        List<ChestSync> rows = new ArrayList<>();
        if (nested == null) {
            return rows;
        }
        for (NestedSource child : nested) {
            rows.add(new ChestSync(child.label(), child.pos(), child.items(), child.counts(), List.of()));
        }
        return rows;
    }

    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.storage_network.storage_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageTerminalMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tier = tag.getInt(TAG_TIER);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, tier);
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
