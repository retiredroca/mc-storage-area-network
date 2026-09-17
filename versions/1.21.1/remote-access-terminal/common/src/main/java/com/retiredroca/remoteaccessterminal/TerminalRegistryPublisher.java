package com.retiredroca.remoteaccessterminal;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.NetworkCapabilities;
import com.retiredroca.mcstorageareanetwork.api.capability.TerminalRegistry;

import net.minecraft.server.MinecraftServer;

/**
 * Publishes Remote Access Terminal's {@link TerminalRegistry} capability: every entry of the saved
 * {@link TerminalLinks} store, always, regardless of chunk loading or proximity.
 *
 * <p>The links live in the overworld-anchored {@link TerminalLinksAccess}, so the running server is
 * captured at server start by the loader initializers and released when it stops.
 */
public final class TerminalRegistryPublisher implements TerminalRegistry {
    private static final TerminalRegistryPublisher INSTANCE = new TerminalRegistryPublisher();
    private static boolean registered;
    private static volatile MinecraftServer server;

    private TerminalRegistryPublisher() {}

    /** Registers the capability once; safe to call from both loaders. */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkCapabilities.register(TerminalRegistry.class, INSTANCE);
    }

    /** Captures the running server ({@code null} once it stops) so {@link #terminals()} can read it. */
    public static void installServer(MinecraftServer value) {
        server = value;
    }

    @Override
    public List<Terminal> terminals() {
        MinecraftServer current = server;
        if (current == null) {
            return List.of();
        }
        TerminalLinks links = TerminalLinksAccess.get(current.overworld()).links();
        List<Terminal> result = new ArrayList<>();
        for (TerminalLinks.Entry entry : links.allEntries()) {
            TerminalLinks.Link link = entry.link();
            result.add(new Terminal(link.dimension(), link.pos(), link.name(), entry.color().getName()));
        }
        return result;
    }
}
