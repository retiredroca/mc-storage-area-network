package com.retiredroca.networkrouting.fabric;

import com.retiredroca.networkrouting.menu.RoutingMenu;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Fabric screen-opening data carrier for the routing screen (terminal or labeled container). */
public final class RoutingMenuOpener implements ExtendedScreenHandlerFactory<BlockPos> {
    private final BlockPos pos;
    private final Component title;

    public RoutingMenuOpener(BlockPos pos, Component title) {
        this.pos = pos;
        this.title = title;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return pos;
    }

    @Override
    public Component getDisplayName() {
        return title;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RoutingMenu(containerId, inventory, pos);
    }
}
