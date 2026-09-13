package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.retiredroca.storagenetwork.block.NetworkShareTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Renders the Network Share Terminal as a white-tinted ender chest that opens like a chest. */
public class NetworkShareTerminalRenderer<T extends AbstractNetworkShareTerminalBlockEntity>
        implements BlockEntityRenderer<T> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("storage_network", "textures/entity/chest/share_terminal.png");
    public static final int TINT = 0xFFFFFF;

    private final ModelPart root;
    private final ModelPart lid;
    private final ModelPart lock;

    public NetworkShareTerminalRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(ModelLayers.CHEST);
        this.lid = this.root.getChild("lid");
        this.lock = this.root.getChild("lock");
    }

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int light, int overlay) {
        float openness = entity.getOpenNess(partialTick);
        float eased = 1.0F - (float) Math.pow(1.0F - (double) openness, 3.0);
        Direction facing = entity.getBlockState().getValue(NetworkShareTerminalBlock.FACING);
        float rotation = facing.get2DDataValue() * 90.0F;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, -0.5, -0.5);

        this.lid.xRot = -eased * 1.5707964F;
        this.lock.xRot = -eased * 1.5707964F;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        this.root.render(poseStack, consumer, light, overlay, 0xFF000000 | TINT);
        poseStack.popPose();
    }
}
