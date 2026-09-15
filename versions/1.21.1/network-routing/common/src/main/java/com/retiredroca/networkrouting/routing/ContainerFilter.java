package com.retiredroca.networkrouting.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An ordered list of filter tokens for one container. Token forms:
 * <ul>
 *   <li>{@code #namespace:path} — an item tag (e.g. {@code #c:ores});</li>
 *   <li>{@code namespace:path} — a specific item type, components ignored (e.g. {@code minecraft:iron_ingot});</li>
 *   <li>{@code namespace} — every item from a mod (e.g. {@code create}).</li>
 * </ul>
 */
public final class ContainerFilter {
    private final List<String> tokens;
    private final List<Item> items = new ArrayList<>();
    private final List<TagKey<Item>> tags = new ArrayList<>();
    private final List<String> namespaces = new ArrayList<>();

    public ContainerFilter(List<String> tokens) {
        List<String> clean = new ArrayList<>();
        for (String raw : tokens) {
            if (raw == null) {
                continue;
            }
            String token = raw.trim();
            if (token.isEmpty() || clean.contains(token)) {
                continue;
            }
            clean.add(token);
            if (token.startsWith("#")) {
                ResourceLocation rl = ResourceLocation.tryParse(token.substring(1));
                if (rl != null) {
                    tags.add(TagKey.create(Registries.ITEM, rl));
                }
            } else if (token.contains(":")) {
                ResourceLocation rl = ResourceLocation.tryParse(token);
                if (rl != null) {
                    BuiltInRegistries.ITEM.getOptional(rl).ifPresent(items::add);
                }
            } else {
                namespaces.add(token.toLowerCase(Locale.ROOT));
            }
        }
        this.tokens = List.copyOf(clean);
    }

    public List<String> tokens() {
        return tokens;
    }

    public boolean isEmpty() {
        return items.isEmpty() && tags.isEmpty() && namespaces.isEmpty();
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (Item item : items) {
            if (stack.is(item)) {
                return true;
            }
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        if (!namespaces.isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return namespaces.contains(key.getNamespace());
        }
        return false;
    }
}
