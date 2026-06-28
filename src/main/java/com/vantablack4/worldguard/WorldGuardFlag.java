package com.vantablack4.worldguard;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum WorldGuardFlag {
    BUILD("build"),
    INTERACT("interact"),
    USE_ENTITY("use-entity"),
    ATTACK_ENTITY("attack-entity"),
    ITEM_USE("item-use");

    private final String id;

    WorldGuardFlag(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<WorldGuardFlag> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (WorldGuardFlag flag : values()) {
            if (flag.id.equals(normalized)) {
                return Optional.of(flag);
            }
        }
        return Optional.empty();
    }

    public static Iterable<String> ids() {
        return Arrays.stream(values()).map(WorldGuardFlag::id).toList();
    }
}
