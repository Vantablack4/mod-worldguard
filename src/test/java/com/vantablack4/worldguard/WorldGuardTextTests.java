package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class WorldGuardTextTests {
    @Test
    void commandMessagesMatchUpstreamWorldGuardWording() {
        assertThat(WorldGuardText.invalidRegionId("bad id!"))
            .isEqualTo("The region name of 'bad id!' contains characters that are not allowed.");
        assertThat(WorldGuardText.globalNotAllowed())
            .isEqualTo("Sorry, you can't use __global__ here.");
        assertThat(WorldGuardText.noRegion("spawn"))
            .isEqualTo("No region could be found with the name of 'spawn'.");
        assertThat(WorldGuardText.specifyInfoRegion())
            .isEqualTo("Please specify the region with /region info -w world_name region_name.");
        assertThat(WorldGuardText.specifyFlagsRegion())
            .isEqualTo("Please specify the region with /region flags -w world_name region_name.");
        assertThat(WorldGuardText.multipleStandingRegions())
            .isEqualTo("You're standing in several regions (please pick one).");
        assertThat(WorldGuardText.createdRegion("spawn"))
            .isEqualTo("A new region has been made named 'spawn'.");
        assertThat(WorldGuardText.updatedRegionArea("spawn"))
            .isEqualTo("Region 'spawn' has been updated with a new area.");
        assertThat(WorldGuardText.removedRegions("spawn"))
            .isEqualTo("Successfully removed spawn.");
        assertThat(WorldGuardText.unknownFlag("badflag"))
            .isEqualTo("Unknown flag specified: badflag");
        assertThat(WorldGuardText.invalidStateFlag("maybe"))
            .isEqualTo("Expected none/allow/deny but got 'maybe'");
        assertThat(WorldGuardText.flagSet("pvp", "spawn", "deny"))
            .isEqualTo("Region flag pvp set on 'spawn' to 'deny'.");
        assertThat(WorldGuardText.flagRemoved("pvp", "spawn"))
            .isEqualTo("Region flag pvp removed from 'spawn'. (Any -g(roups) were also removed.)");
        assertThat(WorldGuardText.prioritySet("spawn", 5))
            .isEqualTo("Priority of 'spawn' set to 5 (higher numbers override).");
        assertThat(WorldGuardText.inheritanceSet("spawn"))
            .isEqualTo("Inheritance set for region 'spawn'.");
        assertThat(WorldGuardText.inheritanceCleared("spawn"))
            .isEqualTo("Inheritance set for region 'spawn'. Region is now orphaned.");
        assertThat(WorldGuardText.membersAdded("spawn"))
            .isEqualTo("Region 'spawn' updated with new members.");
        assertThat(WorldGuardText.membersRemoved("spawn"))
            .isEqualTo("Region 'spawn' updated with members removed.");
        assertThat(WorldGuardText.ownersAdded("spawn"))
            .isEqualTo("Region 'spawn' updated with new owners.");
        assertThat(WorldGuardText.ownersRemoved("spawn"))
            .isEqualTo("Region 'spawn' updated with owners removed.");
        assertThat(WorldGuardText.configurationReloaded())
            .isEqualTo("WorldGuard configuration reloaded.");
        assertThat(WorldGuardText.regionsLoadedAllWorlds())
            .isEqualTo("Successfully load the region data for all worlds.");
        assertThat(WorldGuardText.regionsSavedAllWorlds())
            .isEqualTo("Successfully saved the region data for all worlds.");
    }

    @Test
    void denyMessagesUseUpstreamDefaultTemplate() {
        assertThat(ProtectionDecision.deny("spawn", WorldGuardFlag.BLOCK_BREAK).message())
            .isEqualTo("Hey! Sorry, but you can't break that block here.");
        assertThat(ProtectionDecision.deny("spawn", WorldGuardFlag.BLOCK_PLACE).message())
            .isEqualTo("Hey! Sorry, but you can't place that block here.");
        assertThat(ProtectionDecision.deny("spawn", WorldGuardFlag.CHEST_ACCESS).message())
            .isEqualTo("Hey! Sorry, but you can't open that here.");
        assertThat(ProtectionDecision.deny("spawn", WorldGuardFlag.PVP).message())
            .isEqualTo("Hey! Sorry, but you can't PvP here.");
    }

    @Test
    void unknownFlagResponseIncludesUpstreamAvailableFlagsPrefix() {
        assertThat(WorldGuardText.availableFlags())
            .startsWith("Available flags: ")
            .contains("build", "pvp", "passthrough")
            .endsWith(", ");
    }
}
