package com.retiredroca.networkrouting.client;

import java.util.ArrayList;
import java.util.List;

import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.menu.RoutingMenu;
import com.retiredroca.networkrouting.network.RoutingPackets.ContainerInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Terminal-styled screen shared by the Routing Terminal (container list + maintenance controls) and
 * the Routing Linker (a single labeled container). Both modes show the same filter editor.
 */
public class RoutingScreen extends AbstractContainerScreen<RoutingMenu> {
    private static final int PANEL = 0xF00E1410;
    private static final int BORDER = 0xFF2F7A44;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int DIM = 0xFF9AA79E;
    private static final int GREEN = 0xFF55FF77;
    private static final int WARN = 0xFFFF6B6B;

    private static final int HEADER_Y = 78;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 90;
    private static final int LIST_LIMIT = 9;
    private static final int ROW_H = 11;

    private static final int EDIT_X = 180;
    private static final int NAME_Y = 90;
    private static final int TOKEN_Y = 102;
    private static final int TOKEN_LIMIT = 5;

    private EditBox tokenBox;
    private int selected;
    private int lastState = Integer.MIN_VALUE;

    public RoutingScreen(RoutingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 210;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        if (menu.isTerminal()) {
            for (int i = 0; i < 3; i++) {
                final int bit = 1 << i;
                String name = switch (i) {
                    case 0 -> "Sort";
                    case 1 -> "Defrag";
                    default -> "Trim";
                };
                addRenderableWidget(Button.builder(
                        Component.literal(name + ": " + ((menu.getFlags() & bit) != 0 ? "ON" : "OFF")),
                        b -> NetworkRoutingCommon.platform().sendToggle(menu.getPos(), menu.getFlags() ^ bit))
                        .bounds(x + 8 + i * 94, y + 32, 90, 18).build());
            }
            for (int i = 0; i < 3; i++) {
                final int action = i;
                String name = switch (i) {
                    case 0 -> "Sort now";
                    case 1 -> "Defrag now";
                    default -> "Trim now";
                };
                addRenderableWidget(Button.builder(Component.literal(name),
                        b -> NetworkRoutingCommon.platform().sendAction(menu.getPos(), action))
                        .bounds(x + 8 + i * 94, y + 54, 90, 18).build());
            }
        }
        tokenBox = new EditBox(this.font, x + EDIT_X, y + 158, 112, 16, Component.literal("filter"));
        tokenBox.setMaxLength(160);
        tokenBox.setHint(Component.literal("item / #tag / modid"));
        addRenderableWidget(tokenBox);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> addToken())
                .bounds(x + EDIT_X, y + 176, 54, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Clear"), b -> setTokens(List.of()))
                .bounds(x + EDIT_X + 58, y + 176, 54, 18).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // The server sends the snapshot right after opening, so the first init() often runs before
        // the state is known. Rebuild once it arrives, and whenever the flags change (button labels).
        int state = (menu.isTerminal() ? 1 : 0) | (menu.isBound() ? 2 : 0)
                | (menu.showList() ? 4 : 0) | (menu.getFlags() << 3);
        if (state != lastState) {
            lastState = state;
            rebuildWidgets();
        }
    }

    private ContainerInfo editorTarget() {
        List<ContainerInfo> list = menu.getContainers();
        if (list.isEmpty()) {
            return null;
        }
        if (menu.isTerminal()) {
            if (selected < 0 || selected >= list.size()) {
                selected = 0;
            }
            return list.get(selected);
        }
        return list.get(0);
    }

    private void addToken() {
        String value = tokenBox.getValue().trim();
        if (value.isEmpty()) {
            return;
        }
        ContainerInfo target = editorTarget();
        if (target == null) {
            return;
        }
        List<String> tokens = new ArrayList<>(target.tokens());
        if (!tokens.contains(value)) {
            tokens.add(value);
        }
        tokenBox.setValue("");
        setTokens(tokens);
    }

