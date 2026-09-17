package com.retiredroca.remoteaccessterminal.neoforge;

import com.retiredroca.remoteaccessterminal.config.TerminalSettings;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** NeoForge server config ({@code remote_access_terminal-server.toml}) for the terminal settings. */
public final class RemoteAccessTerminalConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.IntValue MAX_TERMINALS;
    public static final ModConfigSpec.IntValue INVITE_PERMISSION_LEVEL;
    public static final ModConfigSpec.BooleanValue ALLOW_CROSS_DIMENSION;
    public static final ModConfigSpec.IntValue MAX_CHUNKLOADER_TERMINALS;
    public static final ModConfigSpec.IntValue MAX_CHUNKLOADERS_PER_PLAYER;
    public static final ModConfigSpec.IntValue LAZY_CHUNK_RING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        MAX_TERMINALS = builder
                .comment("Maximum number of terminals allowed at once, across every colour and dimension.",
                        "WARNING: every terminal is a block, and each enabled chunk-loader keeps chunks loaded;",
                        "setting this very high is expensive and can cause server lag. Default 80.",
                        "Lowering this value never removes existing terminals; it only limits new placements.")
                .defineInRange("maxTerminals", TerminalSettings.DEFAULT_MAX_TERMINALS, 1,
                        TerminalSettings.MAX_CONFIGURABLE_TERMINALS);
        INVITE_PERMISSION_LEVEL = builder
                .comment("Permission level required to run the invite/uninvite/list commands (0 = everyone).")
                .defineInRange("invitePermissionLevel", 0, 0, 4);
        ALLOW_CROSS_DIMENSION = builder
                .comment("Whether a terminal may send a player into another dimension.")
                .define("allowCrossDimension", true);
        MAX_CHUNKLOADER_TERMINALS = builder
                .comment("Maximum number of chunk-loading terminals allowed at once, across every colour and",
                        "dimension. Each enabled terminal keeps its own chunk ticking (and its lazy ring loaded).",
                        "WARNING: setting this very high keeps a large number of chunks loaded and can cause",
                        "severe server lag. Default 80.")
                .defineInRange("maxChunkloaderTerminals", TerminalSettings.DEFAULT_MAX_CHUNKLOADER_TERMINALS, 0,
                        TerminalSettings.MAX_CONFIGURABLE_CHUNKLOADER_TERMINALS);
        MAX_CHUNKLOADERS_PER_PLAYER = builder
                .comment("Maximum number of chunk-loading terminals a single owner may enable.",
                        "WARNING: raising this multiplies the chunks kept loaded per player and can cause",
                        "server lag. Default 1.")
                .defineInRange("maxChunkloadersPerPlayer", TerminalSettings.DEFAULT_MAX_CHUNKLOADERS_PER_PLAYER, 0,
                        TerminalSettings.MAX_CONFIGURABLE_CHUNKLOADERS_PER_PLAYER);
        LAZY_CHUNK_RING = builder
                .comment("Radius, in chunks, kept loaded (but not ticking) around an enabled terminal's own",
                        "chunk. The default 1 is the 3x3 footprint: the own chunk plus its 8 neighbours.",
                        "0 disables the lazy ring (only the terminal's own chunk stays loaded).",
                        "A radius above 1 is only honoured when the crafting_network mod is present, and is",
                        "then capped at the server's simulation distance (chunks); without crafting_network",
                        "the radius is clamped to 1 at runtime.")
                .defineInRange("lazyChunkRing", TerminalSettings.DEFAULT_LAZY_CHUNK_RING, 0,
                        TerminalSettings.MAX_CONFIGURABLE_LAZY_CHUNK_RING);

        SERVER_SPEC = builder.build();
    }

    private RemoteAccessTerminalConfig() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "remote_access_terminal-server.toml");
    }

    public static void onConfigLoad(ModConfigEvent event) {
        // The Unloading event fires on server stop; reading values then throws.
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            TerminalSettings.setMaxTerminals(MAX_TERMINALS.get());
            TerminalSettings.setInvitePermissionLevel(INVITE_PERMISSION_LEVEL.get());
            TerminalSettings.setAllowCrossDimension(ALLOW_CROSS_DIMENSION.get());
            TerminalSettings.setMaxChunkloaderTerminals(MAX_CHUNKLOADER_TERMINALS.get());
            TerminalSettings.setMaxChunkloadersPerPlayer(MAX_CHUNKLOADERS_PER_PLAYER.get());
            TerminalSettings.setLazyChunkRing(LAZY_CHUNK_RING.get());
        }
    }
}
