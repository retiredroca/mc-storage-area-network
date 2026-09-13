package com.retiredroca.itemnetwork.neoforge;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Config for the built-in shulker-box flattening source. */
public final class ShulkerBoxConfig {
    public static final int MAX_FLATTEN_DEPTH = 3;
    private static final int CONFIG_VERSION = 2;

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.IntValue CONFIG_VERSION_VALUE;
    public static final ModConfigSpec.IntValue FLATTEN_DEPTH;
    public static final ModConfigSpec.BooleanValue BOX_ROW_HIDDEN;
    public static final ModConfigSpec.BooleanValue SAME_TYPE_FIRST;

    private static int flattenDepth = 1;
    private static boolean boxRowHidden = false;
    private static boolean sameTypeFirst = true;

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

        SERVER_SPEC = builder.build();
    }

    private ShulkerBoxConfig() {}

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "item_network_api-server.toml");
    }

    public static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            migrateLegacyConfig(event);
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
            ItemNetworkApi.LOGGER.error("Failed to migrate item_network_api config", e);
        }
    }

    private static void bake() {
        flattenDepth = FLATTEN_DEPTH.get();
        boxRowHidden = BOX_ROW_HIDDEN.get();
        sameTypeFirst = SAME_TYPE_FIRST.get();
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
