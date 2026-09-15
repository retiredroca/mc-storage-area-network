package com.retiredroca.craftingnetwork.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CraftingNetworkConfig {
    public static final int MAX_SUPPORTED_TIER = 5;

    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.IntValue MAX_TIER;

    private static int maxTier = MAX_SUPPORTED_TIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        MAX_TIER = builder
                .comment(
                        "Maximum upgrade tier allowed on this server (0 = base, 1 = 3x3, 2 = 5x5, 3 = 7x7, 4 = 9x9, 5 = 11x11).",
                        "Upgrades beyond this value cannot be applied.",
                        "Tiers are also automatically capped so the scan radius never exceeds the server's",
                        "view-distance or simulation-distance from server.properties.")
                .defineInRange("maxTier", MAX_SUPPORTED_TIER, 0, MAX_SUPPORTED_TIER);

        SERVER_SPEC = builder.build();
    }

    private CraftingNetworkConfig() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "crafting_network-server.toml");
    }

    public static void onConfigLoad(ModConfigEvent event) {
        // The Unloading event fires on server stop; reading values then throws.
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            bake();
        }
    }

    private static void bake() {
        maxTier = MAX_TIER.get();
    }

    public static int getMaxTier() {
        return maxTier;
    }
}
