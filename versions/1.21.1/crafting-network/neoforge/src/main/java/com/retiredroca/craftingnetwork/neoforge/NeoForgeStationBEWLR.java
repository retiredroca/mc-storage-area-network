package com.retiredroca.craftingnetwork.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.craftingnetwork.client.StationBEWLR;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NeoForgeStationBEWLR extends BlockEntityWithoutLevelRenderer {
    public NeoForgeStationBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        StationBEWLR.renderByItem(stack, displayContext, poseStack, bufferSource, light, overlay);
    }
}
