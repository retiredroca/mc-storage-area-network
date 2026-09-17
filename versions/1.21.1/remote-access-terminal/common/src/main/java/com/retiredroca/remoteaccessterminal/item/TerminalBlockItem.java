package com.retiredroca.remoteaccessterminal.item;

import com.retiredroca.remoteaccessterminal.TerminalLinks;
import com.retiredroca.remoteaccessterminal.TerminalLinksAccess;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Block item for a terminal. Placement beyond the global capacity is allowed but left unlinked; the
 * item warns the player, and {@link TerminalBlock} skips the link registration.
 */
public class TerminalBlockItem extends BlockItem {
    private final TerminalBlock terminal;

    public TerminalBlockItem(TerminalBlock block, Item.Properties properties) {
        super(block, properties);
        this.terminal = block;
    }

    public TerminalBlock getTerminal() {
        return terminal;
    }

    public int getTintColor() {
        return terminal.getTintColor();
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null && !context.getLevel().isClientSide()) {
            TerminalLinks links = TerminalLinksAccess.get(context.getLevel()).links();
            if (!links.hasCapacity()) {
                player.displayClientMessage(Component.translatable(
                        "message.remote_access_terminal.terminal_limit_global", links.maxTerminals()), true);
            }
        }
        return super.place(context);
    }
}
