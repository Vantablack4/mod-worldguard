package com.vantablack4.worldguard.storage;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RegionStorageSchema {
    public static final int CURRENT_VERSION = 3;
    public static final String SCHEMA_VERSION_KEY = "schema-version";
    public static final String REGION_PREFIX = "region.";
    public static final String POLYGON_POINTS_KEY = "polygon-points";

    private RegionStorageSchema() {
    }

    public static String prefix(String regionId) {
        return REGION_PREFIX + regionId + ".";
    }

    public static String csv(Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
            .map(value -> value instanceof UUID uuid ? uuid.toString() : String.valueOf(value))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining(","));
    }
}
