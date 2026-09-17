package com.retiredroca.remoteaccessterminal.neoforge;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;
import com.retiredroca.remoteaccessterminal.item.TerminalBlockItem;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** NeoForge registry wiring for the sixteen terminals, their block items and the creative tab. */
public final class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RemoteAccessTerminalCommon.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RemoteAccessTerminalCommon.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RemoteAccessTerminalCommon.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RemoteAccessTerminalCommon.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> MENU =
            MENUS.register("terminal_menu", () -> IMenuTypeExtension.create(TerminalMenu::fromNetwork));

    private static final Map<DyeColor, DeferredBlock<TerminalBlock>> TERMINAL_BLOCKS =
            new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, DeferredItem<TerminalBlockItem>> TERMINAL_ITEMS =
            new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String id = "terminal_" + color.getName();
            DeferredBlock<TerminalBlock> block = BLOCKS.register(id,
                    () -> new TerminalBlock(color, blockProperties()));
            TERMINAL_BLOCKS.put(color, block);
            TERMINAL_ITEMS.put(color, ITEMS.register(id,
                    () -> new TerminalBlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("remote_access_terminal", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.remote_access_terminal"))
                    .icon(() -> new ItemStack(TERMINAL_ITEMS.get(DyeColor.WHITE).get()))
                    .displayItems((params, output) -> {
                        for (DeferredItem<TerminalBlockItem> item : TERMINAL_ITEMS.values()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private Registration() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENUS.register(modEventBus);
    }

    /** The registered blocks keyed by dye. */
    public static Map<DyeColor, DeferredBlock<TerminalBlock>> terminalBlocks() {
        return Collections.unmodifiableMap(TERMINAL_BLOCKS);
    }

    /** The registered block items keyed by dye. */
    public static Map<DyeColor, DeferredItem<TerminalBlockItem>> terminalItems() {
        return Collections.unmodifiableMap(TERMINAL_ITEMS);
    }

    private static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of().sound(SoundType.STONE).strength(1.0F, 10.0F);
    }
}
