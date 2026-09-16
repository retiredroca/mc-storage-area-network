package com.retiredroca.mcstorageareanetwork.fabric;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Config for the built-in shulker-box flattening source and the network ownership policy. */
public final class ShulkerBoxConfig {
    public static final int MAX_FLATTEN_DEPTH = 3;
    private static final int CONFIG_VERSION = 2;
    private static final String FILE_NAME = "mc_storage_area_network.json";

    private static int flattenDepth = 1;
    private static boolean boxRowHidden = false;
    private static boolean sameTypeFirst = true;
    private static boolean ownershipEnabled = true;
    private static boolean teamSharing = true;
    private static final Set<ResourceLocation> excludedContainers = new LinkedHashSet<>();

    private ShulkerBoxConfig() {}

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            writeDefault(configPath);
            apply();
            return;
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(configPath));
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                int version = obj.has("configVersion") ? obj.get("configVersion").getAsInt() : 0;
                if (obj.has("flattenDepth")) {
                    flattenDepth = Math.max(1, Math.min(MAX_FLATTEN_DEPTH, obj.get("flattenDepth").getAsInt()));
                }
                if (obj.has("boxRowHidden")) {
                    boxRowHidden = obj.get("boxRowHidden").getAsBoolean();
                }
                if (obj.has("sameTypeFirst")) {
                    sameTypeFirst = obj.get("sameTypeFirst").getAsBoolean();
                }
                if (obj.has("ownershipEnabled")) {
                    ownershipEnabled = obj.get("ownershipEnabled").getAsBoolean();
                }
                if (obj.has("teamSharing")) {
                    teamSharing = obj.get("teamSharing").getAsBoolean();
                }
                if (obj.has("excludedContainers") && obj.get("excludedContainers").isJsonArray()) {
                    excludedContainers.clear();
                    for (JsonElement entry : obj.getAsJsonArray("excludedContainers")) {
                        ResourceLocation id = ResourceLocation.tryParse(entry.getAsString());
                        if (id != null && !NetworkSettings.isProtectedContainer(id)) {
                            excludedContainers.add(id);
                        }
                    }
                }
                if (version < CONFIG_VERSION) {
                    boxRowHidden = false;
                    writeDefault(configPath);
                }
            }
        } catch (Exception e) {
            McStorageAreaNetwork.LOGGER.error("Failed to load mc_storage_area_network config, using defaults", e);
        }
        apply();
    }

    private static void apply() {
        NetworkSettings.configure(ownershipEnabled, teamSharing);
        NetworkSettings.configureExcludedContainers(excludedContainers);
    }

    private static void writeDefault(Path configPath) {
        JsonObject obj = new JsonObject();
        obj.addProperty("configVersion", CONFIG_VERSION);
        obj.addProperty("flattenDepth", flattenDepth);
        obj.addProperty("boxRowHidden", boxRowHidden);
        obj.addProperty("sameTypeFirst", sameTypeFirst);
        obj.addProperty("ownershipEnabled", ownershipEnabled);
        obj.addProperty("teamSharing", teamSharing);
        JsonArray excluded = new JsonArray();
        excludedContainers.forEach(id -> excluded.add(id.toString()));
        obj.add("excludedContainers", excluded);
        try {
            Files.createDirectories(configPath.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(configPath, gson.toJson(obj));
        } catch (Exception e) {
            McStorageAreaNetwork.LOGGER.error("Failed to write default mc_storage_area_network config", e);
        }
    }

    /** Persist a change to the excluded-container list (used by the in-game toggle). */
    public static void setContainerExcluded(ResourceLocation blockId, boolean excluded) {
        if (blockId == null || NetworkSettings.isProtectedContainer(blockId)) {
            return;
        }
        boolean changed = excluded ? excludedContainers.add(blockId) : excludedContainers.remove(blockId);
        if (!changed) {
            return;
        }
        NetworkSettings.configureExcludedContainers(excludedContainers);
        writeDefault(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
    }

    public static int getFlattenDepth() {
        return flattenDepth;
    }

    public static boolean isBoxRowHidden() {
        return boxRowHidden;
    }

    public static boolean isSameTypeFirst() {
        return sameTypeFirst;
    }

    public static boolean isRawShulkerBoxHidden(ItemStack stack) {
        return boxRowHidden && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }
}
