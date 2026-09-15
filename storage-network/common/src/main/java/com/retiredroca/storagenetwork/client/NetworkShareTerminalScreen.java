package com.retiredroca.storagenetwork.client;

import com.retiredroca.storagenetwork.menu.NetworkShareTerminalMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Simple collection view for an Output Terminal: no search box and no source dropdown. */
public class NetworkShareTerminalScreen extends AbstractContainerScreen<NetworkShareTerminalMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int ROWS = 6;

    public NetworkShareTerminalScreen(NetworkShareTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(BG, x, y, 0, 0, this.imageWidth, ROWS * 18 + 17);
        graphics.blit(BG, x, y + ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
    }
}
