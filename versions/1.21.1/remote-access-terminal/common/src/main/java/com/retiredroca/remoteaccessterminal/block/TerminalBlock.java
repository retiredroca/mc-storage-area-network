package com.retiredroca.remoteaccessterminal.block;

import java.util.UUID;

import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.NetworkBlock;
import com.retiredroca.remoteaccessterminal.PlacementContext;
import com.retiredroca.remoteaccessterminal.RemoteAccessTerminalCommon;
import com.retiredroca.remoteaccessterminal.TerminalChunkLoader;
import com.retiredroca.remoteaccessterminal.TerminalLinks;
import com.retiredroca.remoteaccessterminal.TerminalLinksAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A dye-coloured Remote Access Terminal. Placing one registers a link (dimension + position) in the
 * shared {@link TerminalLinks} store, removes it when the block is broken or exploded, and records
 * the placer with the API's {@link ContainerOwnership} so the API's break protection keeps the block
 * owner-only.
 */
public class TerminalBlock extends Block implements NetworkBlock {
    private final DyeColor color;

    public TerminalBlock(DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }

    /** The opaque ARGB tint derived from the vanilla dye. */
    public int getTintColor() {
        return FastColor.ARGB32.opaque(color.getFireworkColor());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        TerminalLinksAccess access = TerminalLinksAccess.get(serverLevel);
        TerminalLinks links = access.links();
        if (links.register(serverLevel, color, pos)) {
            UUID pending = PlacementContext.take(color, serverLevel.dimension(), pos);
            if (pending != null) {
                links.setOwner(color, serverLevel.dimension(), pos, pending);
                promptNaming(serverLevel, pos, pending);
            }
            access.setDirty();
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel) || !(placer instanceof Player player)) {
            return;
        }
        ContainerOwnership.setOwner(serverLevel, pos, player.getUUID(), player.getGameProfile().getName());
        TerminalLinksAccess access = TerminalLinksAccess.get(serverLevel);
        TerminalLinks links = access.links();
        if (links.setOwner(color, serverLevel.dimension(), pos, player.getUUID())) {
            access.setDirty();
            promptNaming(serverLevel, pos, player.getUUID());
        } else if (links.hasCapacity()) {
            // onPlace registers after setPlacedBy on some loaders; hand the owner over to it.
            PlacementContext.put(color, serverLevel.dimension(), pos, player.getUUID());
        }
    }

    private void promptNaming(ServerLevel level, BlockPos pos, UUID owner) {
        if (level.getServer() == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        RemoteAccessTerminalCommon.platform().openTerminal(player, level.dimension(), pos, color, true);
        RemoteAccessTerminalCommon.platform().sendOpenName(player, level.dimension(), pos, color);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        clearLink(level, pos);
        return result;
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        clearLink(level, pos);
    }

    private void clearLink(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        TerminalLinksAccess access = TerminalLinksAccess.get(serverLevel);
        TerminalLinks links = access.links();
        TerminalChunkLoader.deactivate(serverLevel, links.find(color, serverLevel.dimension(), pos));
        if (links.remove(color, serverLevel.dimension(), pos)) {
            access.setDirty();
        }
        ContainerOwnership.clearOwner(serverLevel, pos);
    }
}
