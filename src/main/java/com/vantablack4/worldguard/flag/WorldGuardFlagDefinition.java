package com.vantablack4.worldguard.flag;

import com.vantablack4.worldguard.FlagState;

public record WorldGuardFlagDefinition(
    String id,
    FlagState defaultState,
    boolean usesMembershipDefault,
    boolean bypassesMemberDeny,
    boolean preventsAllowOnGlobal,
    String... aliases
) {
    public WorldGuardFlagDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Flag id is required");
        }
        id = normalize(id);
        aliases = aliases == null ? new String[0] : aliases.clone();
        for (int index = 0; index < aliases.length; index++) {
            aliases[index] = normalize(aliases[index]);
        }
    }

    public boolean matches(String raw) {
        String normalized = normalize(raw);
        if (id.equals(normalized)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
