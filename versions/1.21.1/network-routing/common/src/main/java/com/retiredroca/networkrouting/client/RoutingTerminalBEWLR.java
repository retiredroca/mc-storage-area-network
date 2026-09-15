package com.retiredroca.networkrouting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class RoutingTerminalBEWLR {
    public static void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BlockState itemState = NetworkRoutingCommon.platform().terminalBlock().defaultBlockState()
                .setValue(RoutingTerminalBlock.FACING, Direction.SOUTH);
        AbstractRoutingTerminalBlockEntity entity = NetworkRoutingCommon.platform()
                .createTerminalBlockEntity(BlockPos.ZERO, itemState);
        mc.getBlockEntityRenderDispatcher().renderItem(entity, poseStack, bufferSource, light, overlay);
    }
}
