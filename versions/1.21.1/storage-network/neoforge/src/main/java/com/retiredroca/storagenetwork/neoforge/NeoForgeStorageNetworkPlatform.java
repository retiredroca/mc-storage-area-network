package com.retiredroca.storagenetwork.neoforge;

import java.nio.file.Path;

import com.retiredroca.storagenetwork.StorageNetworkPlatform;
import com.retiredroca.storagenetwork.block.NetworkShareTerminalBlock;
import com.retiredroca.storagenetwork.block.StorageTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExcludePayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExtractPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSelectPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSyncPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgeStorageNetworkPlatform implements StorageNetworkPlatform {
    @Override
    public MenuType<?> terminalMenuType() {
        return Registration.STORAGE_TERMINAL_MENU.get();
    }

    @Override
    public BlockEntityType<?> terminalBlockEntityType() {
        return Registration.STORAGE_TERMINAL_BE.get();
    }

    @Override
    public Item terminalItem() {
        return Registration.STORAGE_TERMINAL_ITEM.get();
    }

    @Override
    public StorageTerminalBlock terminalBlock() {
        return Registration.STORAGE_TERMINAL_BLOCK.get();
    }

    @Override
    public AbstractStorageTerminalBlockEntity createTerminalBlockEntity(BlockPos pos, BlockState state) {
        return new StorageTerminalBlockEntity(pos, state);
    }

    @Override
    public void openTerminal(ServerPlayer player, AbstractStorageTerminalBlockEntity terminal) {
        if (terminal instanceof StorageTerminalBlockEntity be) {
            player.openMenu(be, buf -> buf.writeBlockPos(be.getBlockPos()));
            PacketDistributor.sendToPlayer(player, be.buildSync());
        }
    }

    @Override
    public void sendTerminalSync(ServerPlayer player, TerminalSyncPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public MenuType<?> shareTerminalMenuType() {
        return Registration.SHARE_TERMINAL_MENU.get();
    }

    @Override
    public NetworkShareTerminalBlock shareTerminalBlock() {
        return Registration.SHARE_TERMINAL_BLOCK.get();
    }

    @Override
    public AbstractNetworkShareTerminalBlockEntity createShareTerminalBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkShareTerminalBlockEntity(pos, state);
    }

    @Override
    public void openShareTerminal(ServerPlayer player, AbstractNetworkShareTerminalBlockEntity share) {
        if (share instanceof NetworkShareTerminalBlockEntity be) {
            player.openMenu(be, buf -> buf.writeBlockPos(be.getBlockPos()));
        }
    }

    @Override
    public void sendExtract(BlockPos pos, ItemStack stack, int mode) {
        PacketDistributor.sendToServer(new TerminalExtractPayload(pos, stack, mode));
    }

    @Override
    public void sendSelect(BlockPos pos, boolean all, BlockPos targetPos, String childName) {
        PacketDistributor.sendToServer(new TerminalSelectPayload(pos, all, targetPos, childName));
    }

    @Override
    public void sendToggleExclude(BlockPos pos, BlockPos containerPos) {
        PacketDistributor.sendToServer(new TerminalExcludePayload(pos, containerPos));
    }

    @Override
    public boolean isServerModded() {
        return Networking.isServerModded();
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
