package com.retiredroca.craftingnetwork.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.retiredroca.craftingnetwork.CraftingNetworkCommon;
import com.retiredroca.craftingnetwork.menu.CraftingSourceInfo;
import com.retiredroca.craftingnetwork.menu.IStationMenu;
import com.retiredroca.craftingnetwork.station.StationState;
import com.retiredroca.craftingnetwork.station.StationStatus;
import com.retiredroca.craftingnetwork.station.StationType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.BlastingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SmokingRecipeBookComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

public class StationScreen extends AbstractContainerScreen<AbstractContainerMenu> implements RecipeUpdateListener {
    private static final ResourceLocation FURNACE_BG = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/furnace.png");
    private static final ResourceLocation BLAST_FURNACE_BG = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/blast_furnace.png");
    private static final ResourceLocation SMOKER_BG = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/smoker.png");
    private static final ResourceLocation BREWING_STAND_BG = ResourceLocation.withDefaultNamespace(
            "textures/gui/container/brewing_stand.png");
    private static final ResourceLocation RECIPE_BOOK_TEX = ResourceLocation.withDefaultNamespace(
            "textures/gui/recipe_book.png");
    private static final ResourceLocation BREW_SLOT_CRAFTABLE = ResourceLocation.withDefaultNamespace(
            "recipe_book/slot_craftable");
    private static final ResourceLocation BREW_SLOT_UNCRAFTABLE = ResourceLocation.withDefaultNamespace(
            "recipe_book/slot_uncraftable");
    private static final ResourceLocation PAGE_FORWARD = ResourceLocation.withDefaultNamespace(
            "recipe_book/page_forward");
    private static final ResourceLocation PAGE_FORWARD_HL = ResourceLocation.withDefaultNamespace(
            "recipe_book/page_forward_highlighted");
    private static final ResourceLocation PAGE_BACKWARD = ResourceLocation.withDefaultNamespace(
            "recipe_book/page_backward");
    private static final ResourceLocation PAGE_BACKWARD_HL = ResourceLocation.withDefaultNamespace(
            "recipe_book/page_backward_highlighted");
    private static final WidgetSprites BREW_FILTER_SPRITES = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled"),
            ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled"),
            ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
            ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));

    private static final int SOURCES_MAX_ROWS = 7;
    private static final int SOURCE_ROW_H = 12;
    private static final int SOURCE_HEADER_H = 14;
    private static final int SOURCE_PANEL_W = 120;
    private static final int SOURCE_ARROW_W = 10;
    private static final int SEARCH_X = 25;
    private static final int SEARCH_Y = 13;
    private static final int SEARCH_W = 81;
    private static final int SOURCE_PANEL_X = SEARCH_X;
    private static final int SOURCE_PANEL_TOP = SEARCH_Y + 16;
    private static final int PANEL_W = 147;
    private static final int PANEL_H = 166;
    private static final int BREW_COLS = 5;
    private static final int BREW_ROWS = 4;
    private static final int BREW_SLOT = 25;
    private static final int BREW_PER_PAGE = BREW_COLS * BREW_ROWS;

    private static final record SourceRow(int id, int depth, String label, boolean hasChildren) {}

    private RecipeBookComponent recipeBookComponent;
    private Button recipeBookToggle;
    private CheckboxWidget shulkerToggle;
    private CheckboxWidget inventoryToggle;
    private EditBox searchBox;
    private boolean widthTooNarrow;
    private long lastCatalogSig = -1;
    private List<Component> panelTooltip;

    private boolean sourcesOpen = false;
    private int sourcesScroll = 0;
    private final Set<Integer> collapsedParents = new HashSet<>();

    private static boolean brewBookOpen = false;
    private int brewPage = 0;
    private boolean brewFilterCraftable = false;
    private final List<String> brewablePotions = new ArrayList<>();

    public StationScreen(AbstractContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private IStationMenu getStationMenu() {
        return (IStationMenu) this.menu;
    }

    private boolean isBrewing() {
        StationType type = getStationMenu().stationType();
        return type != null && type.isBrewing();
    }

    @Override
    protected void init() {
        super.init();
        this.widthTooNarrow = this.width < 379;
        initRecipeBookComponent();
        rebuildPotionList();
        this.recipeBookToggle = new ImageButton(0, 0, 20, 18,
                RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> toggleBook());
        this.shulkerToggle = new CheckboxWidget(0, 0, shulkersFirst(),
                Component.translatable("gui.crafting_network.shulkers_first.tooltip"),
                value -> sendShulkerToggle());
        this.inventoryToggle = new CheckboxWidget(0, 0, inventoryFirst(),
                Component.translatable("gui.crafting_network.inventory_first.tooltip"),
                value -> sendInventoryToggle());
        this.searchBox = new EditBox(this.font, 0, 0, SEARCH_W, 14,
                Component.translatable("gui.recipebook.search_hint"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setHint(Component.translatable("gui.recipebook.search_hint"));
        if (recipeBookComponent != null || isBrewing()) {
            this.addRenderableWidget(this.recipeBookToggle);
        }
        this.addWidget(this.searchBox);
        if (recipeBookComponent != null) {
            this.addWidget(this.recipeBookComponent);
        }
        this.addRenderableWidget(this.shulkerToggle);
        this.addRenderableWidget(this.inventoryToggle);
        recomputeLeftPos();
    }

    private void sendShulkerToggle() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                            this.menu.containerId, 1000));
        }
    }

    private boolean shulkersFirst() {
        StationState state = getStationMenu().getState();
        return state != null && state.shulkersFirst();
    }

    private void sendInventoryToggle() {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                            this.menu.containerId, 1001));
        }
    }

    private boolean inventoryFirst() {
        StationState state = getStationMenu().getState();
        return state != null && state.inventoryFirst();
    }

    private void toggleBook() {
        if (isBrewing()) {
            this.brewBookOpen = !this.brewBookOpen;
            if (!this.brewBookOpen) {
                this.sourcesOpen = false;
            }
        } else if (recipeBookComponent != null) {
            recipeBookComponent.toggleVisibility();
            if (!recipeBookComponent.isVisible()) {
                this.sourcesOpen = false;
            } else {
                bounceTabs();
            }
        }
        recomputeLeftPos();
    }

    private boolean isBookVisible() {
        if (isBrewing()) {
            return this.brewBookOpen;
        }
        return recipeBookComponent != null && recipeBookComponent.isVisible();
    }

    private void initRecipeBookComponent() {
        this.recipeBookComponent = null;
        StationType type = getStationMenu().stationType();
        if (type == null || type.isBrewing()) {
            return;
        }
        if (!(this.menu instanceof RecipeBookMenu<?, ?> rbm)) {
            return;
        }
        RecipeBookComponent component = switch (type) {
            case BLASTING -> new BlastingRecipeBookComponent();
            case SMOKING -> new SmokingRecipeBookComponent();
            default -> new SmeltingRecipeBookComponent();
        };
        if (this.minecraft != null && this.minecraft.player != null) {
            component.init(this.width, this.height, this.minecraft, this.widthTooNarrow, rbm);
            this.recipeBookComponent = component;
            bounceTabs();
        }
    }

    /** Plays the vanilla recipe-book tab bounce animation (vanilla normally only bounces on new recipes). */
    private void bounceTabs() {
        if (this.recipeBookComponent == null) {
            return;
        }
        CraftingNetworkCommon.platform().bounceRecipeBookTabs(this.recipeBookComponent);
    }

    private void rebuildPotionList() {
        this.brewablePotions.clear();
        java.util.Set<String> seenNames = new java.util.HashSet<>();
        BuiltInRegistries.POTION.holders().forEach(holder -> {
            String id = holder.key().location().toString();
            if (id.equals("minecraft:water")) {
                return; // every path starts from water
            }
            // The long_/strong_ variants share a display name with the base potion, so keep one per name.
            String name = PotionContents.createItemStack(Items.POTION, holder).getHoverName().getString();
            if (seenNames.add(name)) {
                this.brewablePotions.add(id);
                this.brewablePotions.add(id + "|splash");
                this.brewablePotions.add(id + "|linger");
            }
        });
    }

    private void selectSource(int sourceId) {
        if (this.minecraft != null && this.minecraft.getConnection() != null) {
            this.minecraft.getConnection().send(
                    new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(
                            this.menu.containerId, sourceId));
        }
        getStationMenu().selectSource(sourceId);
    }

    private void sendBrewTarget(String potionId) {
        CraftingNetworkCommon.platform().sendBrewTarget(getStationMenu().stationPos(), potionId);
        getStationMenu().applyBrewTarget(potionId);
    }

    private ItemStack potionStack(String id) {
        try {
            Item item = Items.POTION;
            String potionId = id;
            if (id.endsWith("|splash")) {
                item = Items.SPLASH_POTION;
                potionId = id.substring(0, id.length() - "|splash".length());
            } else if (id.endsWith("|linger")) {
                item = Items.LINGERING_POTION;
                potionId = id.substring(0, id.length() - "|linger".length());
            }
            Potion potion = BuiltInRegistries.POTION.get(ResourceLocation.parse(potionId));
            if (potion == null) {
                return ItemStack.EMPTY;
            }
            return PotionContents.createItemStack(item, BuiltInRegistries.POTION.wrapAsHolder(potion));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private void recomputeLeftPos() {
        if (recipeBookComponent == null) {
            if (isBrewing() && this.brewBookOpen && !this.widthTooNarrow) {
                this.leftPos = 177 + (this.width - this.imageWidth - 200) / 2;
            } else {
                this.leftPos = Math.max(4, (this.width - this.imageWidth) / 2);
            }
            layoutHud();
            return;
        }
        int bookShift = recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        boolean bookOpen = recipeBookComponent.isVisible();
        this.leftPos = Math.max(4, bookShift + (!bookOpen && !this.widthTooNarrow ? 77 : 0));
        layoutHud();
    }

    private int bookXOffset() {
        return this.widthTooNarrow ? 0 : 86;
    }

    private int machineTabPanelX() {
        return (this.width - PANEL_W) / 2 - bookXOffset();
    }

    private int machineTabPanelY() {
        return (this.height - this.imageHeight) / 2;
    }

    private void layoutHud() {
        if (this.recipeBookToggle != null) {
            this.recipeBookToggle.setPosition(this.leftPos + 5, this.height / 2 - 49);
        }
        if (this.shulkerToggle != null) {
            // Right of the output, centered over the last inventory column (brewing: over the bottles).
            this.shulkerToggle.setPosition(this.leftPos + 156, this.topPos + (isBrewing() ? 55 : 39));
        }
        if (this.inventoryToggle != null) {
            // Directly below the shulkers checkbox, with a little buffer.
            this.inventoryToggle.setPosition(this.leftPos + 156, this.topPos + (isBrewing() ? 68 : 52));
        }
        if (this.searchBox != null) {
            int panelX = machineTabPanelX();
            if (panelX >= 4 && isBookVisible()) {
                this.searchBox.setPosition(panelX + SEARCH_X, machineTabPanelY() + SEARCH_Y);
            } else {
                this.searchBox.setPosition(-1000, -1000);
            }
        }
    }

    private int searchBoxX() {
        return this.searchBox != null ? this.searchBox.getX() : machineTabPanelX() + SEARCH_X;
    }

    private int searchBoxY() {
        return this.searchBox != null ? this.searchBox.getY() : machineTabPanelY() + SEARCH_Y;
    }

    private int searchBoxW() {
        return this.searchBox != null ? this.searchBox.getWidth() : SEARCH_W;
    }

    private int sourcesArrowX() {
        return machineTabPanelX() + SEARCH_X + SEARCH_W - SOURCE_ARROW_W + 1;
    }

    private int sourcesArrowY() {
        return machineTabPanelY() + SEARCH_Y + 2;
    }

    private boolean sourcesArrowAt(double mouseX, double mouseY) {
        int ax = sourcesArrowX();
        int ay = sourcesArrowY();
        return mouseX >= ax && mouseX < ax + SOURCE_ARROW_W && mouseY >= ay && mouseY < ay + 10;
    }

    private void renderSourcesArrow(GuiGraphics guiGraphics) {
        if (!isBookVisible()) {
            return;
        }
        int ax = sourcesArrowX();
        int ay = sourcesArrowY();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 400.0F);
        guiGraphics.fill(ax - 1, ay, ax, ay + 10, 0xFF666666);
        int cx = ax + SOURCE_ARROW_W / 2;
        int cy = ay + 5;
        if (this.sourcesOpen) {
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

    @Override
    public void containerTick() {
        super.containerTick();
        layoutHud();
        if (this.shulkerToggle != null) {
            this.shulkerToggle.setSelected(shulkersFirst());
        }
        if (this.inventoryToggle != null) {
            this.inventoryToggle.setSelected(inventoryFirst());
        }
        if (!isBookVisible()) {
            this.sourcesOpen = false;
        }
        if (recipeBookComponent != null) {
            if (recipeBookComponent.isVisible() && this.minecraft != null && this.minecraft.player != null) {
                long sig = catalogSignature();
                if (sig != this.lastCatalogSig) {
                    this.lastCatalogSig = sig;
                    this.minecraft.player.getInventory().setChanged();
                }
            }
            try {
                recipeBookComponent.tick();
            } catch (Exception ignored) {
            }
        }
    }

    private long catalogSignature() {
        long hash = 0x7f4a7c15L;
        for (int i = 0; i < 63; i++) {
            ItemStack stack = getStationMenu().getCatalogItem(i);
            hash = hash * 31L + (stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()));
            hash = hash * 31L + stack.getCount();
        }
        return hash;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.panelTooltip = null;
        if (recipeBookComponent != null && recipeBookComponent.isVisible() && this.widthTooNarrow) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            if (recipeBookComponent != null) {
                recipeBookComponent.render(guiGraphics, mouseX, mouseY, partialTick);
                recipeBookComponent.renderGhostRecipe(guiGraphics, this.leftPos, this.topPos, true, partialTick);
            }
        }
        if (isBrewing() && this.brewBookOpen) {
            renderBrewBook(guiGraphics, mouseX, mouseY);
        }
        drawStationStatus(guiGraphics, mouseX, mouseY);
        renderSourcesArrow(guiGraphics);
        if (this.sourcesOpen && isBookVisible()) {
            renderSourcePanel(guiGraphics, mouseX, mouseY);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (recipeBookComponent != null) {
            recipeBookComponent.renderTooltip(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
        }
        if (this.panelTooltip != null) {
            guiGraphics.renderComponentTooltip(this.font, this.panelTooltip, mouseX, mouseY);
        }
    }

    private void drawStationStatus(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        StationState state = getStationMenu().getState();
        if (state == null) {
            return;
        }
        StationType type = getStationMenu().stationType();
        int x = this.leftPos;
        int y = this.topPos;

        if (!isBrewing()) {
            ResourceLocation bg = switch (type) {
                case BLASTING -> BLAST_FURNACE_BG;
                case SMOKING -> SMOKER_BG;
                default -> FURNACE_BG;
            };
            if (state.status() == StationStatus.RUNNING && state.progress() > 0) {
                int arrowProgress = state.progress() * 24 / 100;
                guiGraphics.blit(bg, x + 79, y + 17, 176, 14, arrowProgress, 17);
            }
            if (state.status() == StationStatus.RUNNING) {
                int flameProgress = 14 - (state.progress() * 14 / 100);
                if (flameProgress < 0) {
                    flameProgress = 0;
                }
                guiGraphics.blit(bg, x + 81, y + 36 + flameProgress, 176, flameProgress, 14, 14 - flameProgress);
            }
        } else if (state.progress() > 0) {
            // Small bar below the bottle row so it does not overlap the slots.
            int barW = 60;
            int barX = x + 58;
            int barY = y + 78;
            int fill = barW * state.progress() / 100;
            guiGraphics.fill(barX, barY, barX + barW, barY + 4, 0xFF303030);
            guiGraphics.fill(barX, barY, barX + Math.min(barW, fill), barY + 4, 0xFF70C570);
        }

        this.menu.slots.stream()
                .filter(s -> s.index < 5)
                .forEach(s -> {
                    List<com.retiredroca.craftingnetwork.station.StationSlot> slots = state.slots();
                    if (s.index < slots.size()) {
                        ItemStack icon = slots.get(s.index).item();
                        if (!icon.isEmpty()) {
                            guiGraphics.renderItem(icon, this.leftPos + s.x, this.topPos + s.y);
                            guiGraphics.renderItemDecorations(this.font, icon, this.leftPos + s.x, this.topPos + s.y);
                        }
                    }
                });
    }

    private void renderBrewBook(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int bookX = machineTabPanelX();
        int bookY = machineTabPanelY();
        if (bookX < 4) {
            return;
        }
        guiGraphics.blit(RECIPE_BOOK_TEX, bookX, bookY, 1, 1, PANEL_W, PANEL_H);

        List<String> potions = visiblePotions();
        int total = potions.size();
        int totalPages = Math.max(1, (total + BREW_PER_PAGE - 1) / BREW_PER_PAGE);
        if (this.brewPage >= totalPages) {
            this.brewPage = totalPages - 1;
        }
        if (this.brewPage < 0) {
            this.brewPage = 0;
        }
        StationState state = getStationMenu().getState();
        String target = state == null ? "" : state.target();
        List<String> craftable = state == null ? List.of() : state.craftable();
        int start = this.brewPage * BREW_PER_PAGE;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 100.0F);
        for (int i = 0; i < BREW_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= total) {
                break;
            }
            int bx = bookX + 11 + BREW_SLOT * (i % BREW_COLS);
            int by = bookY + 31 + BREW_SLOT * (i / BREW_COLS);
            String id = potions.get(idx);
            ResourceLocation slotSprite = craftable.contains(id) ? BREW_SLOT_CRAFTABLE : BREW_SLOT_UNCRAFTABLE;
            guiGraphics.blitSprite(slotSprite, bx, by, BREW_SLOT, BREW_SLOT);
            if (id.equals(target)) {
                guiGraphics.fill(bx + 1, by + 1, bx + BREW_SLOT - 1, by + BREW_SLOT - 1, 0x7030A030);
            }
            ItemStack potion = potionStack(id);
            guiGraphics.renderFakeItem(potion, bx + 4, by + 4);
        }
        guiGraphics.pose().popPose();

        boolean hoverFilter = mouseX >= bookX + 110 && mouseX < bookX + 136
                && mouseY >= bookY + 12 && mouseY < bookY + 28;
        guiGraphics.blitSprite(BREW_FILTER_SPRITES.get(this.brewFilterCraftable, hoverFilter),
                bookX + 110, bookY + 12, 26, 16);

        boolean canBack = this.brewPage > 0;
        boolean canForward = this.brewPage < totalPages - 1;
        boolean hoverBack = mouseX >= bookX + 38 && mouseX < bookX + 50 && mouseY >= bookY + 137 && mouseY < bookY + 154;
        boolean hoverForward = mouseX >= bookX + 93 && mouseX < bookX + 105 && mouseY >= bookY + 137 && mouseY < bookY + 154;
        if (canBack) {
            guiGraphics.blitSprite(hoverBack ? PAGE_BACKWARD_HL : PAGE_BACKWARD, bookX + 38, bookY + 137, 12, 17);
        }
        if (canForward) {
            guiGraphics.blitSprite(hoverForward ? PAGE_FORWARD_HL : PAGE_FORWARD, bookX + 93, bookY + 137, 12, 17);
        }
        if (totalPages > 1) {
            Component page = Component.translatable("gui.recipebook.page", this.brewPage + 1, totalPages);
            int width = this.font.width(page);
            guiGraphics.drawString(this.font, page, bookX - width / 2 + 73, bookY + 141, 0xFF000000, false);
        }

        String hover = brewBookAt((int) mouseX, (int) mouseY);
        if (hover != null) {
            List<Component> lines = new ArrayList<>();
            ItemStack potion = potionStack(hover);
            lines.add(potion.isEmpty() ? Component.literal(hover) : potion.getHoverName());
            this.panelTooltip = lines;
        }
    }

    private List<String> visiblePotions() {
        if (!this.brewFilterCraftable) {
            return this.brewablePotions;
        }
        StationState state = getStationMenu().getState();
        List<String> craftable = state == null ? List.of() : state.craftable();
        List<String> out = new ArrayList<>();
        for (String id : this.brewablePotions) {
            if (craftable.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    private String brewBookAt(int mouseX, int mouseY) {
        int bookX = machineTabPanelX();
        int bookY = machineTabPanelY();
        if (bookX < 4) {
            return null;
        }
        List<String> potions = visiblePotions();
        int start = this.brewPage * BREW_PER_PAGE;
        for (int i = 0; i < BREW_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= potions.size()) {
                break;
            }
            int bx = bookX + 11 + BREW_SLOT * (i % BREW_COLS);
            int by = bookY + 31 + BREW_SLOT * (i / BREW_COLS);
            if (mouseX >= bx && mouseX < bx + BREW_SLOT && mouseY >= by && mouseY < by + BREW_SLOT) {
                return potions.get(idx);
            }
        }
        return null;
    }

    private void renderSourcePanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<SourceRow> rows = buildSourceRows();
        int total = rows.size();
        int rowsVisible = Math.min(SOURCES_MAX_ROWS, total);
        int panelH = SOURCE_HEADER_H + rowsVisible * SOURCE_ROW_H;
        int px = machineTabPanelX() + SOURCE_PANEL_X;
        int py = machineTabPanelY() + SOURCE_PANEL_TOP;
        int maxScroll = Math.max(0, total - rowsVisible);
        if (this.sourcesScroll > maxScroll) {
            this.sourcesScroll = maxScroll;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500.0F);
        guiGraphics.fill(px, py, px + SOURCE_PANEL_W, py + panelH, 0xEE101010);
        guiGraphics.fill(px, py, px + SOURCE_PANEL_W, py + 1, 0xFF555555);
        guiGraphics.fill(px, py, px + 1, py + panelH, 0xFF555555);
        guiGraphics.fill(px + SOURCE_PANEL_W - 1, py, px + SOURCE_PANEL_W, py + panelH, 0xFF555555);
        guiGraphics.fill(px, py + panelH - 1, px + SOURCE_PANEL_W, py + panelH, 0xFF555555);
        guiGraphics.drawString(this.font, Component.translatable("gui.crafting_network.sources").getString(),
                px + 4, py + 3, 0xFFFFFFFF, false);
        int sel = getStationMenu().getSelectedSource();
        for (int v = 0; v < rowsVisible; v++) {
            int idx = this.sourcesScroll + v;
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
        for (CraftingSourceInfo info : getStationMenu().getSources()) {
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
        int rowsVisible = Math.min(SOURCES_MAX_ROWS, total);
        int panelH = SOURCE_HEADER_H + rowsVisible * SOURCE_ROW_H;
        int px = machineTabPanelX() + SOURCE_PANEL_X;
        int py = machineTabPanelY() + SOURCE_PANEL_TOP;
        if (mouseX < px || mouseX >= px + SOURCE_PANEL_W || mouseY < py + SOURCE_HEADER_H || mouseY >= py + panelH) {
            return -1;
        }
        int rel = (mouseY - (py + SOURCE_HEADER_H)) / SOURCE_ROW_H + this.sourcesScroll;
        return rel < total ? rel : -1;
    }

    private String truncate(String text, int maxWidth) {
        if (font.width(text) > maxWidth) {
            return font.plainSubstrByWidth(text, maxWidth) + "...";
        }
        return text;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        StationType type = getStationMenu().stationType();
        ResourceLocation bg = switch (type) {
            case BLASTING -> BLAST_FURNACE_BG;
            case SMOKING -> SMOKER_BG;
            case BREWING -> BREWING_STAND_BG;
            default -> FURNACE_BG;
        };
        guiGraphics.blit(bg, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        } else if (recipeBookComponent != null && recipeBookComponent.keyPressed(keyCode, scanCode, modifiers)) {
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
        } else if (recipeBookComponent != null && recipeBookComponent.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        boolean inRecipeBook = recipeBookComponent != null && recipeBookComponent.isVisible() && this.widthTooNarrow;
        if (inRecipeBook) {
            return false;
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isBookVisible() && machineTabPanelX() >= 4) {
            if (sourcesArrowAt(mouseX, mouseY)) {
                this.searchBox.setFocused(false);
                this.sourcesOpen = !this.sourcesOpen;
                this.sourcesScroll = 0;
                return true;
            }
            if (this.sourcesOpen) {
                int row = sourcePanelRowAt((int) mouseX, (int) mouseY);
                if (row >= 0) {
                    SourceRow sr = buildSourceRows().get(row);
                    int px = machineTabPanelX() + SOURCE_PANEL_X;
                    if (sr.hasChildren() && mouseX >= px + SOURCE_PANEL_W - 14) {
                        toggleCollapse(sr.id());
                    } else {
                        selectSource(sr.id());
                        this.sourcesOpen = false;
                    }
                    return true;
                }
                this.sourcesOpen = false;
                return true;
            }
            int searchX = searchBoxX();
            int searchY = searchBoxY();
            if (searchX >= 4 && mouseX >= searchX && mouseX < searchX + searchBoxW()
                    && mouseY >= searchY && mouseY < searchY + 14) {
                this.searchBox.setFocused(true);
                return true;
            }
        }
        if (button == 0 && isBrewing() && this.brewBookOpen) {
            int bookX = machineTabPanelX();
            int bookY = machineTabPanelY();
            if (mouseX >= bookX + 110 && mouseX < bookX + 136
                    && mouseY >= bookY + 12 && mouseY < bookY + 28) {
                this.brewFilterCraftable = !this.brewFilterCraftable;
                this.brewPage = 0;
                return true;
            }
            List<String> potions = visiblePotions();
            int totalPages = Math.max(1, (potions.size() + BREW_PER_PAGE - 1) / BREW_PER_PAGE);
            if (this.brewPage > 0 && mouseX >= bookX + 38 && mouseX < bookX + 50
                    && mouseY >= bookY + 137 && mouseY < bookY + 154) {
                this.brewPage--;
                return true;
            }
            if (this.brewPage < totalPages - 1 && mouseX >= bookX + 93 && mouseX < bookX + 105
                    && mouseY >= bookY + 137 && mouseY < bookY + 154) {
                this.brewPage++;
                return true;
            }
            String potion = brewBookAt((int) mouseX, (int) mouseY);
            if (potion != null) {
                sendBrewTarget(potion);
                return true;
            }
        }
        if (recipeBookComponent != null && recipeBookComponent.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(recipeBookComponent);
            return true;
        }
        if (this.widthTooNarrow && recipeBookComponent != null && recipeBookComponent.isVisible()) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (hasShiftDown() || hasControlDown()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (this.sourcesOpen && isBookVisible() && machineTabPanelX() >= 4) {
            int px = machineTabPanelX() + SOURCE_PANEL_X;
            int py = machineTabPanelY() + SOURCE_PANEL_TOP;
            List<SourceRow> rows = buildSourceRows();
            int total = rows.size();
            int rowsVisible = Math.min(SOURCES_MAX_ROWS, total);
            int panelH = SOURCE_HEADER_H + rowsVisible * SOURCE_ROW_H;
            if (mouseX >= px && mouseX < px + SOURCE_PANEL_W && mouseY >= py && mouseY < py + panelH) {
                int maxScroll = Math.max(0, total - rowsVisible);
                this.sourcesScroll = Math.max(0, Math.min(maxScroll, this.sourcesScroll - (scrollY > 0 ? 1 : -1)));
                return true;
            }
        }
        if (isBrewing() && this.brewBookOpen) {
            int px = machineTabPanelX();
            int py = machineTabPanelY();
            if (mouseX >= px && mouseX < px + PANEL_W && mouseY >= py && mouseY < py + PANEL_H) {
                int totalPages = Math.max(1, (visiblePotions().size() + BREW_PER_PAGE - 1) / BREW_PER_PAGE);
                if (scrollY > 0) {
                    this.brewPage = Math.max(0, this.brewPage - 1);
                } else {
                    this.brewPage = Math.min(totalPages - 1, this.brewPage + 1);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        boolean inside = mouseX < (double) guiLeft || mouseY < (double) guiTop
                || mouseX >= (double) (guiLeft + this.imageWidth) || mouseY >= (double) (guiTop + this.imageHeight);
        return (recipeBookComponent != null
                ? recipeBookComponent.hasClickedOutside(mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth,
                        this.imageHeight, mouseButton)
                : true) && inside;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        super.slotClicked(slot, slotId, mouseButton, clickType);
        if (recipeBookComponent != null) {
            recipeBookComponent.slotClicked(slot);
        }
    }

    @Override
    public void recipesUpdated() {
        if (recipeBookComponent != null) {
            recipeBookComponent.recipesUpdated();
        }
    }

    @Override
    public RecipeBookComponent getRecipeBookComponent() {
        return this.recipeBookComponent;
    }
}
