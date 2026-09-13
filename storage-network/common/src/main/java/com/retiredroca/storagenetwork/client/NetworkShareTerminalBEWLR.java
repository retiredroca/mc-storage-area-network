package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.block.NetworkShareTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NetworkShareTerminalBEWLR {
    public static void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        AbstractNetworkShareTerminalBlockEntity entity = StorageNetworkCommon.platform()
                .createShareTerminalBlockEntity(BlockPos.ZERO,
                        StorageNetworkCommon.platform().shareTerminalBlock().defaultBlockState()
                                .setValue(NetworkShareTerminalBlock.FACING, Direction.SOUTH));
        mc.getBlockEntityRenderDispatcher().renderItem(entity, poseStack, bufferSource, light, overlay);
    }
}
