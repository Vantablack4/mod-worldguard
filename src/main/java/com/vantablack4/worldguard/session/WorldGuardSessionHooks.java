package com.vantablack4.worldguard.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;
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

    public static boolean allowTeleport(ServerPlayer player, ServerLevel targetLevel, double x, double y, double z) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowTeleport(player, targetLevel, x, y, z);
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
            current,
            player.getScoreboardName()
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
        List<WorldGuardRegion> regions = service.storage().regions();
        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            regions,
            world,
            from,
            to,
            player.getUUID(),
            service.regionGroups(player, regions),
            service.isAdmin(player)
        );
        if (decision.allowed()) {
            return true;
        }

        service.deny(player, decision.decision());
        return false;
    }

    boolean allowTeleport(ServerPlayer player, ServerLevel targetLevel, double x, double y, double z) {
        if (player == null || targetLevel == null) {
            return true;
        }

        String fromWorld = worldId(player.level());
        String targetWorld = worldId(targetLevel);
        BlockPos from = player.blockPosition();
        BlockPos to = BlockPos.containing(x, y, z);
        if (fromWorld.equals(targetWorld) && from.equals(to)) {
            return true;
        }

        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> groups = service.regionGroups(player, regions);
        boolean bypass = service.isAdmin(player);
        ProtectionDecision exitDecision = WorldGuardSessionRules.checkAny(
            regions,
            fromWorld,
            from,
            player.getUUID(),
            groups,
            bypass,
            WorldGuardFlag.EXIT_VIA_TELEPORT,
            WorldGuardFlag.EXIT
        );
        if (service.deny(player, exitDecision)) {
            return false;
        }

        ProtectionDecision entryDecision = WorldGuardSessionRules.checkAny(
            regions,
            targetWorld,
            to,
            player.getUUID(),
            groups,
            bypass,
            WorldGuardFlag.ENTRY
        );
        return !service.deny(player, entryDecision);
    }

    boolean denyAction(ServerPlayer player, BlockPos pos, WorldGuardFlag... flags) {
        if (player == null) {
            return false;
        }

        BlockPos checkPos = pos == null ? player.blockPosition() : pos;
        List<WorldGuardRegion> regions = service.storage().regions();
        ProtectionDecision decision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(player.level()),
            checkPos,
            player.getUUID(),
            service.regionGroups(player, regions),
            service.isAdmin(player),
            flags
        );
        return service.deny(player, decision);
    }

    boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer victim)) {
            return allowNonPlayerDamage(entity, source);
        }

        Entity damageSource = damageSource(source);
        ServerPlayer attacker = damageSource instanceof ServerPlayer sourcePlayer
            ? sourcePlayer
            : null;
        boolean attackerBypass = attacker != null && service.isAdmin(attacker);
        boolean victimBypass = service.isAdmin(victim);
        boolean invincibilityBypassed = attackerBypass || victimBypass;
        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> victimGroups = service.regionGroups(victim, regions);
        if (WorldGuardSessionRules.enabledRegion(
            regions,
            worldId(victim.level()),
            victim.blockPosition(),
            victim.getUUID(),
            victimGroups,
            invincibilityBypassed,
            WorldGuardFlag.INVINCIBILITY
        ).isPresent()) {
            return false;
        }

        if (source != null && source.is(DamageTypes.FALL) && denyAction(victim, victim.blockPosition(), WorldGuardFlag.FALL_DAMAGE)) {
            victim.resetFallDistance();
            return false;
        }

        if (attacker != null && attacker != victim) {
            return !denyPvp(attacker, victim);
        }
        if (damageSource instanceof LivingEntity && damageSource != victim) {
            ProtectionDecision decision = WorldGuardSessionRules.checkAny(
                regions,
                worldId(victim.level()),
                victim.blockPosition(),
                null,
                false,
                WorldGuardFlag.MOB_DAMAGE
            );
            return decision.allowed();
        }
        return true;
    }

    private boolean allowNonPlayerDamage(LivingEntity victim, DamageSource source) {
        if (victim == null) {
            return true;
        }

        Entity damageSource = damageSource(source);
        ServerPlayer attacker = damageSource instanceof ServerPlayer sourcePlayer
            ? sourcePlayer
            : null;
        List<WorldGuardFlag> flags = WorldGuardSessionRules.nonPlayerDamageFlags(
            victim.getType().getCategory(),
            attacker != null,
            damageSource instanceof LivingEntity
        );
        if (flags.isEmpty()) {
            return true;
        }

        List<WorldGuardRegion> regions = service.storage().regions();
        if (attacker != null) {
            ProtectionDecision decision = WorldGuardSessionRules.checkAny(
                regions,
                worldId(victim.level()),
                victim.blockPosition(),
                attacker.getUUID(),
                service.regionGroups(attacker, regions),
                service.isAdmin(attacker),
                flags.toArray(WorldGuardFlag[]::new)
            );
            return !service.deny(attacker, decision);
        }

        ProtectionDecision decision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(victim.level()),
            victim.blockPosition(),
            null,
            false,
            flags.toArray(WorldGuardFlag[]::new)
        );
        return decision.allowed();
    }

    private boolean denyPvp(ServerPlayer attacker, ServerPlayer victim) {
        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> attackerGroups = service.regionGroups(attacker, regions);
        ProtectionDecision victimRegionDecision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(victim.level()),
            victim.blockPosition(),
            attacker.getUUID(),
            attackerGroups,
            service.isAdmin(attacker),
            WorldGuardFlag.PVP
        );
        if (service.deny(attacker, victimRegionDecision)) {
            return true;
        }

        ProtectionDecision attackerRegionDecision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(attacker.level()),
            attacker.blockPosition(),
            attacker.getUUID(),
            attackerGroups,
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

    private static Entity damageSource(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity entity = source.getEntity();
        return entity == null ? source.getDirectEntity() : entity;
    }
}
