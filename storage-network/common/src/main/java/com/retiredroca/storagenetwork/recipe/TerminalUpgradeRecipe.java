package com.retiredroca.storagenetwork.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.retiredroca.storagenetwork.StorageNetworkCommon;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TerminalUpgradeRecipe implements CraftingRecipe {
    public static final String TAG_TIER = "tier";

    private final int targetTier;

    public TerminalUpgradeRecipe(int targetTier) {
        this.targetTier = targetTier;
    }

    public int getTargetTier() {
        return targetTier;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) {
            return false;
        }
        ItemStack center = input.getItem(4);
        if (!center.is(StorageNetworkCommon.platform().terminalItem())) {
            return false;
        }
        if (readTier(center) != targetTier - 1) {
            return false;
        }
        Item material = materialFor(targetTier);
        for (int i = 0; i < 9; i++) {
            if (i == 4) {
                continue;
            }
            boolean ring = i == 1 || i == 3 || i == 5 || i == 7;
            if (ring) {
                if (!input.getItem(i).is(material)) {
                    return false;
                }
            } else if (!input.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return buildResult();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return buildResult();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    public static int readTier(ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.isEmpty()) {
            return 0;
        }
        return data.copyTag().getInt(TAG_TIER);
    }

    private ItemStack buildResult() {
        ItemStack result = new ItemStack(StorageNetworkCommon.platform().terminalItem());
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_TIER, targetTier);
        BlockEntity.addEntityType(tag, StorageNetworkCommon.platform().terminalBlockEntityType());
        CustomData.set(DataComponents.BLOCK_ENTITY_DATA, result, tag);
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);
        Ingredient ring = Ingredient.of(materialFor(targetTier));
        list.set(1, ring);
        list.set(3, ring);
        list.set(5, ring);
        list.set(7, ring);
        list.set(4, Ingredient.of(previousTierStack()));
        return list;
    }

    @Override
    public boolean isIncomplete() {
        NonNullList<Ingredient> list = getIngredients();
        return list.isEmpty() || list.stream()
                .filter(ingredient -> !ingredient.isEmpty())
                .anyMatch(ingredient -> ingredient.getItems().length == 0);
    }

    private ItemStack previousTierStack() {
        ItemStack stack = new ItemStack(StorageNetworkCommon.platform().terminalItem());
        if (targetTier > 1) {
            CompoundTag tag = new CompoundTag();
            tag.putInt(TAG_TIER, targetTier - 1);
            BlockEntity.addEntityType(tag, StorageNetworkCommon.platform().terminalBlockEntityType());
            CustomData.set(DataComponents.BLOCK_ENTITY_DATA, stack, tag);
        }
        return stack;
    }

    private static Item materialFor(int tier) {
        return switch (tier) {
            case 1 -> Items.IRON_INGOT;
            case 2 -> Items.GOLD_INGOT;
            case 3 -> Items.EMERALD;
            case 4 -> Items.DIAMOND;
            case 5 -> Items.NETHERITE_INGOT;
            default -> Items.AIR;
        };
    }

    public static class Serializer implements RecipeSerializer<TerminalUpgradeRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<TerminalUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
                .group(Codec.INT.fieldOf("target_tier").forGetter(TerminalUpgradeRecipe::getTargetTier))
                .apply(instance, TerminalUpgradeRecipe::new));

        @Override
        public MapCodec<TerminalUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TerminalUpgradeRecipe> streamCodec() {
            return StreamCodec.composite(ByteBufCodecs.INT, TerminalUpgradeRecipe::getTargetTier,
                    TerminalUpgradeRecipe::new);
        }
    }
}