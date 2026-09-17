package com.retiredroca.remoteaccessterminal.client;

import java.util.List;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;
import com.retiredroca.remoteaccessterminal.network.TerminalPackets.Destination;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The destination picker: the player's other terminals in the block's dye, ordered by its sort mode.
 * Uses the vanilla screen background and standard widgets only.
 */
public class TerminalPickerScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int ROWS = 6;
    private static final int ROW_STEP = 22;
    private static final int LIST_X = 10;
    private static final int LIST_Y = 32;
    private static final int ROW_WIDTH = 180;
    private static final int LABEL_COLOR = 4210752;

    private boolean requested;
    private int lastVersion = -1;

    public TerminalPickerScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 200;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        if (!requested && menu.getVersion() == 0) {
            requested = true;
            RemoteAccessTerminalCommon.platform().sendRequestSync();
        }
        int x = this.leftPos;
        int y = this.topPos;
        List<Destination> destinations = menu.getDestinations();
        int shown = Math.min(destinations.size(), ROWS);
        for (int i = 0; i < shown; i++) {
            Destination destination = destinations.get(i);
            addRenderableWidget(Button.builder(destinationLabel(destination),
                    button -> RemoteAccessTerminalCommon.platform()
                            .sendTravel(destination.dimension(), destination.pos()))
                    .bounds(x + LIST_X, y + LIST_Y + i * ROW_STEP, ROW_WIDTH, 20).build());
        }
        if (menu.canEdit()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.settings"),
                    button -> openSettings())
                    .bounds(x + LIST_X, y + imageHeight - 28, ROW_WIDTH, 20).build());
        }
    }

    private static Component destinationLabel(Destination destination) {
        String name = destination.name() == null || destination.name().isBlank()
                ? Component.translatable("gui.remote_access_terminal.unnamed").getString()
                : destination.name();
        return Component.literal(name + " (" + destination.dimension().location().getPath() + ")");
    }

    private void openSettings() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.setScreen(new TerminalSettingsScreen(menu, this.minecraft.player.getInventory(),
                    Component.translatable("gui.remote_access_terminal.settings")));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.getVersion() != lastVersion) {
            lastVersion = menu.getVersion();
            rebuildWidgets();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // The vanilla background is supplied by renderBackground; no bespoke chrome is drawn.
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = menu.getName() == null || menu.getName().isBlank()
                ? this.title
                : Component.literal(menu.getName());
        graphics.drawString(this.font, title, 8, 6, LABEL_COLOR, false);
        Component subtitle;
        if (menu.isLinker()) {
            subtitle = Component.translatable("gui.remote_access_terminal.link_destinations");
        } else {
            Component visibility = menu.isOpen()
                    ? Component.translatable("gui.remote_access_terminal.open")
                    : Component.translatable("gui.remote_access_terminal.private");
            subtitle = Component.translatable("gui.remote_access_terminal.destinations")
                    .append("  ").append(visibility);
        }
        graphics.drawString(this.font, subtitle, 8, 18, LABEL_COLOR, false);
        if (menu.getDestinations().isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.remote_access_terminal.no_destinations"),
                    8, LIST_Y, LABEL_COLOR, false);
        }
    }
}
