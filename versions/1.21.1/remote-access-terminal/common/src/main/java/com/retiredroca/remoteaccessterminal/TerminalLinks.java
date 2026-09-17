package com.retiredroca.remoteaccessterminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.remoteaccessterminal.config.TerminalSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/**
 * Loader-agnostic store of terminal links.
 *
 * <p>Each link carries the dimension and position of the terminal, its owner (permanent), a name, a
 * sort mode and whether it keeps its chunks loaded. Permission state is no longer stored here: whether a terminal is open and who its owner
 * invited live in the API's {@link NetworkPermissions}, and the terminals a player may use are simply
 * every link of a dye that {@link NetworkPermissions#canUse} accepts.
 *
 * <p>Links for every dye and every dimension share this one store, so the terminal cap is counted
 * across all colours and all dimensions.
 */
public final class TerminalLinks {
    /** A single placed terminal: its dimension, position, owner and presentation. */
    public static final class Link {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private UUID owner;
        private String name;
        private SortMode sortMode = SortMode.NEAREST;
        private boolean chunkLoader;
        private UUID chunkLoaderHolder;
        private long chunkLoaderUntil;
        private long chunkLoaderQueuedSince;

        public Link(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public BlockPos pos() {
            return pos;
        }

        public UUID owner() {
            return owner;
        }

        public void setOwner(UUID owner) {
            this.owner = owner;
        }

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = cleanName(name);
        }

        public SortMode sortMode() {
            return sortMode;
        }

        public void setSortMode(SortMode sortMode) {
            this.sortMode = sortMode == null ? SortMode.NEAREST : sortMode;
        }

        /** True while this terminal keeps its own chunk ticking and its lazy ring loaded. */
        public boolean isChunkLoader() {
            return chunkLoader;
        }

        public void setChunkLoader(boolean chunkLoader) {
            this.chunkLoader = chunkLoader;
        }

        /** The player currently holding the chunk-loader lease, or {@code null} when it is free. */
        public UUID chunkLoaderHolder() {
            return chunkLoaderHolder;
        }

        public void setChunkLoaderHolder(UUID chunkLoaderHolder) {
            this.chunkLoaderHolder = chunkLoaderHolder;
        }

        /** Epoch millis at which the lease expires, or 0 when it never expires. */
        public long chunkLoaderUntil() {
            return chunkLoaderUntil;
        }

        public void setChunkLoaderUntil(long chunkLoaderUntil) {
            this.chunkLoaderUntil = chunkLoaderUntil;
        }

        /** Epoch millis at which the holder joined the waiting queue, or 0 when not queued. */
        public long chunkLoaderQueuedSince() {
            return chunkLoaderQueuedSince;
        }

        public void setChunkLoaderQueuedSince(long chunkLoaderQueuedSince) {
            this.chunkLoaderQueuedSince = chunkLoaderQueuedSince;
        }

        /** True while this terminal holds a lease; see {@link #chunkLoaderHolder()}. */
        public boolean holdsChunkLoader() {
            return chunkLoader;
        }

        /** True while this terminal is waiting for a lease. */
        public boolean isChunkLoaderQueued() {
            return chunkLoaderQueuedSince > 0;
        }
    }

    private final Map<DyeColor, List<Link>> links = new EnumMap<>(DyeColor.class);

    private List<Link> slots(DyeColor color) {
        return links.computeIfAbsent(color, key -> new ArrayList<>());
    }

    /** Number of registered terminals of this dye across all dimensions. */
    public int usedCount(DyeColor color) {
        return slots(color).size();
    }

    /** Number of registered terminals across every colour and dimension. */
    public int totalCount() {
        int count = 0;
        for (List<Link> list : links.values()) {
            count += list.size();
        }
        return count;
    }

    /** The configured maximum number of terminals allowed across every colour and dimension. */
    public int maxTerminals() {
        return TerminalSettings.getMaxTerminals();
    }

    /** True while another terminal may be registered anywhere. */
    public boolean hasCapacity() {
        return totalCount() < maxTerminals();
    }

    /** The registered terminals of this dye (immutable). */
    public List<Link> links(DyeColor color) {
        return Collections.unmodifiableList(slots(color));
    }

    /** Finds the terminal of this dye at the given dimension and position, or {@code null}. */
    public Link find(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        for (Link link : slots(color)) {
            if (link.dimension().equals(dimension) && link.pos().equals(pos)) {
                return link;
            }
        }
        return null;
    }

