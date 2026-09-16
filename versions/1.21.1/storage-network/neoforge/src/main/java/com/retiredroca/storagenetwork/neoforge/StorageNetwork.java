package com.retiredroca.storagenetwork.neoforge;

import com.retiredroca.mcstorageareanetwork.api.NetworkBlock;
import com.retiredroca.mcstorageareanetwork.api.NetworkExclusions;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.block.TerminalProtection;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@Mod(StorageNetworkCommon.MODID)
public class StorageNetwork {
    public StorageNetwork(IEventBus modEventBus, ModContainer modContainer) {
        StorageNetworkCommon.setPlatform(new NeoForgeStorageNetworkPlatform());
        StorageNetworkConfig.register(modContainer);
        modEventBus.addListener(StorageNetworkConfig::onConfigLoad);

        Registration.register(modEventBus);
        Networking.register(modEventBus);

        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level
                    && !TerminalProtection.canBreak(level, event.getPos(), event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().displayClientMessage(
                        Component.translatable("block.storage_network.terminal_locked"), true);
            }
        });

        // Crouch + right-click a container (empty hand) toggles its block type in/out of the network.
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            Player player = event.getEntity();
            if (event.getHand() != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive()
                    || !player.getMainHandItem().isEmpty()) {
                return;
            }
            BlockPos pos = event.getPos();
            if (event.getLevel().getBlockState(pos).getBlock() instanceof NetworkBlock
                    || event.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, null) == null) {
                return;
            }
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
            if (event.getLevel().isClientSide()) {
                return;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkExclusions.Result result = NetworkExclusions.toggle(serverPlayer, pos);
                serverPlayer.displayClientMessage(StorageNetworkCommon.exclusionMessage(result), true);
            }
        });
    }
}
