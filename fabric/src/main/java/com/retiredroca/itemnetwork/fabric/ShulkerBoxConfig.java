package com.retiredroca.itemnetwork.fabric;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Config for the built-in shulker-box flattening source. */
public final class ShulkerBoxConfig {
    public static final int MAX_FLATTEN_DEPTH = 3;
    private static final int CONFIG_VERSION = 2;
    private static final String FILE_NAME = "item_network_api.json";

    private static int flattenDepth = 1;
    private static boolean boxRowHidden = false;
    private static boolean sameTypeFirst = true;

    private ShulkerBoxConfig() {}

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            writeDefault(configPath);
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
                if (version < CONFIG_VERSION) {
                    boxRowHidden = false;
                    writeDefault(configPath);
                }
            }
        } catch (Exception e) {
            ItemNetworkApi.LOGGER.error("Failed to load item_network_api config, using defaults", e);
        }
    }

    private static void writeDefault(Path configPath) {
        JsonObject obj = new JsonObject();
        obj.addProperty("configVersion", CONFIG_VERSION);
        obj.addProperty("flattenDepth", flattenDepth);
        obj.addProperty("boxRowHidden", boxRowHidden);
        obj.addProperty("sameTypeFirst", sameTypeFirst);
        try {
            Files.createDirectories(configPath.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(configPath, gson.toJson(obj));
        } catch (Exception e) {
            ItemNetworkApi.LOGGER.error("Failed to write default item_network_api config", e);
        }
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