    /**
     * Registers a terminal of this dye, unless the shared store is already at capacity.
     *
     * @return true when the terminal is registered (or was already), false when the cap is reached.
     */
    public boolean register(ServerLevel level, DyeColor color, BlockPos pos) {
        if (find(color, level.dimension(), pos) != null) {
            return true;
        }
        if (!hasCapacity()) {
            return false;
        }
        slots(color).add(new Link(level.dimension(), pos.immutable()));
        return true;
    }

    /** Removes the terminal at the given dimension and position. */
    public boolean remove(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        Link link = find(color, dimension, pos);
        return link != null && slots(color).remove(link);
    }

    /**
     * Moves a terminal from one dye to another, preserving its link record. The shared count is
     * unchanged, so a move is always allowed when the source link exists.
     *
     * @return false when the source link is missing.
     */
    public boolean changeColor(DyeColor from, ResourceKey<Level> dimension, BlockPos pos, DyeColor to) {
        if (from == to) {
            return true;
        }
        Link link = find(from, dimension, pos);
        if (link == null) {
            return false;
        }
        slots(from).remove(link);
        slots(to).add(link);
        return true;
    }

    public boolean setOwner(DyeColor color, ResourceKey<Level> dimension, BlockPos pos, UUID owner) {
        Link link = find(color, dimension, pos);
        if (link == null) {
            return false;
        }
        link.setOwner(owner);
        return true;
    }

    public UUID owner(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        Link link = find(color, dimension, pos);
        return link == null ? null : link.owner();
    }

