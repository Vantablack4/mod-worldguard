package com.vantablack4.worldguard.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardPolicy;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.model.RegionQueryEngine;

public final class WorldGuardSessionRules {
    private WorldGuardSessionRules() {
    }

    public static WorldGuardSessionSnapshot snapshot(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos
    ) {
        if (world == null || pos == null) {
            return new WorldGuardSessionSnapshot(world, pos, List.of());
        }

        List<String> regionIds = RegionQueryEngine.applicableRegions(regions, world, pos.getX(), pos.getY(), pos.getZ())
            .stream()
            .filter(region -> !region.global())
            .map(WorldGuardRegion::id)
            .toList();
        return new WorldGuardSessionSnapshot(world, pos, regionIds);
    }

    public static WorldGuardMovementDecision movementDecision(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos from,
        BlockPos to,
        UUID playerUuid,
        boolean bypass
    ) {
        return movementDecision(regions, world, from, to, playerUuid, List.of(), bypass);
    }

    public static WorldGuardMovementDecision movementDecision(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos from,
        BlockPos to,
        UUID playerUuid,
        Collection<String> playerGroups,
        boolean bypass
    ) {
        if (bypass || world == null || from == null || to == null || from.equals(to)) {
            return WorldGuardMovementDecision.allow();
        }

        List<WorldGuardRegion> regionList = regionList(regions);
        WorldGuardSessionSnapshot fromSnapshot = snapshot(regionList, world, from);
        WorldGuardSessionSnapshot toSnapshot = snapshot(regionList, world, to);
        if (!changedRegions(fromSnapshot, toSnapshot)) {
            return WorldGuardMovementDecision.allow();
        }

        if (leftRegion(fromSnapshot, toSnapshot)) {
            ProtectionDecision exitDecision = checkAny(
                regionList,
                world,
                from,
                playerUuid,
                playerGroups,
                false,
                WorldGuardFlag.EXIT
            );
            if (!exitDecision.allowed()) {
                return WorldGuardMovementDecision.deny(exitDecision);
            }
        }

        if (enteredRegion(fromSnapshot, toSnapshot)) {
            ProtectionDecision entryDecision = checkAny(
                regionList,
                world,
                to,
                playerUuid,
                playerGroups,
                false,
                WorldGuardFlag.ENTRY
            );
            if (!entryDecision.allowed()) {
                return WorldGuardMovementDecision.deny(entryDecision);
            }
        }

        return WorldGuardMovementDecision.allow();
    }

    public static ProtectionDecision checkAny(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos,
        UUID playerUuid,
        boolean bypass,
        WorldGuardFlag... flags
    ) {
        return checkAny(regions, world, pos, playerUuid, List.of(), bypass, flags);
    }

    public static ProtectionDecision checkAny(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos,
        UUID playerUuid,
        Collection<String> playerGroups,
        boolean bypass,
        WorldGuardFlag... flags
    ) {
        if (bypass || world == null || pos == null || flags == null) {
            return ProtectionDecision.allow();
        }

        List<WorldGuardRegion> regionList = regionList(regions);
        for (WorldGuardFlag flag : flags) {
            if (flag == null) {
                continue;
            }
            ProtectionDecision decision = WorldGuardPolicy.evaluate(
                regionList,
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                flag,
                playerUuid,
                playerGroups,
                false
            );
            if (!decision.allowed()) {
                return decision;
            }
        }
        return ProtectionDecision.allow();
    }

    public static Optional<String> enabledRegion(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos,
        UUID playerUuid,
        boolean bypass,
        WorldGuardFlag flag
    ) {
        return enabledRegion(regions, world, pos, playerUuid, List.of(), bypass, flag);
    }

    public static Optional<String> enabledRegion(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos,
        UUID playerUuid,
        Collection<String> playerGroups,
        boolean bypass,
        WorldGuardFlag flag
    ) {
        if (bypass || world == null || pos == null || flag == null) {
            return Optional.empty();
        }

        RegionQueryEngine.FlagEvaluation evaluation = RegionQueryEngine.queryState(
            regionList(regions),
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            flag,
            playerUuid,
            playerGroups
        );
        return evaluation.state() == FlagState.ALLOW ? Optional.of(evaluation.regionId()) : Optional.empty();
    }

