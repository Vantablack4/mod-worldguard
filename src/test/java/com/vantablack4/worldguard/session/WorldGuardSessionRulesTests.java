package com.vantablack4.worldguard.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobCategory;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
import com.vantablack4.worldguard.model.RegionType;

final class WorldGuardSessionRulesTests {
    private static final String WORLD = "minecraft:overworld";
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void playerDamageAgainstAnimalsChecksAnimalAndEntityFlags() {
        assertThat(WorldGuardSessionRules.nonPlayerDamageFlags(MobCategory.CREATURE, true, false))
            .containsExactly(WorldGuardFlag.DAMAGE_ANIMALS, WorldGuardFlag.ATTACK_ENTITY);
        assertThat(WorldGuardSessionRules.nonPlayerDamageFlags(MobCategory.WATER_CREATURE, true, false))
            .containsExactly(WorldGuardFlag.DAMAGE_ANIMALS, WorldGuardFlag.ATTACK_ENTITY);
    }

    @Test
    void playerDamageAgainstMonstersChecksGenericEntityAttackFlag() {
        assertThat(WorldGuardSessionRules.nonPlayerDamageFlags(MobCategory.MONSTER, true, false))
            .containsExactly(WorldGuardFlag.ATTACK_ENTITY);
    }

    @Test
    void nonPlayerDamageUsesMobDamageFlag() {
        assertThat(WorldGuardSessionRules.nonPlayerDamageFlags(MobCategory.CREATURE, false, true))
            .containsExactly(WorldGuardFlag.MOB_DAMAGE);
        assertThat(WorldGuardSessionRules.nonPlayerDamageFlags(MobCategory.CREATURE, false, false))
            .isEmpty();
    }

