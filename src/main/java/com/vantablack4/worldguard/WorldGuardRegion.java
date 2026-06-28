package com.vantablack4.worldguard;

import java.util.Collections;
import java.util.EnumMap;
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
    Set<UUID> members,
    Map<WorldGuardFlag, FlagState> flags
) {
    public WorldGuardRegion {
        id = WorldGuardStorage.normalizeId(id);
        if (id.isBlank()) {
            throw new IllegalArgumentException("Region id is required");
        }
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("World id is required");
        }
        world = world.trim();

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

        members = members == null ? Set.of() : Set.copyOf(members);
        EnumMap<WorldGuardFlag, FlagState> copiedFlags = new EnumMap<>(WorldGuardFlag.class);
        if (flags != null) {
            copiedFlags.putAll(flags);
        }
        copiedFlags.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() == FlagState.UNSET);
        flags = Collections.unmodifiableMap(copiedFlags);
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
        EnumMap<WorldGuardFlag, FlagState> flags = new EnumMap<>(WorldGuardFlag.class);
        flags.put(WorldGuardFlag.BUILD, FlagState.DENY);
        flags.put(WorldGuardFlag.INTERACT, FlagState.DENY);
        flags.put(WorldGuardFlag.USE_ENTITY, FlagState.DENY);
        flags.put(WorldGuardFlag.ATTACK_ENTITY, FlagState.DENY);
        flags.put(WorldGuardFlag.ITEM_USE, FlagState.DENY);
        return new WorldGuardRegion(id, world, x1, y1, z1, x2, y2, z2, priority, Set.of(), flags);
    }

    public boolean contains(String candidateWorld, int x, int y, int z) {
        return world.equals(candidateWorld)
            && x >= minX
            && x <= maxX
            && y >= minY
            && y <= maxY
            && z >= minZ
            && z <= maxZ;
    }

    public boolean member(UUID playerUuid) {
        return playerUuid != null && members.contains(playerUuid);
    }

    public FlagState flag(WorldGuardFlag flag) {
        return flags.getOrDefault(flag, FlagState.UNSET);
    }

    public WorldGuardRegion withFlag(WorldGuardFlag flag, FlagState state) {
        EnumMap<WorldGuardFlag, FlagState> updated = new EnumMap<>(WorldGuardFlag.class);
        updated.putAll(flags);
        if (state == null || state == FlagState.UNSET) {
            updated.remove(flag);
        } else {
            updated.put(flag, state);
        }
        return new WorldGuardRegion(id, world, minX, minY, minZ, maxX, maxY, maxZ, priority, members, updated);
    }

    public WorldGuardRegion withMember(UUID playerUuid) {
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(members);
        updated.add(playerUuid);
        return new WorldGuardRegion(id, world, minX, minY, minZ, maxX, maxY, maxZ, priority, updated, flags);
    }

    public WorldGuardRegion withoutMember(UUID playerUuid) {
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(members);
        updated.remove(playerUuid);
        return new WorldGuardRegion(id, world, minX, minY, minZ, maxX, maxY, maxZ, priority, updated, flags);
    }

    public String boundsDisplay() {
        return world + " " + minX + " " + minY + " " + minZ + " -> " + maxX + " " + maxY + " " + maxZ;
    }
}
