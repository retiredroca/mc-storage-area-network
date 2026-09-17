package com.retiredroca.remoteaccessterminal.fabric;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;
import com.retiredroca.remoteaccessterminal.item.TerminalBlockItem;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Fabric registry wiring for the sixteen terminals, their block items and the creative tab. */
public final class Registration {
    private static final Map<DyeColor, TerminalBlock> BLOCKS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, TerminalBlockItem> ITEMS = new EnumMap<>(DyeColor.class);

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(RemoteAccessTerminalCommon.MODID, "remote_access_terminal"));

    public static final ExtendedScreenHandlerType<TerminalMenu, TerminalMenu.Data> MENU =
            new ExtendedScreenHandlerType<>(TerminalMenu::fromNetwork, TerminalMenu.Data.STREAM_CODEC);

    private Registration() {
    }

    public static void register() {
        for (DyeColor color : DyeColor.values()) {
            ResourceLocation id = rl("terminal_" + color.getName());
            TerminalBlock block = Registry.register(BuiltInRegistries.BLOCK, id,
                    new TerminalBlock(color, blockProperties()));
            TerminalBlockItem item = Registry.register(BuiltInRegistries.ITEM, id,
                    new TerminalBlockItem(block, new Item.Properties()));
            BLOCKS.put(color, block);
            ITEMS.put(color, item);
        }
        Registry.register(BuiltInRegistries.MENU, rl("terminal_menu"), MENU);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.remote_access_terminal"))
                        .icon(() -> new ItemStack(ITEMS.get(DyeColor.WHITE)))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(TAB_KEY).register(output -> {
            for (TerminalBlockItem item : ITEMS.values()) {
                output.accept(item);
            }
        });
    }

    /** The registered blocks keyed by dye. */
    public static Map<DyeColor, TerminalBlock> blocks() {
        return Collections.unmodifiableMap(BLOCKS);
    }

    /** The registered block items keyed by dye. */
    public static Map<DyeColor, TerminalBlockItem> items() {
        return Collections.unmodifiableMap(ITEMS);
    }

    private static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.0F, 10.0F);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(RemoteAccessTerminalCommon.MODID, path);
    }
}
