package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.retiredroca.storagenetwork.block.StorageTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class StorageTerminalRenderer<T extends AbstractStorageTerminalBlockEntity> implements BlockEntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("storage_network",
            "textures/entity/chest/copper.png");
    private static final int[] TIER_COLORS = { 0xB87333, 0xC0C0C0, 0xF5C020, 0x41CD6D, 0x4EEDE9, 0x55585C };

    private final ModelPart root;
    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart bottom;

    public StorageTerminalRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart baked = context.bakeLayer(ModelLayers.CHEST);
        this.root = baked;
        this.lid = baked.getChild("lid");
        this.lock = baked.getChild("lock");
        this.bottom = baked.getChild("bottom");
    }

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        float openness = entity.getOpenNess(partialTick);
        float eased = 1.0F - (float) Math.pow(1.0F - (double) openness, 3.0);

        Direction facing = entity.getBlockState().getValue(StorageTerminalBlock.FACING);

        poseStack.pushPose();
        TerminalRender.applyFacing(poseStack, facing);

        this.lid.xRot = -eased * 1.5707964F;
        this.lock.xRot = -eased * 1.5707964F;

        VertexConsumer consumer = bufferSource
                .getBuffer(RenderType.entityCutout(TEXTURE));
        this.root.render(poseStack, consumer, light, overlay, 0xFF000000 | TIER_COLORS[easedTier(entity.getTier())]);
        poseStack.popPose();
    }

    private static int easedTier(int tier) {
        return Math.max(0, Math.min(tier, TIER_COLORS.length - 1));
    }
}
