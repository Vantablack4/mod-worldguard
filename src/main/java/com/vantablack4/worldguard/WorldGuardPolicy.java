package com.vantablack4.worldguard;

import com.vantablack4.worldguard.model.RegionQueryEngine;

import java.util.List;
import java.util.UUID;

public final class WorldGuardPolicy {
    private WorldGuardPolicy() {
    }

    public static ProtectionDecision evaluate(
        List<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        WorldGuardFlag flag,
        UUID playerUuid,
        boolean bypass
    ) {
        if (bypass) {
            return ProtectionDecision.allow();
        }

        RegionQueryEngine.FlagEvaluation evaluation = RegionQueryEngine.queryState(
            regions,
            world,
            x,
            y,
            z,
            flag,
            playerUuid
        );

        if (evaluation.state() == FlagState.DENY) {
            return ProtectionDecision.deny(evaluation.regionId(), flag);
        }

        return ProtectionDecision.allow();
    }
}
