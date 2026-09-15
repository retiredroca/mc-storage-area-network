package com.retiredroca.storagenetwork.fabric;

import java.nio.file.Path;

import com.retiredroca.storagenetwork.StorageNetworkPlatform;
import com.retiredroca.storagenetwork.block.NetworkShareTerminalBlock;
import com.retiredroca.storagenetwork.block.StorageTerminalBlock;
import com.retiredroca.storagenetwork.blockentity.AbstractNetworkShareTerminalBlockEntity;
import com.retiredroca.storagenetwork.blockentity.AbstractStorageTerminalBlockEntity;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalExtractPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSelectPayload;
import com.retiredroca.storagenetwork.network.TerminalPackets.TerminalSyncPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricStorageNetworkPlatform implements StorageNetworkPlatform {
    @Override
    public MenuType<?> terminalMenuType() {
        return Registration.STORAGE_TERMINAL_MENU;
    }

    @Override
    public BlockEntityType<?> terminalBlockEntityType() {
        return Registration.STORAGE_TERMINAL_BE;
    }

    @Override
    public Item terminalItem() {
        return Registration.STORAGE_TERMINAL_ITEM;
    }

    @Override
    public StorageTerminalBlock terminalBlock() {
        return Registration.STORAGE_TERMINAL_BLOCK;
    }

    @Override
    public AbstractStorageTerminalBlockEntity createTerminalBlockEntity(BlockPos pos, BlockState state) {
        return new StorageTerminalBlockEntity(pos, state);
    }

    @Override
    public void openTerminal(ServerPlayer player, AbstractStorageTerminalBlockEntity terminal) {
        if (terminal instanceof StorageTerminalBlockEntity be) {
            player.openMenu(be);
            ServerPlayNetworking.send(player, be.buildSync());
        }
    }

    @Override
    public MenuType<?> shareTerminalMenuType() {
        return Registration.SHARE_TERMINAL_MENU;
    }

    @Override
    public NetworkShareTerminalBlock shareTerminalBlock() {
        return Registration.SHARE_TERMINAL_BLOCK;
    }

    @Override
    public AbstractNetworkShareTerminalBlockEntity createShareTerminalBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkShareTerminalBlockEntity(pos, state);
    }

    @Override
    public void openShareTerminal(ServerPlayer player, AbstractNetworkShareTerminalBlockEntity share) {
        if (share instanceof NetworkShareTerminalBlockEntity be) {
            player.openMenu(be);
        }
    }

    @Override
    public void sendTerminalSync(ServerPlayer player, TerminalSyncPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendExtract(BlockPos pos, ItemStack stack, int mode) {
        ClientPlayNetworking.send(new TerminalExtractPayload(pos, stack, mode));
    }

    @Override
    public void sendSelect(BlockPos pos, boolean all, BlockPos targetPos, String childName) {
        ClientPlayNetworking.send(new TerminalSelectPayload(pos, all, targetPos, childName));
    }

    @Override
    public boolean isServerModded() {
        return Networking.isServerModded();
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
