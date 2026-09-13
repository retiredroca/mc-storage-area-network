package com.retiredroca.storagenetwork.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.storagenetwork.client.StorageTerminalBEWLR;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NeoForgeStorageTerminalBEWLR extends BlockEntityWithoutLevelRenderer {
    public NeoForgeStorageTerminalBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        StorageTerminalBEWLR.renderByItem(stack, displayContext, poseStack, bufferSource, light, overlay);
    }
}
