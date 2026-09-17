package com.retiredroca.networkrouting.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.menu.RoutingMenu;
import com.retiredroca.networkrouting.network.RoutingPackets.ContainerInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.lwjgl.glfw.GLFW;

/**
 * Terminal-styled screen shared by the Routing Terminal (container list + maintenance controls) and
 * the Routing Linker (a single labeled container). Both modes show the same filter editor.
 */
public class RoutingScreen extends AbstractContainerScreen<RoutingMenu> {
    private static final int PANEL = 0xF00E1410;
    private static final int SUGGEST_PANEL = 0xFF0E1410;
    private static final int BORDER = 0xFF2F7A44;
    private static final int SCROLL_TRACK = 0xFF203A28;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int DIM = 0xFF9AA79E;
    private static final int GREEN = 0xFF55FF77;
    private static final int WARN = 0xFFFF6B6B;

    private static final int HEADER_Y = 78;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 90;
    private static final int LIST_LIMIT = 9;
    private static final int LIST_WIDTH = 165;
    private static final int ROW_H = 11;

    private static final int EDIT_X = 180;
    private static final int NAME_Y = 90;
    private static final int TOKEN_Y = 102;
    private static final int TOKEN_LIMIT = 5;
    private static final int TOKEN_WIDTH = 110;

    // Suggestions behave like vanilla chat autofill: anchored to the field's top edge, growing
    // upward, navigated with the arrow keys and picked with Tab/Enter (or clicked).
    private static final int SUGGEST_X = EDIT_X;
    private static final int SUGGEST_BOTTOM = 156;
    private static final int SUGGEST_ROW_H = 10;
    private static final int SUGGEST_MAX_ROWS = 7;
    private static final int SUGGEST_MIN_WIDTH = 112;

    private EditBox tokenBox;
    private int selected;
    private int lastState = Integer.MIN_VALUE;
    private boolean typing;
    private int highlighted = -1;
    private int tokenScroll;
    private int listScroll;
    private String tooltipEntry;
    private String lastQuery = "";
    private String lastTokens = "";
    private List<String> suggestions = List.of();
    /** Tokens added from this screen before the server's snapshot echoes them back. */
    private final Set<String> sessionTokens = new HashSet<>();

