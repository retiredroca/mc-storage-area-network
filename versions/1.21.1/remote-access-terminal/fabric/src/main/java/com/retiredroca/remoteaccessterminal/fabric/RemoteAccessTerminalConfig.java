package com.retiredroca.remoteaccessterminal.fabric;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.config.TerminalSettings;

/** Fabric JSON config ({@code remote_access_terminal.json}) for the loader-neutral settings. */
public final class RemoteAccessTerminalConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private RemoteAccessTerminalConfig() {
    }

    public static void load() {
        Path path = RemoteAccessTerminalCommon.platform().configDir().resolve("remote_access_terminal.json");
        if (Files.exists(path)) {
            try {
                JsonElement element = JsonParser.parseString(Files.readString(path));
                if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("maxTerminals")) {
                        TerminalSettings.setMaxTerminals(obj.get("maxTerminals").getAsInt());
                    }
                    if (obj.has("invitePermissionLevel")) {
                        TerminalSettings.setInvitePermissionLevel(obj.get("invitePermissionLevel").getAsInt());
                    }
                    if (obj.has("allowCrossDimension")) {
                        TerminalSettings.setAllowCrossDimension(obj.get("allowCrossDimension").getAsBoolean());
                    }
                    if (obj.has("maxChunkloaderTerminals")) {
                        TerminalSettings.setMaxChunkloaderTerminals(
                                obj.get("maxChunkloaderTerminals").getAsInt());
                    }
                    if (obj.has("maxChunkloadersPerPlayer")) {
                        TerminalSettings.setMaxChunkloadersPerPlayer(
                                obj.get("maxChunkloadersPerPlayer").getAsInt());
                    }
                    if (obj.has("lazyChunkRing")) {
                        TerminalSettings.setLazyChunkRing(obj.get("lazyChunkRing").getAsInt());
                    }
                }
            } catch (Exception e) {
                RemoteAccessTerminalCommon.LOGGER.error(
                        "Failed to load remote_access_terminal config, using defaults", e);
            }
        }
        if (TerminalSettings.chunkLoaderCapsAreHigh()) {
            RemoteAccessTerminalCommon.LOGGER.warn(
                    "remote_access_terminal chunk-loader caps are high (maxChunkloaderTerminals={}, "
                            + "maxChunkloadersPerPlayer={}); each enabled terminal keeps chunks loaded and "
                            + "may cause server lag",
                    TerminalSettings.getMaxChunkloaderTerminals(),
                    TerminalSettings.getMaxChunkloadersPerPlayer());
        }
        save(path);
    }

    private static void save(Path path) {
        JsonObject obj = new JsonObject();
        obj.addProperty("_warning", "High maxTerminals / maxChunkloaderTerminals / maxChunkloadersPerPlayer values "
                + "keep many terminals and chunks loaded and can cause server lag. maxTerminals caps the total "
                + "number of terminals across every colour and dimension; each terminal is a block, and each "
                + "enabled terminal keeps its own chunk fully ticking and loads the lazyChunkRing radius around "
                + "it. A ring above 1 is only honoured when the crafting_network mod is present, and is then "
                + "capped at the server's simulation distance; without it the radius is clamped to 1 (the 3x3 "
                + "footprint).");
        obj.addProperty("maxTerminals", TerminalSettings.getMaxTerminals());
        obj.addProperty("invitePermissionLevel", TerminalSettings.getInvitePermissionLevel());
        obj.addProperty("allowCrossDimension", TerminalSettings.isAllowCrossDimension());
        obj.addProperty("maxChunkloaderTerminals", TerminalSettings.getMaxChunkloaderTerminals());
        obj.addProperty("maxChunkloadersPerPlayer", TerminalSettings.getMaxChunkloadersPerPlayer());
        obj.addProperty("lazyChunkRing", TerminalSettings.getLazyChunkRing());
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(obj));
        } catch (Exception e) {
            RemoteAccessTerminalCommon.LOGGER.error("Failed to write remote_access_terminal config", e);
        }
    }
}
