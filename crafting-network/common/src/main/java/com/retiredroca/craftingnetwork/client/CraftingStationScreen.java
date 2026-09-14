package com.retiredroca.craftingnetwork.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.CraftingStationMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraftingStationScreen extends AbstractContainerScreen<CraftingStationMenu> implements RecipeUpdateListener {
    private static final ResourceLocation BG = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/crafting_table.png");
    private static final ResourceLocation RECIPE_BOOK_TEX = ResourceLocation.withDefaultNamespace(
            "textures/gui/recipe_book.png");
    private static final WidgetSprites TAB_SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("recipe_book/tab"),
            ResourceLocation.withDefaultNamespace("recipe_book/tab_selected"));
    private static final int PANEL_COLS = 9;
    private static final int PANEL_ROWS = 7;
    private static final int PANEL_SLOT = 18;
    private static final int PANEL_PAD = 4;
    private static final int PANEL_HEADER = 14;
    private static final int PANEL_WIDTH = PANEL_COLS * PANEL_SLOT + PANEL_PAD * 2;
    private static final int PANEL_HEIGHT = PANEL_HEADER + PANEL_ROWS * PANEL_SLOT + PANEL_PAD * 2;
    private static final int RECIPE_BOOK_SHIFT = 77;
    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int TAB_W = 35;
    private static final int TAB_H = 27;
    private static final int TAB_STEP = 27;
    private static final int TAB_X_OFF = 30;
    private static final int SEARCH_X = 25;
    private static final int SEARCH_Y = 13;
    private static final int SEARCH_W = 81;
    private static final int SOURCE_ARROW_W = 10;
    private static final int DROP_ROWS = 7;
    private static final int DROP_ROW_H = 18;
    private static final int SOURCE_ROW_H = 12;
    private static final int SOURCE_HEADER_H = 14;
    private static final int SOURCE_PANEL_X = SEARCH_X;
    private static final int SOURCE_PANEL_W = 120;
    private static final int SOURCE_PANEL_TOP = SEARCH_Y + 16;
    private static final int SOURCE_MAX_ROWS = 7;

    private static final record SourceRow(int id, int depth, String label, boolean hasChildren) {}

    private final RecipeBookComponent recipeBookComponent = new RecipeBookComponent();
    private Button storageToggle;
    private Button recipeBookToggle;
    private CheckboxWidget shulkerToggle;
    private EditBox searchBox;
    private boolean widthTooNarrow;
    private boolean storagePanelVisible;
    private long lastCatalogSig = -1;
    private List<Component> panelTooltip;

    private boolean bookSourcesOpen = false;
    private int sourceScroll = 0;
    private final Set<Integer> collapsedParents = new HashSet<>();

    public CraftingStationScreen(CraftingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
        this.recipeBookToggle = new ImageButton(0, 0, 20, 18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
                    this.searchBox.setFocused(false);
                    this.recipeBookComponent.toggleVisibility();
                    if (!this.recipeBookComponent.isVisible()) {
                        this.bookSourcesOpen = false;
                    }
                    recomputeLeftPos();
                });
        this.storageToggle = Button.builder(Component.literal("Storage"),
                button -> {
                    this.storagePanelVisible = !this.storagePanelVisible;
                    recomputeLeftPos();
                }).bounds(0, 0, 44, 14).build();
        this.shulkerToggle = new CheckboxWidget(0, 0, this.menu.isShulkersFirst(),
                Component.translatable("gui.crafting_network.shulkers_first.tooltip"),
                value -> sendShulkerToggle());
        this.searchBox = new EditBox(this.font, 0, 0, SEARCH_W, 14, Component.translatable("gui.recipebook.search_hint"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setHint(Component.translatable("gui.recipebook.search_hint"));
        this.addRenderableWidget(this.recipeBookToggle);
        this.addRenderableWidget(this.shulkerToggle);
        this.addWidget(this.storageToggle);
        this.addWidget(this.recipeBookComponent);
        this.addWidget(this.searchBox);
        recomputeLeftPos();
        layoutHud();
        this.titleLabelX = 29;
    }

    private void selectRow(int sourceId) {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(this.menu.containerId,
                            sourceId));
        }
        this.menu.selectSource(sourceId);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.getInventory().setChanged();
        }
    }

    private void sendShulkerToggle() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                            this.menu.containerId, 1000));
        }
    }

    private void recomputeLeftPos() {
        int bookShift = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        boolean bookOpen = this.recipeBookComponent.isVisible();
        this.leftPos = Math.max(4, bookShift + (!bookOpen && !this.widthTooNarrow ? RECIPE_BOOK_SHIFT : 0));
        if (this.storagePanelVisible) {
            int rightEdge = this.leftPos + this.imageWidth + 8 + PANEL_WIDTH;
            if (rightEdge > this.width - 4) {
                this.leftPos = Math.max(4, this.width - 4 - (this.imageWidth + 8 + PANEL_WIDTH));
            }
        }
        layoutHud();
    }

    private void layoutHud() {
        if (this.recipeBookToggle != null) {
            this.recipeBookToggle.setPosition(this.leftPos + 5, this.height / 2 - 49);
        }
        if (this.storageToggle != null) {
            this.storageToggle.setPosition(this.leftPos + this.imageWidth - 52, this.topPos - 22);
        }
        if (this.shulkerToggle != null) {
            // Right of the output slot, centered over the last inventory column and the output slot.
            this.shulkerToggle.setPosition(this.leftPos + 156, this.topPos + 39);
        }
        if (this.searchBox != null) {
            int panelX = this.leftPos - 10 - PANEL_W;
            if (panelX >= 4) {
                this.searchBox.setPosition(panelX + SEARCH_X, this.topPos + SEARCH_Y);
            } else {
                this.searchBox.setPosition(-1000, -1000);
            }
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        layoutHud();
        if (this.shulkerToggle != null) {
            this.shulkerToggle.setSelected(this.menu.isShulkersFirst());
        }
        if (!this.recipeBookComponent.isVisible()) {
            this.bookSourcesOpen = false;
        }
        if (this.recipeBookComponent.isVisible() && this.minecraft != null && this.minecraft.player != null) {
            long sig = catalogSignature();
            if (sig != this.lastCatalogSig) {
                this.lastCatalogSig = sig;
                this.minecraft.player.getInventory().setChanged();
            }
        }
        this.recipeBookComponent.tick();
    }

    private long catalogSignature() {
        long hash = 0x7f4a7c15L;
        for (int i = 0; i < 63; i++) {
            ItemStack stack = this.menu.getCatalogItem(i);
            hash = hash * 31L + (stack.isEmpty() ? 0
                    : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem()));
            hash = hash * 31L + stack.getCount();
        }
        return hash;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.panelTooltip = null;
        if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
            this.recipeBookComponent.renderGhostRecipe(guiGraphics, this.leftPos, this.topPos, true, partialTick);
        }
        if (this.storagePanelVisible) {
            renderStoragePanel(guiGraphics, mouseX, mouseY);
        }
        if (this.recipeBookComponent.isVisible()) {
            if (this.bookSourcesOpen) {
                renderSourcePanel(guiGraphics, mouseX, mouseY);
            }
            renderSourcesArrow(guiGraphics);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.recipeBookComponent.renderTooltip(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
        if (this.panelTooltip != null) {
            guiGraphics.renderComponentTooltip(this.font, this.panelTooltip, mouseX, mouseY);
        }
    }

    private int machineTabPanelX() {
        return (this.width - PANEL_W) / 2 - 86;
    }

    private int machineTabPanelY() {
        return (this.height - this.imageHeight) / 2;
    }

    private int sourcesArrowX() {
        int px = machineTabPanelX();
        return px + SEARCH_X + SEARCH_W - SOURCE_ARROW_W + 1;
    }

    private int sourcesArrowY() {
        return machineTabPanelY() + SEARCH_Y + 2;
    }

    private boolean sourcesArrowAt(double mouseX, double mouseY) {
        int ax = sourcesArrowX();
        int ay = sourcesArrowY();
        return mouseX >= ax && mouseX < ax + SOURCE_ARROW_W
                && mouseY >= ay && mouseY < ay + 10;
    }

    private void renderSourcesArrow(GuiGraphics guiGraphics) {
        int ax = sourcesArrowX();
        int ay = sourcesArrowY();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400.0F);
        guiGraphics.fill(ax - 1, ay, ax, ay + 10, 0xFF666666);
        int cx = ax + SOURCE_ARROW_W / 2;
        int cy = ay + 5;
        if (this.bookSourcesOpen) {
            for (int i = 0; i < 3; i++) {
                guiGraphics.fill(cx - 2 - i, cy - i - 1, cx - i, cy - i, 0xFFDDDDDD);
                guiGraphics.fill(cx + i, cy - i - 1, cx + 2 + i, cy - i, 0xFFDDDDDD);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                guiGraphics.fill(cx - 2 - i, cy + i, cx - i, cy + i + 1, 0xFFDDDDDD);
                guiGraphics.fill(cx + i, cy + i, cx + 2 + i, cy + i + 1, 0xFFDDDDDD);
            }
        }
        guiGraphics.pose().popPose();
    }

    private void renderSourcePanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<SourceRow> rows = buildSourceRows();
        int total = rows.size();
        int rowsVisible = Math.min(SOURCE_MAX_ROWS, total);
        int panelH = SOURCE_HEADER_H + rowsVisible * SOURCE_ROW_H;
        int bx = machineTabPanelX();
        int by = machineTabPanelY();
        int px = bx + SOURCE_PANEL_X;
        int py = by + SOURCE_PANEL_TOP;
        int maxScroll = Math.max(0, total - rowsVisible);
        if (this.sourceScroll > maxScroll) {
            this.sourceScroll = maxScroll;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500.0F);
        guiGraphics.fill(px, py, px + SOURCE_PANEL_W, py + panelH, 0xEe101010);
        guiGraphics.fill(px, py, px + SOURCE_PANEL_W, py + 1, 0xFF555555);
        guiGraphics.fill(px, py, px + 1, py + panelH, 0xFF555555);
        guiGraphics.fill(px + SOURCE_PANEL_W - 1, py, px + SOURCE_PANEL_W, py + panelH, 0xFF555555);
        guiGraphics.fill(px, py + panelH - 1, px + SOURCE_PANEL_W, py + panelH, 0xFF555555);
        guiGraphics.drawString(this.font, Component.translatable("gui.crafting_network.sources").getString(),
                px + 4, py + 3, 0xFFFFFFFF, false);
        int sel = this.menu.getSelectedSource();
        for (int v = 0; v < rowsVisible; v++) {
            int idx = this.sourceScroll + v;
            if (idx >= total) {
                break;
            }
            SourceRow sr = rows.get(idx);
            int ry = py + SOURCE_HEADER_H + v * SOURCE_ROW_H;
            boolean hovered = mouseX >= px && mouseX < px + SOURCE_PANEL_W && mouseY >= ry && mouseY < ry + SOURCE_ROW_H;
            boolean selected = sel == sr.id();
            guiGraphics.fill(px + 1, ry, px + SOURCE_PANEL_W - 1, ry + SOURCE_ROW_H,
                    hovered ? 0xFF333344 : (selected ? 0xFF282838 : 0xFF181821));
            int cxx = px + SOURCE_PANEL_W - 12;
            int ccy = ry + SOURCE_ROW_H / 2;
            if (sr.hasChildren()) {
                boolean expanded = !this.collapsedParents.contains(sr.id());
                if (expanded) {
                    for (int i = 0; i < 3; i++) {
                        guiGraphics.fill(cxx - 3 + i, ccy - 1 + i, cxx + 4 - i, ccy + i, 0xFFDDDDDD);
                    }
                } else {
                    for (int i = 0; i < 3; i++) {
                        guiGraphics.fill(cxx - 1 + i, ccy - 2 + i, cxx + 1 + i, ccy + 3 - i, 0xFFDDDDDD);
                    }
                }
            }
            guiGraphics.drawString(this.font, truncate(sr.label(), SOURCE_PANEL_W - 26 - sr.depth() * 8),
                    px + 8 + sr.depth() * 8, ry + 2, sr.depth() == 0 ? 0xE0E0E0 : 0xB0B0B0, false);
        }
        guiGraphics.pose().popPose();
    }

    private List<SourceRow> buildSourceRows() {
        List<SourceRow> rows = new ArrayList<>();
        rows.add(new SourceRow(0, 0, Component.translatable("gui.crafting_network.chest_all").getString(), false));
        int id = 2;
        for (CraftingSourceInfo info : this.menu.getSources()) {
            int parentId = id++;
            boolean hasChildren = !info.children().isEmpty();
            boolean expanded = hasChildren && !this.collapsedParents.contains(parentId);
            rows.add(new SourceRow(parentId, 0, info.label(), hasChildren));
            for (CraftingSourceInfo child : info.children()) {
                int childId = id++;
                if (expanded) {
                    rows.add(new SourceRow(childId, 1, child.label(), false));
                }
            }
        }
        return rows;
    }

    private void toggleCollapse(int id) {
        if (!this.collapsedParents.remove(id)) {
            this.collapsedParents.add(id);
        }
    }

    private int sourcePanelRowAt(int mouseX, int mouseY) {
        List<SourceRow> rows = buildSourceRows();
        int total = rows.size();
        int rowsVisible = Math.min(SOURCE_MAX_ROWS, total);
        int panelH = SOURCE_HEADER_H + rowsVisible * SOURCE_ROW_H;
        int px = machineTabPanelX() + SOURCE_PANEL_X;
        int py = machineTabPanelY() + SOURCE_PANEL_TOP;
        if (mouseX < px || mouseX >= px + SOURCE_PANEL_W || mouseY < py + SOURCE_HEADER_H || mouseY >= py + panelH) {
            return -1;
        }
        int rel = (mouseY - (py + SOURCE_HEADER_H)) / SOURCE_ROW_H + this.sourceScroll;
        return rel < total ? rel : -1;
    }

    private void renderStoragePanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + this.imageWidth + 8;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xCC101010);
        guiGraphics.fill(x, y, x + PANEL_WIDTH, y + 1, 0xFF555555);
        guiGraphics.fill(x, y, x + 1, y + PANEL_HEIGHT, 0xFF555555);
        guiGraphics.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF555555);
        guiGraphics.fill(x, y + PANEL_HEIGHT - 1, x + PANEL_WIDTH, y + PANEL_HEIGHT, 0xFF555555);
        String label = this.menu.getSourceLabels().get(this.menu.getSelectedSource());
        guiGraphics.drawString(this.font, "Storage: " + this.font.plainSubstrByWidth(label, PANEL_WIDTH - 12), x + 4,
                y + 2, 0xFFFFFFFF);
        for (int i = 0; i < 63; i++) {
            int cx = x + PANEL_PAD + (i % PANEL_COLS) * PANEL_SLOT;
            int cy = y + PANEL_HEADER + (i / PANEL_COLS) * PANEL_SLOT;
            guiGraphics.fill(cx, cy, cx + 16, cy + 16, 0x50333333);
            ItemStack stack = this.menu.getCatalogItem(i);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, cx, cy);
                guiGraphics.renderItemDecorations(this.font, stack, cx, cy);
                if (mouseX >= cx && mouseX < cx + 16 && mouseY >= cy && mouseY < cy + 16) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(stack.getHoverName().copy().append(" x" + stack.getCount()));
                    lines.add(Component.literal("Source: " + label));
                    this.panelTooltip = lines;
                }
            }
        }
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) > maxWidth) {
            return font.plainSubstrByWidth(text, maxWidth) + "...";
        }
        return text;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        } else if (this.recipeBookComponent.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        } else if (this.recipeBookComponent.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return (!this.widthTooNarrow || !this.recipeBookComponent.isVisible())
                && super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.recipeBookComponent.isVisible()) {
            if (sourcesArrowAt(mouseX, mouseY)) {
                this.searchBox.setFocused(false);
                this.bookSourcesOpen = !this.bookSourcesOpen;
                this.sourceScroll = 0;
                return true;
            }
            if (this.bookSourcesOpen) {
                int row = sourcePanelRowAt((int) mouseX, (int) mouseY);
                if (row >= 0) {
                    SourceRow sr = buildSourceRows().get(row);
                    int px = machineTabPanelX() + SOURCE_PANEL_X;
                    if (sr.hasChildren() && mouseX >= px + SOURCE_PANEL_W - 14) {
                        toggleCollapse(sr.id());
                    } else {
                        selectRow(sr.id());
                    }
                    return true;
                }
            }
        }
        if (button == 0) {
            int px = machineTabPanelX();
            if (mouseX >= px + SEARCH_X && mouseX < px + SEARCH_X + SEARCH_W
                    && mouseY >= machineTabPanelY() + SEARCH_Y && mouseY < machineTabPanelY() + SEARCH_Y + 14) {
                this.searchBox.setFocused(true);
                return true;
            }
            this.searchBox.setFocused(false);
        }
        if (this.recipeBookComponent.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.recipeBookComponent);
            return true;
        }
        if (this.widthTooNarrow && this.recipeBookComponent.isVisible()) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasShiftDown() || hasControlDown()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (this.bookSourcesOpen && this.recipeBookComponent.isVisible()) {
            List<SourceRow> rows = buildSourceRows();
            int total = rows.size();
            int rowsVisible = Math.min(SOURCE_MAX_ROWS, total);
            int panelH = SOURCE_HEADER_H + rowsVisible * SOURCE_ROW_H;
            int px = machineTabPanelX() + SOURCE_PANEL_X;
            int py = machineTabPanelY() + SOURCE_PANEL_TOP;
            if (mouseX >= px && mouseX < px + SOURCE_PANEL_W && mouseY >= py && mouseY < py + panelH) {
                int maxScroll = Math.max(0, total - rowsVisible);
                this.sourceScroll = Math.max(0, Math.min(maxScroll, this.sourceScroll - (scrollY > 0 ? 1 : -1)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        boolean inside = mouseX < (double) guiLeft || mouseY < (double) guiTop
                || mouseX >= (double) (guiLeft + this.imageWidth) || mouseY >= (double) (guiTop + this.imageHeight);
        if (this.storagePanelVisible) {
            int px = guiLeft + this.imageWidth;
            boolean inPanel = mouseX >= px && mouseX < px + PANEL_WIDTH + 16
                    && mouseY >= (double) guiTop && mouseY < (double) (guiTop + PANEL_HEIGHT);
            inside = inside && !inPanel;
        }
        int mx = machineTabPanelX() - TAB_X_OFF;
        int my = machineTabPanelY();
        boolean inPanel = mouseX >= mx && mouseX < mx + TAB_X_OFF + PANEL_W && mouseY >= (double) my
                && mouseY < (double) (my + PANEL_H);
        inside = inside && !inPanel;
        return this.recipeBookComponent.hasClickedOutside(mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth,
                this.imageHeight, mouseButton) && inside;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        super.slotClicked(slot, slotId, mouseButton, clickType);
        this.recipeBookComponent.slotClicked(slot);
    }

    @Override
    public void recipesUpdated() {
        this.recipeBookComponent.recipesUpdated();
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return this.recipeBookComponent;
    }
}