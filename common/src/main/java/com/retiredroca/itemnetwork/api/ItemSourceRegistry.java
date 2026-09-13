package com.retiredroca.itemnetwork.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Registry for {@link ItemSource} providers.
 *
 * <p>Mods register their sources here at mod-init time:
 * <ul>
 *   <li><b>Fabric:</b> declare the entrypoint key {@code item_network_api} returning
 *       {@code ItemSource} instances;</li>
 *   <li><b>NeoForge:</b> send InterModComms {@code register_item_source} whose message supplies a
 *       {@code ItemSource} instance.</li>
 * </ul>
 *
 * <p>Sources are refreshed on every host scan. The registry is harmless when empty.
 */
public final class ItemSourceRegistry {
    private static final List<ItemSource> SOURCES = new ArrayList<>();
    private static final List<Predicate<ItemStack>> HIDDEN_ITEM_FILTERS = new ArrayList<>();

    private ItemSourceRegistry() {}

    public static void register(ItemSource source) {
        if (source != null) {
            SOURCES.add(source);
        }
    }

    /**
     * Registers a predicate marking stacks that should be hidden from regular container listings
     * (but remain represented through sources). For example a mod that flattens shulker boxes may
     * hide the raw box stacks so only the box contents show.
     */
    public static void addHiddenItemFilter(Predicate<ItemStack> hidden) {
        if (hidden != null) {
            HIDDEN_ITEM_FILTERS.add(hidden);
        }
    }

    public static boolean isHidden(ItemStack stack) {
        for (Predicate<ItemStack> filter : HIDDEN_ITEM_FILTERS) {
            if (filter.test(stack)) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemSource> getSources() {
        return Collections.unmodifiableList(SOURCES);
    }

    public static void refreshAll(ServerLevel level, BlockPos hostPos, int chunkRadius) {
        for (ItemSource source : SOURCES) {
            source.refresh(level, hostPos, chunkRadius);
        }
    }

    public static boolean isEmpty() {
        return SOURCES.isEmpty();
    }
}
