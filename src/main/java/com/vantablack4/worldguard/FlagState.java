package com.vantablack4.worldguard;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

public enum FlagState {
    ALLOW("allow"),
    DENY("deny"),
    UNSET("unset");

    private final String id;

    FlagState(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<FlagState> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("none")) {
            return Optional.of(UNSET);
        }
        for (FlagState state : values()) {
            if (state.id.equals(normalized)) {
                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    public static FlagState combine(Collection<FlagState> states) {
        boolean allowed = false;
        if (states == null) {
            return UNSET;
        }
        for (FlagState state : states) {
            if (state == DENY) {
                return DENY;
            }
            if (state == ALLOW) {
                allowed = true;
            }
        }
        return allowed ? ALLOW : UNSET;
    }
}
