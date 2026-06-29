package com.vantablack4.worldguard;

import com.vantablack4.worldguard.model.RegionQueryEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
        return evaluate(regions, world, x, y, z, flag, playerUuid, Set.of(), bypass);
    }

    public static ProtectionDecision evaluate(
        List<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        WorldGuardFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups,
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
            playerUuid,
            playerGroups
        );

        if (evaluation.state() == FlagState.DENY) {
            return ProtectionDecision.deny(evaluation.regionId(), flag);
        }

        return ProtectionDecision.allow();
    }

    public static ProtectionDecision evaluateBuild(
        List<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        UUID playerUuid,
        Collection<String> playerGroups,
        boolean bypass,
        WorldGuardFlag... flags
    ) {
        if (bypass) {
            return ProtectionDecision.allow();
        }

        List<WorldGuardRegion> regionList = regions == null ? List.of() : regions;
        Collection<String> groups = playerGroups == null ? Collections.emptySet() : playerGroups;
        RegionQueryEngine.FlagEvaluation build = RegionQueryEngine.queryState(
            regionList,
            world,
            x,
            y,
            z,
            WorldGuardFlag.BUILD,
            playerUuid,
            groups
        );
        ProtectionDecision buildDenied = build.state() == FlagState.DENY
            ? ProtectionDecision.deny(build.regionId(), WorldGuardFlag.BUILD)
            : null;
        ProtectionDecision specificAllow = null;

        if (flags != null) {
            for (WorldGuardFlag flag : flags) {
                if (flag == null || flag == WorldGuardFlag.BUILD) {
                    continue;
                }
                RegionQueryEngine.FlagEvaluation evaluation = RegionQueryEngine.queryState(
                    regionList,
                    world,
                    x,
                    y,
                    z,
                    flag,
                    playerUuid,
                    groups
                );
                if (evaluation.state() == FlagState.DENY) {
                    return ProtectionDecision.deny(evaluation.regionId(), flag);
                }
                if (evaluation.state() == FlagState.ALLOW) {
                    specificAllow = ProtectionDecision.allow();
                }
            }
        }

        if (specificAllow != null) {
            return specificAllow;
        }
        if (build.state() == FlagState.ALLOW) {
            return ProtectionDecision.allow();
        }
        if (buildDenied != null) {
            return buildDenied;
        }
        return ProtectionDecision.allow();
    }
}
