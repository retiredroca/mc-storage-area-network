package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Renders the Network Share Terminal as a tinted ender chest. */
public class NetworkShareTerminalRenderer<T extends AbstractNetworkShareTerminalBlockEntity>
        implements BlockEntityRenderer<T> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/chest/ender.png");
    public static final int TINT = 0x2FBF9F;

    private final ModelPart root;

    public NetworkShareTerminalRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(ModelLayers.CHEST);
    }

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int light, int overlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        this.root.render(poseStack, consumer, light, overlay, 0xFF000000 | TINT);
    }
}
