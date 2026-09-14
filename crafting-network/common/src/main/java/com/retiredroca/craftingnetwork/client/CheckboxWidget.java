package com.retiredroca.craftingnetwork.client;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A compact 17x17 checkbox (vanilla sprites, no label) used for per-machine toggles. It carries a
 * tooltip for the description and can be synced from the server-side flag via {@link #setSelected}.
 */
public class CheckboxWidget extends AbstractWidget {
    private static final int SIZE = 9;

    private boolean selected;
    private final Consumer<Boolean> onToggle;

    public CheckboxWidget(int x, int y, boolean selected, Component tooltip, Consumer<Boolean> onToggle) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.selected = selected;
        this.onToggle = onToggle;
        setTooltip(Tooltip.create(tooltip));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.selected = !this.selected;
        this.onToggle.accept(this.selected);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int border = isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFA0A0A0;
        graphics.fill(x, y, x + SIZE, y + SIZE, border);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0xFF202020);
        if (selected) {
            graphics.fill(x + 2, y + 2, x + SIZE - 2, y + SIZE - 2, 0xFF4CAF50);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
