package com.retiredroca.networkrouting.neoforge;

import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.menu.RoutingMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NetworkRoutingCommon.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NetworkRoutingCommon.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NetworkRoutingCommon.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, NetworkRoutingCommon.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NetworkRoutingCommon.MODID);

    public static final DeferredBlock<RoutingTerminalBlock> TERMINAL_BLOCK = BLOCKS.register("routing_terminal",
            () -> new RoutingTerminalBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).noOcclusion()));

    public static final DeferredItem<BlockItem> TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("routing_terminal", TERMINAL_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RoutingTerminalBlockEntity>> TERMINAL_BE =
            BLOCK_ENTITIES.register("routing_terminal",
                    () -> BlockEntityType.Builder.of(RoutingTerminalBlockEntity::new, TERMINAL_BLOCK.get())
                            .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<RoutingMenu>> MENU =
            MENUS.register("routing_terminal", () -> IMenuTypeExtension.create(RoutingMenu::fromNetwork));

    public static final DeferredItem<Item> LINKER_ITEM =
            ITEMS.register("routing_linker", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("network_routing", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.network_routing"))
                    .icon(() -> new ItemStack(TERMINAL_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(TERMINAL_ITEM.get());
                        output.accept(LINKER_ITEM.get());
                    })
                    .build());

    private Registration() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
