package com.vantablack4.worldguard.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;

final class WorldGuardProtectionHooksTests {
    @Test
    void nonPlayerBuildMutationHonorsBuildDeny() {
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

        assertThat(WorldGuardProtectionHooks.deniesBuild(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5)
        )).isTrue();
    }

    @Test
    void specificNonPlayerFlagCanDenyMutation() {
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
            Map.of(WorldGuardFlag.FIRE_SPREAD, FlagState.DENY)
        );

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5),
            WorldGuardFlag.FIRE_SPREAD
        )).isTrue();
    }

    @Test
    void membersDoNotBypassNonPlayerBuildMutation() {
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
            Set.of(UUID.randomUUID()),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY)
        );

        assertThat(WorldGuardProtectionHooks.deniesBuild(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5)
        )).isTrue();
    }

    @Test
    void unsetEnvironmentalFlagAllowsNonPlayerMutation() {
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
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        );

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5),
            WorldGuardFlag.FIRE_SPREAD
        )).isFalse();
    }
}
