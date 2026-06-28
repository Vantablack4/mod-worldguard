package com.vantablack4.worldguard;

import java.util.Comparator;
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

        List<WorldGuardRegion> matching = regions.stream()
            .filter(region -> region.contains(world, x, y, z))
            .sorted(Comparator.comparingInt(WorldGuardRegion::priority).reversed())
            .toList();

        for (WorldGuardRegion region : matching) {
            FlagState state = region.flag(flag);
            if (state == FlagState.UNSET) {
                continue;
            }
            if (state == FlagState.ALLOW || region.member(playerUuid)) {
                return ProtectionDecision.allow();
            }
            return ProtectionDecision.deny(region, flag);
        }

        return ProtectionDecision.allow();
    }
}
