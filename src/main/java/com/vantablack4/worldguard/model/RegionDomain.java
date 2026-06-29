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

    public boolean contains(UUID uniqueId, Collection<String> candidateGroups) {
        return contains(uniqueId) || containsAnyGroup(candidateGroups);
    }

    public boolean containsAnyGroup(Collection<String> candidateGroups) {
        if (groups.isEmpty() || candidateGroups == null || candidateGroups.isEmpty()) {
            return false;
        }
        for (String group : normalizeGroups(candidateGroups)) {
            if (groups.contains(group)) {
                return true;
            }
        }
        return false;
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

    public static Set<String> normalizeGroups(Collection<String> rawGroups) {
        if (rawGroups == null || rawGroups.isEmpty()) {
            return Set.of();
        }
        TreeSet<String> normalized = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String group : rawGroups) {
            if (group == null || group.isBlank()) {
                continue;
            }
            String normalizedGroup = normalizeGroup(group);
            if (!normalizedGroup.isBlank()) {
                normalized.add(normalizedGroup);
            }
        }
        return Set.copyOf(normalized);
    }

    public static String normalizeGroup(String group) {
        if (group == null) {
            return "";
        }
        String trimmed = group.trim();
        if (trimmed.regionMatches(true, 0, "g:", 0, 2)) {
            trimmed = trimmed.substring(2).trim();
        } else if (trimmed.regionMatches(true, 0, "group:", 0, 6)) {
            trimmed = trimmed.substring(6).trim();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
