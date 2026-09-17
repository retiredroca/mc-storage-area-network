package com.retiredroca.remoteaccessterminal.fabric;

import com.retiredroca.remoteaccessterminal.block.TerminalBlock;
import com.retiredroca.remoteaccessterminal.client.TerminalPickerScreen;
import com.retiredroca.remoteaccessterminal.client.TerminalSettingsScreen;
import com.retiredroca.remoteaccessterminal.item.TerminalBlockItem;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class RemoteAccessTerminalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Block[] blocks = Registration.blocks().values().toArray(new Block[0]);
        ColorProviderRegistry.BLOCK.register(
                (state, level, pos, tintIndex) -> state.getBlock() instanceof TerminalBlock terminal
                        ? terminal.getTintColor()
                        : -1,
                blocks);

        Item[] items = Registration.items().values().toArray(new Item[0]);
        ColorProviderRegistry.ITEM.register(
                (stack, tintIndex) -> stack.getItem() instanceof TerminalBlockItem item
                        ? item.getTintColor()
                        : -1,
                items);

        MenuScreens.ScreenConstructor<TerminalMenu, AbstractContainerScreen<TerminalMenu>> screens =
                (menu, inventory, title) -> menu.isSettings()
                        ? new TerminalSettingsScreen(menu, inventory, title)
                        : new TerminalPickerScreen(menu, inventory, title);
        MenuScreens.register(Registration.MENU, screens);
        NetworkingClient.registerClient();
    }
}
