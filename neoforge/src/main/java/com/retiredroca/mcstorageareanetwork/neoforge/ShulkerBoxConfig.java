package com.retiredroca.mcstorageareanetwork.neoforge;

import java.nio.file.Files;
import java.nio.file.Path;

import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Config for the shulker-box flattening source and the network ownership policy. */
public final class ShulkerBoxConfig {
    public static final int MAX_FLATTEN_DEPTH = 3;
    private static final int CONFIG_VERSION = 2;

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.IntValue CONFIG_VERSION_VALUE;
    public static final ModConfigSpec.IntValue FLATTEN_DEPTH;
    public static final ModConfigSpec.BooleanValue BOX_ROW_HIDDEN;
    public static final ModConfigSpec.BooleanValue SAME_TYPE_FIRST;
    public static final ModConfigSpec.BooleanValue OWNERSHIP;
    public static final ModConfigSpec.BooleanValue TEAM_SHARING;

    private static int flattenDepth = 1;
    private static boolean boxRowHidden = false;
    private static boolean sameTypeFirst = true;
    private static boolean ownershipEnabled = true;
    private static boolean teamSharing = true;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        CONFIG_VERSION_VALUE = builder
                .comment("Internal config version. Bumping this triggers a migration of legacy settings.")
                .defineInRange("configVersion", CONFIG_VERSION, 1, Integer.MAX_VALUE);

        FLATTEN_DEPTH = builder
                .comment("How many levels of shulker boxes to flatten (1 = boxes inside containers,",
                        "2 = also boxes inside boxes).")
                .defineInRange("flattenDepth", 1, 1, MAX_FLATTEN_DEPTH);

        BOX_ROW_HIDDEN = builder
                .comment("Hide the raw shulker box stacks from the listing so only the flattened box",
                        "contents are shown. Defaults to false.")
                .define("boxRowHidden", false);

        SAME_TYPE_FIRST = builder
                .comment("When inserting into shulker boxes, prefer boxes that already contain the",
                        "same item type over boxes with any free slot.")
                .define("sameTypeFirst", true);

        OWNERSHIP = builder
                .comment("When true, a terminal only shows global (worldgen) storage plus storage placed",
                        "by the player who placed that terminal.")
                .define("ownershipEnabled", true);

        TEAM_SHARING = builder
                .comment("When true, storage placed by players on the same scoreboard team is shared.")
                .define("teamSharing", true);

        SERVER_SPEC = builder.build();
    }

    private ShulkerBoxConfig() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "mc_storage_area_network-server.toml");
    }

    public static void onConfigLoad(ModConfigEvent event) {
        // The Unloading event fires on server stop; reading values then throws.
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            if (event instanceof ModConfigEvent.Loading) {
                migrateLegacyConfig(event);
            }
            bake();
        }
    }

    private static void migrateLegacyConfig(ModConfigEvent event) {
        try {
            Path path = event.getConfig().getFullPath();
            if (!Files.exists(path)) {
                return;
            }
            String text = Files.readString(path);
            if (!text.contains("configVersion") && text.contains("boxRowHidden = true")) {
                BOX_ROW_HIDDEN.set(false);
            }
        } catch (Exception e) {
            McStorageAreaNetwork.LOGGER.error("Failed to migrate mc_storage_area_network config", e);
        }
    }

    private static void bake() {
        flattenDepth = FLATTEN_DEPTH.get();
        boxRowHidden = BOX_ROW_HIDDEN.get();
        sameTypeFirst = SAME_TYPE_FIRST.get();
        ownershipEnabled = OWNERSHIP.get();
        teamSharing = TEAM_SHARING.get();
        NetworkSettings.configure(ownershipEnabled, teamSharing);
    }

    public static int getFlattenDepth() {
        return flattenDepth;
    }

    public static boolean isBoxRowHidden() {
        return boxRowHidden;
    }

    public static boolean isSameTypeFirst() {
        return sameTypeFirst;
    }

    public static boolean isRawShulkerBoxHidden(ItemStack stack) {
        return boxRowHidden && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock;
    }
}
