package com.retiredroca.networkrouting.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;
import com.retiredroca.mcstorageareanetwork.api.NetworkHost;
import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.networkrouting.config.RoutingSettings;
import com.retiredroca.networkrouting.menu.RoutingMenu;
import com.retiredroca.networkrouting.network.RoutingPackets;
import com.retiredroca.networkrouting.network.RoutingPackets.ContainerInfo;
import com.retiredroca.networkrouting.network.RoutingPackets.RoutingSyncPayload;
import com.retiredroca.mcstorageareanetwork.api.NetworkHostLocator;
import com.retiredroca.networkrouting.routing.NetworkSorter;
import com.retiredroca.networkrouting.routing.RoutingLabels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-neutral Routing Terminal logic. The loader subclass supplies the block entity type. */
public abstract class AbstractRoutingTerminalBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FLAG_SORT = 1;
    public static final int FLAG_DEFRAG = 2;
    public static final int FLAG_TRIM = 4;

    public static final int ACTION_SORT = 0;
    public static final int ACTION_DEFRAG = 1;
    public static final int ACTION_TRIM = 2;

    private static final String TAG_FLAGS = "flags";
    private static final String TAG_TIER = "hostTier";
    private static final String TAG_BOUND = "bound";

    private int flags = 0;

    private boolean lidOpen = false;
    private long lidChangeTime = 0;

    private BlockPos hostPos;
    private int hostTier = 0;
    private List<ScannedStorage> storages = new ArrayList<>();
    private long lastScan = 0;

    protected AbstractRoutingTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- host binding / scanning --------------------------------------------

    /** Binds to (or re-binds) the nearest Storage Terminal and refreshes the container list. */
    public void refreshBinding() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        NetworkHost host = NetworkHostLocator.findNearest(serverLevel, worldPosition, RoutingSettings.searchChunks);
        BlockPos newPos = host == null ? null : host.pos();
        int newTier = host == null ? 0 : host.tier();
        boolean changed = !Objects.equals(newPos, hostPos) || newTier != hostTier;
        hostPos = newPos;
        hostTier = newTier;
        if (host == null) {
            storages = new ArrayList<>();
        } else {
            scanNetwork(host);
        }
        if (changed) {
            setChanged();
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void scanNetwork(NetworkHost host) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScan < 400) {
            return;
        }
        lastScan = now;
        storages = new ArrayList<>(
                ItemNetworkServices.scanner().scan(serverLevel, host.pos(), host.chunkRadius()));
        ItemSourceRegistry.refreshAll(serverLevel, host.pos(), host.chunkRadius());
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long time = serverLevel.getGameTime();
        if (time % Math.max(1, RoutingSettings.scanIntervalTicks) == 0) {
            refreshBinding();
        }
        if (flags != 0 && !storages.isEmpty()
                && time % Math.max(1, RoutingSettings.maintenanceIntervalTicks) == 0) {
            runMaintenance();
        }
    }

    public void runMaintenance() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if ((flags & FLAG_SORT) != 0) {
            NetworkSorter.sort(serverLevel, worldPosition, storages);
        }
        if ((flags & FLAG_DEFRAG) != 0) {
            NetworkSorter.defrag(storages);
        }
        if ((flags & FLAG_TRIM) != 0) {
            NetworkSorter.trim(serverLevel, worldPosition, storages);
        }
    }

    /** Runs a one-shot maintenance pass (see {@code ACTION_*}). */
    public void runAction(int action) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        switch (action) {
            case ACTION_SORT -> NetworkSorter.sort(serverLevel, worldPosition, storages);
            case ACTION_DEFRAG -> NetworkSorter.defrag(storages);
            case ACTION_TRIM -> NetworkSorter.trim(serverLevel, worldPosition, storages);
            default -> {
            }
        }
    }

    public boolean isBound() {
        return hostPos != null;
    }

    public int getHostTier() {
        return hostTier;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int value) {
        this.flags = value;
        setChanged();
    }

    public List<ScannedStorage> getStorages() {
        return storages;
    }

    /** Full snapshot for the terminal screen: every non-sink container plus its filter tokens. */
    public RoutingSyncPayload buildSync() {
        List<ContainerInfo> containers = new ArrayList<>();
        if (level instanceof ServerLevel serverLevel) {
            for (ScannedStorage storage : storages) {
                if (storage.collectionOnly() || !NetworkSettings.isStorageContainer(storage.blockId())) {
                    continue;
                }
                List<String> tokens = RoutingLabels.get(serverLevel, storage.pos());
                containers.add(new ContainerInfo(storage.label(), storage.pos(),
                        tokens == null ? List.of() : tokens));
            }
        }
        int state = RoutingPackets.STATE_TERMINAL | RoutingPackets.STATE_LIST
                | (isBound() ? RoutingPackets.STATE_BOUND : 0);
        return new RoutingSyncPayload(worldPosition, state, hostTier, flags, containers);
    }

    // --- lid animation ------------------------------------------------------

    public void startOpen(Player player) {
        if (level == null || level.isClientSide || lidOpen) {
            return;
        }
        lidOpen = true;
        lidChangeTime = level.getGameTime();
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 1);
    }

    public void stopOpen(Player player) {
        if (level == null || level.isClientSide || !lidOpen) {
            return;
        }
        lidOpen = false;
        lidChangeTime = level.getGameTime();
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

    // --- menu provider ------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.network_routing.routing_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RoutingMenu(containerId, playerInventory, worldPosition);
    }

    // --- nbt ----------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flags = tag.getInt(TAG_FLAGS);
        hostTier = tag.getInt(TAG_TIER);
        if (tag.getBoolean(TAG_BOUND)) {
            // Position is re-resolved on the next server tick; tint is restored from hostTier.
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_FLAGS, flags);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(TAG_FLAGS, flags);
        tag.putInt(TAG_TIER, hostTier);
        tag.putBoolean(TAG_BOUND, isBound());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
