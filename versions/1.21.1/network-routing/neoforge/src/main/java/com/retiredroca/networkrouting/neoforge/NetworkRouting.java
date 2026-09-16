package com.retiredroca.networkrouting.neoforge;

import com.retiredroca.mcstorageareanetwork.api.NetworkSettings;
import com.retiredroca.mcstorageareanetwork.api.StorageRouter;
import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.routing.NetworkRoutingRule;
import com.retiredroca.networkrouting.routing.RoutingLabels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(NetworkRoutingCommon.MODID)
public class NetworkRouting {
    public NetworkRouting(IEventBus modEventBus, ModContainer modContainer) {
        NetworkRoutingCommon.setPlatform(new NeoForgeNetworkRoutingPlatform());
        Registration.register(modEventBus);
        Networking.register(modEventBus);
        StorageRouter.register(NetworkRoutingRule.INSTANCE);

        NeoForge.EVENT_BUS.addListener(NetworkRouting::onRightClickBlock);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.getItem() != Registration.LINKER_ITEM.get() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockPos pos = event.getPos();
        // Only primary storage (chest/double chest, trapped chest, barrel); leave other blocks alone.
        if (!NetworkSettings.isStorageContainer(
                BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(pos).getBlock()))) {
            return;
        }
        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);

        if (event.getLevel().isClientSide()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && event.getLevel() instanceof ServerLevel serverLevel) {
            if (player.isShiftKeyDown()) {
                RoutingLabels.clear(serverLevel, pos);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.network_routing.filter_cleared"), true);
            } else {
                NetworkRoutingCommon.platform().openMenu(serverPlayer, pos, false);
            }
        }
    }
}
