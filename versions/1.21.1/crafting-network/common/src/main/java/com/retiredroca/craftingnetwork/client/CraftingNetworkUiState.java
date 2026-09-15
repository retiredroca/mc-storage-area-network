package com.retiredroca.craftingnetwork.client;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;


public final class CraftingNetworkUiState {
    private static final int UNSET = Integer.MIN_VALUE;
    private static final Path FILE = CraftingNetworkCommon.platform().configDir().resolve("crafting_network-client.json");

    private static int dropdownX = UNSET;
    private static int dropdownY = UNSET;
    private static boolean loaded = false;

    private CraftingNetworkUiState() {}

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (!Files.exists(FILE)) {
                return;
            }
            JsonElement element = JsonParser.parseString(Files.readString(FILE));
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject obj = element.getAsJsonObject();
            dropdownX = obj.get("dropdownX").getAsInt();
            dropdownY = obj.get("dropdownY").getAsInt();
        } catch (Exception e) {
            CraftingNetworkCommon.LOGGER.error("Failed to load crafting_network client UI state, using defaults", e);
            dropdownX = UNSET;
            dropdownY = UNSET;
        }
    }

    public static boolean hasDropdown() {
        return getDropdownX() != UNSET;
    }

    public static int getDropdownX() {
        load();
        return dropdownX;
    }

    public static int getDropdownY() {
        load();
        return dropdownY;
    }

    public static void setDropdownX(int x) {
        dropdownX = x;
    }

    public static void setDropdownY(int y) {
        dropdownY = y;
    }

    public static void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("dropdownX", getDropdownX());
            obj.addProperty("dropdownY", getDropdownY());
            Files.createDirectories(FILE.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(FILE, gson.toJson(obj));
        } catch (Exception e) {
            CraftingNetworkCommon.LOGGER.error("Failed to save crafting_network client UI state", e);
        }
    }
}