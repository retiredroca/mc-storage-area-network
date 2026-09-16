package com.retiredroca.mcstorageareanetwork.neoforge;

import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ForceBreakPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client side of the API: the operator break-confirmation screen. */
@Mod(value = McStorageAreaNetwork.MODID, dist = Dist.CLIENT)
public class McStorageAreaNetworkClient {
    public McStorageAreaNetworkClient(IEventBus modEventBus) {
    }

    public static void openConfirm(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PacketDistributor.sendToServer(new ForceBreakPayload(pos));
            }
            minecraft.setScreen(null);
        },
                Component.translatable("gui.mc_storage_area_network.confirm_break.title"),
                Component.translatable("gui.mc_storage_area_network.confirm_break.message")));
    }
}
