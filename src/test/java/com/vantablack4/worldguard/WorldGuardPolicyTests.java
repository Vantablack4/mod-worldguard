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

    @Test
    void denyWinsWhenEqualPriorityRegionsConflict() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion allow = new WorldGuardRegion(
            "shop",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            5,
            Set.of(),
            Map.of(WorldGuardFlag.INTERACT, FlagState.ALLOW)
        );
        WorldGuardRegion deny = new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            10,
            10,
            10,
            5,
            Set.of(),
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        );

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(allow, deny),
            "minecraft:overworld",
            5,
            5,
            5,
            WorldGuardFlag.INTERACT,
            player,
            false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.regionId()).isEqualTo("spawn");
    }

    @Test
    void childRegionsInheritParentFlags() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion parent = new WorldGuardRegion(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            20,
            20,
            20,
            3,
            Set.of(),
            Map.of(WorldGuardFlag.ITEM_USE, FlagState.DENY)
        );
        WorldGuardRegion child = new WorldGuardRegion(
            "shop",
            "minecraft:overworld",
            2,
            2,
            2,
            4,
            4,
            4,
            3,
            "spawn",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of()
        );

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(parent, child),
            "minecraft:overworld",
            3,
            3,
            3,
            WorldGuardFlag.ITEM_USE,
            player,
            false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.regionId()).isEqualTo("shop");
    }

    @Test
    void globalRegionsApplyAfterPhysicalRegions() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.INTERACT, FlagState.DENY);

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(global),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.INTERACT,
            player,
            false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.regionId()).isEqualTo(WorldGuardRegion.GLOBAL_REGION_ID);
    }

    @Test
    void blankGlobalRegionDoesNotDenyMembershipDefaultBuild() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld");

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(global),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            player,
            false
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void globalRegionMembersMakeMembershipDefaultsApplyEverywhere() {
        UUID member = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withMember(member);

        ProtectionDecision memberDecision = WorldGuardPolicy.evaluate(
            List.of(global),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            member,
            false
        );
        ProtectionDecision outsiderDecision = WorldGuardPolicy.evaluate(
            List.of(global),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            outsider,
            false
        );

        assertThat(memberDecision.allowed()).isTrue();
        assertThat(outsiderDecision.allowed()).isFalse();
        assertThat(outsiderDecision.regionId()).isEqualTo(WorldGuardRegion.GLOBAL_REGION_ID);
    }

    @Test
    void globalBuildAllowIsIgnoredButDenyStillApplies() {
        UUID player = UUID.randomUUID();
        WorldGuardRegion buildAllow = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.BUILD, FlagState.ALLOW);
        WorldGuardRegion buildDeny = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.BUILD, FlagState.DENY);

        ProtectionDecision allowDecision = WorldGuardPolicy.evaluate(
            List.of(buildAllow),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            player,
            false
        );
        ProtectionDecision denyDecision = WorldGuardPolicy.evaluate(
            List.of(buildDeny),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            player,
            false
        );

        assertThat(allowDecision.allowed()).isTrue();
        assertThat(denyDecision.allowed()).isFalse();
        assertThat(denyDecision.regionId()).isEqualTo(WorldGuardRegion.GLOBAL_REGION_ID);
    }

    @Test
    void ownersInheritedFromParentsBypassMemberDeny() {
        UUID owner = UUID.randomUUID();
        WorldGuardRegion parent = new WorldGuardRegion(
            "town",
            "minecraft:overworld",
            0,
            0,
            0,
            20,
            20,
            20,
            2,
            "",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(owner),
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        );
        WorldGuardRegion child = new WorldGuardRegion(
            "plot",
            "minecraft:overworld",
            1,
            1,
            1,
            3,
            3,
            3,
            2,
            "town",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of()
        );

        ProtectionDecision decision = WorldGuardPolicy.evaluate(
            List.of(parent, child),
            "minecraft:overworld",
            2,
            2,
            2,
            WorldGuardFlag.INTERACT,
            owner,
            false
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void ownerGroupsInheritedFromParentsBypassMemberDeny() {
        UUID player = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        WorldGuardRegion parent = new WorldGuardRegion(
            "town",
            "minecraft:overworld",
            0,
            0,
            0,
            20,
            20,
            20,
            2,
            "",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(),
            Set.of(),
            Set.of("mayor"),
            Set.of(),
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        );
        WorldGuardRegion child = new WorldGuardRegion(
            "plot",
            "minecraft:overworld",
            1,
            1,
            1,
            3,
            3,
            3,
            2,
            "town",
            com.vantablack4.worldguard.model.RegionType.CUBOID,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of(),
            Map.of()
        );

        ProtectionDecision groupOwnerDecision = WorldGuardPolicy.evaluate(
            List.of(parent, child),
            "minecraft:overworld",
            2,
            2,
            2,
            WorldGuardFlag.INTERACT,
            player,
            Set.of("Mayor"),
            false
        );
        ProtectionDecision outsiderDecision = WorldGuardPolicy.evaluate(
            List.of(parent, child),
            "minecraft:overworld",
            2,
            2,
            2,
            WorldGuardFlag.INTERACT,
            outsider,
            Set.of(),
            false
        );

        assertThat(groupOwnerDecision.allowed()).isTrue();
        assertThat(outsiderDecision.allowed()).isFalse();
        assertThat(outsiderDecision.regionId()).isEqualTo("plot");
    }

    @Test
    void globalRegionMemberGroupsMakeMembershipDefaultsApplyEverywhere() {
        UUID member = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();
        WorldGuardRegion global = new WorldGuardRegion(
            WorldGuardRegion.GLOBAL_REGION_ID,
            "minecraft:overworld",
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            "",
            com.vantablack4.worldguard.model.RegionType.GLOBAL,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of("resident"),
            Map.of()
        );

        ProtectionDecision memberDecision = WorldGuardPolicy.evaluate(
            List.of(global),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            member,
            Set.of("Resident"),
            false
        );
        ProtectionDecision outsiderDecision = WorldGuardPolicy.evaluate(
            List.of(global),
            "minecraft:overworld",
            100,
            64,
            100,
            WorldGuardFlag.BUILD,
            outsider,
            Set.of(),
            false
        );

        assertThat(memberDecision.allowed()).isTrue();
        assertThat(outsiderDecision.allowed()).isFalse();
        assertThat(outsiderDecision.regionId()).isEqualTo(WorldGuardRegion.GLOBAL_REGION_ID);
    }
}