    public RoutingScreen(RoutingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 210;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        typing = false;
        highlighted = -1;
        tokenScroll = 0;
        listScroll = 0;
        lastQuery = "";
        lastTokens = "";
        suggestions = List.of();
        sessionTokens.clear();
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
        if (tokenBox != null && !tokenBox.isFocused()) {
            typing = false;
            highlighted = -1;
        }
        // The server sends the snapshot right after opening, so the first init() often runs before
        // the state is known. Rebuild once it arrives, and whenever the flags change (button labels).
        int state = (menu.isTerminal() ? 1 : 0) | (menu.isBound() ? 2 : 0)
                | (menu.showList() ? 4 : 0) | (menu.getFlags() << 3);
        if (state != lastState) {
            lastState = state;
            rebuildWidgets();
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean handled = super.charTyped(codePoint, modifiers);
        if (tokenBox != null && tokenBox.isFocused()) {
            typing = true;
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tokenBox != null && tokenBox.isFocused()) {
            boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
            if (suggestionsVisible()) {
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    moveHighlight(1);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_UP) {
                    moveHighlight(-1);
                    return true;
                }
                if ((enter || keyCode == GLFW.GLFW_KEY_TAB) && highlighted >= 0) {
                    addSuggestion(suggestions.get(highlighted));
                    return true;
                }
            } else if (enter) {
                addToken();
                return true;
            }
            typing = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.signum(scrollY);
        if (step != 0 && overTokenColumn(mouseX, mouseY)) {
            tokenScroll = clamp(tokenScroll - step, 0, maxTokenScroll());
            return true;
        }
        if (step != 0 && menu.showList() && overContainerList(mouseX, mouseY)) {
            listScroll = clamp(listScroll - step, 0, maxListScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean overTokenColumn(double mouseX, double mouseY) {
        int x = this.leftPos + EDIT_X;
        int y = this.topPos + TOKEN_Y;
        return mouseX >= x && mouseX <= x + TOKEN_WIDTH + 8
                && mouseY >= y - 2 && mouseY <= y + TOKEN_LIMIT * ROW_H;
    }

    private boolean overContainerList(double mouseX, double mouseY) {
        int x = this.leftPos + LIST_X;
        int y = this.topPos + LIST_Y;
        return mouseX >= x && mouseX <= x + LIST_WIDTH + 8
                && mouseY >= y - 2 && mouseY <= y + LIST_LIMIT * ROW_H;
    }

    private int maxTokenScroll() {
        ContainerInfo target = editorTarget();
        int size = target == null ? 0 : target.tokens().size();
        return Math.max(0, size - TOKEN_LIMIT);
    }

    private int maxListScroll() {
        return Math.max(0, menu.getContainers().size() - LIST_LIMIT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshSuggestions();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void moveHighlight(int delta) {
        int size = suggestions.size();
        if (size == 0) {
            highlighted = -1;
            return;
        }
        highlighted = highlighted < 0 ? 0 : Math.floorMod(highlighted + delta, size);
    }

    private void refreshSuggestions() {
        if (tokenBox == null) {
            suggestions = List.of();
            highlighted = -1;
            return;
        }
        String query = tokenBox.getValue();
        List<String> tokens = editorTokens();
        String signature = String.join("\u0000", tokens);
        if (query.equals(lastQuery) && signature.equals(lastTokens)) {
            return;
        }
        boolean queryChanged = !query.equals(lastQuery);
        lastQuery = query;
        lastTokens = signature;
        suggestions = FilterSuggestions.matching(query, tokens);
        if (queryChanged || highlighted >= suggestions.size()) {
            highlighted = suggestions.isEmpty() ? -1 : 0;
        }
    }

    /**
     * The selected container's tokens, plus anything added from this screen that the server has not
     * echoed back yet — so an added id leaves the suggestion list immediately.
     */
    private List<String> editorTokens() {
        ContainerInfo target = editorTarget();
        if (target == null) {
            sessionTokens.clear();
            return List.of();
        }
        List<String> tokens = target.tokens();
        sessionTokens.removeIf(tokens::contains);
        if (sessionTokens.isEmpty()) {
            return tokens;
        }
        List<String> union = new ArrayList<>(tokens);
        for (String token : sessionTokens) {
            if (!union.contains(token)) {
                union.add(token);
            }
        }
        return union;
    }

    private boolean suggestionsVisible() {
        return tokenBox != null && tokenBox.isFocused() && typing && !tokenBox.getValue().isBlank()
                && !suggestions.isEmpty();
    }

    private int suggestionRows() {
        return Math.min(suggestions.size(), SUGGEST_MAX_ROWS);
    }

    /** Top edge of the list, relative to the panel origin (the pose is translated in renderLabels). */
    private int suggestionTop() {
        return SUGGEST_BOTTOM - (suggestionRows() * SUGGEST_ROW_H + 2);
    }

    /** The list is as wide as its widest visible name, clamped to the window. */
    private int suggestionWidth() {
        int content = 0;
        for (int i = 0; i < suggestionRows(); i++) {
            content = Math.max(content, this.font.width(label(suggestions.get(i))));
        }
        int available = this.width - (this.leftPos + SUGGEST_X) - 4;
        return Math.max(SUGGEST_MIN_WIDTH, Math.min(content + 6, Math.max(SUGGEST_MIN_WIDTH, available)));
    }

    private static String label(String entry) {
        return FilterSuggestions.displayName(entry);
    }

    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        int rows = suggestionRows();
        int x = SUGGEST_X;
        int y = suggestionTop();
        int width = suggestionWidth();
        int height = rows * SUGGEST_ROW_H + 2;
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + y;
        if (mouseX >= absoluteX && mouseX < absoluteX + width) {
            int row = ((int) mouseY - absoluteY - 1) / SUGGEST_ROW_H;
            if (row >= 0 && row < rows) {
                highlighted = row;
                tooltipEntry = suggestions.get(row);
            }
        }
        graphics.fill(x, y, x + width, y + height, SUGGEST_PANEL);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
        for (int i = 0; i < rows; i++) {
            int rowY = y + 1 + i * SUGGEST_ROW_H;
            String name = label(suggestions.get(i));
            if (this.font.width(name) > width - 4) {
                name = this.font.plainSubstrByWidth(name, width - 4);
            }
            graphics.drawString(this.font, name, x + 2, rowY + 1, i == highlighted ? GREEN : TEXT, false);
        }
        if (suggestions.size() > rows) {
            graphics.drawString(this.font, "-" + (suggestions.size() - rows) + " more", x + 2,
                    y + height + 1, DIM, false);
        }
    }

    private boolean clickSuggestion(double mouseX, double mouseY) {
        if (!suggestionsVisible()) {
            return false;
        }
        int rows = suggestionRows();
        int absoluteX = this.leftPos + SUGGEST_X;
        int absoluteY = this.topPos + suggestionTop();
        int width = suggestionWidth();
        if (mouseX < absoluteX || mouseX >= absoluteX + width) {
            return false;
        }
        int row = ((int) mouseY - absoluteY - 1) / SUGGEST_ROW_H;
        if (row < 0 || row >= rows) {
            return false;
        }
        String value = suggestions.get(row);
        tokenBox.setValue(value);
        tokenBox.setCursorPosition(value.length());
        typing = false;
        highlighted = -1;
        return true;
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

    /** Adds a picked suggestion straight to the filter, keeping the typed text and the list open. */
    private void addSuggestion(String value) {
        ContainerInfo target = editorTarget();
        if (target == null) {
            return;
        }
        List<String> tokens = new ArrayList<>(target.tokens());
        if (!tokens.contains(value)) {
            tokens.add(value);
        }
        sessionTokens.add(value);
        setTokens(tokens);
        lastTokens = "";
        typing = true;
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
        sessionTokens.add(value);
        tokenBox.setValue("");
        lastQuery = "";
        lastTokens = "";
        setTokens(tokens);
    }

    private void setTokens(List<String> tokens) {
        ContainerInfo target = editorTarget();
        if (target == null) {
            return;
        }
        sessionTokens.retainAll(tokens);
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
        tooltipEntry = null;
        graphics.drawString(this.font, this.title, 8, 8, GREEN, false);
        if (menu.isTerminal()) {
            String status = menu.isBound()
                    ? "Connected to Storage Terminal (Tier " + menu.getTier() + ")"
                    : "Not connected";
            graphics.drawString(this.font, status, 8, 21, menu.isBound() ? DIM : WARN, false);
        }
        if (menu.showList()) {
            renderList(graphics, mouseX, mouseY);
        }
        renderEditor(graphics, mouseX, mouseY);
        // Drawn last in the container screen's GUI pass (after the widget loop, depth test off), so
        // the autofill list always sits above the widgets below the field.
        if (suggestionsVisible()) {
            renderSuggestions(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (tooltipEntry != null) {
            graphics.renderTooltip(this.font, Component.literal(tooltipEntry), mouseX, mouseY);
            tooltipEntry = null;
        } else {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = LIST_X;
        graphics.drawString(this.font, "Containers", x, HEADER_Y, DIM, false);
        if (menu.isTerminal() && !menu.isBound()) {
            graphics.drawString(this.font, "No network to connect to.", x, LIST_Y, WARN, false);
            graphics.drawString(this.font, "Please install a Storage Terminal.", x, LIST_Y + ROW_H, WARN, false);
            return;
        }
        List<ContainerInfo> list = menu.getContainers();
        if (list.isEmpty()) {
            graphics.drawString(this.font, "(none found)", x, LIST_Y, DIM, false);
            return;
        }
        listScroll = clamp(listScroll, 0, maxListScroll());
        int rows = Math.min(list.size() - listScroll, LIST_LIMIT);
        for (int i = 0; i < rows; i++) {
            int index = listScroll + i;
            ContainerInfo info = list.get(index);
            String name = info.label();
            if (name.length() > 19) {
                name = name.substring(0, 19);
            }
            String row = (index == selected ? "> " : "  ") + name
                    + (info.tokens().isEmpty() ? "" : "  [" + info.tokens().size() + "]");
            graphics.drawString(this.font, row, x, LIST_Y + i * ROW_H, index == selected ? GREEN : TEXT, false);
        }
        drawScrollbar(graphics, x + LIST_WIDTH + 4, LIST_Y, LIST_LIMIT * ROW_H - 2,
                LIST_LIMIT, list.size(), listScroll);
    }

    private void renderEditor(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = EDIT_X;
        graphics.drawString(this.font, "Filter", x, HEADER_Y, DIM, false);
        ContainerInfo target = editorTarget();
        if (target == null) {
            return;
        }
        String name = target.label();
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        graphics.drawString(this.font, name, x, NAME_Y, TEXT, false);

        List<String> tokens = target.tokens();
        tokenScroll = clamp(tokenScroll, 0, maxTokenScroll());
        int rows = Math.min(tokens.size() - tokenScroll, TOKEN_LIMIT);
        int absoluteX = this.leftPos + x;
        int absoluteY = this.topPos + TOKEN_Y;
        for (int i = 0; i < rows; i++) {
            int rowY = TOKEN_Y + i * ROW_H;
            String token = tokens.get(tokenScroll + i);
            graphics.drawString(this.font, "- " + label(token), x, rowY, GREEN, false);
        }
        drawScrollbar(graphics, x + TOKEN_WIDTH + 6, TOKEN_Y, TOKEN_LIMIT * ROW_H - 2,
                TOKEN_LIMIT, tokens.size(), tokenScroll);
        graphics.drawString(this.font, "click to remove", x, 198, DIM, false);
        if (mouseX >= absoluteX && mouseX <= absoluteX + TOKEN_WIDTH
                && mouseY >= absoluteY && mouseY <= absoluteY + rows * ROW_H) {
            int row = ((int) mouseY - absoluteY) / ROW_H;
            int index = tokenScroll + row;
            if (row >= 0 && row < rows && index < tokens.size()) {
                tooltipEntry = tokens.get(index);
            }
        }
    }

    private void drawScrollbar(GuiGraphics graphics, int x, int top, int height, int visible, int total, int scroll) {
        if (total <= visible || height <= 0) {
            return;
        }
        graphics.fill(x, top, x + 2, top + height, SCROLL_TRACK);
        int thumb = Math.max(6, height * visible / total);
        int travel = height - thumb;
        int maxScroll = Math.max(1, total - visible);
        int offset = travel * scroll / maxScroll;
        graphics.fill(x, top + offset, x + 2, top + offset + thumb, BORDER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickSuggestion(mouseX, mouseY)) {
            return true;
        }
        if (menu.showList()) {
            int listX = this.leftPos + LIST_X;
            int listY = this.topPos + LIST_Y;
            int rows = Math.min(menu.getContainers().size() - listScroll, LIST_LIMIT);
            if (mouseX >= listX && mouseX <= listX + LIST_WIDTH) {
                int row = (int) ((mouseY - listY) / ROW_H);
                int index = listScroll + row;
                if (row >= 0 && row < rows && index < menu.getContainers().size()) {
                    selected = index;
                    sessionTokens.clear();
                    lastTokens = "";
                    return true;
                }
            }
        }
        ContainerInfo target = editorTarget();
        if (target != null) {
            int ex = this.leftPos + EDIT_X;
            int ey = this.topPos + TOKEN_Y;
            List<String> tokens = target.tokens();
            if (mouseX >= ex && mouseX <= ex + TOKEN_WIDTH) {
                int row = (int) ((mouseY - ey) / ROW_H);
                int index = tokenScroll + row;
                if (row >= 0 && row < TOKEN_LIMIT && index < tokens.size()) {
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
