package com.retiredroca.networkrouting.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.retiredroca.mcstorageareanetwork.api.ScannedStorage;
import com.retiredroca.mcstorageareanetwork.api.StorageRouter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-side maintenance passes driven by a Routing Terminal. */
public final class NetworkSorter {
    private NetworkSorter() {}

    /** Moves stacks into their nearest matching labeled container. */
    public static boolean sort(ServerLevel level, BlockPos refPos, List<ScannedStorage> storages) {
        boolean moved = false;
        for (ScannedStorage source : storages) {
            if (!source.supportsExtraction()) {
                continue;
            }
            for (ItemStack stack : source.enumerate()) {
                if (stack.isEmpty()) {
                    continue;
                }
                ScannedStorage target = bestTarget(level, refPos, storages, source, stack);
                if (target == null) {
                    continue;
                }
                int taken = source.extract(stack, stack.getCount());
                if (taken <= 0) {
                    continue;
                }
                ItemStack remainder = target.insert(stack.copyWithCount(taken));
                if (!remainder.isEmpty()) {
                    source.insert(remainder);
                }
                moved = true;
            }
        }
        return moved;
    }

    private static ScannedStorage bestTarget(ServerLevel level, BlockPos refPos, List<ScannedStorage> storages,
            ScannedStorage source, ItemStack stack) {
        ScannedStorage best = null;
        int bestScore = 0;
        for (ScannedStorage candidate : storages) {
            if (candidate.collectionOnly()) {
                continue;
            }
            int score = StorageRouter.priority(level, refPos, candidate.pos(), stack);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        if (best == null || best.pos().equals(source.pos())) {
            return null;
        }
        return best;
    }

    /** Merges partial stacks of the same item inside each container to free slots. */
    public static boolean defrag(List<ScannedStorage> storages) {
        boolean changed = false;
        for (ScannedStorage storage : storages) {
            if (!storage.supportsExtraction()) {
                continue;
            }
            Map<Item, Integer> stackCounts = new HashMap<>();
            for (ItemStack stack : storage.enumerate()) {
                if (!stack.isEmpty()) {
                    stackCounts.merge(stack.getItem(), 1, Integer::sum);
                }
            }
            for (Item item : stackCounts.keySet()) {
                if (stackCounts.get(item) < 2) {
                    continue;
                }
                ItemStack proto = new ItemStack(item);
                int total = storage.count(proto);
                if (total <= 0) {
                    continue;
                }
                int taken = storage.extract(proto, total);
                if (taken <= 0) {
                    continue;
                }
                ItemStack remainder = storage.insert(proto.copyWithCount(taken));
                if (!remainder.isEmpty()) {
                    storage.insert(remainder);
                }
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Evicts stacks from labeled containers that do not match their filter: nearest unfiltered
     * container first, else the output sink (exposed Output Terminal), else cancel.
     */
    public static boolean trim(ServerLevel level, BlockPos refPos, List<ScannedStorage> storages) {
        boolean moved = false;
        for (ScannedStorage source : storages) {
            if (!source.supportsExtraction()) {
                continue;
            }
            ContainerFilter filter = RoutingLabels.filter(level, source.pos());
            if (filter == null) {
                continue;
            }
            for (ItemStack stack : source.enumerate()) {
                if (stack.isEmpty() || filter.matches(stack)) {
                    continue;
                }
                ScannedStorage dest = nearestUnfiltered(level, storages, source);
                if (dest == null) {
                    dest = outputSink(storages, source);
                }
                if (dest == null) {
                    return moved;
                }
                int taken = source.extract(stack, stack.getCount());
                if (taken <= 0) {
                    continue;
                }
                ItemStack remainder = dest.insert(stack.copyWithCount(taken));
                if (!remainder.isEmpty()) {
                    source.insert(remainder);
                }
                moved = true;
            }
        }
        return moved;
    }

    private static ScannedStorage nearestUnfiltered(ServerLevel level, List<ScannedStorage> storages,
            ScannedStorage source) {
        ScannedStorage best = null;
        double bestDist = Double.MAX_VALUE;
        for (ScannedStorage candidate : storages) {
            if (candidate.pos().equals(source.pos()) || candidate.collectionOnly()) {
                continue;
            }
            if (RoutingLabels.isLabeled(level, candidate.pos())) {
                continue;
            }
            double dist = candidate.pos().distSqr(source.pos());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private static ScannedStorage outputSink(List<ScannedStorage> storages, ScannedStorage source) {
        for (ScannedStorage candidate : storages) {
            if (!candidate.pos().equals(source.pos()) && candidate.collectionOnly()) {
                return candidate;
            }
        }
        return null;
    }
}
