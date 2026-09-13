package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class StorageTerminalBEWLR {
    public static void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        int tier = 0;
        if (stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
            CompoundTag tag = stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
            tier = tag.getInt("tier");
        }

        Minecraft mc = Minecraft.getInstance();
        BlockState defaultState = StorageNetworkCommon.platform().terminalBlock().defaultBlockState();
        AbstractStorageTerminalBlockEntity entity = StorageNetworkCommon.platform()
                .createTerminalBlockEntity(BlockPos.ZERO, defaultState);

        CompoundTag loadTag = new CompoundTag();
        loadTag.putInt("tier", tier);
        entity.loadWithComponents(loadTag, mc.level.registryAccess());

        mc.getBlockEntityRenderDispatcher().renderItem(entity, poseStack, bufferSource, light, overlay);
    }
}
