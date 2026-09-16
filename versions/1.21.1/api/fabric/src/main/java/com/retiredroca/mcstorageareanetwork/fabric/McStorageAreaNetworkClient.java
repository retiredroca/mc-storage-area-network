package com.retiredroca.mcstorageareanetwork.fabric;

import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ConfirmBreakPayload;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ForceBreakPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/** Client side of the API: the operator break-confirmation screen. */
public class McStorageAreaNetworkClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfirmBreakPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(new ConfirmScreen(confirmed -> {
                        if (confirmed) {
                            ClientPlayNetworking.send(new ForceBreakPayload(payload.pos()));
                        }
                        minecraft.setScreen(null);
                    },
                            Component.translatable("gui.mc_storage_area_network.confirm_break.title"),
                            Component.translatable("gui.mc_storage_area_network.confirm_break.message")));
                }));
    }
}
