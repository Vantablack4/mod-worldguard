package com.vantablack4.worldguard;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.vantablack4.worldguard.hook.WorldGuardProtectionHooks;
import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

public final class WorldGuardHooks {
    private final WorldGuardService service;

    public WorldGuardHooks(WorldGuardService service) {
        this.service = service;
        WorldGuardProtectionHooks.configure(service);
        WorldGuardSessionHooks.configure(service);
    }

    public void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> WorldGuardSessionHooks.refresh(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            service.evict(handler.getPlayer().getUUID());
            WorldGuardSessionHooks.evict(handler.getPlayer().getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            WorldGuardSessionHooks.clear();
            VantablackWorldGuardMod.LOGGER.info("Vantablack WorldGuard stopped");
        });
        ServerTickEvents.END_SERVER_TICK.register(server ->
            server.getPlayerList().getPlayers().forEach(WorldGuardSessionHooks::refresh)
        );

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }
            return !denyAny(serverPlayer, world, pos, WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_BREAK);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return denyAny(serverPlayer, world, pos, WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_BREAK)
                ? InteractionResult.FAIL
                : InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            BlockPos clicked = hitResult.getBlockPos();
            BlockPos placementTarget = clicked.relative(hitResult.getDirection());
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BlockItem
                && denyAny(serverPlayer, world, placementTarget, WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE)) {
                return InteractionResult.FAIL;
            }
            BlockPos bucketTarget = WorldGuardProtectionHooks.bucketMutationTarget(stack, clicked, hitResult.getDirection());
            if (bucketTarget != null
                && denyAny(serverPlayer, world, bucketTarget, WorldGuardProtectionHooks.bucketMutationFlags(world, stack, clicked))) {
                return InteractionResult.FAIL;
            }
            if (WorldGuardProtectionHooks.isContainerAccess(world, clicked)
                && denyAny(serverPlayer, world, clicked, WorldGuardFlag.CHEST_ACCESS, WorldGuardFlag.USE)) {
                return InteractionResult.FAIL;
            }
            if (!stack.isEmpty()
                && denyAny(serverPlayer, world, clicked, WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE)) {
                return InteractionResult.FAIL;
            }
            return denyAny(serverPlayer, world, clicked, WorldGuardFlag.INTERACT, WorldGuardFlag.USE)
                ? InteractionResult.FAIL
                : InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() == Items.ENDER_PEARL
                && denyAny(serverPlayer, world, player.blockPosition(), WorldGuardFlag.ENDERPEARL, WorldGuardFlag.EXIT_VIA_TELEPORT)) {
                return InteractionResult.FAIL;
            }
            if (stack.getItem() == Items.CHORUS_FRUIT
                && denyAny(serverPlayer, world, player.blockPosition(), WorldGuardFlag.CHORUS_TELEPORT, WorldGuardFlag.EXIT_VIA_TELEPORT)) {
                return InteractionResult.FAIL;
            }
            return denyAny(serverPlayer, world, player.blockPosition(), WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE)
                ? InteractionResult.FAIL
                : InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (WorldGuardProtectionHooks.isEntityContainerAccess(entity)
                && denyAny(serverPlayer, world, entity.blockPosition(), WorldGuardFlag.CHEST_ACCESS, WorldGuardFlag.USE_ENTITY)) {
                return InteractionResult.FAIL;
            }
            return denyAny(serverPlayer, world, entity.blockPosition(), WorldGuardFlag.USE_ENTITY)
                ? InteractionResult.FAIL
                : InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return denyAny(serverPlayer, world, entity.blockPosition(), WorldGuardFlag.ATTACK_ENTITY)
                ? InteractionResult.FAIL
                : InteractionResult.PASS;
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, player, params) ->
            !WorldGuardSessionHooks.denyAction(player, player.blockPosition(), WorldGuardFlag.SEND_CHAT)
        );

        EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) -> {
            if (player instanceof ServerPlayer serverPlayer
                && WorldGuardSessionHooks.denyAction(serverPlayer, sleepingPos, WorldGuardFlag.SLEEP)) {
                return Player.BedSleepingProblem.OTHER_PROBLEM;
            }
            return null;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(WorldGuardSessionHooks::allowDamage);
    }

    private boolean denyAny(ServerPlayer player, Level world, BlockPos pos, WorldGuardFlag... flags) {
        return service.deny(player, service.checkAny(player, worldId(world), pos, flags));
    }

    private static String worldId(Level world) {
        return world.dimension().identifier().toString();
    }
}
