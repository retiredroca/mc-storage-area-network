package com.retiredroca.storagenetwork.fabric;

import com.retiredroca.mcstorageareanetwork.api.NetworkBlock;
import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;
import com.retiredroca.storagenetwork.StorageNetworkCommon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class StorageNetwork implements ModInitializer {
    @Override
    public void onInitialize() {
        StorageNetworkCommon.setPlatform(new FabricStorageNetworkPlatform());
        StorageNetworkConfig.load();
        Registration.register();
        Networking.register();

        // Crouch + right-click a container (empty hand) toggles its block type in/out of the network.
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive()
                    || !player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            if (level.getBlockState(pos).getBlock() instanceof NetworkBlock
                    || ItemStorage.SIDED.find(level, pos, null) == null) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkExclusions.Result result = NetworkExclusions.toggle(serverPlayer, pos);
                serverPlayer.displayClientMessage(StorageNetworkCommon.exclusionMessage(result), true);
            }
            return InteractionResult.SUCCESS;
        });
    }
}
