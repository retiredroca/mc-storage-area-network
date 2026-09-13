package com.retiredroca.craftingnetwork.fabric;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.block.CraftingStationBlock;
import com.retiredroca.craftingnetwork.block.StationBlock;
import com.retiredroca.craftingnetwork.menu.BrewingStationMenu;
import com.retiredroca.craftingnetwork.menu.CookingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingStationOpenData;
import com.retiredroca.craftingnetwork.menu.StationOpenData;
import com.retiredroca.craftingnetwork.recipe.CraftingUpgradeRecipe;
import com.retiredroca.craftingnetwork.station.StationType;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class Registration {
    public static final CraftingStationBlock CRAFTING_STATION_BLOCK = new CraftingStationBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).noOcclusion());

    public static final BlockItem CRAFTING_STATION_ITEM = new BlockItem(CRAFTING_STATION_BLOCK, new Item.Properties());

    public static final BlockEntityType<CraftingStationBlockEntity> CRAFTING_STATION_BE =
            BlockEntityType.Builder.of(CraftingStationBlockEntity::new, CRAFTING_STATION_BLOCK).build(null);

    public static final ExtendedScreenHandlerType<CraftingStationMenu, CraftingStationOpenData> CRAFTING_STATION_MENU =
            new ExtendedScreenHandlerType<>(CraftingStationMenu::fromNetwork, CraftingStationOpenData.STREAM_CODEC);

    public static final StationBlock SMELTING_STATION_BLOCK = new StationBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion());
    public static final StationBlock BLASTING_STATION_BLOCK = new StationBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion());
    public static final StationBlock SMOKING_STATION_BLOCK = new StationBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion());
    public static final StationBlock BREWING_STATION_BLOCK = new StationBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion());

    public static final BlockItem SMELTING_STATION_ITEM = new BlockItem(SMELTING_STATION_BLOCK, new Item.Properties());
    public static final BlockItem BLASTING_STATION_ITEM = new BlockItem(BLASTING_STATION_BLOCK, new Item.Properties());
    public static final BlockItem SMOKING_STATION_ITEM = new BlockItem(SMOKING_STATION_BLOCK, new Item.Properties());
    public static final BlockItem BREWING_STATION_ITEM = new BlockItem(BREWING_STATION_BLOCK, new Item.Properties());

    public static final BlockEntityType<StationBlockEntity> STATION_BE =
            BlockEntityType.Builder.of(StationBlockEntity::new,
                    SMELTING_STATION_BLOCK, BLASTING_STATION_BLOCK, SMOKING_STATION_BLOCK, BREWING_STATION_BLOCK).build(null);

    public static final ExtendedScreenHandlerType<CookingStationMenu, StationOpenData> COOKING_STATION_MENU =
            new ExtendedScreenHandlerType<>(CookingStationMenu::fromNetwork, StationOpenData.STREAM_CODEC);
    public static final ExtendedScreenHandlerType<BrewingStationMenu, StationOpenData> BREWING_STATION_MENU =
            new ExtendedScreenHandlerType<>(BrewingStationMenu::fromNetwork, StationOpenData.STREAM_CODEC);

    public static final ResourceKey<CreativeModeTab> CRAFTING_CENTRAL_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(CraftingNetworkCommon.MODID, "crafting_network"));

    private Registration() {}

    private static final String[] TIER_NAMES = {
        "Copper", "Iron", "Gold", "Emerald", "Diamond", "Netherite"
    };

    public static ItemStack stationWithTier(int tier) {
        ItemStack stack = new ItemStack(CRAFTING_STATION_ITEM);
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tier);
        BlockEntity.addEntityType(tag, getCraftingStationBEType());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(TIER_NAMES[tier] + " Crafting Terminal"));
        return stack;
    }

    public static ItemStack stationWithTier(StationType type, int tier) {
        ItemStack stack = new ItemStack(getStationItem(type));
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tier);
        BlockEntity.addEntityType(tag, STATION_BE);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(Component.translatable("block.crafting_network." + type.path()).getString() + " " + TIER_NAMES[tier]));
        return stack;
    }

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, rl("crafting_terminal"), CRAFTING_STATION_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("crafting_terminal"), CRAFTING_STATION_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, rl("crafting_terminal"), CRAFTING_STATION_BE);
        Registry.register(BuiltInRegistries.MENU, rl("crafting_terminal"), CRAFTING_STATION_MENU);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, rl("crafting_upgrade"),
                CraftingUpgradeRecipe.Serializer.INSTANCE);

        Registry.register(BuiltInRegistries.BLOCK, rl("smelting_terminal"), SMELTING_STATION_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("smelting_terminal"), SMELTING_STATION_ITEM);
        Registry.register(BuiltInRegistries.BLOCK, rl("blasting_terminal"), BLASTING_STATION_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("blasting_terminal"), BLASTING_STATION_ITEM);
        Registry.register(BuiltInRegistries.BLOCK, rl("smoking_terminal"), SMOKING_STATION_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("smoking_terminal"), SMOKING_STATION_ITEM);
        Registry.register(BuiltInRegistries.BLOCK, rl("brewing_terminal"), BREWING_STATION_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("brewing_terminal"), BREWING_STATION_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, rl("station"), STATION_BE);
        Registry.register(BuiltInRegistries.MENU, rl("cooking_terminal"), COOKING_STATION_MENU);
        Registry.register(BuiltInRegistries.MENU, rl("brewing_terminal"), BREWING_STATION_MENU);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CRAFTING_CENTRAL_TAB_KEY,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.crafting_network"))
                        .icon(() -> stationWithTier(0))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(CRAFTING_CENTRAL_TAB_KEY).register(output -> {
            if (!Networking.isServerModded()) {
                return;
            }
            for (int i = 0; i < TIER_NAMES.length; i++) {
                output.accept(stationWithTier(i));
                for (StationType type : StationType.values()) {
                    output.accept(stationWithTier(type, i));
                }
            }
        });
    }

    public static Item getCraftingStationItem() {
        return CRAFTING_STATION_ITEM;
    }

    public static BlockEntityType<CraftingStationBlockEntity> getCraftingStationBEType() {
        return CRAFTING_STATION_BE;
    }

    public static BlockEntityType<StationBlockEntity> getStationBEType() {
        return STATION_BE;
    }

    public static Block getStationBlock(StationType type) {
        return switch (type) {
            case SMELTING -> SMELTING_STATION_BLOCK;
            case BLASTING -> BLASTING_STATION_BLOCK;
            case SMOKING -> SMOKING_STATION_BLOCK;
            case BREWING -> BREWING_STATION_BLOCK;
        };
    }

    public static Item getStationItem(StationType type) {
        return switch (type) {
            case SMELTING -> SMELTING_STATION_ITEM;
            case BLASTING -> BLASTING_STATION_ITEM;
            case SMOKING -> SMOKING_STATION_ITEM;
            case BREWING -> BREWING_STATION_ITEM;
        };
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(CraftingNetworkCommon.MODID, path);
    }
}