    /** Every registered terminal across all colours and dimensions. */
    public List<Link> allLinks() {
        List<Link> result = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            result.addAll(slots(color));
        }
        return result;
    }

    /** A registered link together with the dye it is filed under. */
    public record Entry(DyeColor color, Link link) {
    }

    /** Every registered terminal across all colours and dimensions, tagged with its dye. */
    public List<Entry> allEntries() {
        List<Entry> result = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            for (Link link : slots(color)) {
                result.add(new Entry(color, link));
            }
        }
        return result;
    }

    /** Total number of terminals currently flagged as chunk loaders across every colour and dimension. */
    public int chunkLoaderCount() {
        int count = 0;
        for (Link link : allLinks()) {
            if (link.holdsChunkLoader()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Number of chunk-loader leases held by {@code player}. Falls back to the link owner for records
     * written before leases existed.
     */
    public int chunkLoaderCount(UUID player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (Link link : allLinks()) {
            if (link.holdsChunkLoader() && player.equals(holderOf(link))) {
                count++;
            }
        }
        return count;
    }

    /** The lease holder of {@code link}, falling back to its owner for records saved without one. */
    public static UUID holderOf(Link link) {
        return link.chunkLoaderHolder() != null ? link.chunkLoaderHolder() : link.owner();
    }

    /** Every terminal waiting for a chunk-loader lease, oldest request first. */
    public List<Link> queuedLinks() {
        List<Link> result = new ArrayList<>();
        for (Link link : allLinks()) {
            if (link.isChunkLoaderQueued()) {
                result.add(link);
            }
        }
        result.sort((a, b) -> Long.compare(a.chunkLoaderQueuedSince(), b.chunkLoaderQueuedSince()));
        return result;
    }

    /** The 1-based queue position of {@code link}, or 0 when it is not queued. */
    public int queuePosition(Link link) {
        if (link == null || !link.isChunkLoaderQueued()) {
            return 0;
        }
        int position = 1;
        for (Link other : allLinks()) {
            if (other != link && other.isChunkLoaderQueued()
                    && other.chunkLoaderQueuedSince() < link.chunkLoaderQueuedSince()) {
                position++;
            }
        }
        return position;
    }

    public String getName(DyeColor color, ResourceKey<Level> dimension, BlockPos pos) {
        Link link = find(color, dimension, pos);
        return link == null ? null : link.name();
    }

    public boolean setName(DyeColor color, ResourceKey<Level> dimension, BlockPos pos, String name) {
        Link link = find(color, dimension, pos);
        if (link == null) {
            return false;
        }
        link.setName(name);
        return true;
    }

    /**
     * Every terminal of this dye the player may use, in registration order, across every dimension.
     * Colour is the grouping; each link's own level is resolved for the {@link NetworkPermissions#canUse}
     * check, so a terminal's dimension and loaded state do not matter. Callers exclude the source link
     * from its own list.
     */
    public List<Link> usableFor(ServerLevel level, Player player, DyeColor color) {
        List<Link> result = new ArrayList<>();
        for (Link link : slots(color)) {
            if (NetworkPermissions.canUse(levelFor(level, link.dimension()), link.pos(), player)) {
                result.add(link);
            }
        }
        return result;
    }

    private static ServerLevel levelFor(ServerLevel level, ResourceKey<Level> dimension) {
        if (level.getServer() == null) {
            return level;
        }
        ServerLevel target = level.getServer().getLevel(dimension);
        return target == null ? level : target;
    }

    /**
     * Trims, strips formatting codes and caps a terminal name.
     *
     * @return the cleaned name, or {@code null} when it is empty.
     */
    public static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.replaceAll("\u00a7.", "").trim();
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Loads the store from {@code tag}.
     *
     * <p>Layout: a {@code colors} compound keyed by dye name, each holding a list of link compounds
     * ({@code dimension}, {@code pos}, optional {@code owner}, {@code name}, {@code sort}, optional
     * {@code chunkLoader} plus the lease fields {@code chunkLoaderHolder}/{@code chunkLoaderUntil}/
     * {@code chunkLoaderQueued}). Malformed entries are skipped.
     */
    public void read(CompoundTag tag) {
        links.clear();
        CompoundTag colors = tag.getCompound("colors");
        for (DyeColor color : DyeColor.values()) {
            ListTag entries = colors.getList(color.getName(), Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                try {
                    ResourceLocation dimensionId = ResourceLocation.parse(entry.getString("dimension"));
                    ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
                    Link link = new Link(dimension, BlockPos.of(entry.getLong("pos")));
                    if (entry.contains("owner")) {
                        link.setOwner(UUID.fromString(entry.getString("owner")));
                    }
                    if (entry.contains("name")) {
                        link.setName(entry.getString("name"));
                    }
                    if (entry.contains("sort")) {
                        link.setSortMode(SortMode.byName(entry.getString("sort")));
                    }
                    if (entry.contains("chunkLoader")) {
                        link.setChunkLoader(entry.getBoolean("chunkLoader"));
                    }
                    if (entry.contains("chunkLoaderHolder")) {
                        link.setChunkLoaderHolder(UUID.fromString(entry.getString("chunkLoaderHolder")));
                    }
                    if (entry.contains("chunkLoaderUntil")) {
                        link.setChunkLoaderUntil(entry.getLong("chunkLoaderUntil"));
                    }
                    if (entry.contains("chunkLoaderQueued")) {
                        link.setChunkLoaderQueuedSince(entry.getLong("chunkLoaderQueued"));
                    }
                    if (link.isChunkLoader() && link.chunkLoaderHolder() == null) {
                        // Records written before leases existed carry no holder; fall back to the owner.
                        link.setChunkLoaderHolder(link.owner());
                    }
                    slots(color).add(link);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    /**
     * Writes the store into {@code tag}; see {@link #read(CompoundTag)} for the layout.
     *
     * @return the same {@code tag} for chaining.
     */
    public CompoundTag write(CompoundTag tag) {
        CompoundTag colors = new CompoundTag();
        for (DyeColor color : DyeColor.values()) {
            List<Link> list = slots(color);
            if (list.isEmpty()) {
                continue;
            }
            ListTag entries = new ListTag();
            for (Link link : list) {
                CompoundTag entry = new CompoundTag();
                entry.putString("dimension", link.dimension().location().toString());
                entry.putLong("pos", link.pos().asLong());
                if (link.owner() != null) {
                    entry.putString("owner", link.owner().toString());
                }
                if (link.name() != null) {
                    entry.putString("name", link.name());
                }
                entry.putString("sort", link.sortMode().name());
                if (link.isChunkLoader()) {
                    entry.putBoolean("chunkLoader", true);
                }
                if (link.chunkLoaderHolder() != null) {
                    entry.putString("chunkLoaderHolder", link.chunkLoaderHolder().toString());
                }
                if (link.chunkLoaderUntil() > 0) {
                    entry.putLong("chunkLoaderUntil", link.chunkLoaderUntil());
                }
                if (link.chunkLoaderQueuedSince() > 0) {
                    entry.putLong("chunkLoaderQueued", link.chunkLoaderQueuedSince());
                }
                entries.add(entry);
            }
            colors.put(color.getName(), entries);
        }
        tag.put("colors", colors);
        return tag;
    }
}
