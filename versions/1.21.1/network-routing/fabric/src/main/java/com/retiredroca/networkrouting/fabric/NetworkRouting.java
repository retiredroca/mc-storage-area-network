package com.retiredroca.networkrouting.fabric;

import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;
import com.retiredroca.mcstorageareanetwork.api.StorageRouter;
import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.block.TerminalProtection;
import com.retiredroca.networkrouting.routing.NetworkRoutingRule;
import com.retiredroca.networkrouting.routing.RoutingLabels;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public class NetworkRouting implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkRoutingCommon.setPlatform(new FabricNetworkRoutingPlatform());
        Registration.register();
        Networking.register();
        StorageRouter.register(NetworkRoutingRule.INSTANCE);

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() != Registration.LINKER_ITEM) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            // Only primary storage (chest/double chest, trapped chest, barrel); leave other blocks alone.
            if (!NetworkSettings.isStorageContainer(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()))) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                if (player.isShiftKeyDown()) {
                    RoutingLabels.clear(serverLevel, pos);
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.network_routing.filter_cleared"), true);
                } else {
                    NetworkRoutingCommon.platform().openMenu(serverPlayer, pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel && !TerminalProtection.canBreak(serverLevel, pos, player)) {
                player.displayClientMessage(Component.translatable("block.network_routing.terminal_locked"), true);
                return false;
            }
            return true;
        });
    }
}
