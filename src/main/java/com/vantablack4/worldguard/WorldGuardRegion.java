package com.vantablack4.worldguard;

import com.vantablack4.worldguard.model.RegionDomain;
import com.vantablack4.worldguard.model.RegionType;
import com.vantablack4.worldguard.flag.WorldGuardFlagDefinition;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record WorldGuardRegion(
    String id,
    String world,
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    int priority,
    String parentId,
    RegionType type,
    Set<UUID> owners,
    Set<UUID> members,
    Set<String> ownerGroups,
    Set<String> memberGroups,
    Map<WorldGuardFlag, FlagState> flags,
    Map<WorldGuardValueFlag, WorldGuardFlagValue> valueFlags,
    Map<String, WorldGuardRegionGroup> flagGroups,
    List<PolygonPoint> polygonPoints
) {
    public static final String GLOBAL_REGION_ID = "__global__";
    public static final String ANY_WORLD = "*";

    public record PolygonPoint(int x, int z) {
    }

    public WorldGuardRegion {
        id = WorldGuardStorage.normalizeId(id);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Region id is required");
        }
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("World id is required");
        }
        world = world.trim();
        type = type == null ? RegionType.CUBOID : type;
        if (id.equals(GLOBAL_REGION_ID)) {
            type = RegionType.GLOBAL;
        }
        parentId = WorldGuardStorage.normalizeId(parentId);
        if (!parentId.isBlank() && parentId.equals(id)) {
            throw new IllegalArgumentException("Region cannot be its own parent: " + id);
        }
        polygonPoints = normalizePolygonPoints(type, polygonPoints);

        int originalMinX = minX;
        int originalMinY = minY;
        int originalMinZ = minZ;
        int originalMaxX = maxX;
        int originalMaxY = maxY;
        int originalMaxZ = maxZ;
        minX = Math.min(originalMinX, originalMaxX);
        minY = Math.min(originalMinY, originalMaxY);
        minZ = Math.min(originalMinZ, originalMaxZ);
        maxX = Math.max(originalMinX, originalMaxX);
        maxY = Math.max(originalMinY, originalMaxY);
        maxZ = Math.max(originalMinZ, originalMaxZ);

        if (type == RegionType.GLOBAL) {
            minX = 0;
            minY = 0;
            minZ = 0;
            maxX = 0;
            maxY = 0;
            maxZ = 0;
        } else if (type == RegionType.POLYGON) {
            minX = polygonPoints.stream().mapToInt(PolygonPoint::x).min().orElseThrow();
            maxX = polygonPoints.stream().mapToInt(PolygonPoint::x).max().orElseThrow();
            minZ = polygonPoints.stream().mapToInt(PolygonPoint::z).min().orElseThrow();
            maxZ = polygonPoints.stream().mapToInt(PolygonPoint::z).max().orElseThrow();
        }

        owners = owners == null ? Set.of() : Set.copyOf(owners);
        members = members == null ? Set.of() : Set.copyOf(members);
        RegionDomain ownerDomain = new RegionDomain(owners, ownerGroups);
        RegionDomain memberDomain = new RegionDomain(members, memberGroups);
        ownerGroups = ownerDomain.groups();
        memberGroups = memberDomain.groups();

        EnumMap<WorldGuardFlag, FlagState> copiedFlags = new EnumMap<>(WorldGuardFlag.class);
        if (flags != null) {
            copiedFlags.putAll(flags);
        }
        copiedFlags.entrySet().removeIf(entry ->
            entry.getValue() == null
                || entry.getValue() == FlagState.UNSET
        );
        flags = Collections.unmodifiableMap(copiedFlags);

        EnumMap<WorldGuardValueFlag, WorldGuardFlagValue> copiedValueFlags = new EnumMap<>(WorldGuardValueFlag.class);
        if (valueFlags != null) {
            valueFlags.forEach((flag, value) -> {
                if (flag != null && value != null && value.type() == flag.type()) {
                    copiedValueFlags.put(flag, value);
                }
            });
        }
        valueFlags = Collections.unmodifiableMap(copiedValueFlags);

        HashMap<String, WorldGuardRegionGroup> copiedFlagGroups = new HashMap<>();
        if (flagGroups != null) {
            flagGroups.forEach((flagId, group) -> {
                String normalizedFlagId = WorldGuardFlagDefinition.normalize(flagId);
                if (!normalizedFlagId.isBlank() && group != null) {
                    copiedFlagGroups.put(normalizedFlagId, group);
                }
            });
        }
        flagGroups = Collections.unmodifiableMap(copiedFlagGroups);
    }

    public WorldGuardRegion(
        String id,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int priority,
        String parentId,
        RegionType type,
        Set<UUID> owners,
        Set<UUID> members,
        Set<String> ownerGroups,
        Set<String> memberGroups,
        Map<WorldGuardFlag, FlagState> flags,
        List<PolygonPoint> polygonPoints
    ) {
        this(
            id,
            world,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            priority,
            parentId,
            type,
            owners,
            members,
            ownerGroups,
            memberGroups,
            flags,
            Map.of(),
            Map.of(),
            polygonPoints
        );
    }

    public WorldGuardRegion(
        String id,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int priority,
        String parentId,
        RegionType type,
        Set<UUID> owners,
        Set<UUID> members,
        Set<String> ownerGroups,
        Set<String> memberGroups,
        Map<WorldGuardFlag, FlagState> flags
    ) {
        this(
            id,
            world,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            priority,
            parentId,
            type,
            owners,
            members,
            ownerGroups,
            memberGroups,
            flags,
            Map.of(),
            Map.of(),
            List.of()
        );
    }

    public WorldGuardRegion(
        String id,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        int priority,
        Set<UUID> members,
        Map<WorldGuardFlag, FlagState> flags
    ) {
        this(
            id,
            world,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            priority,
            "",
            RegionType.CUBOID,
            Set.of(),
            members,
            Set.of(),
            Set.of(),
            flags
        );
    }

    public static WorldGuardRegion defaultProtected(
        String id,
        String world,
        int x1,
        int y1,
        int z1,
        int x2,
        int y2,
        int z2,
        int priority
    ) {
        return new WorldGuardRegion(id, world, x1, y1, z1, x2, y2, z2, priority, Set.of(), defaultProtectedFlags());
    }

    public static WorldGuardRegion defaultProtectedPolygon(
        String id,
        String world,
        int minY,
        int maxY,
        List<PolygonPoint> polygonPoints,
        int priority
    ) {
        return new WorldGuardRegion(
            id,
            world,
            0,
            minY,
            0,
            0,
            maxY,
            0,
            priority,
            "",
            RegionType.POLYGON,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            defaultProtectedFlags(),
            polygonPoints
        );
    }

    public static WorldGuardRegion global(String world) {
        return new WorldGuardRegion(
            GLOBAL_REGION_ID,
            world == null || world.isBlank() ? ANY_WORLD : world,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            "",
            RegionType.GLOBAL,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of()
        );
    }

    public boolean contains(String candidateWorld, int x, int y, int z) {
        if (!type.physicalArea() || !appliesToWorld(candidateWorld) || y < minY || y > maxY) {
            return false;
        }
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }
        if (type == RegionType.POLYGON) {
            return containsPolygon(x, z);
        }
        return true;
    }

    public boolean appliesToWorld(String candidateWorld) {
        return world.equals(ANY_WORLD) || world.equals(candidateWorld);
    }

    public boolean global() {
        return type == RegionType.GLOBAL || id.equals(GLOBAL_REGION_ID);
    }

    public boolean owner(UUID playerUuid) {
        return playerUuid != null && owners.contains(playerUuid);
    }

    public boolean owner(UUID playerUuid, Collection<String> playerGroups) {
        return ownersDomain().contains(playerUuid, playerGroups);
    }

    public boolean member(UUID playerUuid) {
        return playerUuid != null && (members.contains(playerUuid) || owners.contains(playerUuid));
    }

    public boolean member(UUID playerUuid, Collection<String> playerGroups) {
        return owner(playerUuid, playerGroups) || membersDomain().contains(playerUuid, playerGroups);
    }

    public RegionDomain ownersDomain() {
        return new RegionDomain(owners, ownerGroups);
    }

    public RegionDomain membersDomain() {
        return new RegionDomain(members, memberGroups);
    }

    public FlagState flag(WorldGuardFlag flag) {
        return flags.getOrDefault(flag, FlagState.UNSET);
    }

    public java.util.Optional<WorldGuardFlagValue> value(WorldGuardValueFlag flag) {
        if (flag == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(valueFlags.get(flag));
    }

    public java.util.Optional<WorldGuardRegionGroup> explicitFlagGroup(String rawFlagId) {
        String flagId = WorldGuardFlagDefinition.normalize(rawFlagId);
        if (flagId.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(flagGroups.get(flagId));
    }

    public WorldGuardRegionGroup flagGroup(WorldGuardFlag flag) {
        if (flag == null || !flag.supportsRegionGroup()) {
            return WorldGuardRegionGroup.ALL;
        }
        return explicitFlagGroup(flag.id()).orElse(flag.defaultGroup());
    }

    public WorldGuardRegionGroup flagGroup(WorldGuardValueFlag flag) {
        if (flag == null || !flag.supportsRegionGroup()) {
            return WorldGuardRegionGroup.ALL;
        }
        return explicitFlagGroup(flag.id()).orElse(flag.defaultGroup());
    }

    public WorldGuardRegion withFlag(WorldGuardFlag flag, FlagState state) {
        EnumMap<WorldGuardFlag, FlagState> updated = new EnumMap<>(WorldGuardFlag.class);
        updated.putAll(flags);
        if (state == null || state == FlagState.UNSET) {
            updated.remove(flag);
        } else {
            updated.put(flag, state);
        }
        return copy(parentId, type, owners, members, ownerGroups, memberGroups, updated);
    }

    public WorldGuardRegion withValue(WorldGuardValueFlag flag, WorldGuardFlagValue value) {
        if (flag == null) {
            return this;
        }
        EnumMap<WorldGuardValueFlag, WorldGuardFlagValue> updated = new EnumMap<>(WorldGuardValueFlag.class);
        updated.putAll(valueFlags);
        if (value == null || value.type() != flag.type()) {
            updated.remove(flag);
        } else {
            updated.put(flag, value);
        }
        return copy(parentId, type, owners, members, ownerGroups, memberGroups, flags, updated, flagGroups);
    }

    public WorldGuardRegion withoutValue(WorldGuardValueFlag flag) {
        return withValue(flag, null);
    }

    public WorldGuardRegion withFlagGroup(WorldGuardFlag flag, WorldGuardRegionGroup group) {
        if (flag == null || !flag.supportsRegionGroup()) {
            return this;
        }
        return withFlagGroup(flag.id(), group);
    }

    public WorldGuardRegion withFlagGroup(WorldGuardValueFlag flag, WorldGuardRegionGroup group) {
        if (flag == null || !flag.supportsRegionGroup()) {
            return this;
        }
        return withFlagGroup(flag.id(), group);
    }

    public WorldGuardRegion withoutFlagGroup(WorldGuardFlag flag) {
        return withFlagGroup(flag, null);
    }

    public WorldGuardRegion withoutFlagGroup(WorldGuardValueFlag flag) {
        return withFlagGroup(flag, null);
    }

    private WorldGuardRegion withFlagGroup(String rawFlagId, WorldGuardRegionGroup group) {
        String flagId = WorldGuardFlagDefinition.normalize(rawFlagId);
        if (flagId.isBlank()) {
            return this;
        }
        HashMap<String, WorldGuardRegionGroup> updated = new HashMap<>(flagGroups);
        if (group == null) {
            updated.remove(flagId);
        } else {
            updated.put(flagId, group);
        }
        return copy(parentId, type, owners, members, ownerGroups, memberGroups, flags, valueFlags, updated);
    }

    public WorldGuardRegion withParent(String rawParentId) {
        return copy(WorldGuardStorage.normalizeId(rawParentId), type, owners, members, ownerGroups, memberGroups, flags);
    }

    public WorldGuardRegion withoutParent() {
        return withParent("");
    }

    public WorldGuardRegion withOwner(UUID playerUuid) {
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(owners);
        updated.add(playerUuid);
        return copy(parentId, type, updated, members, ownerGroups, memberGroups, flagsForGlobalMembership(flags));
    }

    public WorldGuardRegion withoutOwner(UUID playerUuid) {
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(owners);
        updated.remove(playerUuid);
        return copy(parentId, type, updated, members, ownerGroups, memberGroups, flags);
    }

    public WorldGuardRegion withoutOwners() {
        return copy(parentId, type, Set.of(), members, Set.of(), memberGroups, flags);
    }

    public WorldGuardRegion withOwnerGroup(String group) {
        java.util.HashSet<String> updated = new java.util.HashSet<>(ownerGroups);
        updated.addAll(RegionDomain.normalizeGroups(Set.of(group)));
        return copy(parentId, type, owners, members, updated, memberGroups, flagsForGlobalMembership(flags));
    }

    public WorldGuardRegion withoutOwnerGroup(String group) {
        java.util.HashSet<String> updated = new java.util.HashSet<>(ownerGroups);
        updated.removeAll(RegionDomain.normalizeGroups(Set.of(group)));
        return copy(parentId, type, owners, members, updated, memberGroups, flags);
    }

    public WorldGuardRegion withMember(UUID playerUuid) {
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(members);
        updated.add(playerUuid);
        return copy(parentId, type, owners, updated, ownerGroups, memberGroups, flagsForGlobalMembership(flags));
    }

    public WorldGuardRegion withoutMember(UUID playerUuid) {
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(members);
        updated.remove(playerUuid);
        return copy(parentId, type, owners, updated, ownerGroups, memberGroups, flags);
    }

    public WorldGuardRegion withoutMembers() {
        return copy(parentId, type, owners, Set.of(), ownerGroups, Set.of(), flags);
    }

    public WorldGuardRegion withMemberGroup(String group) {
        java.util.HashSet<String> updated = new java.util.HashSet<>(memberGroups);
        updated.addAll(RegionDomain.normalizeGroups(Set.of(group)));
        return copy(parentId, type, owners, members, ownerGroups, updated, flagsForGlobalMembership(flags));
    }

    public WorldGuardRegion withoutMemberGroup(String group) {
        java.util.HashSet<String> updated = new java.util.HashSet<>(memberGroups);
        updated.removeAll(RegionDomain.normalizeGroups(Set.of(group)));
        return copy(parentId, type, owners, members, ownerGroups, updated, flags);
    }

    public String boundsDisplay() {
        if (global()) {
            return world + " " + minX + " " + minY + " " + minZ + " -> " + maxX + " " + maxY + " " + maxZ;
        }
        if (type == RegionType.POLYGON) {
            return world + " polygon points=" + polygonPoints.size() + " y=" + minY + " -> " + maxY
                + " bounds " + minX + " " + minZ + " -> " + maxX + " " + maxZ;
        }
        return world + " " + minX + " " + minY + " " + minZ + " -> " + maxX + " " + maxY + " " + maxZ;
    }

    private Map<WorldGuardFlag, FlagState> flagsForGlobalMembership(Map<WorldGuardFlag, FlagState> currentFlags) {
        if (!global() || currentFlags.containsKey(WorldGuardFlag.PASSTHROUGH)) {
            return currentFlags;
        }
        EnumMap<WorldGuardFlag, FlagState> updated = new EnumMap<>(WorldGuardFlag.class);
        updated.putAll(currentFlags);
        updated.put(WorldGuardFlag.PASSTHROUGH, FlagState.DENY);
        return updated;
    }

    private WorldGuardRegion copy(
        String parentId,
        RegionType type,
        Set<UUID> owners,
        Set<UUID> members,
        Set<String> ownerGroups,
        Set<String> memberGroups,
        Map<WorldGuardFlag, FlagState> flags
    ) {
        return copy(parentId, type, owners, members, ownerGroups, memberGroups, flags, valueFlags, flagGroups);
    }

    private WorldGuardRegion copy(
        String parentId,
        RegionType type,
        Set<UUID> owners,
        Set<UUID> members,
        Set<String> ownerGroups,
        Set<String> memberGroups,
        Map<WorldGuardFlag, FlagState> flags,
        Map<WorldGuardValueFlag, WorldGuardFlagValue> valueFlags,
        Map<String, WorldGuardRegionGroup> flagGroups
    ) {
        return new WorldGuardRegion(
            id,
            world,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            priority,
            parentId,
            type,
            owners,
            members,
            ownerGroups,
            memberGroups,
            flags,
            valueFlags,
            flagGroups,
            polygonPoints
        );
    }

    private boolean containsPolygon(int x, int z) {
        boolean inside = false;
        for (int index = 0, previous = polygonPoints.size() - 1; index < polygonPoints.size(); previous = index++) {
            PolygonPoint start = polygonPoints.get(previous);
            PolygonPoint end = polygonPoints.get(index);
            if (onSegment(start, end, x, z)) {
                return true;
            }
            boolean crossesZ = start.z() > z != end.z() > z;
            if (crossesZ) {
                double intersectionX = (double) (end.x() - start.x()) * (z - start.z())
                    / (double) (end.z() - start.z()) + start.x();
                if (x < intersectionX) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static boolean onSegment(PolygonPoint start, PolygonPoint end, int x, int z) {
        long cross = (long) (x - start.x()) * (end.z() - start.z())
            - (long) (z - start.z()) * (end.x() - start.x());
        if (cross != 0L) {
            return false;
        }
        return x >= Math.min(start.x(), end.x())
            && x <= Math.max(start.x(), end.x())
            && z >= Math.min(start.z(), end.z())
            && z <= Math.max(start.z(), end.z());
    }

    private static List<PolygonPoint> normalizePolygonPoints(RegionType type, List<PolygonPoint> points) {
        if (type != RegionType.POLYGON) {
            return List.of();
        }
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("Polygon regions require at least three points");
        }
        List<PolygonPoint> copied = points.stream()
            .map(point -> {
                if (point == null) {
                    throw new IllegalArgumentException("Polygon points cannot be null");
                }
                return new PolygonPoint(point.x(), point.z());
            })
            .toList();
        if (copied.stream().distinct().count() < 3) {
            throw new IllegalArgumentException("Polygon regions require at least three unique points");
        }
        return List.copyOf(copied);
    }

    private static EnumMap<WorldGuardFlag, FlagState> defaultProtectedFlags() {
        EnumMap<WorldGuardFlag, FlagState> flags = new EnumMap<>(WorldGuardFlag.class);
        flags.put(WorldGuardFlag.BUILD, FlagState.DENY);
        flags.put(WorldGuardFlag.BLOCK_BREAK, FlagState.DENY);
        flags.put(WorldGuardFlag.BLOCK_PLACE, FlagState.DENY);
        flags.put(WorldGuardFlag.USE, FlagState.DENY);
        flags.put(WorldGuardFlag.INTERACT, FlagState.DENY);
        flags.put(WorldGuardFlag.USE_ENTITY, FlagState.DENY);
        flags.put(WorldGuardFlag.ATTACK_ENTITY, FlagState.DENY);
        flags.put(WorldGuardFlag.ITEM_USE, FlagState.DENY);
        flags.put(WorldGuardFlag.CHEST_ACCESS, FlagState.DENY);
        flags.put(WorldGuardFlag.TNT, FlagState.DENY);
        flags.put(WorldGuardFlag.CREEPER_EXPLOSION, FlagState.DENY);
        flags.put(WorldGuardFlag.ENDERDRAGON_BLOCK_DAMAGE, FlagState.DENY);
        flags.put(WorldGuardFlag.GHAST_FIREBALL, FlagState.DENY);
        flags.put(WorldGuardFlag.OTHER_EXPLOSION, FlagState.DENY);
        flags.put(WorldGuardFlag.BREEZE_WIND_CHARGE, FlagState.DENY);
        flags.put(WorldGuardFlag.WITHER_DAMAGE, FlagState.DENY);
        flags.put(WorldGuardFlag.MOB_GRIEF, FlagState.DENY);
        flags.put(WorldGuardFlag.ENDER_BUILD, FlagState.DENY);
        flags.put(WorldGuardFlag.RAVAGER_RAVAGE, FlagState.DENY);
        flags.put(WorldGuardFlag.PISTONS, FlagState.DENY);
        flags.put(WorldGuardFlag.FIRE_SPREAD, FlagState.DENY);
        flags.put(WorldGuardFlag.LAVA_FIRE, FlagState.DENY);
        flags.put(WorldGuardFlag.WATER_FLOW, FlagState.DENY);
        flags.put(WorldGuardFlag.LAVA_FLOW, FlagState.DENY);
        return flags;
    }
}
