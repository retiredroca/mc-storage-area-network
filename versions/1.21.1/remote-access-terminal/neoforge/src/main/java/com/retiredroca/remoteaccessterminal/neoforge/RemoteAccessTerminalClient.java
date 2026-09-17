package com.retiredroca.remoteaccessterminal.neoforge;

import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;
import com.retiredroca.remoteaccessterminal.client.TerminalPickerScreen;
import com.retiredroca.remoteaccessterminal.client.TerminalSettingsScreen;
import com.retiredroca.remoteaccessterminal.item.TerminalBlockItem;
import com.retiredroca.remoteaccessterminal.menu.TerminalMenu;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

@Mod(value = RemoteAccessTerminalCommon.MODID, dist = Dist.CLIENT)
public class RemoteAccessTerminalClient {
    public RemoteAccessTerminalClient(IEventBus modEventBus) {
        modEventBus.addListener(RemoteAccessTerminalClient::registerBlockColors);
        modEventBus.addListener(RemoteAccessTerminalClient::registerItemColors);
        modEventBus.addListener(RemoteAccessTerminalClient::registerScreens);
        NetworkingClient.register();
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        MenuScreens.ScreenConstructor<TerminalMenu, AbstractContainerScreen<TerminalMenu>> screens =
                (menu, inventory, title) -> menu.isSettings()
                        ? new TerminalSettingsScreen(menu, inventory, title)
                        : new TerminalPickerScreen(menu, inventory, title);
        event.register(Registration.MENU.get(), screens);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        Block[] blocks = Registration.terminalBlocks().values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
        event.register(
                (state, level, pos, tintIndex) -> state.getBlock() instanceof TerminalBlock terminal
                        ? terminal.getTintColor()
                        : -1,
                blocks);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (DeferredItem<TerminalBlockItem> item : Registration.terminalItems().values()) {
            event.register((stack, tintIndex) -> item.get().getTintColor(), item.get());
        }
    }
}
