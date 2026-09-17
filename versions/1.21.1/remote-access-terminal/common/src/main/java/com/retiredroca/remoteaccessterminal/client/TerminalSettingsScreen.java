package com.retiredroca.remoteaccessterminal.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.TerminalChunkLoader;
import com.retiredroca.remoteaccessterminal.config.TerminalSettings;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * Owner/team settings: rename, dye change, sort mode, open/private, invites and unlink. Vanilla
 * screen background, labels and buttons only; the dye picker is a grid of ordinary buttons showing
 * each colour's running count (a colour is never disabled by count).
 */
public class TerminalSettingsScreen extends AbstractContainerScreen<TerminalMenu> {
    private static final int LABEL_COLOR = 4210752;

    private static final int NAME_Y = 18;
    private static final int COLOR_COLUMNS = 4;
    private static final int COLOR_CELL_W = 74;
    private static final int COLOR_STEP_X = 76;
    private static final int COLOR_STEP_Y = 21;
    private static final int COLOR_X = 8;
    private static final int COLOR_Y = 50;
    private static final int INVITE_ROWS = 2;
    private static final int INVITE_Y = 208;
    private static final int INVITE_STEP = 20;

    private final Set<UUID> selectedInvites = new HashSet<>();
    private boolean invitesInitialized;
    private int page;
    private int lastVersion = -1;
    private EditBox nameBox;
    private Button chunkLoaderButton;
    private long labelMinute;
    private int labelQueue = -1;
    private boolean labelActive;

