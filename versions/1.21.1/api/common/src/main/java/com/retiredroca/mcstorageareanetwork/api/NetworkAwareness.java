package com.retiredroca.mcstorageareanetwork.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports which sister mods are installed on the running instance.
 *
 * <p>Each loader entrypoint installs a {@linkplain #setPresenceTest(Predicate) presence test} before
 * anything else initialises (Fabric's {@code FabricLoader}, NeoForge's {@code ModList}); until one
 * is installed every query is {@code false}. The detected set is logged once, when the test arrives.
 */
public final class NetworkAwareness {
    private static final Logger LOGGER = LoggerFactory.getLogger("mc_storage_area_network");
    private static final AtomicBoolean LOGGED = new AtomicBoolean();

    private static volatile Predicate<SisterMods> presenceTest;

    private NetworkAwareness() {}

    /** Installs the loader's presence test; the first call logs the detected set. */
    public static void setPresenceTest(Predicate<SisterMods> test) {
        presenceTest = test;
        if (test != null && LOGGED.compareAndSet(false, true)) {
            Set<SisterMods> detected = present();
            LOGGER.info("Detected sister mods: {}", detected.isEmpty() ? "none" : detected);
        }
    }

    /** True when the sister mod is installed; false before a presence test is installed. */
    public static boolean isPresent(SisterMods mod) {
        Predicate<SisterMods> test = presenceTest;
        return mod != null && test != null && test.test(mod);
    }

    /** The sister mods present on this instance, in enum order (never null, possibly empty). */
    public static Set<SisterMods> present() {
        Set<SisterMods> detected = EnumSet.noneOf(SisterMods.class);
        for (SisterMods mod : SisterMods.values()) {
            if (isPresent(mod)) {
                detected.add(mod);
            }
        }
        return Collections.unmodifiableSet(detected);
    }
}
