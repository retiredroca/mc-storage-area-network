package com.retiredroca.remoteaccessterminal.menu;

import java.util.List;
import java.util.UUID;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.SortMode;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.Destination;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A slotless menu backing both the destination picker and the settings screen. The server opens it
 * with {@link Data}, then pushes a {@link TerminalSyncPayload} snapshot right after.
 */
public class TerminalMenu extends AbstractContainerMenu {
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final boolean settings;
    private final boolean linker;

    private DyeColor color;
    private String name;
    private boolean open;
    private boolean chunkLoader;
    private long chunkLoaderUntil;
    private int chunkLoaderQueuePosition;
    private boolean canEdit;
    private SortMode sortMode = SortMode.NEAREST;
    private List<Destination> destinations = List.of();
    private List<Integer> counts = List.of();
    private int total;
    private List<UUID> invites = List.of();
    private int version;

    public TerminalMenu(int containerId, Inventory playerInventory, ResourceKey<Level> dimension, BlockPos pos,
            DyeColor color, boolean settings, boolean linker) {
        super(RemoteAccessTerminalCommon.platform().terminalMenuType(), containerId);
        this.dimension = dimension;
        this.pos = pos;
        this.color = color;
        this.settings = settings;
        this.linker = linker;
    }

    public static TerminalMenu fromNetwork(int containerId, Inventory playerInventory, Data data) {
        return new TerminalMenu(containerId, playerInventory, data.dimension(), data.pos(), data.color(),
                data.settings(), data.linker());
    }

    public static TerminalMenu fromNetwork(int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        return fromNetwork(containerId, playerInventory, Data.STREAM_CODEC.decode(buffer));
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isSettings() {
        return settings;
    }

    public boolean isLinker() {
        return linker;
    }

    public DyeColor getColor() {
        return color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isChunkLoader() {
        return chunkLoader;
    }

    /** Epoch millis at which the chunk-loader lease expires, or 0 when it never expires. */
    public long getChunkLoaderUntil() {
        return chunkLoaderUntil;
    }

    /** The player's position in the chunk-loader queue, or 0 when they are not queued. */
    public int getChunkLoaderQueuePosition() {
        return chunkLoaderQueuePosition;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public int count(DyeColor dye) {
        int index = dye.getId();
        return index >= 0 && index < counts.size() ? counts.get(index) : 0;
    }

    /** Total number of registered terminals across every colour and dimension. */
    public int getTotal() {
        return total;
    }

    public List<UUID> getInvites() {
        return invites;
    }

    /** Increments whenever a snapshot arrives, letting screens rebuild after {@code init()}. */
    public int getVersion() {
        return version;
    }

    /** Applies a server snapshot. */
    public void updateSync(TerminalSyncPayload payload) {
        this.color = payload.color();
        this.name = payload.name();
        this.open = payload.open();
        this.chunkLoader = payload.chunkLoader();
        this.chunkLoaderUntil = payload.chunkLoaderUntil();
        this.chunkLoaderQueuePosition = payload.chunkLoaderQueuePosition();
        this.canEdit = payload.canEdit();
        this.sortMode = SortMode.byId(payload.sortMode());
        this.destinations = payload.destinations();
        this.counts = payload.counts();
        this.total = payload.total();
        this.invites = payload.invites();
        this.version++;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.level().dimension().equals(dimension)) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    /** Opening data sent over the extended screen-handler channel. */
    public record Data(ResourceKey<Level> dimension, BlockPos pos, DyeColor color, boolean settings,
            boolean linker) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION), Data::dimension,
                BlockPos.STREAM_CODEC, Data::pos,
                DyeColor.STREAM_CODEC, Data::color,
                ByteBufCodecs.BOOL, Data::settings,
                ByteBufCodecs.BOOL, Data::linker,
                Data::new);
    }
}
