package com.retiredroca.networkrouting.menu;

import java.util.List;

import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.networkrouting.NetworkRoutingCommon;
import com.retiredroca.networkrouting.network.RoutingPackets;
import com.retiredroca.networkrouting.network.RoutingPackets.ContainerInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A slotless menu for both the Routing Terminal and the Routing Linker. The {@code pos} is the
 * routing terminal in terminal mode, or the container itself in linker mode.
 */
public class RoutingMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    private int state;
    private int tier;
    private int flags;
    private List<ContainerInfo> containers = List.of();

    public RoutingMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        super(NetworkRoutingCommon.platform().menuType(), containerId);
        this.pos = pos;
    }

    public static RoutingMenu fromNetwork(int containerId, Inventory playerInventory, BlockPos pos) {
        return new RoutingMenu(containerId, playerInventory, pos);
    }

    public static RoutingMenu fromNetwork(int containerId, Inventory playerInventory,
            RegistryFriendlyByteBuf buffer) {
        return new RoutingMenu(containerId, playerInventory, buffer.readBlockPos());
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isTerminal() {
        return (state & RoutingPackets.STATE_TERMINAL) != 0;
    }

    /** Whether the screen shows the container list (terminal) / the single labeled row (linker). */
    public boolean showList() {
        return (state & RoutingPackets.STATE_LIST) != 0;
    }

    public boolean isBound() {
        return (state & RoutingPackets.STATE_BOUND) != 0;
    }

    public int getTier() {
        return tier;
    }

    public int getFlags() {
        return flags;
    }

    public List<ContainerInfo> getContainers() {
        return containers;
    }

    public void updateSync(int state, int tier, int flags, List<ContainerInfo> containers) {
        this.state = state;
        this.tier = tier;
        this.flags = flags;
        this.containers = containers;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return true;
        }
        // The menu was opened via canUse (terminal) or canEdit (linker); keep it open while either holds.
        return NetworkPermissions.canUse(level, pos, player) || NetworkPermissions.canEdit(level, pos, player);
    }
}
