package com.retiredroca.craftingnetwork.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.mcstorageareanetwork.api.ShulkerBoxHelper;

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
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Loader-neutral crafting station. Scans nearby storage through the MC Storage Area Network API, keeps a
 * merged source list, and opens the crafting menu. The loader supplies the block entity type and
 * the menu-provider plumbing.
 */
public abstract class AbstractCraftingStationBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_TIER = "tier";
    private static final String TAG_PINNED = "pinnedSource";
    private static final String TAG_SHULKERS_FIRST = "shulkersFirst";
    private static final long[] CHUNK_RADII = { 0, 1, 2, 3, 4, 5 };

    private static final record BoxLeaf(String label, int[] slots) {}

    private int tier = 0;
    private boolean lidOpen = false;
    private long lidChangeTime = 0;

    private final List<ScannedStorage> scannedStorages = new ArrayList<>();
    private final Map<BlockPos, List<BoxLeaf>> nestedBoxes = new HashMap<>();
    private long lastScan = 0;

    private BlockPos pinnedSource = null;
    private boolean shulkersFirst = false;

    protected AbstractCraftingStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    public void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        scanNetwork();
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
        return Component.translatable("container.crafting_network.crafting_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CraftingStationMenu(containerId, playerInventory, this);
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
        shulkersFirst = tag.getBoolean(TAG_SHULKERS_FIRST);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_TIER, tier);
        tag.putBoolean(TAG_SHULKERS_FIRST, shulkersFirst);
        if (pinnedSource != null) {
            tag.putIntArray(TAG_PINNED, new int[] {
                    pinnedSource.getX() - worldPosition.getX(),
                    pinnedSource.getY() - worldPosition.getY(),
                    pinnedSource.getZ() - worldPosition.getZ()
            });
        }
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
