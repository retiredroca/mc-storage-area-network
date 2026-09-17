package com.retiredroca.mcstorageareanetwork.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-wide permission store shared by every mod in the set: per-owner invite lists and
 * per-position private flags. It is anchored on the overworld, so all dimensions see the same data.
 *
 * <p>Every position is <em>open</em> by default (usable by anyone). Marking it private with
 * {@link #setOpen} restricts use to players the owner trusts. Three checks build on this:
 * <ul>
 *   <li>{@link #canUse} - may the player use the block? True when the position is open, or when the
 *       player is its owner, an invited player, or a teammate (with
 *       {@link NetworkSettings#teamSharing()} on).
 *   <li>{@link #canEdit} - may the player reconfigure the block? True for the owner, an invited
 *       player, a teammate, or an operator (permission level 2). Being open is deliberately
 *       <em>not</em> enough, so a public block can still be used by anyone while only its owner and
 *       trusted players change its setup.
 *   <li>{@link #canBreak} - identical to {@link #canEdit}; breaking is treated as an edit.
 * </ul>
 *
 * <p>Owners and teams come from {@link ContainerOwnership#ownerOf} and
 * {@link ContainerOwnership#canSee}, so the scoreboard-team rule lives in exactly one place and
 * unowned blocks stay shared by everyone, matching the rest of the API. Invites are keyed by the
 * owner's UUID rather than by position, so a player invited by an owner may edit every position
 * that owner holds.
 */
public final class NetworkPermissions {
    private static final String DATA_NAME = "mc_storage_area_network_permissions";

    private NetworkPermissions() {}

    public static final class Data extends SavedData {
        public static final SavedData.Factory<Data> FACTORY =
                new SavedData.Factory<>(Data::new, Data::load, DataFixTypes.LEVEL);

        private final Map<UUID, Set<UUID>> invites = new HashMap<>();
        private final Set<BlockPos> privatePositions = new HashSet<>();

        public Data() {}

        public static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            ListTag inviteList = tag.getList("invites", Tag.TAG_COMPOUND);
            for (int i = 0; i < inviteList.size(); i++) {
                CompoundTag entry = inviteList.getCompound(i);
                UUID owner = parseUuid(entry.getString("owner"));
                if (owner == null) {
                    continue;
                }
                Set<UUID> targets = new HashSet<>();
                ListTag targetList = entry.getList("targets", Tag.TAG_STRING);
                for (int j = 0; j < targetList.size(); j++) {
                    UUID target = parseUuid(targetList.getString(j));
                    if (target != null) {
                        targets.add(target);
                    }
                }
                data.invites.put(owner, targets);
            }
            ListTag privateList = tag.getList("private", Tag.TAG_COMPOUND);
            for (int i = 0; i < privateList.size(); i++) {
                data.privatePositions.add(BlockPos.of(privateList.getCompound(i).getLong("pos")));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag inviteList = new ListTag();
            invites.forEach((owner, targets) -> {
                CompoundTag entry = new CompoundTag();
                entry.putString("owner", owner.toString());
                ListTag targetList = new ListTag();
                for (UUID target : targets) {
                    targetList.add(StringTag.valueOf(target.toString()));
                }
                entry.put("targets", targetList);
                inviteList.add(entry);
            });
            tag.put("invites", inviteList);

            ListTag privateList = new ListTag();
            for (BlockPos pos : privatePositions) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("pos", pos.asLong());
                privateList.add(entry);
            }
            tag.put("private", privateList);
            return tag;
        }

        Map<UUID, Set<UUID>> invites() {
            return invites;
        }

        Set<BlockPos> privatePositions() {
            return privatePositions;
        }

        private static UUID parseUuid(String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private static Data data(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(Data.FACTORY, DATA_NAME);
    }

    /** True unless the position was privatised with {@link #setOpen}; everything else is open. */
    public static boolean isOpen(ServerLevel level, BlockPos pos) {
        return !data(level).privatePositions().contains(pos);
    }

    /** Marks the position private ({@code open == false}) or open again ({@code open == true}). */
    public static void setOpen(ServerLevel level, BlockPos pos, boolean open) {
        Data data = data(level);
        boolean changed = open ? data.privatePositions().remove(pos) : data.privatePositions().add(pos);
        if (changed) {
            data.setDirty();
        }
    }

    /** Adds {@code target} to {@code owner}'s invite list; true when the list actually changed. */
    public static boolean invite(ServerLevel level, UUID owner, UUID target) {
        if (owner == null || target == null || owner.equals(target)) {
            return false;
        }
        Data data = data(level);
        if (data.invites().computeIfAbsent(owner, key -> new HashSet<>()).add(target)) {
            data.setDirty();
            return true;
        }
        return false;
    }

    /** Removes {@code target} from {@code owner}'s invite list; true when they were invited. */
    public static boolean uninvite(ServerLevel level, UUID owner, UUID target) {
        if (owner == null || target == null) {
            return false;
        }
        Data data = data(level);
        Set<UUID> targets = data.invites().get(owner);
        if (targets == null || !targets.remove(target)) {
            return false;
        }
        if (targets.isEmpty()) {
            data.invites().remove(owner);
        }
        data.setDirty();
        return true;
    }

    /** True when {@code owner} invited {@code target}. */
    public static boolean isInvited(ServerLevel level, UUID owner, UUID target) {
        Set<UUID> targets = data(level).invites().get(owner);
        return targets != null && targets.contains(target);
    }

    /** Snapshot of {@code owner}'s invite list (never null; empty when nobody is invited). */
    public static Set<UUID> invitesOf(ServerLevel level, UUID owner) {
        Set<UUID> targets = data(level).invites().get(owner);
        return targets == null ? Set.of() : Set.copyOf(targets);
    }

    /** Replaces {@code owner}'s invite list; an empty collection clears it (GUI/command "amend"). */
    public static void setInvites(ServerLevel level, UUID owner, Collection<UUID> targets) {
        if (owner == null) {
            return;
        }
        Data data = data(level);
        Set<UUID> next = new HashSet<>();
        if (targets != null) {
            for (UUID target : targets) {
                if (target != null && !owner.equals(target)) {
                    next.add(target);
                }
            }
        }
        if (next.isEmpty()) {
            if (data.invites().remove(owner) != null) {
                data.setDirty();
            }
            return;
        }
        data.invites().put(owner, next);
        data.setDirty();
    }

    /** True when the player may use the block: it is open, or they are its owner/trusted. */
    public static boolean canUse(ServerLevel level, BlockPos pos, Player player) {
        return isOpen(level, pos) || isTrusted(level, pos, player);
    }

    /** True when the player may reconfigure the block: its owner/trusted, or an operator. */
    public static boolean canEdit(ServerLevel level, BlockPos pos, Player player) {
        return player.hasPermissions(2) || isTrusted(level, pos, player);
    }

    /** True when the player may break the block; identical to {@link #canEdit}. */
    public static boolean canBreak(ServerLevel level, BlockPos pos, Player player) {
        return canEdit(level, pos, player);
    }

    /**
     * True for the position's owner, a teammate (via {@link ContainerOwnership#canSee}), or a player
     * the owner invited. Operators are <em>not</em> included; callers add them where they count.
     */
    static boolean isTrusted(ServerLevel level, BlockPos pos, Player player) {
        ContainerOwnership.Entry owner = ContainerOwnership.ownerOf(level, pos);
        if (ContainerOwnership.canSee(level, owner, viewer(player))) {
            return true;
        }
        return owner != null && isInvited(level, owner.id(), player.getUUID());
    }

    private static ContainerOwnership.Entry viewer(Player player) {
        return new ContainerOwnership.Entry(player.getUUID(), player.getGameProfile().getName());
    }
}
