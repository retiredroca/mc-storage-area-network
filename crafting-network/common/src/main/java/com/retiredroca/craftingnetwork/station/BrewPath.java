package com.retiredroca.craftingnetwork.station;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * Resolves the shortest brewing path from a water bottle to a target potion using the level's
 * {@link PotionBrewing} table, and holds the ordered ingredient stack list to apply.
 */
public final class BrewPath {
    private static final Map<String, Holder<Potion>> HOLDER_BY_KEY = new HashMap<>();
    private static ServerLevel ingredientLevel;
    private static List<ItemStack> ingredientList;
    private static final Map<String, List<ItemStack>> PATH_CACHE = new HashMap<>();
    private static final Map<String, List<ItemStack>> STATE_CACHE = new HashMap<>();

    private final List<ItemStack> states;
    private final List<ItemStack> ingredients;

    private BrewPath(List<ItemStack> states, List<ItemStack> ingredients) {
        this.states = states;
        this.ingredients = ingredients;
    }

    public List<ItemStack> states() {
        return states;
    }

    public List<ItemStack> ingredients() {
        return ingredients;
    }

    public boolean empty() {
        return ingredients.isEmpty();
    }

    public static BrewPath compute(ServerLevel level, Holder<Potion> target) {
        String key = target.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        List<ItemStack> cached = PATH_CACHE.get(key);
        if (cached != null && STATE_CACHE.containsKey(key)) {
            return new BrewPath(STATE_CACHE.get(key), cached);
        }
        BrewPath built = resolve(level, target);
        PATH_CACHE.put(key, built.ingredients());
        STATE_CACHE.put(key, built.states());
        return built;
    }

    private static BrewPath resolve(ServerLevel level, Holder<Potion> target) {
        PotionBrewing brewing = level.potionBrewing();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);

        String targetId = target.unwrapKey().map(k -> k.location().toString()).orElse("");
        if (targetId.isEmpty() || !holderByKey().containsKey(targetId)) {
            return emptyPath();
        }

        Map<String, String> parent = new HashMap<>();
        Map<String, ItemStack> stepIngredient = new HashMap<>();
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();

        String waterId = potionIdOf(water);
        visited.add(waterId);
        queue.add(waterId);

        int guard = 0;
        while (!queue.isEmpty() && guard++ < 20000) {
            String id = queue.poll();
            if (id.equals(targetId)) {
                break;
            }
            Holder<Potion> holder = holderByKey().get(id);
            if (holder == null) {
                continue;
            }
            ItemStack state = PotionContents.createItemStack(Items.POTION, holder);
            for (ItemStack ingredient : ingredientsFor(level)) {
                ItemStack mixed;
                try {
                    mixed = brewing.mix(state.copy(), ingredient);
                } catch (Exception e) {
                    continue;
                }
                String mixedId = potionIdOf(mixed);
                if (mixedId.isEmpty() || visited.contains(mixedId)) {
                    continue;
                }
                visited.add(mixedId);
                parent.put(mixedId, id);
                stepIngredient.put(mixedId, ingredient.copy());
                queue.add(mixedId);
            }
        }

        if (!visited.contains(targetId)) {
            return emptyPath();
        }

        List<String> chain = new ArrayList<>();
        String cur = targetId;
        while (parent.containsKey(cur)) {
            chain.add(cur);
            cur = parent.get(cur);
        }
        chain.add(cur);
        Collections.reverse(chain);

        List<ItemStack> states = new ArrayList<>(chain.size());
        List<ItemStack> ingredients = new ArrayList<>(chain.size() - 1);
        for (int i = 0; i < chain.size(); i++) {
            states.add(PotionContents.createItemStack(Items.POTION, holderByKey().get(chain.get(i))));
            if (i > 0) {
                ingredients.add(stepIngredient.get(chain.get(i)));
            }
        }
        return new BrewPath(states, ingredients);
    }

    private static Map<String, Holder<Potion>> holderByKey() {
        if (HOLDER_BY_KEY.isEmpty()) {
            BuiltInRegistries.POTION.holders().forEach(holder ->
                    HOLDER_BY_KEY.put(holder.key().location().toString(), holder));
        }
        return HOLDER_BY_KEY;
    }

    private static List<ItemStack> ingredientsFor(ServerLevel level) {
        if (ingredientLevel != level) {
            ingredientLevel = level;
            ingredientList = null;
        }
        if (ingredientList == null) {
            PotionBrewing brewing = level.potionBrewing();
            List<ItemStack> out = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                try {
                    if (brewing.isIngredient(new ItemStack(item))) {
                        out.add(new ItemStack(item));
                    }
                } catch (Exception e) {
                    // registry items may reject stack creation; ignore
                }
            }
            ingredientList = out;
        }
        return ingredientList;
    }

    public static String potionIdOf(ItemStack stack) {
        Optional<Holder<Potion>> p = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
        return p.flatMap(Holder::unwrapKey).map(k -> k.location().toString()).orElse("");
    }

    private static BrewPath emptyPath() {
        return new BrewPath(List.of(), List.of());
    }
}