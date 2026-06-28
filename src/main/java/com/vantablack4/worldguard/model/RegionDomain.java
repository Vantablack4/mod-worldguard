package com.vantablack4.worldguard.model;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public record RegionDomain(
    Set<UUID> uniqueIds,
    Set<String> groups
) {
    public RegionDomain {
        uniqueIds = uniqueIds == null ? Set.of() : Set.copyOf(uniqueIds);
        groups = normalizeGroups(groups);
    }

    public static RegionDomain empty() {
        return new RegionDomain(Set.of(), Set.of());
    }

    public static RegionDomain ofPlayers(Collection<UUID> uniqueIds) {
        return new RegionDomain(uniqueIds == null ? Set.of() : Set.copyOf(uniqueIds), Set.of());
    }

    public boolean contains(UUID uniqueId) {
        return uniqueId != null && uniqueIds.contains(uniqueId);
    }

    public boolean emptyDomain() {
        return uniqueIds.isEmpty() && groups.isEmpty();
    }

    public RegionDomain withPlayer(UUID uniqueId) {
        if (uniqueId == null) {
            return this;
        }
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(uniqueIds);
        updated.add(uniqueId);
        return new RegionDomain(updated, groups);
    }

    public RegionDomain withoutPlayer(UUID uniqueId) {
        if (uniqueId == null) {
            return this;
        }
        java.util.HashSet<UUID> updated = new java.util.HashSet<>(uniqueIds);
        updated.remove(uniqueId);
        return new RegionDomain(updated, groups);
    }

    private static Set<String> normalizeGroups(Collection<String> rawGroups) {
        if (rawGroups == null || rawGroups.isEmpty()) {
            return Set.of();
        }
        TreeSet<String> normalized = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String group : rawGroups) {
            if (group == null || group.isBlank()) {
                continue;
            }
            normalized.add(group.trim().toLowerCase(Locale.ROOT));
        }
        return Set.copyOf(normalized);
    }
}
