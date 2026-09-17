package com.retiredroca.remoteaccessterminal.fabric;

import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Fabric screen-opening data carrier for the terminal picker/settings menu. */
public final class TerminalMenuOpener implements ExtendedScreenHandlerFactory<TerminalMenu.Data> {
    private final TerminalMenu.Data data;
    private final Component title;

    public TerminalMenuOpener(TerminalMenu.Data data, Component title) {
        this.data = data;
        this.title = title;
    }

    @Override
    public TerminalMenu.Data getScreenOpeningData(ServerPlayer player) {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return TerminalMenu.fromNetwork(containerId, inventory, data);
    }
}
