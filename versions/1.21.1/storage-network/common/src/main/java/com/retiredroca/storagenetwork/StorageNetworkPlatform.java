package com.retiredroca.storagenetwork;

import java.nio.file.Path;

import com.retiredroca.storagenetwork.block.NetworkShareTerminalBlock;
import com.retiredroca.storagenetwork.block.StorageTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Loader-specific services used by the loader-neutral Storage Network code. */
public interface StorageNetworkPlatform {
    MenuType<?> terminalMenuType();

    BlockEntityType<?> terminalBlockEntityType();

    Item terminalItem();

    StorageTerminalBlock terminalBlock();

    AbstractStorageTerminalBlockEntity createTerminalBlockEntity(BlockPos pos, BlockState state);

    void openTerminal(ServerPlayer player, AbstractStorageTerminalBlockEntity terminal);

    MenuType<?> shareTerminalMenuType();

    NetworkShareTerminalBlock shareTerminalBlock();

    AbstractNetworkShareTerminalBlockEntity createShareTerminalBlockEntity(BlockPos pos, BlockState state);

    void openShareTerminal(ServerPlayer player, AbstractNetworkShareTerminalBlockEntity share);

    /** Send the terminal contents snapshot to a player. */
    void sendTerminalSync(ServerPlayer player, TerminalSyncPayload payload);

    /** C2S: request extracting a stack from the terminal. */
    void sendExtract(BlockPos pos, ItemStack stack, int mode);

    /** C2S: set the deposit target (all or a specific container/child). */
    void sendSelect(BlockPos pos, boolean all, BlockPos targetPos, String childName);

    /** C2S: toggle the container's block type in/out of the network. */
    void sendToggleExclude(BlockPos pos, BlockPos containerPos);

    boolean isServerModded();

    Path configDir();
}
