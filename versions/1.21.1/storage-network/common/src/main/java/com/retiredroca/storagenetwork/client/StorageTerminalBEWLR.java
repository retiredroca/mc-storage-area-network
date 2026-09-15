package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.block.StorageTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        // Chest-geometry items render like vanilla chests: front (lock) toward the viewer, which the
        // shared item transform shows for FACING = SOUTH.
        BlockState itemState = StorageNetworkCommon.platform().terminalBlock().defaultBlockState()
                .setValue(StorageTerminalBlock.FACING, Direction.SOUTH);
        AbstractStorageTerminalBlockEntity entity = StorageNetworkCommon.platform()
                .createTerminalBlockEntity(BlockPos.ZERO, itemState);

        CompoundTag loadTag = new CompoundTag();
        loadTag.putInt("tier", tier);
        entity.loadWithComponents(loadTag, mc.level.registryAccess());

        mc.getBlockEntityRenderDispatcher().renderItem(entity, poseStack, bufferSource, light, overlay);
    }
}
