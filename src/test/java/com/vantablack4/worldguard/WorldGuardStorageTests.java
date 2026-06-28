package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

final class WorldGuardStorageTests {
    @TempDir
    Path tempDir;

    @Test
    void roundTripsRegions() {
        UUID member = UUID.randomUUID();
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            1,
            2,
            3,
            4,
            5,
            8,
            Set.of(member),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY, WorldGuardFlag.ITEM_USE, FlagState.ALLOW)
        ));

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);
        WorldGuardRegion region = reloaded.find("spawn").orElseThrow();

        assertThat(region.world()).isEqualTo("minecraft:overworld");
        assertThat(region.priority()).isEqualTo(8);
        assertThat(region.members()).containsExactly(member);
        assertThat(region.flag(WorldGuardFlag.BUILD)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.ITEM_USE)).isEqualTo(FlagState.ALLOW);
    }

    @Test
    void removesUnsetFlags() {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            1,
            1,
            1,
            0,
            Set.of(),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY)
        ));

        storage.setFlag("spawn", WorldGuardFlag.BUILD, FlagState.UNSET);

        assertThat(storage.find("spawn").orElseThrow().flag(WorldGuardFlag.BUILD)).isEqualTo(FlagState.UNSET);
    }
}
