package com.retiredroca.mcstorageareanetwork.fabric;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.mcstorageareanetwork.api.BreakProtection;
import com.retiredroca.mcstorageareanetwork.api.ContainerOwnership;
import com.retiredroca.mcstorageareanetwork.api.CrafterAutomation;
import com.retiredroca.mcstorageareanetwork.api.ItemNetworkServices;
import com.retiredroca.mcstorageareanetwork.api.ItemSource;
import com.retiredroca.mcstorageareanetwork.api.ItemSourceRegistry;
import com.retiredroca.mcstorageareanetwork.api.NetworkAwareness;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ConfirmBreakPayload;
import com.retiredroca.mcstorageareanetwork.api.ProtectionPackets.ForceBreakPayload;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionContext;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionHooks;
import com.retiredroca.mcstorageareanetwork.api.interaction.InteractionType;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

/**
 * Fabric entrypoint for the MC Storage Area Network API. Installs the platform scanner and loads every
 * {@code mc_storage_area_network} entrypoint as a registered {@link ItemSource}.
 */
public class McStorageAreaNetwork implements ModInitializer {
    public static final String MODID = "mc_storage_area_network";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String SOURCE_ENTRYPOINT = "mc_storage_area_network";

    @Override
    public void onInitialize() {
        NetworkAwareness.setPresenceTest(mod -> FabricLoader.getInstance().isModLoaded(mod.id()));
        ShulkerBoxConfig.load();
        ItemNetworkServices.setScanner(new FabricItemScanner());
        ItemNetworkServices.setConfigService(ShulkerBoxConfig::setContainerExcluded);
        ItemSourceRegistry.register(new ShulkerItemSource());
        ItemSourceRegistry.addHiddenItemFilter(ShulkerBoxConfig::isRawShulkerBoxHidden);
        InteractionHooks.registerBuiltins();
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
            if (level instanceof ServerLevel serverLevel && entity instanceof Container) {
                ContainerOwnership.clearOwner(serverLevel, pos);
            }
        });

        // Break protection for the mod set: owner/team may break; operators must confirm.
        PayloadTypeRegistry.playC2S().register(ForceBreakPayload.TYPE, ForceBreakPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ConfirmBreakPayload.TYPE, ConfirmBreakPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ForceBreakPayload.TYPE, (payload, context) ->
                context.player().server.execute(() -> {
                    if (context.player().hasPermissions(2)
                            && context.player().level() instanceof ServerLevel serverLevel) {
                        BreakProtection.forceBreak(serverLevel, payload.pos());
                    }
                }));
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> {
            if (!(level instanceof ServerLevel serverLevel) || !BreakProtection.isProtected(serverLevel, pos)) {
                return true;
            }
            if (BreakProtection.needsOpConfirmation(serverLevel, pos, player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ServerPlayNetworking.send(serverPlayer, new ConfirmBreakPayload(pos));
                }
                return false;
            }
            if (!BreakProtection.canBreak(serverLevel, pos, player)) {
                player.displayClientMessage(
                        Component.translatable("block.mc_storage_area_network.protected"), true);
                return false;
            }
            return true;
        });
        for (ItemSource source : FabricLoader.getInstance().getEntrypoints(SOURCE_ENTRYPOINT, ItemSource.class)) {
            ItemSourceRegistry.register(source);
            LOGGER.info("Registered item source: {}", source.getSourceName());
        }

        // Drive linked crafters every tick (the automation throttles itself).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                CrafterAutomation.tick(level);
            }
        });

        // Right-click dispatch: mods register hooks; the API built-ins (crafter link toggle, container
        // exclusion toggle) run last. Item-on-block is offered first when the main hand holds an item.
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getMainHandItem();
            InteractionContext context = new InteractionContext(player, level, hand, stack,
                    hitResult.getBlockPos(), hitResult.getDirection(), hitResult.getLocation(),
                    player.isSecondaryUseActive(), level.isClientSide);
            boolean handled = false;
            if (!stack.isEmpty()) {
                handled = dispatch(level.isClientSide, InteractionType.ITEM_ON_BLOCK, context);
            }
            if (!handled) {
                handled = dispatch(level.isClientSide, InteractionType.BLOCK_USE, context);
            }
            return handled ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });

        // Using an item while aiming at air (the pick is a genuine miss) is offered as AIR_USE.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (hand != InteractionHand.MAIN_HAND || InteractionHooks.isEmpty(InteractionType.AIR_USE)) {
                return InteractionResultHolder.pass(stack);
            }
            HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
            if (hit.getType() != HitResult.Type.MISS) {
                return InteractionResultHolder.pass(stack);
            }
            InteractionContext context = new InteractionContext(player, level, hand, stack,
                    null, null, null, player.isSecondaryUseActive(), level.isClientSide);
            boolean handled = dispatch(level.isClientSide, InteractionType.AIR_USE, context);
            return handled ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
        });
    }

    private static boolean dispatch(boolean clientSide, InteractionType type, InteractionContext context) {
        return clientSide
                ? InteractionHooks.dispatchClient(type, context)
                : InteractionHooks.dispatchServer(type, context);
    }
}
