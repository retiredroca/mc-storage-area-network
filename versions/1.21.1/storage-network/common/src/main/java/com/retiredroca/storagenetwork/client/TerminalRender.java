package com.retiredroca.storagenetwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.core.Direction;

/**
 * Shared transform helpers for terminal block-entity rendering.
 *
 * <p>The terminal blocks use {@link net.minecraft.world.level.block.RenderShape#ENTITYBLOCK_ANIMATED},
 * so the block model / {@code blockstates/*.json} is <b>never drawn</b> — the block entity renderer
 * is the only thing that orients the block. Always rotate through {@link #applyFacing}; editing the
 * blockstate JSON will not change what you see in the world.
 *
 * <p>The rotation matches vanilla's {@code ChestRenderer} ({@code -facing.toYRot()}).
 */
public final class TerminalRender {
    private TerminalRender() {}

    /**
     * Rotates the pose about the block centre so the model's front (which points south, i.e. +Z) ends
     * up pointing along {@code facing}.
     */
    public static void applyFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5, -0.5, -0.5);
    }
}
