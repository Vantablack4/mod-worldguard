package com.vantablack4.worldguard.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardService;

public final class WorldGuardSessionHooks {
    private static volatile WorldGuardSessionRuntime runtime;

    private WorldGuardSessionHooks() {
    }

    public static void configure(WorldGuardService service) {
        runtime = new WorldGuardSessionRuntime(service);
    }

    public static void clear() {
        runtime = null;
    }

    public static void evict(UUID playerUuid) {
        WorldGuardSessionRuntime active = runtime;
        if (active != null) {
            active.evict(playerUuid);
        }
    }

    public static void refresh(ServerPlayer player) {
        WorldGuardSessionRuntime active = runtime;
        if (active != null) {
            active.refresh(player);
        }
    }

    public static boolean allowMovement(ServerPlayer player, Vec3 previousPosition) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowMovement(player, previousPosition);
    }

    public static boolean denyAction(ServerPlayer player, BlockPos pos, WorldGuardFlag... flags) {
        WorldGuardSessionRuntime active = runtime;
        return active != null && active.denyAction(player, pos, flags);
    }

    public static boolean denyItemPickup(ServerPlayer player, Entity itemEntity) {
        return denyAction(player, itemEntity == null ? null : itemEntity.blockPosition(), WorldGuardFlag.ITEM_PICKUP);
    }

    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowDamage(entity, source);
    }
}

final class WorldGuardSessionRuntime {
    private final WorldGuardService service;
    private final Map<UUID, WorldGuardSessionSnapshot> snapshots = new HashMap<>();

    WorldGuardSessionRuntime(WorldGuardService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    void evict(UUID playerUuid) {
        snapshots.remove(playerUuid);
    }

    void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }

        WorldGuardSessionSnapshot current = snapshot(player);
        WorldGuardSessionSnapshot previous = snapshots.put(player.getUUID(), current);
        if (previous == null) {
            return;
        }

        for (WorldGuardSessionMessage message : WorldGuardSessionRules.messagesForTransition(
            service.storage().regions(),
            previous,
            current
        )) {
            player.sendSystemMessage(Component.literal(message.message()).withStyle(ChatFormatting.YELLOW));
        }
    }

    boolean allowMovement(ServerPlayer player, Vec3 previousPosition) {
        if (player == null || previousPosition == null) {
            return true;
        }

        String world = worldId(player.level());
        BlockPos from = BlockPos.containing(previousPosition);
        BlockPos to = player.blockPosition();
        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            service.storage().regions(),
            world,
            from,
            to,
            player.getUUID(),
            service.isAdmin(player)
        );
        if (decision.allowed()) {
            return true;
        }

        service.deny(player, decision.decision());
        return false;
    }

    boolean denyAction(ServerPlayer player, BlockPos pos, WorldGuardFlag... flags) {
        if (player == null) {
            return false;
        }

        BlockPos checkPos = pos == null ? player.blockPosition() : pos;
        ProtectionDecision decision = WorldGuardSessionRules.checkAny(
            service.storage().regions(),
            worldId(player.level()),
            checkPos,
            player.getUUID(),
            service.isAdmin(player),
            flags
        );
        return service.deny(player, decision);
    }

    boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer victim)) {
            return true;
        }

        ServerPlayer attacker = source != null && source.getEntity() instanceof ServerPlayer sourcePlayer
            ? sourcePlayer
            : null;
        boolean attackerBypass = attacker != null && service.isAdmin(attacker);
        boolean victimBypass = service.isAdmin(victim);
        boolean invincibilityBypassed = attackerBypass || victimBypass;
        if (WorldGuardSessionRules.enabledRegion(
            service.storage().regions(),
            worldId(victim.level()),
            victim.blockPosition(),
            victim.getUUID(),
            invincibilityBypassed,
            WorldGuardFlag.INVINCIBILITY
        ).isPresent()) {
            return false;
        }

        if (source != null && source.is(DamageTypes.FALL) && denyAction(victim, victim.blockPosition(), WorldGuardFlag.FALL_DAMAGE)) {
            victim.resetFallDistance();
            return false;
        }

        return attacker == null || attacker == victim || !denyPvp(attacker, victim);
    }

    private boolean denyPvp(ServerPlayer attacker, ServerPlayer victim) {
        ProtectionDecision victimRegionDecision = WorldGuardSessionRules.checkAny(
            service.storage().regions(),
            worldId(victim.level()),
            victim.blockPosition(),
            attacker.getUUID(),
            service.isAdmin(attacker),
            WorldGuardFlag.PVP
        );
        if (service.deny(attacker, victimRegionDecision)) {
            return true;
        }

        ProtectionDecision attackerRegionDecision = WorldGuardSessionRules.checkAny(
            service.storage().regions(),
            worldId(attacker.level()),
            attacker.blockPosition(),
            attacker.getUUID(),
            service.isAdmin(attacker),
            WorldGuardFlag.PVP
        );
        return service.deny(attacker, attackerRegionDecision);
    }

    private WorldGuardSessionSnapshot snapshot(ServerPlayer player) {
        return WorldGuardSessionRules.snapshot(
            service.storage().regions(),
            worldId(player.level()),
            player.blockPosition()
        );
    }

    private static String worldId(Level world) {
        return world.dimension().identifier().toString();
    }
}
