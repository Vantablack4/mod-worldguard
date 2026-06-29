package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.vantablack4.worldguard.WorldGuardRegion.PolygonPoint;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
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
    void mutatesOwnerAndMemberGroupsByWorld() {
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
            Set.of(),
            Map.of()
        ));
        storage.save(new WorldGuardRegion(
            "town",
            "minecraft:the_nether",
            0,
            0,
            0,
            10,
            10,
            10,
            4,
            Set.of(),
            Map.of()
        ));

        storage.addOwnerGroup("town", "minecraft:overworld", "g:Mayor");
        storage.addMemberGroup("town", "minecraft:overworld", "group:Resident");
        storage.addMemberGroup("town", "minecraft:the_nether", "visitor");
        storage.removeMemberGroup("town", "minecraft:overworld", "resident");
        storage.clearOwners("town", "minecraft:overworld");
        storage.clearMembers("town", "minecraft:the_nether");

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);

        WorldGuardRegion overworld = reloaded.find("town", "minecraft:overworld").orElseThrow();
        WorldGuardRegion nether = reloaded.find("town", "minecraft:the_nether").orElseThrow();
        assertThat(overworld.ownerGroups()).isEmpty();
        assertThat(overworld.memberGroups()).isEmpty();
        assertThat(nether.ownerGroups()).isEmpty();
        assertThat(nether.memberGroups()).isEmpty();
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
        assertThat(stored).contains("schema-version=4");
        assertThat(stored).contains(".id=claim");
        assertThat(stored).contains(".type=polygon");
        assertThat(stored).contains(".min-x=-2");
        assertThat(stored).contains(".max-z=5");
        assertThat(stored).contains(".polygon-points=-2,1;4,1;4,5;0,5");
    }

    @Test
    void storesGlobalRegionsPerWorld() {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.findOrCreateGlobal("minecraft:overworld");
        storage.setFlag(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:overworld", WorldGuardFlag.PVP, FlagState.DENY);
        storage.findOrCreateGlobal("minecraft:the_nether");
        storage.setFlag(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:the_nether", WorldGuardFlag.PVP, FlagState.ALLOW);

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);

        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:overworld").orElseThrow()
            .flag(WorldGuardFlag.PVP)).isEqualTo(FlagState.DENY);
        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:the_nether").orElseThrow()
            .flag(WorldGuardFlag.PVP)).isEqualTo(FlagState.ALLOW);

        assertThat(reloaded.delete(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:overworld")).isTrue();
        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:overworld")).isEmpty();
        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:the_nether")).isPresent();
    }

    @Test
    void normalizesBukkitStyleDefaultWorldAliases() {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.findOrCreateGlobal("world");
        storage.setFlag(WorldGuardRegion.GLOBAL_REGION_ID, "world", WorldGuardFlag.SOIL_DRY, FlagState.DENY);
        storage.findOrCreateGlobal("world_nether");
        storage.findOrCreateGlobal("world_the_end");

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);

        WorldGuardRegion overworld = reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:overworld")
            .orElseThrow();
        assertThat(overworld.world()).isEqualTo("minecraft:overworld");
        assertThat(overworld.flag(WorldGuardFlag.SOIL_DRY)).isEqualTo(FlagState.DENY);
        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "world")).contains(overworld);
        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:the_nether")).isPresent();
        assertThat(reloaded.find(WorldGuardRegion.GLOBAL_REGION_ID, "minecraft:the_end")).isPresent();
    }

    @Test
    void deleteCanRefuseRegionsWithChildren() {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(region("parent"));
        storage.save(region("child").withParent("parent"));

        assertThat(storage.childRegions("parent", "minecraft:overworld"))
            .extracting(WorldGuardRegion::id)
            .containsExactly("child");
        assertThat(storage.delete("parent", "minecraft:overworld", WorldGuardStorage.DeleteMode.REFUSE_CHILDREN))
            .isFalse();

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);
        assertThat(reloaded.find("parent", "minecraft:overworld")).isPresent();
        assertThat(reloaded.find("child", "minecraft:overworld").orElseThrow().parentId())
            .isEqualTo("parent");
    }

    @Test
    void deleteCanUnsetParentInChildren() {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(region("parent"));
        storage.save(region("child").withParent("parent"));

        assertThat(storage.delete(
            "parent",
            "minecraft:overworld",
            WorldGuardStorage.DeleteMode.UNSET_PARENT_IN_CHILDREN
        )).isTrue();

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);
        assertThat(reloaded.find("parent", "minecraft:overworld")).isEmpty();
        assertThat(reloaded.find("child", "minecraft:overworld").orElseThrow().parentId())
            .isBlank();
    }

    @Test
    void deleteCanRemoveDescendantChildren() {
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(region("parent"));
        storage.save(region("child").withParent("parent"));
        storage.save(region("grandchild").withParent("child"));

        assertThat(storage.descendantRegions("parent", "minecraft:overworld"))
            .extracting(WorldGuardRegion::id)
            .containsExactly("child", "grandchild");
        assertThat(storage.delete("parent", "minecraft:overworld", WorldGuardStorage.DeleteMode.REMOVE_CHILDREN))
            .isTrue();

        WorldGuardStorage reloaded = WorldGuardStorage.load(tempDir);
        assertThat(reloaded.find("parent", "minecraft:overworld")).isEmpty();
        assertThat(reloaded.find("child", "minecraft:overworld")).isEmpty();
        assertThat(reloaded.find("grandchild", "minecraft:overworld")).isEmpty();
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

    @Test
    void roundTripsTypedValuesAndFlagGroups() throws IOException {
        WorldGuardFlagValue blockedCommands = WorldGuardFlagValue.parse(
            WorldGuardValueFlag.BLOCKED_CMDS,
            "/spawn,/home"
        ).orElseThrow();
        WorldGuardFlagValue teleport = WorldGuardFlagValue.location(
            "minecraft:overworld",
            10.5D,
            64D,
            -2.25D,
            90F,
            12.5F
        ).orElseThrow();
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        storage.save(new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            1,
            Set.of(),
            Map.of(WorldGuardFlag.ENTRY, FlagState.DENY)
        ).withValue(WorldGuardValueFlag.BLOCKED_CMDS, blockedCommands)
            .withValue(WorldGuardValueFlag.TELEPORT, teleport)
            .withFlagGroup(WorldGuardFlag.ENTRY, WorldGuardRegionGroup.MEMBERS)
            .withFlagGroup(WorldGuardValueFlag.TELEPORT, WorldGuardRegionGroup.OWNERS));

        WorldGuardRegion reloaded = WorldGuardStorage.load(tempDir).find("spawn").orElseThrow();
        String stored = Files.readString(tempDir.resolve("regions.properties"));

        assertThat(reloaded.value(WorldGuardValueFlag.BLOCKED_CMDS)).contains(blockedCommands);
        assertThat(reloaded.value(WorldGuardValueFlag.TELEPORT)).contains(teleport);
        assertThat(reloaded.flagGroup(WorldGuardFlag.ENTRY)).isEqualTo(WorldGuardRegionGroup.MEMBERS);
        assertThat(reloaded.flagGroup(WorldGuardValueFlag.TELEPORT)).isEqualTo(WorldGuardRegionGroup.OWNERS);
        assertThat(stored).contains(".flag.blocked-cmds=/home,/spawn");
        assertThat(stored).contains(".flag.teleport=minecraft\\:overworld,10.5,64.0,-2.25,90.0,12.5");
        assertThat(stored).contains(".flag.entry-group=members");
        assertThat(stored).contains(".flag.teleport-group=owners");
    }

    private static WorldGuardRegion region(String id) {
        return WorldGuardRegion.defaultProtected(
            id,
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            0
        );
    }
}
