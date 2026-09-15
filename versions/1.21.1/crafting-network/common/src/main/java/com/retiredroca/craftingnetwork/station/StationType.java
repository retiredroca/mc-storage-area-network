package com.retiredroca.craftingnetwork.station;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The four standalone processor stations. Each is a self-contained machine block that pulls
 * materials and fuel from the station network storage, processes them, and pushes results back.
 *
 * <p>Stations share the crafting station's tier system: higher tiers render with a brighter tint,
 * tick faster, and scan a larger chunk radius.
 */
public enum StationType {
    SMELTING("smelting_terminal", RecipeType.SMELTING, Blocks.FURNACE.defaultBlockState(), RecipeBookType.FURNACE, 200),
    BLASTING("blasting_terminal", RecipeType.BLASTING, Blocks.BLAST_FURNACE.defaultBlockState(), RecipeBookType.BLAST_FURNACE, 100),
    SMOKING("smoking_terminal", RecipeType.SMOKING, Blocks.SMOKER.defaultBlockState(), RecipeBookType.SMOKER, 100),
    BREWING("brewing_terminal", null, Blocks.BREWING_STAND.defaultBlockState(), null, 400);

    private static final float[] SPEED = { 1.0F, 1.25F, 1.5F, 1.8F, 2.2F, 2.8F };

    private final String path;
    private final RecipeType<? extends AbstractCookingRecipe> recipeType;
    private final BlockState baseState;
    private final RecipeBookType recipeBookType;
    private final int baseCookTicks;

    StationType(String path, RecipeType<? extends AbstractCookingRecipe> recipeType, BlockState baseState,
            RecipeBookType recipeBookType, int baseCookTicks) {
        this.path = path;
        this.recipeType = recipeType;
        this.baseState = baseState;
        this.recipeBookType = recipeBookType;
        this.baseCookTicks = baseCookTicks;
    }

    public String path() {
        return path;
    }

    public String descriptionId() {
        return "block.crafting_network." + path;
    }

    public String containerId() {
        return "container.crafting_network." + path;
    }

    public RecipeType<? extends AbstractCookingRecipe> recipeType() {
        return recipeType;
    }

    public boolean isBrewing() {
        return this == BREWING;
    }

    public BlockState baseState() {
        return baseState;
    }

    public RecipeBookType recipeBookType() {
        return recipeBookType;
    }

    public int baseCookTicks() {
        return baseCookTicks;
    }

    /** Progress per tick, scaled by tier (tier 0 = vanilla speed). */
    public float speed(int tier) {
        return SPEED[Math.max(0, Math.min(tier, SPEED.length - 1))];
    }

    public Block block() {
        return CraftingNetworkCommon.platform().stationBlock(this);
    }

    public static StationType fromBlock(Block block) {
        if (block != null) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null) {
                String path = id.getPath();
                for (StationType type : values()) {
                    if (type.path.equals(path)) {
                        return type;
                    }
                }
            }
        }
        return null;
    }

    public static StationType fromPath(String path) {
        if (path != null) {
            for (StationType type : values()) {
                if (type.path.equals(path) || type.name().equals(path)) {
                    return type;
                }
            }
        }
        return SMELTING;
    }
}