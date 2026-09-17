package com.retiredroca.remoteaccessterminal;

import java.util.Locale;

/**
 * Ordering of the destinations shown in the picker, persisted per link.
 * <ul>
 * <li>{@code NEAREST} - closest to the current terminal first.</li>
 * <li>{@code NAME} - alphabetical by terminal name.</li>
 * <li>{@code COLOUR} - by dye colour, then name.</li>
 * </ul>
 */
public enum SortMode {
    NEAREST,
    NAME,
    COLOUR;

    /** The translation key of this mode's display name. */
    public String displayKey() {
        return "sort.remote_access_terminal." + name().toLowerCase(Locale.ROOT);
    }

    /** The next mode in the picker's cycle. */
    public SortMode next() {
        SortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** Resolves a mode by name (case-insensitive), falling back to {@link #NEAREST}. */
    public static SortMode byName(String name) {
        for (SortMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return NEAREST;
    }

    /** Resolves a mode by ordinal, falling back to {@link #NEAREST} when out of range. */
    public static SortMode byId(int id) {
        SortMode[] values = values();
        return id >= 0 && id < values.length ? values[id] : NEAREST;
    }
}
