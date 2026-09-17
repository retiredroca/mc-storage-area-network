package com.retiredroca.remoteaccessterminal;

import com.retiredroca.remoteaccessterminal.config.TerminalSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Shared teleport-and-sound used by the terminal destination menu (and, later, any command). Unlike
 * the reference mod this supports cross-dimensional travel: the player is moved into the target
 * terminal's {@link ServerLevel}, preserving their yaw and pitch.
 */
public final class TerminalTravel {
    private static final SoundEvent TELEPORT_SOUND =
            BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.chorus_fruit.teleport"));

    private TerminalTravel() {
    }

    /** Teleports to a resolved link. */
    public static boolean teleport(ServerPlayer player, ServerLevel sourceLevel, BlockPos source,
            TerminalLinks.Link target) {
        return teleport(player, sourceLevel, source, target.dimension(), target.pos());
    }

    /**
     * Teleports the player from the source terminal to the terminal at {@code targetDimension} and
     * {@code target}, preserving yaw and pitch.
     *
     * @return false when the target dimension is missing or cross-dimension travel is disabled.
     */
    public static boolean teleport(ServerPlayer player, ServerLevel sourceLevel, BlockPos source,
            ResourceKey<Level> targetDimension, BlockPos target) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        ServerLevel targetLevel = server.getLevel(targetDimension);
        if (targetLevel == null) {
            return false;
        }
        if (!TerminalSettings.isAllowCrossDimension() && !targetDimension.equals(sourceLevel.dimension())) {
            return false;
        }
        player.teleportTo(targetLevel, target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        sourceLevel.playSound(null, source, TELEPORT_SOUND, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }
}
