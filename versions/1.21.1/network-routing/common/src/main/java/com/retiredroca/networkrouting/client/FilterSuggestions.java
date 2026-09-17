package com.retiredroca.networkrouting.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client-side candidate ids for the routing filter box. Built lazily and cached for the session,
 * the list only contains entries a survival player can obtain: every modded item, plus vanilla items
 * except the technical/unobtainable ids in {@link #UNOBTAINABLE_ITEMS}. Item tags are offered as
 * {@code #namespace:path}, and only when the tag contains at least one obtainable item.
 *
 * <p>Matching is a case-insensitive substring test anywhere in the id, so {@code ingot} finds
 * {@code minecraft:gold_ingot}. Suggestions are filtered against one container's current filter
 * tokens, so an id that is already covered by that filter (directly, through its namespace/mod token,
 * or through a tag token it belongs to) stops being offered until it is removed again.
 */
final class FilterSuggestions {
    /**
     * Vanilla item ids that cannot be obtained in survival (technical, worldgen-only and
     * command-only items). Modded items are never filtered, so this is a single list of full
     * {@code namespace:path} ids to extend when more vanilla ids should be hidden.
     */
    private static final Set<String> UNOBTAINABLE_ITEMS = Set.of(
            "minecraft:air",
            "minecraft:barrier",
            "minecraft:light",
            "minecraft:structure_void",
            "minecraft:nether_portal",
            "minecraft:end_portal",
            "minecraft:end_gateway",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:debug_stick",
            "minecraft:spawner",
            "minecraft:budding_amethyst",
            "minecraft:reinforced_deepslate",
            "minecraft:petrified_oak_slab",
            "minecraft:moving_piston",
            "minecraft:piston_head",
            "minecraft:fire",
            "minecraft:soul_fire",
            "minecraft:water",
            "minecraft:lava",
            "minecraft:bubble_column");

    private static final int MAX_SUGGESTIONS = 8;

    private static List<String> itemIds;
    private static List<String> tagIds;
    private static Map<String, Set<String>> itemTags;

    private FilterSuggestions() {}

    /**
     * The suggestions containing the typed text (case-insensitive, anywhere in the id), capped at
     * {@value #MAX_SUGGESTIONS} and skipping anything already covered by {@code tokens} (the selected
     * container's filter). A leading {@code #} switches to item tags.
     */
    static List<String> matching(String typed, Collection<String> tokens) {
        String query = typed.trim().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        if (query.isEmpty()) {
            return out;
        }
        Filter filter = Filter.parse(tokens);
        boolean tags = query.startsWith("#");
        String needle = tags ? query.substring(1) : query;
        for (String id : tags ? tags() : items()) {
            if (filter.covers(id)) {
                continue;
            }
            if (matches(tags ? id.substring(1) : id, needle)) {
                out.add(id);
                if (out.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }
        }
        return out;
    }

    private static boolean matches(String id, String needle) {
        return needle.isEmpty() || id.contains(needle);
    }

    /**
     * The label shown for a suggestion or filter entry: the item's localized name for
     * {@code namespace:path}, or a tidied path for a {@code #tag} / bare namespace.
     */
    static String displayName(String entry) {
        if (entry.startsWith("#")) {
            return prettify(pathOf(entry.substring(1)));
        }
        if (entry.indexOf(':') < 0) {
            return prettify(entry);
        }
        ResourceLocation id = ResourceLocation.tryParse(entry);
        if (id != null) {
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item).getHoverName().getString();
            }
        }
        return prettify(pathOf(entry));
    }

    private static String pathOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static String prettify(String path) {
        String spaced = path.replace('_', ' ').trim();
        if (spaced.isEmpty()) {
            return path;
        }
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** The tokens of one container, parsed into the coverage they give. */
    private record Filter(Set<String> items, Set<String> namespaces, Set<String> tags) {
        static Filter parse(Collection<String> tokens) {
            Set<String> items = new HashSet<>();
            Set<String> namespaces = new HashSet<>();
            Set<String> tags = new HashSet<>();
            for (String raw : tokens) {
                if (raw == null) {
                    continue;
                }
                String token = raw.trim().toLowerCase(Locale.ROOT);
                if (token.isEmpty()) {
                    continue;
                }
                if (token.startsWith("#")) {
                    tags.add(token.substring(1));
                } else if (token.indexOf(':') >= 0) {
                    items.add(token);
                } else {
                    namespaces.add(token);
                }
            }
            return new Filter(items, namespaces, tags);
        }

        /** Whether this filter already covers the candidate (with or without its leading {@code #}). */
        boolean covers(String candidate) {
            if (candidate.startsWith("#")) {
                return tags.contains(candidate.substring(1));
            }
            int colon = candidate.indexOf(':');
            if (colon > 0 && namespaces.contains(candidate.substring(0, colon))) {
                return true;
            }
            if (items.contains(candidate)) {
                return true;
            }
            Set<String> memberships = itemTagMap().get(candidate);
            if (memberships == null) {
                return false;
            }
            for (String tag : memberships) {
                if (tags.contains(tag)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static List<String> items() {
        if (itemIds == null) {
            List<String> out = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null && !UNOBTAINABLE_ITEMS.contains(id.toString())) {
                    out.add(id.toString());
                }
            }
            out.sort(String::compareTo);
            itemIds = List.copyOf(out);
        }
        return itemIds;
    }

    private static List<String> tags() {
        if (tagIds == null) {
            Set<String> obtainable = new HashSet<>(items());
            List<String> out = new ArrayList<>();
            BuiltInRegistries.ITEM.getTags().forEach(pair -> {
                for (Holder<Item> holder : pair.getSecond()) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
                    if (id != null && obtainable.contains(id.toString())) {
                        out.add("#" + pair.getFirst().location());
                        return;
                    }
                }
            });
            out.sort(String::compareTo);
            tagIds = List.copyOf(out);
        }
        return tagIds;
    }

    /** Every item's tag ids (without the leading {@code #}), used for tag-token coverage. */
    private static Map<String, Set<String>> itemTagMap() {
        if (itemTags == null) {
            Map<String, Set<String>> map = new HashMap<>();
            BuiltInRegistries.ITEM.getTags().forEach(pair -> {
                String tag = pair.getFirst().location().toString();
                for (Holder<Item> holder : pair.getSecond()) {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(holder.value());
                    if (id != null) {
                        map.computeIfAbsent(id.toString(), key -> new HashSet<>()).add(tag);
                    }
                }
            });
            itemTags = map;
        }
        return itemTags;
    }
}