    private void setTokens(List<String> tokens) {
        ContainerInfo target = editorTarget();
        if (target == null) {
            return;
        }
        NetworkRoutingCommon.platform().sendSetFilter(menu.getPos(), target.pos(), tokens, menu.isTerminal());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth, y + 1, BORDER);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, BORDER);
        graphics.fill(x, y, x + 1, y + imageHeight, BORDER);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, BORDER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 8, GREEN, false);
        if (menu.isTerminal()) {
            String status = menu.isBound()
                    ? "Connected to Storage Terminal (Tier " + menu.getTier() + ")"
                    : "Not connected";
            graphics.drawString(this.font, status, 8, 21, menu.isBound() ? DIM : WARN, false);
        }
        if (menu.showList()) {
            renderList(graphics);
        }
        renderEditor(graphics);
    }

    private void renderList(GuiGraphics graphics) {
        int x = LIST_X;
        int y = LIST_Y;
        graphics.drawString(this.font, "Containers", x, HEADER_Y, DIM, false);
        if (menu.isTerminal() && !menu.isBound()) {
            graphics.drawString(this.font, "No network to connect to.", x, y, WARN, false);
            graphics.drawString(this.font, "Please install a Storage Terminal.", x, y + ROW_H, WARN, false);
            return;
        }
        List<ContainerInfo> list = menu.getContainers();
        if (list.isEmpty()) {
            graphics.drawString(this.font, "(none found)", x, y, DIM, false);
            return;
        }
        int shown = Math.min(list.size(), LIST_LIMIT);
        for (int i = 0; i < shown; i++) {
            ContainerInfo info = list.get(i);
            String label = info.label();
            if (label.length() > 19) {
                label = label.substring(0, 19);
            }
            String row = (i == selected ? "> " : "  ") + label
                    + (info.tokens().isEmpty() ? "" : "  [" + info.tokens().size() + "]");
            graphics.drawString(this.font, row, x, y + i * ROW_H, i == selected ? GREEN : TEXT, false);
        }
    }

    private void renderEditor(GuiGraphics graphics) {
        int x = EDIT_X;
        graphics.drawString(this.font, "Filter", x, HEADER_Y, DIM, false);
        ContainerInfo target = editorTarget();
        if (target == null) {
            return;
        }
        String label = target.label();
        if (label.length() > 16) {
            label = label.substring(0, 16);
        }
        graphics.drawString(this.font, label, x, NAME_Y, TEXT, false);

        List<String> tokens = target.tokens();
        for (int i = 0; i < tokens.size() && i < TOKEN_LIMIT; i++) {
            String token = tokens.get(i);
            if (token.length() > 17) {
                token = token.substring(0, 17);
            }
            graphics.drawString(this.font, "- " + token, x, TOKEN_Y + i * ROW_H, GREEN, false);
        }
        if (tokens.size() > TOKEN_LIMIT) {
            graphics.drawString(this.font, "... +" + (tokens.size() - TOKEN_LIMIT), x,
                    TOKEN_Y + TOKEN_LIMIT * ROW_H, DIM, false);
        }
        graphics.drawString(this.font, "click to remove", x, 198, DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (menu.showList()) {
            int listX = this.leftPos + LIST_X;
            int listY = this.topPos + LIST_Y;
            int shown = Math.min(menu.getContainers().size(), LIST_LIMIT);
            if (mouseX >= listX && mouseX <= listX + 165) {
                int index = (int) ((mouseY - listY) / ROW_H);
                if (index >= 0 && index < shown) {
                    selected = index;
                    return true;
                }
            }
        }
        ContainerInfo target = editorTarget();
        if (target != null) {
            int ex = this.leftPos + EDIT_X;
            int ey = this.topPos + TOKEN_Y;
            List<String> tokens = target.tokens();
            if (mouseX >= ex && mouseX <= ex + 110) {
                int index = (int) ((mouseY - ey) / ROW_H);
                if (index >= 0 && index < Math.min(tokens.size(), TOKEN_LIMIT)) {
                    List<String> copy = new ArrayList<>(tokens);
                    copy.remove(index);
                    setTokens(copy);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
