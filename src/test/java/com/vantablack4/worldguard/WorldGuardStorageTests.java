package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vantablack4.worldguard.WorldGuardRegion.PolygonPoint;
import com.vantablack4.worldguard.model.RegionType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
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

    @Test
    void roundTripsParentsOwnersGroupsAndGlobalRegions() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(new WorldGuardRegion(
            "town",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            4,
            "",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(owner),
            Set.of(member),
            Set.of("mayor"),
            Set.of("resident"),
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        ));
        storage.save(new WorldGuardRegion(
            "plot",
            "minecraft:overworld",
            1,
            1,
            1,
            2,
            2,
            2,
            4,
            "town",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of()
        ));
        storage.save(WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.PVP, FlagState.DENY));

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);

        WorldGuardRegion town = reloaded.find("town").orElseThrow();
        WorldGuardRegion plot = reloaded.find("plot").orElseThrow();
        WorldGuardRegion global = reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID).orElseThrow();

        assertThat(town.owners()).containsExactly(owner);
        assertThat(town.members()).containsExactly(member);
        assertThat(town.ownerGroups()).containsExactly("mayor");
        assertThat(town.memberGroups()).containsExactly("resident");
        assertThat(plot.parentId()).isEqualTo("town");
        assertThat(global.global()).isTrue();
        assertThat(global.flag(WorldGuardFlag.PVP)).isEqualTo(FlagState.DENY);
    }

    @Test
    void roundTripsPolygonRegionsWithBoundingProperties() throws IOException {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(WorldGuardRegion.defaultProtectedPolygon(
            "Claim",
            "minecraft:overworld",
            4,
            18,
            List.of(
                new PolygonPoint(-2, 1),
                new PolygonPoint(4, 1),
                new PolygonPoint(4, 5),
                new PolygonPoint(0, 5)
            ),
            9
        ));

        WorldGuardRegion region = WorldGuardStorage.load(tempDir).find("claim").orElseThrow();
        String stored = Files.readString(tempDir.resolve("regions.properties"));

        assertThat(region.type()).isEqualTo(RegionType.POLYGON);
        assertThat(region.priority()).isEqualTo(9);
        assertThat(region.minX()).isEqualTo(-2);
        assertThat(region.maxX()).isEqualTo(4);
        assertThat(region.minY()).isEqualTo(4);
        assertThat(region.maxY()).isEqualTo(18);
        assertThat(region.polygonPoints()).containsExactly(
            new PolygonPoint(-2, 1),
            new PolygonPoint(4, 1),
            new PolygonPoint(4, 5),
            new PolygonPoint(0, 5)
        );
        assertThat(region.contains("minecraft:overworld", 1, 10, 3)).isTrue();
        assertThat(region.contains("minecraft:overworld", -2, 10, 5)).isFalse();
        assertThat(stored).contains("schema-version=3");
        assertThat(stored).contains("region.claim.type=polygon");
        assertThat(stored).contains("region.claim.min-x=-2");
        assertThat(stored).contains("region.claim.max-z=5");
        assertThat(stored).contains("region.claim.polygon-points=-2,1;4,1;4,5;0,5");
    }

    @Test
    void skipsMalformedDomainUuidsWithoutDroppingRegion() throws IOException {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("regions.properties"), """
            region.spawn.id=spawn
            region.spawn.world=minecraft:overworld
            region.spawn.min-x=0
            region.spawn.min-y=0
            region.spawn.min-z=0
            region.spawn.max-x=1
            region.spawn.max-y=1
            region.spawn.max-z=1
            region.spawn.priority=1
            region.spawn.members=not-a-uuid
            region.spawn.flag.build=deny
            """);

        WorldGuardRegion region = WorldGuardStorage.load(tempDir).find("spawn").orElseThrow();

        assertThat(region.members()).isEmpty();
        assertThat(region.type()).isEqualTo(RegionType.CUBOID);
        assertThat(region.flag(WorldGuardFlag.BUILD)).isEqualTo(FlagState.DENY);
    }
}
