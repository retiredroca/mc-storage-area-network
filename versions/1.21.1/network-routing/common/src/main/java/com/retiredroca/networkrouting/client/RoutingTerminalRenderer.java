package com.retiredroca.networkrouting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.retiredroca.networkrouting.block.RoutingTerminalBlock;
import com.retiredroca.networkrouting.blockentity.AbstractRoutingTerminalBlockEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders the Routing Terminal using the vanilla chest geometry. The casing is drawn untinted (so
 * the screen/keyboard keep their colours) and the front lock band is tinted with the bound Storage
 * Terminal's tier.
 */
public class RoutingTerminalRenderer<T extends AbstractRoutingTerminalBlockEntity>
        implements BlockEntityRenderer<T> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("network_routing",
            "textures/entity/chest/network_routing.png");
    private static final int[] TIER_COLORS = { 0xB87333, 0xC0C0C0, 0xF5C020, 0x41CD6D, 0x4EEDE9, 0x55585C };
    private static final int UNBOUND = 0x8A8F98;

    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart bottom;

    public RoutingTerminalRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart baked = context.bakeLayer(ModelLayers.CHEST);
        this.lid = baked.getChild("lid");
        this.lock = baked.getChild("lock");
        this.bottom = baked.getChild("bottom");
    }

    @Override
    public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int light,
            int overlay) {
        float openness = entity.getOpenNess(partialTick);
        float eased = 1.0F - (float) Math.pow(1.0F - (double) openness, 3.0);

        Direction facing = entity.getBlockState().getValue(RoutingTerminalBlock.FACING);

        poseStack.pushPose();
        TerminalRender.applyFacing(poseStack, facing);

        this.lid.xRot = -eased * 1.5707964F;
        this.lock.xRot = -eased * 1.5707964F;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        this.bottom.render(poseStack, consumer, light, overlay, 0xFFFFFFFF);
        this.lid.render(poseStack, consumer, light, overlay, 0xFFFFFFFF);
        int tint = entity.isBound()
                ? TIER_COLORS[Math.max(0, Math.min(entity.getHostTier(), TIER_COLORS.length - 1))]
                : UNBOUND;
        this.lock.render(poseStack, consumer, light, overlay, 0xFF000000 | tint);
        poseStack.popPose();
    }
}
