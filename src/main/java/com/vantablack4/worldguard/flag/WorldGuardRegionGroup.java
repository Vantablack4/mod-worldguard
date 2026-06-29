package com.vantablack4.worldguard.flag;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum WorldGuardRegionGroup {
    MEMBERS("members", RegionAssociation.MEMBER, RegionAssociation.OWNER),
    OWNERS("owners", RegionAssociation.OWNER),
    NON_MEMBERS("non-members", RegionAssociation.NON_MEMBER),
    NON_OWNERS("non-owners", RegionAssociation.MEMBER, RegionAssociation.NON_MEMBER),
    ALL("all", RegionAssociation.OWNER, RegionAssociation.MEMBER, RegionAssociation.NON_MEMBER),
    NONE("none");

    private final String id;
    private final Set<RegionAssociation> associations;

    WorldGuardRegionGroup(String id, RegionAssociation... associations) {
        this.id = id;
        this.associations = Arrays.stream(associations).collect(Collectors.toUnmodifiableSet());
    }

    public String id() {
        return id;
    }

    public boolean contains(RegionAssociation association) {
        return association != null && associations.contains(association);
    }

    public static Optional<WorldGuardRegionGroup> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (WorldGuardRegionGroup group : values()) {
            if (group.id.equals(normalized) || group.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(normalized)) {
                return Optional.of(group);
            }
        }
        return Optional.empty();
    }

    public enum RegionAssociation {
        OWNER,
        MEMBER,
        NON_MEMBER
    }
}
