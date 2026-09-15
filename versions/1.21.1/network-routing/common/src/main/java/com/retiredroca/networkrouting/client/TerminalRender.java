package com.retiredroca.networkrouting.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;

/** Shared facing transform for the Routing Terminal (the blockstate model is never drawn). */
public final class TerminalRender {
    private TerminalRender() {}

    public static void applyFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5, -0.5, -0.5);
    }
}
