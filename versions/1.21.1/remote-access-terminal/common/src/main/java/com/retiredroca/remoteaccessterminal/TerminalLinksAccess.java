package com.retiredroca.remoteaccessterminal;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent, overworld-anchored {@link TerminalLinks} store. Keeping every dimension's terminals in
 * one {@link SavedData} lets a colour's capacity be counted across all dimensions and lets a terminal
 * in another dimension be resolved and travelled to.
 */
public final class TerminalLinksAccess extends SavedData {
    public static final String DATA_NAME = "remote_access_terminal_links";

    private static final SavedData.Factory<TerminalLinksAccess> FACTORY =
            new SavedData.Factory<>(TerminalLinksAccess::new, TerminalLinksAccess::load, DataFixTypes.LEVEL);

    private static final TerminalLinksAccess CLIENT = new TerminalLinksAccess();

    private final TerminalLinks links = new TerminalLinks();

    public TerminalLinks links() {
        return links;
    }

    public static TerminalLinksAccess load(CompoundTag tag, HolderLookup.Provider registries) {
        TerminalLinksAccess access = new TerminalLinksAccess();
        access.links.read(tag);
        return access;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return links.write(tag);
    }

    /** The server-side store when one exists, else an empty client-side placeholder. */
    public static TerminalLinksAccess get(Level level) {
        if (level instanceof ServerLevel serverLevel && serverLevel.getServer() != null) {
            return serverLevel.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        }
        return CLIENT;
    }
}