    public TerminalSettingsScreen(TerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 276;
        this.inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        if (!invitesInitialized && menu.getVersion() > 0) {
            selectedInvites.clear();
            selectedInvites.addAll(menu.getInvites());
            invitesInitialized = true;
        }
        int x = this.leftPos;
        int y = this.topPos;

        nameBox = new EditBox(this.font, x + 8, y + NAME_Y, 200, 20, Component.empty());
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.translatable("gui.remote_access_terminal.name"));
        nameBox.setValue(menu.getName() == null ? "" : menu.getName());
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.rename"),
                button -> RemoteAccessTerminalCommon.platform().sendRename(menu.getDimension(), menu.getPos(),
                        menu.getColor(), nameBox.getValue()))
                .bounds(x + 212, y + NAME_Y, 100, 20).build());

        buildColorWidgets(x, y);

        this.chunkLoaderButton = Button.builder(chunkLoaderLabel(),
                button -> RemoteAccessTerminalCommon.platform().sendSetChunkLoader(chunkLoaderNextState()))
                .bounds(x + 8, y + 150, 304, 20).build();
        this.chunkLoaderButton.active = menu.canEdit();
        addRenderableWidget(this.chunkLoaderButton);
        refreshChunkLoaderLabel();

        addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.sort",
                Component.translatable(menu.getSortMode().displayKey())),
                button -> RemoteAccessTerminalCommon.platform().sendSetSort(menu.getSortMode().next().ordinal()))
                .bounds(x + 8, y + 172, 150, 20).build());
        addRenderableWidget(Button.builder(menu.isOpen()
                ? Component.translatable("gui.remote_access_terminal.open")
                : Component.translatable("gui.remote_access_terminal.private"),
                button -> RemoteAccessTerminalCommon.platform().sendSetOpen(!menu.isOpen()))
                .bounds(x + 162, y + 172, 150, 20).build());

        buildInviteWidgets(x, y);

        addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.apply_invites"),
                button -> RemoteAccessTerminalCommon.platform()
                        .sendSetInvites(new ArrayList<>(selectedInvites)))
                .bounds(x + 8, y + 252, 110, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(x + 124, y + 252, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(x + 148, y + 252, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.unlink"), button -> {
            RemoteAccessTerminalCommon.platform().sendUnlink();
            this.onClose();
        }).bounds(x + 202, y + 252, 110, 20).build());
    }

    /** A grid of ordinary buttons, one per dye, each showing its running count; never disabled. */
    private void buildColorWidgets(int x, int y) {
        int index = 0;
        for (DyeColor color : DyeColor.values()) {
            int column = index % COLOR_COLUMNS;
            int row = index / COLOR_COLUMNS;
            DyeColor target = color;
            addRenderableWidget(Button.builder(
                    Component.literal(colorName(color) + " " + menu.count(color)),
                    button -> {
                        if (target != menu.getColor()) {
                            RemoteAccessTerminalCommon.platform().sendSetColor(target);
                        }
                    }).bounds(x + COLOR_X + column * COLOR_STEP_X, y + COLOR_Y + row * COLOR_STEP_Y,
                            COLOR_CELL_W, 20).build());
            index++;
        }
    }

    private static String colorName(DyeColor color) {
        StringBuilder builder = new StringBuilder();
        for (String part : color.getName().split("_")) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    /** Idle terminals switch on; a running lease or a queued request is switched off/cancelled. */
    private boolean chunkLoaderNextState() {
        return !menu.isChunkLoader() && menu.getChunkLoaderQueuePosition() <= 0;
    }

    /** Keeps the countdown on the chunk-loader button current without rebuilding every tick. */
    private void refreshChunkLoaderLabel() {
        if (chunkLoaderButton == null) {
            return;
        }
        int queue = menu.getChunkLoaderQueuePosition();
        long minute = menu.isChunkLoader() && menu.getChunkLoaderUntil() > 0
                ? (menu.getChunkLoaderUntil() - System.currentTimeMillis()) / 60_000L
                : 0L;
        if (minute == labelMinute && queue == labelQueue && menu.isChunkLoader() == labelActive) {
            return;
        }
        labelMinute = minute;
        labelQueue = queue;
        labelActive = menu.isChunkLoader();
        chunkLoaderButton.setMessage(chunkLoaderLabel());
    }

    private Component chunkLoaderLabel() {
        Component state;
        if (menu.getChunkLoaderQueuePosition() > 0) {
            state = Component.translatable("gui.remote_access_terminal.queued",
                    menu.getChunkLoaderQueuePosition());
        } else if (!menu.isChunkLoader()) {
            state = Component.translatable("gui.remote_access_terminal.off");
        } else if (menu.getChunkLoaderUntil() <= 0) {
            state = Component.translatable("gui.remote_access_terminal.on");
        } else {
            state = Component.translatable("gui.remote_access_terminal.on_time",
                    TerminalChunkLoader.remainingLabel(menu.getChunkLoaderUntil()));
        }
        return Component.translatable("gui.remote_access_terminal.chunkloader", state);
    }

    private void buildInviteWidgets(int x, int y) {
        List<PlayerInfo> players = onlinePlayers();
        int pages = Math.max(1, (players.size() + INVITE_ROWS - 1) / INVITE_ROWS);
        if (page >= pages) {
            page = pages - 1;
        }
        if (page < 0) {
            page = 0;
        }
        int start = page * INVITE_ROWS;
        for (int i = 0; i < INVITE_ROWS; i++) {
            int index = start + i;
            if (index >= players.size()) {
                break;
            }
            PlayerInfo info = players.get(index);
            UUID id = info.getProfile().getId();
            boolean selected = selectedInvites.contains(id);
            String label = (selected ? "[x] " : "[ ] ") + info.getProfile().getName();
            addRenderableWidget(Button.builder(Component.literal(label), button -> {
                if (!selectedInvites.remove(id)) {
                    selectedInvites.add(id);
                }
                rebuildWidgets();
            }).bounds(x + 8, y + INVITE_Y + i * INVITE_STEP, 300, 20).build());
        }
    }

    private List<PlayerInfo> onlinePlayers() {
        if (this.minecraft == null || this.minecraft.getConnection() == null) {
            return List.of();
        }
        List<PlayerInfo> players = new ArrayList<>(this.minecraft.getConnection().getOnlinePlayers());
        UUID local = this.minecraft.player == null ? null : this.minecraft.player.getUUID();
        players.removeIf(info -> info == null || info.getProfile() == null
                || (local != null && local.equals(info.getProfile().getId())));
        players.sort((a, b) -> a.getProfile().getName().compareToIgnoreCase(b.getProfile().getName()));
        return players;
    }

    private void changePage(int delta) {
        page += delta;
        rebuildWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.getVersion() != lastVersion) {
            lastVersion = menu.getVersion();
            rebuildWidgets();
            return;
        }
        refreshChunkLoaderLabel();
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
        graphics.drawString(this.font, Component.translatable("gui.remote_access_terminal.color"),
                8, 38, LABEL_COLOR, false);
        graphics.drawString(this.font,
                Component.translatable("gui.remote_access_terminal.total", menu.getTotal(),
                        TerminalSettings.getMaxTerminals()),
                8, 138, LABEL_COLOR, false);
        graphics.drawString(this.font, Component.translatable("gui.remote_access_terminal.invites"),
                8, 196, LABEL_COLOR, false);
        int pages = Math.max(1, (onlinePlayers().size() + INVITE_ROWS - 1) / INVITE_ROWS);
        graphics.drawString(this.font, Component.literal((page + 1) + "/" + pages), 176, 258, LABEL_COLOR, false);
    }
}
