package com.retiredroca.mcstorageareanetwork.api;

/**
 * The companion mods that make up the storage-area-network set, each named by its loader mod id.
 *
 * <p>Use {@link NetworkAwareness} to find out which of these are installed on the running instance;
 * this enum only names them so callers never have to hard-code a mod id.
 */
public enum SisterMods {
    API("mc_storage_area_network"),
    STORAGE("storage_network"),
    CRAFTING("crafting_network"),
    ROUTING("network_routing"),
    ACCESS("remote_access_terminal");

    private final String id;

    SisterMods(String id) {
        this.id = id;
    }

    /** The loader mod id this entry is loaded under. */
    public String id() {
        return id;
    }

    /** The entry with {@code id}, or {@code null} when no entry matches. */
    public static SisterMods byId(String id) {
        if (id == null) {
            return null;
        }
        for (SisterMods mod : values()) {
            if (mod.id.equals(id)) {
                return mod;
            }
        }
        return null;
    }
}
