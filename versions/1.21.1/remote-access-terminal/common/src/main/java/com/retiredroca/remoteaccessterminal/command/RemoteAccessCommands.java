package com.retiredroca.remoteaccessterminal.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.NetworkPermissions;
import com.retiredroca.remoteaccessterminal.TerminalChunkLoader;
import com.retiredroca.remoteaccessterminal.TerminalLinks;
import com.retiredroca.remoteaccessterminal.TerminalLinksAccess;
import com.retiredroca.remoteaccessterminal.TerminalTravel;
import com.retiredroca.remoteaccessterminal.block.TerminalBlock;
import com.retiredroca.remoteaccessterminal.config.TerminalSettings;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;

/**
 * The {@code /remoteaccess} command tree.
 *
 * <p>The tree lives here so both loaders register the same commands; each loader only wires it into
 * its own dispatcher. Terminals are addressed by {@code <color> <x> <y> <z>}; because links are
 * stored per dimension and the arguments carry no dimension, a lookup prefers the executor's current
 * dimension and otherwise falls back to a record with that position in any dimension. Invites and
 * the open/private flag live in the API ({@link NetworkPermissions}); ownership and chunk tickets are
 * handled through {@link ContainerOwnership} and {@link TerminalChunkLoader}.
 */
public final class RemoteAccessCommands {
    private RemoteAccessCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGESTIONS = (context, builder) -> {
        for (DyeColor color : DyeColor.values()) {
            builder.suggest(color.getName());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> X_SUGGESTIONS = (context, builder) -> {
        Set<Integer> seen = new HashSet<>();
        for (TerminalLinks.Link link : suggestedLinks(context)) {
            if (seen.add(link.pos().getX())) {
                builder.suggest(link.pos().getX());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> Y_SUGGESTIONS = (context, builder) -> {
        Integer x = optionalInt(context, "x");
        Set<Integer> seen = new HashSet<>();
        for (TerminalLinks.Link link : suggestedLinks(context)) {
            if ((x == null || link.pos().getX() == x) && seen.add(link.pos().getY())) {
                builder.suggest(link.pos().getY());
            }
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> Z_SUGGESTIONS = (context, builder) -> {
        Integer x = optionalInt(context, "x");
        Integer y = optionalInt(context, "y");
        Set<Integer> seen = new HashSet<>();
        for (TerminalLinks.Link link : suggestedLinks(context)) {
            if ((x == null || link.pos().getX() == x) && (y == null || link.pos().getY() == y)
                    && seen.add(link.pos().getZ())) {
                builder.suggest(link.pos().getZ());
            }
        }
        return builder.buildFuture();
    };

    /** Builds the {@code /remoteaccess} tree; the caller registers it with its loader's dispatcher. */
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("remoteaccess")
                .then(Commands.literal("invite")
                        .requires(RemoteAccessCommands::canInvite)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(RemoteAccessCommands::invite)))
                .then(Commands.literal("uninvite")
                        .requires(RemoteAccessCommands::canInvite)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(RemoteAccessCommands::uninvite)))
                .then(Commands.literal("list")
                        .requires(RemoteAccessCommands::canInvite)
                        .executes(RemoteAccessCommands::listInvites))
                .then(Commands.literal("name")
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                                .executes(RemoteAccessCommands::rename)))))))
                .then(Commands.literal("global")
                        .then(Commands.literal("on")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .then(coordinates(context -> setGlobal(context, true)))))
                        .then(Commands.literal("off")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .then(coordinates(context -> setGlobal(context, false))))))
                .then(Commands.literal("unlink")
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                .then(coordinates(RemoteAccessCommands::unlink))))
                .then(Commands.literal("unlinkall")
                        .requires(RemoteAccessCommands::canAdmin)
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                .executes(RemoteAccessCommands::unlinkAll)))
                .then(Commands.literal("assign")
                        .requires(RemoteAccessCommands::canAdmin)
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .then(coordinates(RemoteAccessCommands::assign)))))
                .then(Commands.literal("goto")
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                .then(coordinates(RemoteAccessCommands::gotoTerminal))))
                .then(Commands.literal("admin")
                        .requires(RemoteAccessCommands::canAdmin)
                        .then(Commands.literal("list")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .executes(context -> adminList(context, false))))
                        .then(Commands.literal("ghosts")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .executes(context -> adminList(context, true))))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .then(coordinates(RemoteAccessCommands::adminDelete))))
                        .then(Commands.literal("deleteall")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .executes(RemoteAccessCommands::adminDeleteAll)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .then(coordinates(RemoteAccessCommands::adminRemove))))
                        .then(Commands.literal("removeall")
                                .then(Commands.argument("color", StringArgumentType.word()).suggests(COLOR_SUGGESTIONS)
                                        .executes(RemoteAccessCommands::adminRemoveAll)))
                        .then(Commands.literal("chunkloader")
                                .then(Commands.literal("list")
                                        .executes(RemoteAccessCommands::adminChunkLoaderList))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .suggests(COLOR_SUGGESTIONS)
                                                .then(coordinates(RemoteAccessCommands::adminChunkLoaderClear))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> coordinates(
            Command<CommandSourceStack> command) {
        return Commands.argument("x", IntegerArgumentType.integer()).suggests(X_SUGGESTIONS)
                .then(Commands.argument("y", IntegerArgumentType.integer()).suggests(Y_SUGGESTIONS)
                        .then(Commands.argument("z", IntegerArgumentType.integer()).suggests(Z_SUGGESTIONS)
                                .executes(command)));
    }

    private static boolean canInvite(CommandSourceStack source) {
        return source.hasPermission(TerminalSettings.getInvitePermissionLevel());
    }

    private static boolean canAdmin(CommandSourceStack source) {
        return source.hasPermission(2);
    }

    private static List<TerminalLinks.Link> suggestedLinks(CommandContext<CommandSourceStack> context) {
        DyeColor color = parseColor(context);
        if (color == null) {
            return List.of();
        }
        return TerminalLinksAccess.get(context.getSource().getLevel()).links().links(color);
    }

    private static DyeColor parseColor(CommandContext<CommandSourceStack> context) {
        return DyeColor.byName(StringArgumentType.getString(context, "color"), null);
    }

    private static Integer optionalInt(CommandContext<CommandSourceStack> context, String name) {
        try {
            return context.getArgument(name, Integer.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BlockPos blockPos(CommandContext<CommandSourceStack> context) {
        return new BlockPos(IntegerArgumentType.getInteger(context, "x"), IntegerArgumentType.getInteger(context, "y"),
                IntegerArgumentType.getInteger(context, "z"));
    }

    /** A player executor, or null after sending the shared "players only" failure. */
    private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.player_only"));
        }
        return player;
    }

    /** The record matching {@code color} and {@code pos}, preferring the executor's dimension. */
    private static TerminalLinks.Link findLink(CommandContext<CommandSourceStack> context, DyeColor color,
            BlockPos pos) {
        TerminalLinks links = links(context);
        TerminalLinks.Link local = links.find(color, context.getSource().getLevel().dimension(), pos);
        if (local != null) {
            return local;
        }
        for (TerminalLinks.Link link : links.links(color)) {
            if (link.pos().equals(pos)) {
                return link;
            }
        }
        return null;
    }

    private static TerminalLinks links(CommandContext<CommandSourceStack> context) {
        return TerminalLinksAccess.get(context.getSource().getLevel()).links();
    }

    private static TerminalLinksAccess access(CommandContext<CommandSourceStack> context) {
        return TerminalLinksAccess.get(context.getSource().getLevel());
    }

    private static ServerLevel linkLevel(CommandContext<CommandSourceStack> context, TerminalLinks.Link link) {
        return context.getSource().getServer().getLevel(link.dimension());
    }

    private static void releaseTicket(MinecraftServer server, TerminalLinks.Link link) {
        if (!link.isChunkLoader()) {
            return;
        }
        ServerLevel level = server.getLevel(link.dimension());
        if (level != null) {
            TerminalChunkLoader.deactivate(level, link);
        } else {
            link.setChunkLoader(false);
        }
    }

    private static boolean failUnknownColor(CommandContext<CommandSourceStack> context, DyeColor color) {
        if (color != null) {
            return false;
        }
        context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.unknown_color",
                StringArgumentType.getString(context, "color")));
        return true;
    }

    private static boolean failUnknownTerminal(CommandContext<CommandSourceStack> context, DyeColor color,
            TerminalLinks.Link link, BlockPos pos) {
        if (link != null) {
            return false;
        }
        context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.unknown_terminal",
                color.getName(), pos.getX(), pos.getY(), pos.getZ()));
        return true;
    }

    private static boolean failDimensionMissing(CommandContext<CommandSourceStack> context, ServerLevel level) {
        if (level != null) {
            return false;
        }
        context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.dimension_missing"));
        return true;
    }

    private static boolean failNoEdit(CommandContext<CommandSourceStack> context, ServerLevel level, BlockPos pos,
            ServerPlayer player) {
        if (level != null && NetworkPermissions.canEdit(level, pos, player)) {
            return false;
        }
        context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.no_edit"));
        return true;
    }

    private static int invite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = requirePlayer(context);
        if (owner == null) {
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        if (owner.getUUID().equals(target.getUUID())) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.invite_self"));
            return 0;
        }
        ServerLevel level = owner.serverLevel();
        String targetName = target.getGameProfile().getName();
        if (NetworkPermissions.isInvited(level, owner.getUUID(), target.getUUID())) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.already_invited",
                    targetName));
            return 0;
        }
        NetworkPermissions.invite(level, owner.getUUID(), target.getUUID());
        context.getSource().sendSuccess(() -> Component.translatable("message.remote_access_terminal.invited",
                targetName), false);
        return 1;
    }

    private static int uninvite(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer owner = requirePlayer(context);
        if (owner == null) {
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        if (owner.getUUID().equals(target.getUUID())) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.uninvite_self"));
            return 0;
        }
        ServerLevel level = owner.serverLevel();
        String targetName = target.getGameProfile().getName();
        if (!NetworkPermissions.uninvite(level, owner.getUUID(), target.getUUID())) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.not_invited",
                    targetName));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("message.remote_access_terminal.uninvited",
                targetName), false);
        return 1;
    }

    private static int listInvites(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer owner = requirePlayer(context);
        if (owner == null) {
            return 0;
        }
        Set<UUID> invited = NetworkPermissions.invitesOf(owner.serverLevel(), owner.getUUID());
        if (invited.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.remote_access_terminal.invites_none"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("message.remote_access_terminal.invites_header"), false);
        for (UUID uuid : invited) {
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(uuid);
            Component name = Component.literal(online != null ? online.getGameProfile().getName() : uuid.toString());
            source.sendSuccess(() -> Component.translatable("message.remote_access_terminal.invites_entry", name),
                    false);
        }
        return invited.size();
    }

    private static int rename(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (failUnknownTerminal(context, color, link, pos)) {
            return 0;
        }
        ServerLevel level = linkLevel(context, link);
        if (failDimensionMissing(context, level) || failNoEdit(context, level, link.pos(), player)) {
            return 0;
        }
        String cleaned = TerminalLinks.cleanName(StringArgumentType.getString(context, "name"));
        TerminalLinksAccess access = access(context);
        if (access.links().setName(color, link.dimension(), link.pos(), cleaned)) {
            access.setDirty();
        }
        if (cleaned == null) {
            context.getSource().sendSuccess(
                    () -> Component.translatable("message.remote_access_terminal.name_cleared"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("message.remote_access_terminal.renamed",
                    cleaned), false);
        }
        return 1;
    }

    private static int setGlobal(CommandContext<CommandSourceStack> context, boolean open) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (failUnknownTerminal(context, color, link, pos)) {
            return 0;
        }
        ServerLevel level = linkLevel(context, link);
        if (failDimensionMissing(context, level) || failNoEdit(context, level, link.pos(), player)) {
            return 0;
        }
        NetworkPermissions.setOpen(level, link.pos(), open);
        String key = open ? "message.remote_access_terminal.open_set" : "message.remote_access_terminal.private_set";
        context.getSource().sendSuccess(() -> Component.translatable(key, color.getName(), pos.getX(), pos.getY(),
                pos.getZ()), false);
        return 1;
    }

    private static int unlink(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (failUnknownTerminal(context, color, link, pos)) {
            return 0;
        }
        ServerLevel level = linkLevel(context, link);
        if (failDimensionMissing(context, level) || failNoEdit(context, level, link.pos(), player)) {
            return 0;
        }
        releaseTicket(context.getSource().getServer(), link);
        TerminalLinksAccess access = access(context);
        if (access.links().remove(color, link.dimension(), link.pos())) {
            access.setDirty();
        }
        context.getSource().sendSuccess(() -> Component.translatable("message.remote_access_terminal.unlinked",
                color.getName(), pos.getX(), pos.getY(), pos.getZ()), false);
        return 1;
    }

    private static int unlinkAll(CommandContext<CommandSourceStack> context) {
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        TerminalLinksAccess access = access(context);
        List<TerminalLinks.Link> records = new ArrayList<>(links(context).links(color));
        int removed = 0;
        for (TerminalLinks.Link link : records) {
            releaseTicket(server, link);
            if (access.links().remove(color, link.dimension(), link.pos())) {
                removed++;
            }
        }
        if (removed > 0) {
            access.setDirty();
        }
        int count = removed;
        source.sendSuccess(() -> Component.translatable("message.remote_access_terminal.unlinked_all", count,
                color.getName()), removed == 0);
        return removed;
    }

    private static int assign(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (failUnknownTerminal(context, color, link, pos)) {
            return 0;
        }
        ServerLevel level = linkLevel(context, link);
        if (failDimensionMissing(context, level)) {
            return 0;
        }
        ContainerOwnership.setOwner(level, link.pos(), target.getUUID(), target.getGameProfile().getName());
        TerminalLinksAccess access = access(context);
        access.links().setOwner(color, link.dimension(), link.pos(), target.getUUID());
        access.setDirty();
        context.getSource().sendSuccess(() -> Component.translatable("message.remote_access_terminal.assigned",
                color.getName(), pos.getX(), pos.getY(), pos.getZ(), target.getGameProfile().getName()), false);
        return 1;
    }

    private static int gotoTerminal(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = requirePlayer(context);
        if (player == null) {
            return 0;
        }
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (failUnknownTerminal(context, color, link, pos)) {
            return 0;
        }
        ServerLevel level = linkLevel(context, link);
        if (failDimensionMissing(context, level)) {
            return 0;
        }
        if (!NetworkPermissions.canUse(level, link.pos(), player)) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.no_use"));
            return 0;
        }
        if (!TerminalTravel.teleport(player, player.serverLevel(), player.blockPosition(), link)) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.goto_failed"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("message.remote_access_terminal.goto_ok",
                color.getName(), pos.getX(), pos.getY(), pos.getZ()), false);
        return 1;
    }

    private static int adminList(CommandContext<CommandSourceStack> context, boolean onlyGhosts) {
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        List<TerminalLinks.Link> records = new ArrayList<>();
        for (TerminalLinks.Link link : links(context).links(color)) {
            if (onlyGhosts && !isGhost(server, color, link)) {
                continue;
            }
            records.add(link);
        }
        if (records.isEmpty()) {
            String key = onlyGhosts ? "message.remote_access_terminal.admin_ghosts_empty"
                    : "message.remote_access_terminal.admin_list_empty";
            source.sendSuccess(() -> Component.translatable(key, color.getName()), false);
            return 0;
        }
        String header = onlyGhosts ? "message.remote_access_terminal.admin_ghosts_header"
                : "message.remote_access_terminal.admin_list_header";
        source.sendSuccess(() -> Component.translatable(header, color.getName(), records.size()), false);
        for (TerminalLinks.Link link : records) {
            Component line = entry(source, color, link, onlyGhosts && isGhost(server, color, link));
            source.sendSuccess(() -> line, false);
        }
        return records.size();
    }

    /** Lists every held lease and queued request, with holder and remaining time. */
    private static int adminChunkLoaderList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        TerminalLinks links = links(context);
        List<TerminalLinks.Entry> entries = new ArrayList<>();
        for (TerminalLinks.Entry entry : links.allEntries()) {
            if (entry.link().holdsChunkLoader() || entry.link().isChunkLoaderQueued()) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(
                    "message.remote_access_terminal.admin_chunkloader_empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "message.remote_access_terminal.admin_chunkloader_header", entries.size()), false);
        for (TerminalLinks.Entry entry : entries) {
            TerminalLinks.Link link = entry.link();
            Component state = chunkLoaderState(links, link);
            Component line = Component.translatable("message.remote_access_terminal.admin_chunkloader_entry",
                    entry.color().getName(), link.pos().getX(), link.pos().getY(), link.pos().getZ(),
                    link.dimension().location().toString(), ownerLabel(source, TerminalLinks.holderOf(link)),
                    state);
            source.sendSuccess(() -> line, false);
        }
        return entries.size();
    }

    /** Releases a held lease or drops a queued request, promoting the next request in line. */
    private static int adminChunkLoaderClear(CommandContext<CommandSourceStack> context) {
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks links = links(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (link == null || (!link.holdsChunkLoader() && !link.isChunkLoaderQueued())) {
            context.getSource().sendFailure(Component.translatable(
                    "message.remote_access_terminal.admin_chunkloader_none", pos.getX(), pos.getY(), pos.getZ()));
            return 0;
        }
        Component previous = chunkLoaderState(links, link);
        ServerLevel level = linkLevel(context, link);
        if (level != null) {
            TerminalChunkLoader.clear(level, link);
        } else {
            link.setChunkLoader(false);
            link.setChunkLoaderHolder(null);
            link.setChunkLoaderUntil(0);
            link.setChunkLoaderQueuedSince(0);
        }
        access(context).setDirty();
        context.getSource().sendSuccess(() -> Component.translatable(
                "message.remote_access_terminal.admin_chunkloader_cleared", color.getName(), pos.getX(), pos.getY(),
                pos.getZ(), previous), true);
        return 1;
    }

    /** Describes a link's chunk-loader state: time left, no expiry, or its queue position. */
    private static Component chunkLoaderState(TerminalLinks links, TerminalLinks.Link link) {
        if (!link.holdsChunkLoader()) {
            return Component.translatable(
                    "message.remote_access_terminal.admin_chunkloader_state_queued", links.queuePosition(link));
        }
        if (link.chunkLoaderUntil() <= 0) {
            return Component.translatable(
                    "message.remote_access_terminal.admin_chunkloader_state_unlimited");
        }
        return Component.translatable("message.remote_access_terminal.admin_chunkloader_state_active",
                TerminalChunkLoader.remainingLabel(link.chunkLoaderUntil()));
    }

    private static int adminDelete(CommandContext<CommandSourceStack> context) {
        return deleteRecords(context, false);
    }
    private static int adminDeleteAll(CommandContext<CommandSourceStack> context) {
        return deleteAllRecords(context, false);
    }

    private static int adminRemove(CommandContext<CommandSourceStack> context) {
        return deleteRecords(context, true);
    }

    private static int adminRemoveAll(CommandContext<CommandSourceStack> context) {
        return deleteAllRecords(context, true);
    }

    /**
     * Drops a single record. The chunk ticket is always released: once the record is gone nothing is
     * left that could ever release it, so skipping that would strand the loaded chunks. Only the
     * API's owner record is conditional, on {@code withOwnership}.
     */
    private static int deleteRecords(CommandContext<CommandSourceStack> context, boolean withOwnership) {
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        BlockPos pos = blockPos(context);
        TerminalLinks.Link link = findLink(context, color, pos);
        if (link == null) {
            context.getSource().sendFailure(Component.translatable("message.remote_access_terminal.admin_nothing",
                    color.getName(), pos.getX(), pos.getY(), pos.getZ()));
            return 0;
        }
        ServerLevel level = linkLevel(context, link);
        releaseTicket(context.getSource().getServer(), link);
        if (withOwnership && level != null) {
            ContainerOwnership.clearOwner(level, link.pos());
        }
        TerminalLinksAccess access = access(context);
        if (access.links().remove(color, link.dimension(), link.pos())) {
            access.setDirty();
        }
        String key = withOwnership ? "message.remote_access_terminal.admin_removed"
                : "message.remote_access_terminal.admin_deleted";
        context.getSource().sendSuccess(() -> Component.translatable(key, color.getName(), pos.getX(), pos.getY(),
                pos.getZ()), true);
        return 1;
    }

    /** Drops every record of the colour; see {@link #deleteRecords} for {@code withOwnership}. */
    private static int deleteAllRecords(CommandContext<CommandSourceStack> context, boolean withOwnership) {
        DyeColor color = parseColor(context);
        if (failUnknownColor(context, color)) {
            return 0;
        }
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        TerminalLinksAccess access = access(context);
        List<TerminalLinks.Link> records = new ArrayList<>(links(context).links(color));
        int removed = 0;
        for (TerminalLinks.Link link : records) {
            ServerLevel level = server.getLevel(link.dimension());
            releaseTicket(server, link);
            if (withOwnership && level != null) {
                ContainerOwnership.clearOwner(level, link.pos());
            }
            if (access.links().remove(color, link.dimension(), link.pos())) {
                removed++;
            }
        }
        if (removed > 0) {
            access.setDirty();
        }
        int count = removed;
        String key = withOwnership ? "message.remote_access_terminal.admin_removed_all"
                : "message.remote_access_terminal.admin_deleted_all";
        source.sendSuccess(() -> Component.translatable(key, count, color.getName()), removed == 0);
        return removed;
    }

    private static Component entry(CommandSourceStack source, DyeColor color, TerminalLinks.Link link,
            boolean ghost) {
        TerminalLinks links = TerminalLinksAccess.get(source.getLevel()).links();
        boolean open = NetworkPermissions.isOpen(source.getLevel(), link.pos());
        Component stateOpen = Component.translatable(
                open ? "gui.remote_access_terminal.open" : "gui.remote_access_terminal.private");
        Component stateChunk = link.isChunkLoader() || link.isChunkLoaderQueued()
                ? chunkLoaderState(links, link)
                : Component.translatable("gui.remote_access_terminal.off");
        Component line = Component.translatable("message.remote_access_terminal.admin_entry", color.getName(),
                link.pos().getX(), link.pos().getY(), link.pos().getZ(), link.dimension().location().toString(),
                link.name() == null ? "-" : link.name(), ownerLabel(source, link.owner()), stateOpen, stateChunk);
        if (ghost) {
            line = line.copy().append(Component.translatable("message.remote_access_terminal.ghost_marker"));
        }
        return line;
    }

    private static String ownerLabel(CommandSourceStack source, UUID owner) {
        if (owner == null) {
            return "-";
        }
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(owner);
        return online != null ? online.getGameProfile().getName() : owner.toString();
    }

    /**
     * True when the record's block is no longer a terminal of its colour. Only confirmed when the
     * chunk is loaded, so an unloaded chunk is never mistaken for a ghost.
     */
    private static boolean isGhost(MinecraftServer server, DyeColor color, TerminalLinks.Link link) {
        ServerLevel level = server.getLevel(link.dimension());
        if (level == null || !level.isLoaded(link.pos())) {
            return false;
        }
        Block block = level.getBlockState(link.pos()).getBlock();
        return !(block instanceof TerminalBlock terminal && terminal.getColor() == color);
    }
}
