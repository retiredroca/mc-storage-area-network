package com.retiredroca.networkrouting.fabric;

import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.menu.RoutingMenu;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class Registration {
    public static final RoutingTerminalBlock TERMINAL_BLOCK = new RoutingTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).noOcclusion());

    public static final BlockItem TERMINAL_ITEM = new BlockItem(TERMINAL_BLOCK, new Item.Properties());

    public static final BlockEntityType<RoutingTerminalBlockEntity> TERMINAL_BE =
            BlockEntityType.Builder.of(RoutingTerminalBlockEntity::new, TERMINAL_BLOCK).build(null);

    public static final ExtendedScreenHandlerType<RoutingMenu, BlockPos> MENU =
            new ExtendedScreenHandlerType<>(RoutingMenu::fromNetwork, BlockPos.STREAM_CODEC);

    public static final Item LINKER_ITEM = new Item(new Item.Properties().stacksTo(1));

    public static final ResourceKey<CreativeModeTab> TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(NetworkRoutingCommon.MODID, "network_routing"));

    private Registration() {}

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, rl("routing_terminal"), TERMINAL_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("routing_terminal"), TERMINAL_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, rl("routing_terminal"), TERMINAL_BE);
        Registry.register(BuiltInRegistries.MENU, rl("routing_terminal"), MENU);
        Registry.register(BuiltInRegistries.ITEM, rl("routing_linker"), LINKER_ITEM);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.network_routing"))
                        .icon(() -> new ItemStack(TERMINAL_ITEM))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(TAB_KEY).register(output -> {
            output.accept(TERMINAL_ITEM);
            output.accept(LINKER_ITEM);
        });
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(NetworkRoutingCommon.MODID, path);
    }
}
