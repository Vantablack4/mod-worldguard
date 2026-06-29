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
