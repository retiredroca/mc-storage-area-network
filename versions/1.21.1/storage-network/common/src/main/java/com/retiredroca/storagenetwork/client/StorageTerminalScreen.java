package com.retiredroca.storagenetwork.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.retiredroca.storagenetwork.menu.StorageTerminalMenu;
import com.retiredroca.storagenetwork.StorageNetworkCommon;
import com.retiredroca.storagenetwork.network.TerminalPackets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class StorageTerminalScreen extends AbstractContainerScreen<StorageTerminalMenu> {
    private static final ResourceLocation BG = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/gui/container/generic_54.png");
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 6;
    private static final int SLOT = 18;
    private static final int GRID_TOP = 18;
    private static final int SEARCH_BOX_W = 140;
    private static final int SEARCH_BOX_H = 12;
    private static final int CHEST_DROP_W = 120;
    private static final int CHEST_DROP_H = 14;
    private static final int CHEST_ROW_H = 18;
    private static final int CHEST_MAX_ROWS = 10;

    private List<ItemSorter.VirtualItem> displayItems = List.of();
    private int scrollOffset = 0;
    private int lastDataVersion = -1;
    private EditBox searchBox;
    private int searchBoxX;
    private int searchBoxY;
    private boolean draggingBar = false;
    private int dragDX = 0;
    private int dragDY = 0;

    private int chestX;
    private int chestY;
    private boolean chestOpen = false;
    private int chestScroll = 0;
    private int selectedChest = -1;
    private int selectedChild = -1;
    private final Set<Integer> collapsedChests = new HashSet<>();
    private List<SourceRow> cachedRows;
    private int cachedRowsDataVersion = -1;
    private int cachedRowsCollapseVersion = -1;
    private int collapseVersion = 0;
    private boolean draggingChest = false;
    private int dragCX = 0;
    private int dragCY = 0;

    public StorageTerminalScreen(StorageTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 128;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBoxX = TerminalUiState.hasSearch()
                ? Math.max(0, Math.min(this.width - SEARCH_BOX_W, TerminalUiState.getSearchX()))
                : Math.max(0, Math.min(this.width - SEARCH_BOX_W,
                        this.leftPos + (this.imageWidth - SEARCH_BOX_W) / 2));
        this.searchBoxY = TerminalUiState.hasSearch()
                ? Math.max(0, Math.min(this.height - SEARCH_BOX_H, TerminalUiState.getSearchY()))
                : Math.max(0, Math.min(this.height - SEARCH_BOX_H, this.topPos + this.imageHeight + 8));
        this.searchBox = new EditBox(this.font, searchBoxX, searchBoxY, SEARCH_BOX_W, SEARCH_BOX_H,
                Component.translatable("gui.storage_network.search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(s -> rebuild());
        this.addWidget(this.searchBox);
        // Chest selector floats to the right of the inventory screen, detached like the search field.
        this.chestX = TerminalUiState.hasChest()
                ? Math.max(0, Math.min(this.width - CHEST_DROP_W, TerminalUiState.getChestX()))
                : Math.max(0, Math.min(this.width - CHEST_DROP_W, this.leftPos + this.imageWidth + 8));
        this.chestY = TerminalUiState.hasChest()
                ? Math.max(0, Math.min(this.height - CHEST_DROP_H, TerminalUiState.getChestY()))
                : Math.max(0, Math.min(this.height - CHEST_DROP_H, this.topPos));
        rebuild();
    }

    @Override
    public void onClose() {
        TerminalUiState.setSearchX(searchBoxX);
        TerminalUiState.setSearchY(searchBoxY);
        TerminalUiState.setChestX(chestX);
        TerminalUiState.setChestY(chestY);
        TerminalUiState.save();
        super.onClose();
    }

    private void rebuild() {
        if (this.minecraft != null) {
            List<ItemStack> items = menu.getServerItems();
            List<Integer> counts = menu.getServerCounts();
            List<TerminalPackets.ChestSync> chests = menu.getServerChests();
            if (selectedChest >= 0 && selectedChest < chests.size()) {
                List<TerminalPackets.ChestSync> children = chests.get(selectedChest).children();
                if (selectedChild >= 0 && selectedChild < children.size()) {
                    items = children.get(selectedChild).items();
                    counts = children.get(selectedChild).counts();
                } else {
                    items = chests.get(selectedChest).items();
                    counts = chests.get(selectedChest).counts();
                }
            }
            this.displayItems = ItemSorter.filter(ItemSorter.build(items, counts),
                    searchBox == null ? "" : searchBox.getValue());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.getDataVersion() != lastDataVersion) {
            lastDataVersion = menu.getDataVersion();
            rebuild();
        }
    }

    private static class SourceRow {
        final int chestIndex;
        final int childIndex;

        SourceRow(int chestIndex, int childIndex) {
            this.chestIndex = chestIndex;
            this.childIndex = childIndex;
        }
    }

    private List<SourceRow> visibleRows() {
        if (cachedRows != null && cachedRowsDataVersion == menu.getDataVersion()
                && cachedRowsCollapseVersion == collapseVersion) {
            return cachedRows;
        }
        List<SourceRow> rows = new ArrayList<>();
        rows.add(new SourceRow(-1, -1));
        List<TerminalPackets.ChestSync> chests = menu.getServerChests();
        for (int i = 0; i < chests.size(); i++) {
            rows.add(new SourceRow(i, -1));
            if (!collapsedChests.contains(i)) {
                for (int c = 0; c < chests.get(i).children().size(); c++) {
                    rows.add(new SourceRow(i, c));
                }
            }
        }
        cachedRows = rows;
        cachedRowsDataVersion = menu.getDataVersion();
        cachedRowsCollapseVersion = collapseVersion;
        return rows;
    }

    private int chestRowCount() {
        return visibleRows().size();
    }

    private int chestVisibleRows() {
        return Math.min(CHEST_MAX_ROWS, chestRowCount());
    }

    private int chestListWidth() {
        int max = CHEST_DROP_W;
        List<TerminalPackets.ChestSync> chests = menu.getServerChests();
        for (SourceRow row : visibleRows()) {
            if (row.chestIndex < 0) {
                String name = Component.translatable("gui.storage_network.chest_all").getString();
                max = Math.max(max, font.width(name));
                String sub = Component.translatable("gui.storage_network.nested_items",
                        menu.getServerItems().size()).getString();
                max = Math.max(max, font.width(sub));
            } else if (row.childIndex < 0) {
                TerminalPackets.ChestSync chest = chests.get(row.chestIndex);
                max = Math.max(max, font.width(chest.name()) + (chest.children().isEmpty() ? 0 : 14));
                String sub = Component.translatable("gui.storage_network.nested_items",
                        chest.items().size()).getString();
                max = Math.max(max, font.width(sub) + (chest.children().isEmpty() ? 0 : 14));
            } else {
                TerminalPackets.ChestSync child = chests.get(row.chestIndex).children().get(row.childIndex);
                max = Math.max(max, font.width(child.name()) + 14);
                max = Math.max(max, font.width(Component.translatable("gui.storage_network.nested_items",
                        child.items().size()).getString()) + 14);
            }
        }
        max += 8;
        return Math.min(max, Math.max(CHEST_DROP_W, this.width - chestX - 4));
    }

    private int rowAt(double mouseX, double mouseY) {
        int width = chestOpen ? chestListWidth() : CHEST_DROP_W;
        if (mouseX < chestX || mouseX > chestX + width || mouseY < chestY + CHEST_DROP_H + 2) {
            return -1;
        }
        int row = (int) ((mouseY - (chestY + CHEST_DROP_H + 2)) / CHEST_ROW_H);
        if (row >= chestVisibleRows() || row + chestScroll >= chestRowCount()) {
            return -1;
        }
        return row;
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) > maxWidth) {
            return font.plainSubstrByWidth(text, maxWidth) + "...";
        }
        return text;
    }

    private String headerText() {
        List<TerminalPackets.ChestSync> chests = menu.getServerChests();
        String text;
        if (selectedChest >= 0 && selectedChest < chests.size()) {
            List<TerminalPackets.ChestSync> children = chests.get(selectedChest).children();
            if (selectedChild >= 0 && selectedChild < children.size()) {
                text = children.get(selectedChild).name();
            } else {
                text = chests.get(selectedChest).name();
            }
        } else {
            text = Component.translatable("gui.storage_network.chest_all").getString();
        }
        return truncate(text, CHEST_DROP_W - 24);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasShiftDown() || hasControlDown()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (chestOpen && mouseX >= chestX && mouseX <= chestX + chestListWidth()
                && mouseY >= chestY + CHEST_DROP_H + 2) {
            int maxScroll = Math.max(0, chestRowCount() - CHEST_MAX_ROWS);
            chestScroll = Math.max(0, Math.min(maxScroll, chestScroll - (scrollY > 0 ? 1 : -1)));
            return true;
        }
        int maxOffset = Math.max(0, (int) Math.ceil(displayItems.size() / (double) (GRID_COLS * GRID_ROWS)) - 1);
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset - (scrollY > 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int row = rowAt(mouseX, mouseY);
            boolean inHeader = mouseX >= chestX && mouseX <= chestX + CHEST_DROP_W
                    && mouseY > chestY + 1 && mouseY <= chestY + CHEST_DROP_H;
            boolean inDragStrip = mouseX >= chestX - 5 && mouseX <= chestX + CHEST_DROP_W + 5
                    && (mouseY >= chestY - 6 && mouseY <= chestY
                    || mouseY >= chestY + 1 && mouseY <= chestY + CHEST_DROP_H
                    && (mouseX < chestX + 3 || mouseX > chestX + CHEST_DROP_W - 3));
            if (!inHeader && !inDragStrip && row < 0) {
                chestOpen = false;
            }
            if (chestOpen && row >= 0) {
                int absolute = row + chestScroll;
                List<SourceRow> rows = visibleRows();
                if (absolute >= 0 && absolute < rows.size()) {
                    SourceRow chosen = rows.get(absolute);
                    // Crouch-click a container row to toggle its block type in/out of the network.
                    if (hasShiftDown() && chosen.chestIndex >= 0 && chosen.childIndex < 0) {
                        BlockPos containerPos = menu.getServerChests().get(chosen.chestIndex).pos();
                        StorageNetworkCommon.platform().sendToggleExclude(menu.getPos(), containerPos);
                        chestOpen = false;
                        return true;
                    }
                    if (chosen.chestIndex < 0) {
                        selectedChest = -1;
                        selectedChild = -1;
                    } else if (chosen.childIndex < 0) {
                        if (!menu.getServerChests().get(chosen.chestIndex).children().isEmpty()
                                && mouseX >= chestX + chestListWidth() - 14) {
                            if (!collapsedChests.remove(chosen.chestIndex)) {
                                collapsedChests.add(chosen.chestIndex);
                            }
                            collapseVersion++;
                            return true;
                        }
                        selectedChest = chosen.chestIndex;
                        selectedChild = -1;
                    } else {
                        selectedChest = chosen.chestIndex;
                        selectedChild = chosen.childIndex;
                    }
                    chestOpen = false;
                    chestScroll = 0;
                    scrollOffset = 0;
                    rebuild();
                    notifySelection();
                }
                return true;
            }
            if (inDragStrip) {
                draggingChest = true;
                dragCX = (int) mouseX - chestX;
                dragCY = (int) mouseY - chestY;
                return true;
            }
            if (inHeader) {
                chestOpen = !chestOpen;
                chestScroll = 0;
                return true;
            }
        }
        if (searchBox != null && searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0 && mouseX >= searchBoxX - 5 && mouseX <= searchBoxX + SEARCH_BOX_W + 5
                && mouseY >= searchBoxY - 5 && mouseY <= searchBoxY + SEARCH_BOX_H + 5) {
            draggingBar = true;
            dragDX = (int) mouseX - searchBoxX;
            dragDY = (int) mouseY - searchBoxY;
            return true;
        }
        if (button == 0) {
            int index = slotAt(mouseX, mouseY);
            if (index >= 0) {
                boolean fullStack = hasShiftDown();
                ItemSorter.VirtualItem item = displayItems.get(index);
                StorageNetworkCommon.platform().sendExtract(menu.getPos(), item.stack(), fullStack ? 1 : 0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingChest) {
            chestX = (int) mouseX - dragCX;
            chestY = (int) mouseY - dragCY;
            chestX = Math.max(0, Math.min(this.width - CHEST_DROP_W, chestX));
            chestY = Math.max(0, Math.min(this.height - CHEST_DROP_H, chestY));
            return true;
        }
        if (draggingBar && searchBox != null) {
            searchBoxX = (int) mouseX - dragDX;
            searchBoxY = (int) mouseY - dragDY;
            searchBoxX = Math.max(0, Math.min(this.width - SEARCH_BOX_W, searchBoxX));
            searchBoxY = Math.max(0, Math.min(this.height - SEARCH_BOX_H, searchBoxY));
            searchBox.setX(searchBoxX);
            searchBox.setY(searchBoxY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingBar = false;
        draggingChest = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void notifySelection() {
        BlockPos target = BlockPos.ZERO;
        String child = "";
        List<TerminalPackets.ChestSync> chests = menu.getServerChests();
        boolean all = selectedChest < 0 || selectedChest >= chests.size();
        if (!all) {
            target = chests.get(selectedChest).pos();
            if (selectedChild >= 0 && selectedChild < chests.get(selectedChest).children().size()) {
                child = chests.get(selectedChest).children().get(selectedChild).name();
            }
        }
        StorageNetworkCommon.platform().sendSelect(menu.getPos(), all, target, child);
    }

    private int slotAt(double mouseX, double mouseY) {
        int gridLeft = this.leftPos + 8;
        int gridTop = this.topPos + GRID_TOP;
        if (mouseX < gridLeft || mouseY < gridTop) {
            return -1;
        }
        int col = (int) ((mouseX - gridLeft) / SLOT);
        int row = (int) ((mouseY - gridTop) / SLOT);
        if (col < 0 || col >= GRID_COLS || row < 0 || row >= GRID_ROWS) {
            return -1;
        }
        int idx = row * GRID_COLS + col + scrollOffset * GRID_COLS * GRID_ROWS;
        return idx < displayItems.size() ? idx : -1;
    }

    @Override
    public boolean charTyped(char code, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(code, modifiers);
        }
        if (searchBox != null && Character.isLetterOrDigit(code)) {
            searchBox.setFocused(true);
            return searchBox.charTyped(code, modifiers);
        }
        return super.charTyped(code, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchBox.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(BG, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Separately-positioned EMI-style search field (overlay, below the inventory)
        graphics.fill(searchBoxX - 1, searchBoxY - 1, searchBoxX + SEARCH_BOX_W + 1, searchBoxY + SEARCH_BOX_H + 1, 0xFF000000);
        graphics.fill(searchBoxX, searchBoxY, searchBoxX + SEARCH_BOX_W, searchBoxY + SEARCH_BOX_H, 0xFF101010);

        // Highlight which slots are visible if there are more pages
        int total = displayItems.size();
        int capacity = GRID_COLS * GRID_ROWS;
        int maxOffset = Math.max(0, (int) Math.ceil(total / (double) capacity) - 1);
        if (maxOffset > 0) {
            String pageText = Component.translatable("gui.storage_network.page",
                    scrollOffset + 1, maxOffset + 1).getString();
            graphics.drawString(font, pageText, x + this.imageWidth - font.width(pageText) - 8, y + 6, 0x404040, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        String tierText = Component.translatable("gui.storage_network.tier", menu.getServerTier()).getString();
        graphics.drawString(font, tierText, this.titleLabelX + font.width(this.title) + 2, this.titleLabelY, 0x404040, false);

        graphics.drawString(font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private void renderChestSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        // Header
        graphics.fill(chestX - 1, chestY - 1, chestX + CHEST_DROP_W + 1, chestY + CHEST_DROP_H + 1, 0xFF000000);
        graphics.fill(chestX, chestY, chestX + CHEST_DROP_W, chestY + CHEST_DROP_H, 0xFF101010);
        graphics.drawString(font, headerText(), chestX + 3, chestY + 3, 0xE0E0E0, false);
        graphics.drawString(font, "v", chestX + CHEST_DROP_W - 9, chestY + 3, 0xE0E0E0, false);

        if (chestOpen) {
            int rows = chestVisibleRows();
            int listW = chestListWidth();
            int listTop = chestY + CHEST_DROP_H + 2;
            int listH = rows * CHEST_ROW_H;
            graphics.fill(chestX - 1, listTop - 1, chestX + listW + 1, listTop + listH + 1, 0xFF000000);
            int hoveredRow = rowAt(mouseX, mouseY);
            List<SourceRow> allRows = visibleRows();
            List<TerminalPackets.ChestSync> chests = menu.getServerChests();
            for (int v = 0; v < rows; v++) {
                int absolute = v + chestScroll;
                if (absolute >= allRows.size()) {
                    break;
                }
                SourceRow row = allRows.get(absolute);
                int y = listTop + v * CHEST_ROW_H;
                boolean selected = row.chestIndex < 0
                        ? selectedChest == -1
                        : (row.childIndex < 0
                                ? (selectedChest == row.chestIndex && selectedChild == -1)
                                : (selectedChest == row.chestIndex && selectedChild == row.childIndex));
                int bg = hoveredRow == v ? 0xFF202020 : (selected ? 0xFF181818 : 0xFF101010);
                graphics.fill(chestX, y, chestX + listW, y + CHEST_ROW_H, bg);
                int indent = row.chestIndex < 0 || row.childIndex < 0 ? 0 : 8;
                int tx = chestX + 3 + indent;
                if (row.chestIndex < 0) {
                    graphics.drawString(font, Component.translatable("gui.storage_network.chest_all").getString(),
                            tx, y + 2, 0xE0E0E0, false);
                    String sub = Component.translatable("gui.storage_network.nested_items",
                            menu.getServerItems().size()).getString();
                    graphics.drawString(font, truncate(sub, listW - 6 - indent), tx, y + 11, 0x707070, false);
                } else if (row.childIndex < 0) {
                    TerminalPackets.ChestSync chest = chests.get(row.chestIndex);
                    if (!chest.children().isEmpty()) {
                        graphics.drawString(font,
                                collapsedChests.contains(row.chestIndex) ? "v" : "^",
                                chestX + listW - 11, y + 4, 0x909090, false);
                    }
                    graphics.drawString(font, truncate(chest.name(), listW - 6 - 14), tx, y + 2, 0xE0E0E0, false);
                    String sub = Component.translatable("gui.storage_network.nested_items",
                            chest.items().size()).getString();
                    graphics.drawString(font, truncate(sub, listW - 6 - 14), tx, y + 11, 0x909090, false);
                } else {
                    TerminalPackets.ChestSync child = chests.get(row.chestIndex).children().get(row.childIndex);
                    graphics.drawString(font, truncate(child.name(), listW - 6 - indent), tx, y + 2, 0xE0E0E0, false);
                    graphics.drawString(font,
                            truncate(Component.translatable("gui.storage_network.nested_items",
                                    child.items().size()).getString(), listW - 6 - indent),
                            tx, y + 11, 0x808080, false);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.searchBox.render(graphics, mouseX, mouseY, partialTick);

        // Render virtual item grid over the double-chest slot recesses
        int gridLeft = this.leftPos + 8;
        int gridTop = this.topPos + GRID_TOP;
        int base = scrollOffset * GRID_COLS * GRID_ROWS;
        for (int v = 0; v < GRID_ROWS * GRID_COLS; v++) {
            int visIdx = base + v;
            if (visIdx >= displayItems.size()) {
                break;
            }
            int col = v % GRID_COLS;
            int row = v / GRID_COLS;
            int sx = gridLeft + col * SLOT;
            int sy = gridTop + row * SLOT;
            ItemSorter.VirtualItem item = displayItems.get(visIdx);
            graphics.renderItem(item.stack(), sx, sy);
            graphics.renderItemDecorations(this.font, item.stack(), sx, sy, String.valueOf(item.count()));
        }

        // Hover highlight on the terminal grid slot, matching the vanilla player-slot highlight
        int hovered = slotAt(mouseX, mouseY);
        if (hovered >= 0 && hovered < displayItems.size()) {
            int col = hovered % GRID_COLS;
            int row = (hovered / GRID_COLS) % GRID_ROWS;
            int hx = gridLeft + col * SLOT;
            int hy = gridTop + row * SLOT;
            AbstractContainerScreen.renderSlotHighlight(graphics, hx, hy, 0);
        }

        renderChestSelector(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        int index = slotAt(mouseX, mouseY);
        if (index >= 0 && index < displayItems.size()) {
            ItemSorter.VirtualItem item = displayItems.get(index);
            graphics.renderTooltip(this.font, item.stack(), mouseX, mouseY);
        }
    }
}