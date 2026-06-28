package com.vantablack4.worldguard.model;

import java.util.Locale;
import java.util.Optional;

public enum RegionType {
    CUBOID("cuboid", true),
    POLYGON("polygon", true),
    GLOBAL("global", false);

    private final String id;
    private final boolean physicalArea;

    RegionType(String id, boolean physicalArea) {
        this.id = id;
        this.physicalArea = physicalArea;
    }

    public String id() {
        return id;
    }

    public boolean physicalArea() {
        return physicalArea;
    }

    public static Optional<RegionType> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (RegionType type : values()) {
            if (type.id.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
