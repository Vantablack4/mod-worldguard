package com.vantablack4.worldguard.worldedit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.WorldGuardRegion.PolygonPoint;
import com.vantablack4.worldguard.model.RegionType;

final class WorldEditRegionSelectionTests {
    @Test
    void createsDefaultProtectedRegionFromSelectionBounds() {
        WorldEditRegionSelection selection = new WorldEditRegionSelection(
            "minecraft:overworld",
            -3,
            64,
            8,
            9,
            72,
            12
        );

        WorldGuardRegion region = selection.toDefaultProtectedRegion("Spawn", 7);

        assertThat(region.id()).isEqualTo("spawn");
        assertThat(region.world()).isEqualTo("minecraft:overworld");
        assertThat(region.minX()).isEqualTo(-3);
        assertThat(region.maxZ()).isEqualTo(12);
        assertThat(region.priority()).isEqualTo(7);
        assertThat(region.flag(WorldGuardFlag.BUILD)).isEqualTo(FlagState.DENY);
        assertThat(selection.volume()).isEqualTo(585);
    }

    @Test
    void createsDefaultProtectedPolygonRegionFromSelectionPoints() {
        WorldEditRegionSelection selection = WorldEditRegionSelection.polygonal(
            "minecraft:overworld",
            20,
            40,
            List.of(
                new PolygonPoint(0, 0),
                new PolygonPoint(6, 0),
                new PolygonPoint(3, 5)
            ),
            123L
        );

        WorldGuardRegion region = selection.toDefaultProtectedRegion("Market", 11);

        assertThat(selection.type()).isEqualTo(RegionType.POLYGON);
        assertThat(selection.volume()).isEqualTo(123L);
        assertThat(region.type()).isEqualTo(RegionType.POLYGON);
        assertThat(region.priority()).isEqualTo(11);
        assertThat(region.polygonPoints()).containsExactly(
            new PolygonPoint(0, 0),
            new PolygonPoint(6, 0),
            new PolygonPoint(3, 5)
        );
        assertThat(region.contains("minecraft:overworld", 3, 30, 2)).isTrue();
        assertThat(region.contains("minecraft:overworld", 0, 30, 5)).isFalse();
        assertThat(region.flag(WorldGuardFlag.BUILD)).isEqualTo(FlagState.DENY);
    }
}
