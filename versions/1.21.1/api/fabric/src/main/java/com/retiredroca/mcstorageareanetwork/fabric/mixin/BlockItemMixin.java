package com.retiredroca.mcstorageareanetwork.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Records which player placed each storage container so terminals can filter by ownership. */
@Mixin(BlockItem.class)
public class BlockItemMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void mc_storage_area_network$trackContainerPlacement(BlockPlaceContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() == InteractionResult.FAIL) {
            return;
        }
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!(context.getItemInHand().getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block block = blockItem.getBlock();
        BlockPos clicked = context.getClickedPos();
        BlockPos placed = level.getBlockState(clicked).getBlock() == block ? clicked
                : clicked.relative(context.getClickedFace());
        if (level.getBlockState(placed).getBlock() != block) {
            return;
        }
        if (!(level.getBlockEntity(placed) instanceof Container)) {
            return;
        }
        ContainerOwnership.setOwner(serverLevel, placed, player.getUUID(), player.getGameProfile().getName());
    }
}