    @Test
    void entryDenyBlocksCrossingIntoRegion() {
        WorldGuardRegion region = region("spawn", Map.of(WorldGuardFlag.ENTRY, FlagState.DENY));

        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            Set.of(region),
            WORLD,
            new BlockPos(-1, 5, 5),
            new BlockPos(0, 5, 5),
            PLAYER,
            false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.decision().regionId()).isEqualTo("spawn");
        assertThat(decision.decision().flag()).isEqualTo(WorldGuardFlag.ENTRY);
    }

    @Test
    void entryDenyDoesNotTrapMovementWithinRegion() {
        WorldGuardRegion region = region("spawn", Map.of(WorldGuardFlag.ENTRY, FlagState.DENY));

        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5),
            new BlockPos(2, 5, 5),
            PLAYER,
            false
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void entryDenyUsesDefaultNonMemberGroupForMovement() {
        WorldGuardRegion region = new WorldGuardRegion(
            "spawn",
            WORLD,
            0,
            0,
            0,
            10,
            10,
            10,
            0,
            Set.of(PLAYER),
            Map.of(WorldGuardFlag.ENTRY, FlagState.DENY)
        );

        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            Set.of(region),
            WORLD,
            new BlockPos(-1, 5, 5),
            new BlockPos(0, 5, 5),
            PLAYER,
            false
        );

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void exitDenyBlocksLeavingRegion() {
        WorldGuardRegion region = region("jail", Map.of(WorldGuardFlag.EXIT, FlagState.DENY));

        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            Set.of(region),
            WORLD,
            new BlockPos(5, 5, 5),
            new BlockPos(11, 5, 5),
            PLAYER,
            false
        );

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.decision().regionId()).isEqualTo("jail");
        assertThat(decision.decision().flag()).isEqualTo(WorldGuardFlag.EXIT);
    }

    @Test
    void notifyFlagsCreateTransitionMessages() {
        WorldGuardRegion region = region(
            "spawn",
            Map.of(
                WorldGuardFlag.NOTIFY_ENTER,
                FlagState.ALLOW,
                WorldGuardFlag.NOTIFY_LEAVE,
                FlagState.ALLOW
            )
        );
        WorldGuardSessionSnapshot outside = WorldGuardSessionRules.snapshot(
            Set.of(region),
            WORLD,
            new BlockPos(-1, 5, 5)
        );
        WorldGuardSessionSnapshot inside = WorldGuardSessionRules.snapshot(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5)
        );

        assertThat(WorldGuardSessionRules.messagesForTransition(Set.of(region), outside, inside, "Steve"))
            .extracting(WorldGuardSessionMessage::message)
            .containsExactly("Steve entered NOTIFY region: spawn");
        assertThat(WorldGuardSessionRules.messagesForTransition(Set.of(region), inside, outside, "Steve"))
            .extracting(WorldGuardSessionMessage::message)
            .containsExactly("Steve left NOTIFY region");
    }

    @Test
    void greetingAndFarewellMessagesUseTypedRegionValues() {
        WorldGuardRegion region = region("spawn", Map.of())
            .withValue(
                WorldGuardValueFlag.GREETING,
                WorldGuardFlagValue.parse(WorldGuardValueFlag.GREETING, "Welcome").orElseThrow()
            )
            .withValue(
                WorldGuardValueFlag.FAREWELL,
                WorldGuardFlagValue.parse(WorldGuardValueFlag.FAREWELL, "Goodbye").orElseThrow()
            );
        WorldGuardSessionSnapshot outside = WorldGuardSessionRules.snapshot(
            Set.of(region),
            WORLD,
            new BlockPos(-1, 5, 5)
        );
        WorldGuardSessionSnapshot inside = WorldGuardSessionRules.snapshot(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5)
        );

        assertThat(WorldGuardSessionRules.messagesForTransition(Set.of(region), outside, inside, "Steve"))
            .extracting(WorldGuardSessionMessage::message)
            .containsExactly("Welcome");
        assertThat(WorldGuardSessionRules.messagesForTransition(Set.of(region), inside, outside, "Steve"))
            .extracting(WorldGuardSessionMessage::message)
            .containsExactly("Goodbye");
    }

    @Test
    void blockedCommandsDenyMatchingCommandPrefixes() {
        WorldGuardRegion region = region("spawn", Map.of())
            .withValue(
                WorldGuardValueFlag.BLOCKED_CMDS,
                WorldGuardFlagValue.parse(WorldGuardValueFlag.BLOCKED_CMDS, "/home,/spawn").orElseThrow()
            );

        assertThat(WorldGuardSessionRules.commandAllowed(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5),
            PLAYER,
            Set.of(),
            false,
            "home base"
        )).isFalse();
        assertThat(WorldGuardSessionRules.commandAllowed(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5),
            PLAYER,
            Set.of(),
            false,
            "msg Deniz hello"
        )).isTrue();
    }

    @Test
    void allowedCommandsWhitelistMatchingCommandPrefixes() {
        WorldGuardRegion region = region("spawn", Map.of())
            .withValue(
                WorldGuardValueFlag.ALLOWED_CMDS,
                WorldGuardFlagValue.parse(WorldGuardValueFlag.ALLOWED_CMDS, "/spawn").orElseThrow()
            );

        assertThat(WorldGuardSessionRules.commandAllowed(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5),
            PLAYER,
            Set.of(),
            false,
            "spawn"
        )).isTrue();
        assertThat(WorldGuardSessionRules.commandAllowed(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 5),
            PLAYER,
            Set.of(),
            false,
            "home"
        )).isFalse();
    }

    @Test
    void respawnLocationUsesSpawnFlagDefaultMemberGroup() {
        WorldGuardFlagValue.LocationValue spawn = WorldGuardFlagValue.location(
            WORLD,
            25,
            72,
            -4,
            90,
            10
        ).orElseThrow().asLocation().orElseThrow();
        WorldGuardRegion region = new WorldGuardRegion(
            "spawn",
            WORLD,
            0,
            0,
            0,
            10,
            10,
            10,
            0,
            "",
            RegionType.CUBOID,
            Set.of(),
            Set.of(PLAYER),
            Set.of(),
            Set.of(),
            Map.of()
        ).withValue(
            WorldGuardValueFlag.SPAWN,
            WorldGuardFlagValue.location(WORLD, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()).orElseThrow()
        );

        assertThat(WorldGuardSessionRules.respawnLocation(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 1),
            PLAYER,
            Set.of()
        )).contains(spawn);
        assertThat(WorldGuardSessionRules.respawnLocation(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 1),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            Set.of()
        )).isEmpty();
    }

    @Test
    void respawnLocationHonorsExplicitSpawnGroup() {
        WorldGuardFlagValue spawn = WorldGuardFlagValue.location(WORLD, 1, 2, 3, 4, 5).orElseThrow();
        WorldGuardRegion region = region("spawn", Map.of())
            .withValue(WorldGuardValueFlag.SPAWN, spawn)
            .withFlagGroup(WorldGuardValueFlag.SPAWN, WorldGuardRegionGroup.ALL);

        assertThat(WorldGuardSessionRules.respawnLocation(
            Set.of(region),
            WORLD,
            new BlockPos(1, 5, 1),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            Set.of()
        )).contains(spawn.asLocation().orElseThrow());
    }

    @Test
    void timeLockAcceptsUpstreamAbsoluteAndRelativeNumericValues() {
        assertThat(WorldGuardSessionRules.timeLock("6000"))
            .contains(new WorldGuardSessionRules.TimeLock(6000, false));
        assertThat(WorldGuardSessionRules.timeLock("+1200"))
            .contains(new WorldGuardSessionRules.TimeLock(1200, true));
        assertThat(WorldGuardSessionRules.timeLock("-500"))
            .contains(new WorldGuardSessionRules.TimeLock(-500, true));
        assertThat(WorldGuardSessionRules.timeLock("day")).isEmpty();
    }

    @Test
    void weatherLockRecognizesUpstreamWeatherIds() {
        assertThat(WorldGuardSessionRules.weatherLock("clear"))
            .contains(WorldGuardSessionRules.WeatherLock.CLEAR);
        assertThat(WorldGuardSessionRules.weatherLock("rain"))
            .contains(WorldGuardSessionRules.WeatherLock.RAIN);
        assertThat(WorldGuardSessionRules.weatherLock("thunder-storm"))
            .contains(WorldGuardSessionRules.WeatherLock.THUNDER_STORM);
        assertThat(WorldGuardSessionRules.weatherLock("snow")).isEmpty();
    }

    @Test
    void invincibilityFlagIsEnabledOnlyByAllowState() {
        WorldGuardRegion invincible = region("arena", Map.of(WorldGuardFlag.INVINCIBILITY, FlagState.ALLOW));

        assertThat(WorldGuardSessionRules.enabledRegion(
            Set.of(invincible),
            WORLD,
            new BlockPos(5, 5, 5),
            PLAYER,
            false,
            WorldGuardFlag.INVINCIBILITY
        )).contains("arena");
    }

    @Test
    void bypassIgnoresSessionDecisions() {
        WorldGuardRegion region = region("spawn", Map.of(WorldGuardFlag.SEND_CHAT, FlagState.DENY));

        assertThat(WorldGuardSessionRules.checkAny(
            Set.of(region),
            WORLD,
            new BlockPos(5, 5, 5),
            PLAYER,
            true,
            WorldGuardFlag.SEND_CHAT
        ).allowed()).isTrue();
    }

    private static WorldGuardRegion region(String id, Map<WorldGuardFlag, FlagState> flags) {
        return new WorldGuardRegion(id, WORLD, 0, 0, 0, 10, 10, 10, 0, Set.of(), flags);
    }
}
