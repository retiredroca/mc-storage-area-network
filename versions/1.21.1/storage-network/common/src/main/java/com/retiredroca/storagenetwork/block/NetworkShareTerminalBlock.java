package com.retiredroca.storagenetwork.block;

import com.mojang.serialization.MapCodec;
import com.retiredroca.mcstorageareanetwork.api.CollectionOnlyStorage;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.NetworkBlock;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Output Terminal: a collection-only sink that receives crafted output. It is hidden from
 * terminal listings, anyone can open it, but only its owner can break it.
 */
public class NetworkShareTerminalBlock extends BaseEntityBlock implements CollectionOnlyStorage, NetworkBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public NetworkShareTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(NetworkShareTerminalBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof AbstractNetworkShareTerminalBlockEntity share) {
            // Crouch + right-click toggles whether this sink is listed in the network. Only the owner
            // (or their scoreboard team, when sharing is on) may toggle it.
            if (player.isSecondaryUseActive()) {
                ContainerOwnership.Entry viewer = new ContainerOwnership.Entry(player.getUUID(),
                        player.getGameProfile().getName());
                if (!ContainerOwnership.canSee(serverLevel, ContainerOwnership.ownerOf(serverLevel, pos), viewer)) {
                    player.displayClientMessage(Component.translatable("block.storage_network.terminal_locked"), true);
                    return InteractionResult.CONSUME;
                }
                boolean exposed = share.toggleExposedToNetwork();
                player.displayClientMessage(Component.translatable(exposed
                        ? "message.storage_network.output_exposed"
                        : "message.storage_network.output_hidden"), true);
                return InteractionResult.CONSUME;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                share.startOpen(serverPlayer);
                StorageNetworkCommon.platform().openShareTerminal(serverPlayer, share);
            }
        }
        return InteractionResult.CONSUME;
    }
}
