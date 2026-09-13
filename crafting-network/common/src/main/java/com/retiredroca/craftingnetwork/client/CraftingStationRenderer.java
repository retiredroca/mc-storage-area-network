package com.retiredroca.craftingnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.retiredroca.craftingnetwork.blockentity.AbstractCraftingStationBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingStationRenderer<T extends AbstractCraftingStationBlockEntity> implements BlockEntityRenderer<T> {
    private static final BlockState CRAFTING_TABLE = Blocks.CRAFTING_TABLE.defaultBlockState();
    private static final int[] TIER_COLORS = { 0xB87333, 0xC0C0C0, 0xF5C020, 0x41CD6D, 0x4EEDE9, 0x55585C };

    public CraftingStationRenderer(BlockEntityRendererProvider.Context context) {}

    public static void renderTintedCraftingTable(MultiBufferSource buffers, PoseStack poseStack, int light,
            int overlay, int tier) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(CRAFTING_TABLE);
        RenderType renderType = ItemBlockRenderTypes.getRenderType(CRAFTING_TABLE, false);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        int color = TIER_COLORS[Math.max(0, Math.min(tier, TIER_COLORS.length - 1))];
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create();
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(CRAFTING_TABLE, direction, random)) {
                consumer.putBulkData(pose, quad, red, green, blue, 1.0F, light, overlay);
            }
        }
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(CRAFTING_TABLE, null, random)) {
            consumer.putBulkData(pose, quad, red, green, blue, 1.0F, light, overlay);
        }
    }

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        poseStack.pushPose();
        renderTintedCraftingTable(bufferSource, poseStack, light, overlay, entity.getTier());
        poseStack.popPose();
    }
}