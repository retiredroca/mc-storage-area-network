package com.retiredroca.storagenetwork.client;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.retiredroca.storagenetwork.StorageNetworkCommon;

public final class TerminalUiState {
    private static final int UNSET = Integer.MIN_VALUE;

    private static int searchX = UNSET;
    private static int searchY = UNSET;
    private static int chestX = UNSET;
    private static int chestY = UNSET;
    private static boolean loaded = false;

    private TerminalUiState() {}

    private static Path file() {
        return StorageNetworkCommon.platform().configDir().resolve("storage_network-client.json");
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Path path = file();
            if (!Files.exists(path)) {
                return;
            }
            JsonElement element = JsonParser.parseString(Files.readString(path));
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject obj = element.getAsJsonObject();
            searchX = obj.get("searchX").getAsInt();
            searchY = obj.get("searchY").getAsInt();
            chestX = obj.get("chestX").getAsInt();
            chestY = obj.get("chestY").getAsInt();
        } catch (Exception e) {
            StorageNetworkCommon.LOGGER.error("Failed to load storage_network client UI state, using defaults", e);
            searchX = UNSET;
            searchY = UNSET;
            chestX = UNSET;
            chestY = UNSET;
        }
    }

    public static boolean hasSearch() {
        return getSearchX() != UNSET;
    }

    public static int getSearchX() {
        load();
        return searchX;
    }

    public static int getSearchY() {
        load();
        return searchY;
    }

    public static void setSearchX(int x) {
        searchX = x;
    }

    public static void setSearchY(int y) {
        searchY = y;
    }

    public static boolean hasChest() {
        return getChestX() != UNSET;
    }

    public static int getChestX() {
        load();
        return chestX;
    }

    public static int getChestY() {
        load();
        return chestY;
    }

    public static void setChestX(int x) {
        chestX = x;
    }

    public static void setChestY(int y) {
        chestY = y;
    }

    public static void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("searchX", getSearchX());
            obj.addProperty("searchY", getSearchY());
            obj.addProperty("chestX", getChestX());
            obj.addProperty("chestY", getChestY());
            Path path = file();
            Files.createDirectories(path.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(path, gson.toJson(obj));
        } catch (Exception e) {
            StorageNetworkCommon.LOGGER.error("Failed to save storage_network client UI state", e);
        }
    }
}
