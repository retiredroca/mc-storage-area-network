package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.block.StorageTerminalBlock;
import com.retiredroca.storagenetwork.menu.StorageTerminalMenu;
import com.retiredroca.storagenetwork.recipe.TerminalUpgradeRecipe;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class Registration {
    public static final StorageTerminalBlock STORAGE_TERMINAL_BLOCK = new StorageTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).noOcclusion());

    public static final BlockItem STORAGE_TERMINAL_ITEM = new BlockItem(STORAGE_TERMINAL_BLOCK, new Item.Properties());

    public static final BlockEntityType<StorageTerminalBlockEntity> STORAGE_TERMINAL_BE =
            BlockEntityType.Builder.of(StorageTerminalBlockEntity::new, STORAGE_TERMINAL_BLOCK).build(null);

    public static final ExtendedScreenHandlerType<StorageTerminalMenu, BlockPos> STORAGE_TERMINAL_MENU =
            new ExtendedScreenHandlerType<>(StorageTerminalMenu::fromNetwork, BlockPos.STREAM_CODEC);

    public static final ResourceKey<CreativeModeTab> STORAGE_CENTRAL_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(StorageNetworkCommon.MODID, "storage_network"));

    private Registration() {}

    private static final String[] TIER_NAMES = {
        "Copper", "Iron", "Gold", "Emerald", "Diamond", "Netherite"
    };

    public static ItemStack terminalWithTier(int tier) {
        ItemStack stack = new ItemStack(STORAGE_TERMINAL_ITEM);
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tier);
        BlockEntity.addEntityType(tag, getTerminalBEType());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(TIER_NAMES[tier] + " Storage Terminal"));
        return stack;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, rl("storage_terminal"), STORAGE_TERMINAL_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("storage_terminal"), STORAGE_TERMINAL_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, rl("storage_terminal"), STORAGE_TERMINAL_BE);
        Registry.register(BuiltInRegistries.MENU, rl("storage_terminal"), STORAGE_TERMINAL_MENU);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, rl("terminal_upgrade"),
                TerminalUpgradeRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, STORAGE_CENTRAL_TAB_KEY,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.storage_network"))
                        .icon(() -> terminalWithTier(0))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(STORAGE_CENTRAL_TAB_KEY).register(output -> {
            if (!Networking.isServerModded()) {
                return;
            }
            for (int i = 0; i < TIER_NAMES.length; i++) {
                output.accept(terminalWithTier(i));
            }
        });
    }

    public static Item getTerminalItem() {
        return STORAGE_TERMINAL_ITEM;
    }

    public static BlockEntityType<StorageTerminalBlockEntity> getTerminalBEType() {
        return STORAGE_TERMINAL_BE;
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(StorageNetworkCommon.MODID, path);
    }
}
