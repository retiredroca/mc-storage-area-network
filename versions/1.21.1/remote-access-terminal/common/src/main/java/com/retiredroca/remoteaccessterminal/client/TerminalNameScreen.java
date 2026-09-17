package com.retiredroca.remoteaccessterminal.client;

import org.lwjgl.glfw.GLFW;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

/** Skippable naming popup shown after a terminal is placed. */
public class TerminalNameScreen extends Screen {
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    private final DyeColor color;
    private EditBox nameBox;

    public TerminalNameScreen(ResourceKey<Level> dimension, BlockPos pos, DyeColor color) {
        super(Component.translatable("gui.remote_access_terminal.name_terminal"));
        this.dimension = dimension;
        this.pos = pos;
        this.color = color;
    }

    @Override
    protected void init() {
        nameBox = new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 24, 200, 20,
                Component.translatable("gui.remote_access_terminal.name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(initialName());
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.save"),
                button -> save()).bounds(this.width / 2 - 102, this.height / 2 + 4, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.remote_access_terminal.skip"),
                button -> this.onClose()).bounds(this.width / 2 + 2, this.height / 2 + 4, 100, 20).build());
        setFocused(nameBox);
    }

    private String initialName() {
        if (this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.containerMenu instanceof TerminalMenu menu) {
            return menu.getName() == null ? "" : menu.getName();
        }
        return "";
    }

    private void save() {
        RemoteAccessTerminalCommon.platform().sendRename(dimension, pos, color, nameBox.getValue());
        this.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 48, 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
