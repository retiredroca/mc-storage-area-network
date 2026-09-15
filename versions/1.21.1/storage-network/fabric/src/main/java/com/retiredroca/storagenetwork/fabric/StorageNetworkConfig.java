package com.retiredroca.storagenetwork.fabric;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.config.TerminalSettings;

import net.fabricmc.loader.api.FabricLoader;

public final class StorageNetworkConfig {
    private StorageNetworkConfig() {}

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("storage_network.json");
        if (!Files.exists(configPath)) {
            writeDefault(configPath);
            return;
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(configPath));
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("maxTier")) {
                    TerminalSettings.setMaxTier(obj.get("maxTier").getAsInt());
                }
            }
        } catch (Exception e) {
            StorageNetworkCommon.LOGGER.error("Failed to load storage_network config, using defaults", e);
        }
    }

    private static void writeDefault(Path configPath) {
        JsonObject obj = new JsonObject();
        obj.addProperty("maxTier", TerminalSettings.MAX_SUPPORTED_TIER);
        try {
            Files.createDirectories(configPath.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(configPath, gson.toJson(obj));
        } catch (Exception e) {
            StorageNetworkCommon.LOGGER.error("Failed to write default storage_network config", e);
        }
    }
}
