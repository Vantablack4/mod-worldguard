package com.vantablack4.worldguard.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardPolicy;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
import com.vantablack4.worldguard.model.RegionQueryEngine;

public final class WorldGuardSessionRules {
    private static final Pattern TIME_LOCK_PATTERN = Pattern.compile("[+-]?\\d+");

    private WorldGuardSessionRules() {
    }

    public record TimeLock(long value, boolean relative) {
    }

    public enum WeatherLock {
        CLEAR,
        RAIN,
        THUNDER_STORM
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
        if (usesBuildOverride(flags)) {
            return WorldGuardPolicy.evaluateBuild(
                regionList,
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                playerUuid,
                playerGroups,
                false,
                flags
            );
        }
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

    private static boolean usesBuildOverride(WorldGuardFlag... flags) {
        if (flags == null || flags.length < 2) {
            return false;
        }
        for (WorldGuardFlag flag : flags) {
            if (flag == WorldGuardFlag.BUILD) {
                return true;
            }
        }
        return false;
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
        return messagesForTransition(regions, previous, current, playerName, null, List.of());
    }

    public static List<WorldGuardSessionMessage> messagesForTransition(
        Collection<WorldGuardRegion> regions,
        WorldGuardSessionSnapshot previous,
        WorldGuardSessionSnapshot current,
        String playerName,
        UUID playerUuid,
        Collection<String> playerGroups
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
        if (leftRegion(previous, current)) {
            valueAt(regionList, previous, WorldGuardValueFlag.FAREWELL, playerUuid, playerGroups)
                .ifPresent(message -> messages.add(new WorldGuardSessionMessage(null, "", message.serialized())));
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
        if (enteredRegion(previous, current)) {
            valueAt(regionList, current, WorldGuardValueFlag.GREETING, playerUuid, playerGroups)
                .ifPresent(message -> messages.add(new WorldGuardSessionMessage(null, "", message.serialized())));
        }

        return List.copyOf(messages);
    }

    public static boolean commandAllowed(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos,
        UUID playerUuid,
        Collection<String> playerGroups,
        boolean bypass,
        String command
    ) {
        if (bypass || world == null || pos == null || command == null || command.isBlank()) {
            return true;
        }

        List<WorldGuardRegion> regionList = regionList(regions);
        RegionQueryEngine.ValueEvaluation blocked = RegionQueryEngine.queryValue(
            regionList,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            WorldGuardValueFlag.BLOCKED_CMDS,
            playerUuid,
            playerGroups
        );
        if (blocked.value().map(WorldGuardFlagValue::asSet).orElse(Set.of()).stream()
            .anyMatch(rule -> commandMatches(rule, command))) {
            return false;
        }

        RegionQueryEngine.ValueEvaluation allowed = RegionQueryEngine.queryValue(
            regionList,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            WorldGuardValueFlag.ALLOWED_CMDS,
            playerUuid,
            playerGroups
        );
        Set<String> allowedCommands = allowed.value().map(WorldGuardFlagValue::asSet).orElse(Set.of());
        return allowedCommands.isEmpty() || allowedCommands.stream().anyMatch(rule -> commandMatches(rule, command));
    }

    public static Optional<WorldGuardFlagValue.LocationValue> respawnLocation(
        Collection<WorldGuardRegion> regions,
        String world,
        BlockPos pos,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        if (world == null || pos == null) {
            return Optional.empty();
        }
        return RegionQueryEngine.queryValue(
            regionList(regions),
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            WorldGuardValueFlag.SPAWN,
            playerUuid,
            playerGroups
        ).value().flatMap(WorldGuardFlagValue::asLocation);
    }

    public static Optional<TimeLock> timeLock(String raw) {
        if (raw == null || !TIME_LOCK_PATTERN.matcher(raw.trim()).matches()) {
            return Optional.empty();
        }
        String normalized = raw.trim();
        try {
            return Optional.of(new TimeLock(
                Long.parseLong(normalized),
                normalized.startsWith("+") || normalized.startsWith("-")
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Optional<WeatherLock> weatherLock(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "clear", "none", "sun", "sunny" -> Optional.of(WeatherLock.CLEAR);
            case "rain", "downfall" -> Optional.of(WeatherLock.RAIN);
            case "thunder-storm", "thunderstorm", "storm", "thunder" -> Optional.of(WeatherLock.THUNDER_STORM);
            default -> Optional.empty();
        };
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

    private static Optional<WorldGuardFlagValue> valueAt(
        List<WorldGuardRegion> regions,
        WorldGuardSessionSnapshot snapshot,
        WorldGuardValueFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        return RegionQueryEngine.queryValue(
            regions,
            snapshot.world(),
            snapshot.pos().getX(),
            snapshot.pos().getY(),
            snapshot.pos().getZ(),
            flag,
            playerUuid,
            playerGroups
        ).value();
    }

    static boolean commandMatches(String rule, String command) {
        String normalizedRule = normalizeCommand(rule);
        String normalizedCommand = normalizeCommand(command);
        return !normalizedRule.isBlank()
            && (normalizedCommand.equals(normalizedRule) || normalizedCommand.startsWith(normalizedRule + " "));
    }

    private static String normalizeCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static List<WorldGuardRegion> regionList(Collection<WorldGuardRegion> regions) {
        return regions == null ? List.of() : List.copyOf(regions);
    }
}
