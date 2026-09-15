package com.retiredroca.craftingnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CraftingStationBEWLR {
    public static void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        int tier = 0;
        if (stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
            CompoundTag tag = stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
            tier = tag.getInt("tier");
        }
        CraftingStationRenderer.renderTintedCraftingTable(bufferSource, poseStack, light, overlay, tier);
    }
}