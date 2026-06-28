package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vantablack4.worldguard.WorldGuardRegion.PolygonPoint;
import com.vantablack4.worldguard.model.RegionType;

import org.junit.jupiter.api.Test;

final class WorldGuardRegionTests {
    @Test
    void normalizesBoundsAndIds() {
        WorldGuardRegion region = new WorldGuardRegion(
            " Spawn Area! ",
            "minecraft:overworld",
            10,
            90,
            -4,
            -10,
            60,
            4,
            3,
            Set.of(),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY)
        );

        assertThat(region.id()).isEqualTo("spawn_area");
        assertThat(region.minX()).isEqualTo(-10);
        assertThat(region.maxX()).isEqualTo(10);
        assertThat(region.minY()).isEqualTo(60);
        assertThat(region.maxY()).isEqualTo(90);
        assertThat(region.contains("minecraft:overworld", 0, 70, 0)).isTrue();
        assertThat(region.contains("minecraft:the_nether", 0, 70, 0)).isFalse();
    }

    @Test
    void defaultProtectedRegionsDenyCoreActions() {
        WorldGuardRegion region = WorldGuardRegion.defaultProtected(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            1,
            1,
            1,
            0
        );

        assertThat(region.flag(WorldGuardFlag.BUILD)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.BLOCK_BREAK)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.BLOCK_PLACE)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.USE)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.INTERACT)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.ITEM_USE)).isEqualTo(FlagState.DENY);
    }

    @Test
    void globalRegionsDoNotContainPhysicalPositions() {
        WorldGuardRegion region = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.INTERACT, FlagState.DENY);

        assertThat(region.global()).isTrue();
        assertThat(region.contains("minecraft:overworld", 0, 64, 0)).isFalse();
        assertThat(region.appliesToWorld("minecraft:overworld")).isTrue();
        assertThat(region.flag(WorldGuardFlag.INTERACT)).isEqualTo(FlagState.DENY);
    }

    @Test
    void ownerAndMemberDomainsAreSeparate() {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();

        WorldGuardRegion region = new WorldGuardRegion(
            "plot",
            "minecraft:overworld",
            0,
            0,
            0,
            1,
            1,
            1,
            0,
            "",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(owner),
            Set.of(member),
            Set.of("mayor"),
            Set.of("resident"),
            Map.of()
        );

        assertThat(region.owner(owner)).isTrue();
        assertThat(region.member(owner)).isTrue();
        assertThat(region.member(member)).isTrue();
        assertThat(region.ownersDomain().groups()).containsExactly("mayor");
        assertThat(region.membersDomain().groups()).containsExactly("resident");
    }

    @Test
    void polygonRegionsUseTwoDimensionalFootprintAndYBounds() {
        WorldGuardRegion region = WorldGuardRegion.defaultProtectedPolygon(
            "triangle",
            "minecraft:overworld",
            10,
            12,
            List.of(
                new PolygonPoint(0, 0),
                new PolygonPoint(4, 0),
                new PolygonPoint(0, 4)
            ),
            5
        );

        assertThat(region.type()).isEqualTo(RegionType.POLYGON);
        assertThat(region.minX()).isZero();
        assertThat(region.maxZ()).isEqualTo(4);
        assertThat(region.contains("minecraft:overworld", 1, 11, 1)).isTrue();
        assertThat(region.contains("minecraft:overworld", 2, 10, 0)).isTrue();
        assertThat(region.contains("minecraft:overworld", 3, 11, 3)).isFalse();
        assertThat(region.contains("minecraft:overworld", 1, 9, 1)).isFalse();
        assertThat(region.contains("minecraft:the_nether", 1, 11, 1)).isFalse();
    }
}
