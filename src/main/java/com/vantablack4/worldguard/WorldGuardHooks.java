package com.vantablack4.worldguard;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class WorldGuardHooks {
    private final WorldGuardService service;

    public WorldGuardHooks(WorldGuardService service) {
        this.service = service;
    }

    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> service.evict(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> VantablackWorldGuardMod.LOGGER.info("Vantablack WorldGuard stopped"));

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }
            return !deny(serverPlayer, WorldGuardFlag.BUILD, world, pos);
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return deny(serverPlayer, WorldGuardFlag.BUILD, world, pos) ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            BlockPos clicked = hitResult.getBlockPos();
            BlockPos placementTarget = clicked.relative(hitResult.getDirection());
            if (player.getItemInHand(hand).getItem() instanceof BlockItem
                && deny(serverPlayer, WorldGuardFlag.BUILD, world, placementTarget)) {
                return InteractionResult.FAIL;
            }
            if (!player.getItemInHand(hand).isEmpty()
                && deny(serverPlayer, WorldGuardFlag.ITEM_USE, world, clicked)) {
                return InteractionResult.FAIL;
            }
            return deny(serverPlayer, WorldGuardFlag.INTERACT, world, clicked) ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.PASS;
            }
            return deny(serverPlayer, WorldGuardFlag.ITEM_USE, world, player.blockPosition()) ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return deny(serverPlayer, WorldGuardFlag.USE_ENTITY, world, entity.blockPosition()) ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            return deny(serverPlayer, WorldGuardFlag.ATTACK_ENTITY, world, entity.blockPosition()) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }

    private boolean deny(ServerPlayer player, WorldGuardFlag flag, Level world, BlockPos pos) {
        return service.deny(player, service.check(player, flag, worldId(world), pos));
    }

    private static String worldId(Level world) {
        return world.dimension().identifier().toString();
    }
}
