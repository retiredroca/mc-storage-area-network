package com.retiredroca.craftingnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class StationBEWLR {
    public static void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        int tier = 0;
        if (stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
            CompoundTag tag = stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
            tier = tag.getInt("tier");
        }

        StationType type = StationType.SMELTING;
        for (StationType candidate : StationType.values()) {
            if (stack.getItem() == CraftingNetworkCommon.platform().stationItem(candidate)) {
                type = candidate;
                break;
            }
        }

        StationRenderer.renderTintedModel(bufferSource, poseStack, light, overlay, type.baseState(), tier);
    }
}