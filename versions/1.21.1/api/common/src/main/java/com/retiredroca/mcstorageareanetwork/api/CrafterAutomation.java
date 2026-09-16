package com.retiredroca.mcstorageareanetwork.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;

/**
 * Hooks vanilla Crafters into a network. A linked crafter remembers its crafting pattern and, each
 * cycle, pulls the ingredients it needs from the nearest Storage Terminal's network, crafts, and
 * routes the result (plus any recipe remainders) back into the network. It pauses whenever the
 * network can't supply an ingredient and resumes once it can.
 */
public final class CrafterAutomation {
    /** Chunk radius searched for the Storage Terminal that supplies the crafter. */
    public static final int HOST_SEARCH_CHUNKS = 2;
    private static final int CRAFT_INTERVAL_TICKS = 10;

    public enum Result { LINKED, UNLINKED, NO_PATTERN, NOT_ALLOWED, NOT_A_CRAFTER }

    private CrafterAutomation() {}

    public static boolean isLinked(ServerLevel level, BlockPos pos) {
        return CrafterLinks.pattern(level, pos) != null;
    }

    /** Crouch-click a crafter to link it (capturing its current grid) or unlink it. */
    public static Result toggle(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        CrafterBlockEntity crafter = crafterAt(level, pos);
        if (crafter == null) {
            return Result.NOT_A_CRAFTER;
        }
        if (!canModify(level, pos, player)) {
            return Result.NOT_ALLOWED;
        }
        if (CrafterLinks.pattern(level, pos) != null) {
            CrafterLinks.clear(level, pos);
            return Result.UNLINKED;
        }
        List<ItemStack> pattern = capturePattern(level, crafter);
        if (pattern == null) {
            return Result.NO_PATTERN;
        }
        CrafterLinks.set(level, pos, pattern);
        return Result.LINKED;
    }

    /** Server tick: drives every linked crafter (throttled). */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % CRAFT_INTERVAL_TICKS != 0) {
            return;
        }
        for (Map.Entry<BlockPos, List<ItemStack>> entry : CrafterLinks.all(level).entrySet()) {
            process(level, entry.getKey(), entry.getValue());
        }
    }

    public static Component message(Result result) {
        String key = switch (result) {
            case LINKED -> "message.mc_storage_area_network.crafter_linked";
            case UNLINKED -> "message.mc_storage_area_network.crafter_unlinked";
            case NO_PATTERN -> "message.mc_storage_area_network.crafter_no_pattern";
            case NOT_ALLOWED, NOT_A_CRAFTER -> "message.mc_storage_area_network.crafter_not_allowed";
        };
        return Component.translatable(key);
    }

    // --- internals ----------------------------------------------------------------------

    private static void process(ServerLevel level, BlockPos pos, List<ItemStack> pattern) {
        CrafterBlockEntity crafter = crafterAt(level, pos);
        if (crafter == null) {
            CrafterLinks.clear(level, pos);
            return;
        }
        NetworkHost host = NetworkHostLocator.findNearest(level, pos, HOST_SEARCH_CHUNKS);
        if (host == null) {
            return; // no network in range: paused
        }
        List<ScannedStorage> storages = ItemNetworkServices.scanner().scan(level, host.pos(), host.chunkRadius());
        if (!restock(crafter, pattern, storages)) {
            return; // ingredient missing: paused until the network is restocked
        }
        craft(level, pos, crafter, storages);
    }

    /** Makes sure each pattern slot holds at least one item, pulling missing ones from the network. */
    private static boolean restock(CrafterBlockEntity crafter, List<ItemStack> pattern,
            List<ScannedStorage> storages) {
        for (int slot = 0; slot < CrafterLinks.SIZE && slot < pattern.size(); slot++) {
            ItemStack want = pattern.get(slot);
            if (want.isEmpty()) {
                continue;
            }
            ItemStack have = crafter.getItem(slot);
            if (!have.isEmpty()) {
                if (ItemStack.isSameItemSameComponents(have, want)) {
                    continue;
                }
                return false; // occupied by something else; don't disturb the player's grid
            }
            if (extract(storages, want, 1) <= 0) {
                return false;
            }
            crafter.setItem(slot, want.copyWithCount(1));
        }
        return true;
    }

    private static void craft(ServerLevel level, BlockPos pos, CrafterBlockEntity crafter,
            List<ScannedStorage> storages) {
        CraftingInput input = crafter.asCraftInput();
        Optional<RecipeHolder<CraftingRecipe>> found = CrafterBlock.getPotentialResults(level, input);
        if (found.isEmpty()) {
            return;
        }
        CraftingRecipe recipe = found.get().value();
        ItemStack output = recipe.assemble(input, level.registryAccess());
        List<ItemStack> remainders = recipe.getRemainingItems(input);
        for (int slot = 0; slot < crafter.getContainerSize(); slot++) {
            if (!crafter.getItem(slot).isEmpty()) {
                crafter.removeItem(slot, 1);
            }
            ItemStack remainder = slot < remainders.size() ? remainders.get(slot) : ItemStack.EMPTY;
            if (!remainder.isEmpty()) {
                crafter.setItem(slot, remainder.copy());
            }
        }
        crafter.setChanged();
        insert(level, pos, storages, output);
        for (ItemStack remainder : remainders) {
            insert(level, pos, storages, remainder);
        }
        level.playSound(null, pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private static int extract(List<ScannedStorage> storages, ItemStack item, int max) {
        int taken = 0;
        for (ScannedStorage storage : storages) {
            if (taken >= max) {
                break;
            }
            taken += storage.extract(item, max - taken);
        }
        return taken;
    }

    private static void insert(ServerLevel level, BlockPos refPos, List<ScannedStorage> storages, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = stack.copy();
        for (ScannedStorage storage : StorageRouter.order(level, refPos, storages, remaining)) {
            if (remaining.isEmpty()) {
                break;
            }
            remaining = storage.insert(remaining);
        }
        if (!remaining.isEmpty()) {
            Block.popResource(level, refPos, remaining);
        }
    }

    private static List<ItemStack> capturePattern(ServerLevel level, CrafterBlockEntity crafter) {
        if (CrafterBlock.getPotentialResults(level, crafter.asCraftInput()).isEmpty()) {
            return null;
        }
        List<ItemStack> pattern = new ArrayList<>();
        boolean any = false;
        for (int slot = 0; slot < CrafterLinks.SIZE; slot++) {
            ItemStack stack = crafter.getItem(slot);
            if (!stack.isEmpty()) {
                any = true;
                pattern.add(stack.copyWithCount(1));
            } else {
                pattern.add(ItemStack.EMPTY);
            }
        }
        return any ? pattern : null;
    }

    private static CrafterBlockEntity crafterAt(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter ? crafter : null;
    }

    private static boolean canModify(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (player.hasPermissions(2)) {
            return true;
        }
        return ContainerOwnership.canSee(level, ContainerOwnership.ownerOf(level, pos),
                new ContainerOwnership.Entry(player.getUUID(), player.getGameProfile().getName()));
    }
}
