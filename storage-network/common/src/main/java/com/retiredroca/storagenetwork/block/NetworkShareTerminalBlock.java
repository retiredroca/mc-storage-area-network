package com.retiredroca.storagenetwork.block;

import com.mojang.serialization.MapCodec;
import com.retiredroca.mcstorageareanetwork.api.CollectionOnlyStorage;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Network Share Terminal: a collection-only sink that receives crafted output. It is hidden from
 * terminal listings, anyone can open it, but only its owner can break it.
 */
public class NetworkShareTerminalBlock extends BaseEntityBlock implements CollectionOnlyStorage {
    public NetworkShareTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(NetworkShareTerminalBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return StorageNetworkCommon.platform().createShareTerminalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && placer instanceof Player player) {
            ContainerOwnership.setOwner(serverLevel, pos, player.getUUID(), player.getGameProfile().getName());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AbstractNetworkShareTerminalBlockEntity share
                && player instanceof ServerPlayer serverPlayer) {
            StorageNetworkCommon.platform().openShareTerminal(serverPlayer, share);
        }
        return InteractionResult.CONSUME;
    }
}
