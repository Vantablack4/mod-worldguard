package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

final class WorldGuardPolicyTests {
    @Test
    void denyFlagBlocksNonMembers() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion region = new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            0,
            Set.of(),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY)
        );

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(region),
            "minecraft:overworld",
            5,
            5,
            5,
            WorldGuardFlag.BUILD,
            player,
            false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.regionId()).isEqualTo("spawn");
    }

    @Test
    void higherPriorityAllowOverridesLowerDeny() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion lower = new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            0,
            Set.of(),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY)
        );
        WorldGuardRegion higher = new WorldGuardRegion(
            "shop",
            "minecraft:overworld",
            1,
            1,
            1,
            2,
            2,
            2,
            10,
            Set.of(),
            Map.of(WorldGuardFlag.BUILD, FlagState.ALLOW)
        );

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(lower, higher),
            "minecraft:overworld",
            1,
            1,
            1,
            WorldGuardFlag.BUILD,
            player,
            false
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void membersBypassDenyInTheirRegion() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion region = new WorldGuardRegion(
            "home",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            0,
            Set.of(player),
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        );

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(region),
            "minecraft:overworld",
            5,
            5,
            5,
            WorldGuardFlag.INTERACT,
            player,
            false
        );

        assertThat(decision.allowed()).isTrue();
    }
}