    public static List<WorldGuardFlag> nonPlayerDamageFlags(
        MobCategory victimCategory,
        boolean playerAttacker,
        boolean nonPlayerAttacker
    ) {
        if (playerAttacker) {
            List<WorldGuardFlag> flags = new ArrayList<>();
            if (protectedAnimalCategory(victimCategory)) {
                flags.add(WorldGuardFlag.DAMAGE_ANIMALS);
            }
            flags.add(WorldGuardFlag.ATTACK_ENTITY);
            return List.copyOf(flags);
        }
        if (nonPlayerAttacker) {
            return List.of(WorldGuardFlag.MOB_DAMAGE);
        }
        return List.of();
    }

    public static List<WorldGuardSessionMessage> messagesForTransition(
        Collection<WorldGuardRegion> regions,
        WorldGuardSessionSnapshot previous,
        WorldGuardSessionSnapshot current
    ) {
        return messagesForTransition(regions, previous, current, "");
    }

    public static List<WorldGuardSessionMessage> messagesForTransition(
        Collection<WorldGuardRegion> regions,
        WorldGuardSessionSnapshot previous,
        WorldGuardSessionSnapshot current,
        String playerName
    ) {
        if (previous == null || current == null) {
            return List.of();
        }

        List<WorldGuardRegion> regionList = regionList(regions);
        Map<String, WorldGuardRegion> byId = RegionQueryEngine.byId(regionList);
        Set<String> previousIds = new HashSet<>(previous.regionIds());
        Set<String> currentIds = new HashSet<>(current.regionIds());
        boolean sameWorld = previous.world().equals(current.world());
        List<WorldGuardSessionMessage> messages = new ArrayList<>();
        String name = playerName == null ? "" : playerName;

        boolean leftNotifyRegion = false;
        for (String regionId : previous.regionIds()) {
            if ((!sameWorld || !currentIds.contains(regionId))
                && notifyEnabled(byId.get(regionId), byId, WorldGuardFlag.NOTIFY_LEAVE)) {
                leftNotifyRegion = true;
                break;
            }
        }
        if (leftNotifyRegion) {
            messages.add(new WorldGuardSessionMessage(
                WorldGuardFlag.NOTIFY_LEAVE,
                "",
                name + " left NOTIFY region"
            ));
        }

        List<String> entered = new ArrayList<>();
        for (String regionId : current.regionIds()) {
            if ((!sameWorld || !previousIds.contains(regionId))
                && notifyEnabled(byId.get(regionId), byId, WorldGuardFlag.NOTIFY_ENTER)) {
                entered.add(regionId);
            }
        }
        if (!entered.isEmpty()) {
            messages.add(new WorldGuardSessionMessage(
                WorldGuardFlag.NOTIFY_ENTER,
                String.join(", ", entered),
                name + " entered NOTIFY region: " + String.join(", ", entered)
            ));
        }

        return List.copyOf(messages);
    }

    private static boolean changedRegions(WorldGuardSessionSnapshot previous, WorldGuardSessionSnapshot current) {
        return !Set.copyOf(previous.regionIds()).equals(Set.copyOf(current.regionIds()));
    }

    private static boolean protectedAnimalCategory(MobCategory category) {
        return category == MobCategory.CREATURE
            || category == MobCategory.AMBIENT
            || category == MobCategory.AXOLOTLS
            || category == MobCategory.UNDERGROUND_WATER_CREATURE
            || category == MobCategory.WATER_CREATURE
            || category == MobCategory.WATER_AMBIENT;
    }

    private static boolean enteredRegion(WorldGuardSessionSnapshot previous, WorldGuardSessionSnapshot current) {
        Set<String> previousIds = new HashSet<>(previous.regionIds());
        for (String regionId : current.regionIds()) {
            if (!previousIds.contains(regionId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean leftRegion(WorldGuardSessionSnapshot previous, WorldGuardSessionSnapshot current) {
        Set<String> currentIds = new HashSet<>(current.regionIds());
        for (String regionId : previous.regionIds()) {
            if (!currentIds.contains(regionId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean notifyEnabled(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        WorldGuardFlag flag
    ) {
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            FlagState state = current.flag(flag);
            if (state == FlagState.ALLOW) {
                return true;
            }
            if (state == FlagState.DENY) {
                return false;
            }
            current = current.parentId().isBlank() ? null : byId.get(current.parentId());
        }
        return false;
    }

    private static List<WorldGuardRegion> regionList(Collection<WorldGuardRegion> regions) {
        return regions == null ? List.of() : List.copyOf(regions);
    }
}
