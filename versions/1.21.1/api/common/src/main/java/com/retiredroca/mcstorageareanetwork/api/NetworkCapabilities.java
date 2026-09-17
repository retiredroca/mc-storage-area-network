package com.retiredroca.mcstorageareanetwork.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of capability implementations published by the mods in the set.
 *
 * <p>A capability is a plain interface (see {@code api.capability}); the mod that owns the feature
 * registers its implementation at init and any other mod can ask for it without a compile-time
 * dependency on the publisher.
 *
 * <p>There is a single implementation per capability: registering another for the same interface
 * replaces the previous one.
 */
public final class NetworkCapabilities {
    private static final Map<Class<?>, Object> CAPABILITIES = new ConcurrentHashMap<>();

    private NetworkCapabilities() {}

    /** Publishes {@code implementation} for {@code capability}, replacing any earlier registration. */
    public static <T> void register(Class<T> capability, T implementation) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(implementation, "implementation");
        CAPABILITIES.put(capability, implementation);
    }

    /** The implementation registered for {@code capability}, or empty when there is none. */
    public static <T> Optional<T> get(Class<T> capability) {
        if (capability == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(capability.cast(CAPABILITIES.get(capability)));
    }

    /** True when an implementation is registered for {@code capability}. */
    public static <T> boolean has(Class<T> capability) {
        return capability != null && CAPABILITIES.containsKey(capability);
    }
}
