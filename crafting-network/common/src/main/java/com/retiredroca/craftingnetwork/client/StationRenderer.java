package com.retiredroca.craftingnetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.retiredroca.craftingnetwork.blockentity.AbstractStationBlockEntity;
import com.retiredroca.craftingnetwork.station.StationType;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class StationRenderer<T extends AbstractStationBlockEntity> implements BlockEntityRenderer<T> {
    private static final int[] TIER_COLORS = { 0xB87333, 0xC0C0C0, 0xF5C020, 0x41CD6D, 0x4EEDE9, 0x55585C };

    public StationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        if (entity.getLevel() == null) return;
        
        BlockState state = entity.getBlockState();
        StationType type = entity.type();
        int tier = entity.getTier();
        
        BlockState baseState = type.baseState();

        // Only furnace, blast furnace, and smoker have HORIZONTAL_FACING property.
        // The vanilla blockstate variants already bake the rotation into the model,
        // so we only need to pass the facing through to the model shaper here.
        boolean hasFacing = baseState.hasProperty(BlockStateProperties.HORIZONTAL_FACING);
        if (hasFacing) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            baseState = baseState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }

        poseStack.pushPose();
        renderTintedModel(bufferSource, poseStack, light, overlay, baseState, tier);
        poseStack.popPose();
    }

    public static void renderTintedModel(MultiBufferSource buffers, PoseStack poseStack, int light,
            int overlay, BlockState state, int tier) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getBlockModel(state);
        RenderType renderType = ItemBlockRenderTypes.getRenderType(state, false);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        int color = TIER_COLORS[Math.max(0, Math.min(tier, TIER_COLORS.length - 1))];
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        var pose = poseStack.last();
        RandomSource random = RandomSource.create();
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(state, direction, random)) {
                consumer.putBulkData(pose, quad, red, green, blue, 1.0F, light, overlay);
            }
        }
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            consumer.putBulkData(pose, quad, red, green, blue, 1.0F, light, overlay);
        }
    }
}