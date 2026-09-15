package com.retiredroca.mcstorageareanetwork.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Registry of {@link StoragePriority} rules.
 *
 * <p>Host mods call {@link #order} (or query {@link #priority}) immediately before inserting a
 * stack, so a rule registered by a companion mod can pull matching containers to the front. The
 * registry is harmless while empty: {@link #order} then returns the input list unchanged and
 * {@link #priority} returns {@code 0}.
 */
public final class StorageRouter {
    private static final List<StoragePriority> RULES = new ArrayList<>();

    private StorageRouter() {}

    public static void register(StoragePriority rule) {
        if (rule != null) {
            RULES.add(rule);
        }
    }

    public static boolean isEmpty() {
        return RULES.isEmpty();
    }

    /** Total routing priority (sum of all rules) for inserting {@code stack} at {@code pos}. */
    public static int priority(ServerLevel level, BlockPos refPos, BlockPos pos, ItemStack stack) {
        if (RULES.isEmpty() || level == null) {
            return 0;
        }
        int total = 0;
        for (StoragePriority rule : RULES) {
            total += rule.priority(level, refPos, pos, stack);
        }
        return total;
    }

    /** Stable-sorts {@code storages} so higher-priority containers come first for {@code stack}. */
    public static List<ScannedStorage> order(ServerLevel level, BlockPos refPos, List<ScannedStorage> storages,
            ItemStack stack) {
        if (RULES.isEmpty() || storages.isEmpty() || stack.isEmpty() || level == null) {
            return storages;
        }
        List<ScannedStorage> ordered = new ArrayList<>(storages);
        ordered.sort(Comparator.comparingInt(
                (ScannedStorage s) -> priority(level, refPos, s.pos(), stack)).reversed());
        return ordered;
    }
}
