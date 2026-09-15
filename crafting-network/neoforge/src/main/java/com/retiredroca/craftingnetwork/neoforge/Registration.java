package com.retiredroca.craftingnetwork.neoforge;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.block.CraftingStationBlock;
import com.retiredroca.craftingnetwork.block.StationBlock;
import com.retiredroca.craftingnetwork.menu.BrewingStationMenu;
import com.retiredroca.craftingnetwork.menu.CookingStationMenu;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;
import com.retiredroca.craftingnetwork.recipe.CraftingUpgradeRecipe;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CraftingNetworkCommon.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CraftingNetworkCommon.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CraftingNetworkCommon.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CraftingNetworkCommon.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CraftingNetworkCommon.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CraftingNetworkCommon.MODID);

    public static final DeferredBlock<CraftingStationBlock> CRAFTING_STATION_BLOCK =
            BLOCKS.register("crafting_terminal", () -> new CraftingStationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).noOcclusion()));

    public static final DeferredItem<BlockItem> CRAFTING_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("crafting_terminal", CRAFTING_STATION_BLOCK);

    public static final DeferredBlock<StationBlock> SMELTING_STATION =
            BLOCKS.register("smelting_terminal", () -> new StationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion()));
    public static final DeferredBlock<StationBlock> BLASTING_STATION =
            BLOCKS.register("blasting_terminal", () -> new StationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion()));
    public static final DeferredBlock<StationBlock> SMOKING_STATION =
            BLOCKS.register("smoking_terminal", () -> new StationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion()));
    public static final DeferredBlock<StationBlock> BREWING_STATION =
            BLOCKS.register("brewing_terminal", () -> new StationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.5f).noOcclusion()));

    public static final DeferredItem<BlockItem> SMELTING_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("smelting_terminal", SMELTING_STATION);
    public static final DeferredItem<BlockItem> BLASTING_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("blasting_terminal", BLASTING_STATION);
    public static final DeferredItem<BlockItem> SMOKING_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("smoking_terminal", SMOKING_STATION);
    public static final DeferredItem<BlockItem> BREWING_STATION_ITEM =
            ITEMS.registerSimpleBlockItem("brewing_terminal", BREWING_STATION);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> CRAFTING_UPGRADE_RECIPE =
            RECIPE_SERIALIZERS.register("crafting_upgrade", () -> CraftingUpgradeRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingStationBlockEntity>> CRAFTING_STATION_BE =
            BLOCK_ENTITIES.register("crafting_terminal",
                    () -> BlockEntityType.Builder.of(CraftingStationBlockEntity::new, CRAFTING_STATION_BLOCK.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StationBlockEntity>> STATION_BE =
            BLOCK_ENTITIES.register("station",
                    () -> BlockEntityType.Builder.of(StationBlockEntity::new,
                            SMELTING_STATION.get(), BLASTING_STATION.get(), SMOKING_STATION.get(), BREWING_STATION.get())
                            .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingStationMenu>> CRAFTING_STATION_MENU =
            MENUS.register("crafting_terminal",
                    () -> IMenuTypeExtension.create(CraftingStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CookingStationMenu>> COOKING_STATION_MENU =
            MENUS.register("cooking_terminal",
                    () -> IMenuTypeExtension.create(CookingStationMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<BrewingStationMenu>> BREWING_STATION_MENU =
            MENUS.register("brewing_terminal",
                    () -> IMenuTypeExtension.create(BrewingStationMenu::fromNetwork));

    private static final String[] TIER_NAMES = {
        "Copper", "Iron", "Gold", "Emerald", "Diamond", "Netherite"
    };

    public static ItemStack stationWithTier(int tier) {
        ItemStack stack = new ItemStack(CRAFTING_STATION_ITEM.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tier);
        BlockEntity.addEntityType(tag, getCraftingStationBEType());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Crafting Terminal " + TIER_NAMES[tier]));
        return stack;
    }

    public static ItemStack stationWithTier(StationType type, int tier) {
        ItemStack stack = new ItemStack(getStationItem(type));
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tier);
        BlockEntity.addEntityType(tag, STATION_BE.get());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(Component.translatable("block.crafting_network." + type.path()).getString() + " " + TIER_NAMES[tier]));
        return stack;
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CRAFTING_CENTRAL_TAB =
            CREATIVE_MODE_TABS.register("crafting_network", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.crafting_network"))
                    .icon(() -> stationWithTier(0))
                    .displayItems((params, output) -> {
                        if (!Networking.isServerModded()) {
                            return;
                        }
                        // Group by terminal type (all Crafting Terminal tiers, then all Smelting, ...),
                        // matching the Storage Network tab's ordering.
                        for (int i = 0; i < TIER_NAMES.length; i++) {
                            output.accept(stationWithTier(i));
                        }
                        for (StationType type : StationType.values()) {
                            for (int i = 0; i < TIER_NAMES.length; i++) {
                                output.accept(stationWithTier(type, i));
                            }
                        }
                    })
                    .build());

    public static Item getCraftingStationItem() {
        return CRAFTING_STATION_BLOCK.get().asItem();
    }

    public static BlockEntityType<CraftingStationBlockEntity> getCraftingStationBEType() {
        return CRAFTING_STATION_BE.get();
    }

    public static BlockEntityType<StationBlockEntity> getStationBEType() {
        return STATION_BE.get();
    }

    public static Block getStationBlock(StationType type) {
        return switch (type) {
            case SMELTING -> SMELTING_STATION.get();
            case BLASTING -> BLASTING_STATION.get();
            case SMOKING -> SMOKING_STATION.get();
            case BREWING -> BREWING_STATION.get();
        };
    }

    public static Item getStationItem(StationType type) {
        return switch (type) {
            case SMELTING -> SMELTING_STATION_ITEM.get();
            case BLASTING -> BLASTING_STATION_ITEM.get();
            case SMOKING -> SMOKING_STATION_ITEM.get();
            case BREWING -> BREWING_STATION_ITEM.get();
        };
    }

    private Registration() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
