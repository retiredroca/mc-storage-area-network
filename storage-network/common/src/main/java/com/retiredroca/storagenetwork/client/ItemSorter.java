package com.retiredroca.storagenetwork.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;

/** Client-side display sorting/filtering. Extraction stays identity-based, so this never
 *  needs to round-trip to the server. */
public final class ItemSorter {
    private ItemSorter() {}

    public record VirtualItem(ItemStack stack, int count) {}

    public static List<VirtualItem> build(List<ItemStack> items, List<Integer> counts) {
        List<VirtualItem> out = new ArrayList<>();
        int n = Math.min(items.size(), counts.size());
        for (int i = 0; i < n; i++) {
            if (!items.get(i).isEmpty()) {
                out.add(new VirtualItem(items.get(i), counts.get(i)));
            }
        }
        return out;
    }

    public static List<VirtualItem> filter(List<VirtualItem> input, String search) {
        List<VirtualItem> list = new ArrayList<>(input);
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            list.removeIf(v -> !matches(v.stack(), query));
        }
        return list;
    }

    public static List<VirtualItem> filterAndSort(List<VirtualItem> input, String search, SortMode sort) {
        List<VirtualItem> list = new ArrayList<>(input);
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty()) {
            list.removeIf(v -> !matches(v.stack(), query));
        }
        list.sort((a, b) -> compare(a, b, sort));
        return list;
    }

    private static boolean matches(ItemStack stack, String query) {
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        return name.contains(query) || id.contains(query);
    }

    private static int compare(VirtualItem a, VirtualItem b, SortMode sort) {
        return switch (sort) {
            case NAME -> a.stack().getHoverName().getString().compareToIgnoreCase(b.stack().getHoverName().getString());
            case TYPE -> typeKey(a.stack()).compareToIgnoreCase(typeKey(b.stack()));
            case TAG -> primaryTag(a.stack()).compareToIgnoreCase(primaryTag(b.stack()));
            case MOD -> modKey(a.stack()).compareToIgnoreCase(modKey(b.stack()));
            case EQUIPMENT -> equipmentKey(a.stack()).compareToIgnoreCase(equipmentKey(b.stack()));
        };
    }

    private static String typeKey(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof ArmorItem) return "armor";
        if (item instanceof SwordItem) return "weapon";
        if (item instanceof DiggerItem || item instanceof TieredItem) return "tool";
        if (item instanceof BlockItem) return "block";
        if (stack.get(DataComponents.FOOD) != null) return "food";
        return "other";
    }

    private static String modKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }

    private static String primaryTag(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().tags()
                .findFirst()
                .map(tag -> tag.location().toString())
                .orElse("");
    }

    private static String equipmentKey(ItemStack stack) {
        if (isCuriosAccessory(stack)) {
            return "accessory";
        }
        EquipmentSlot slot = equipmentSlotOf(stack);
        return switch (slot.getType()) {
            case EquipmentSlot.Type.HUMANOID_ARMOR -> "armor/" + slot.getName();
            case EquipmentSlot.Type.ANIMAL_ARMOR -> "body";
            case EquipmentSlot.Type.HAND -> "hand";
        };
    }

    private static EquipmentSlot equipmentSlotOf(ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }
        return EquipmentSlot.MAINHAND;
    }

    /** Soft-compat with Curios: Curios exposes item slots as item tags in the "curios"
     *  namespace (e.g. curios:ring, curios:necklace). If the item has any such tag it is
     *  classified as an accessory. Pure data-based, no hard dependency and no reflection
     *  into Curios internals. */
    private static boolean isCuriosAccessory(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().tags()
                .anyMatch(tag -> tag.location().getNamespace().equals("curios"));
    }
}