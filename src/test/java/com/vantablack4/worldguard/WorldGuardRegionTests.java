package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

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
        assertThat(region.flag(WorldGuardFlag.INTERACT)).isEqualTo(FlagState.DENY);
        assertThat(region.flag(WorldGuardFlag.ITEM_USE)).isEqualTo(FlagState.DENY);
    }
}
