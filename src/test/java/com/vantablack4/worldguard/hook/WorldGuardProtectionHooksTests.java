package com.vantablack4.worldguard.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;

final class WorldGuardProtectionHooksTests {
    @Test
    void chestAccessTargetsInventoryBlocksOnly() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(
            new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState()),
            Blocks.CHEST
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.ENDER_CHEST)).isTrue();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.CRAFTING_TABLE)).isFalse();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.ANVIL)).isFalse();
        assertThat(WorldGuardProtectionHooks.isEntityContainerAccess(null)).isFalse();
    }

    @Test
    void bucketTargetsClickedFluidForPickupAndAdjacentBlockForPlacement() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BlockPos clicked = new BlockPos(5, 64, 5);

        assertThat(WorldGuardProtectionHooks.bucketMutationTarget(Fluids.EMPTY, clicked, Direction.UP)).isEqualTo(clicked);
        assertThat(WorldGuardProtectionHooks.bucketMutationTarget(Fluids.WATER, clicked, Direction.UP)).isEqualTo(clicked.above());
        assertThat(WorldGuardProtectionHooks.bucketMutationFlags(Fluids.WATER, null))
            .contains(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE, WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE, WorldGuardFlag.WATER_FLOW);
        assertThat(WorldGuardProtectionHooks.bucketMutationFlags(Fluids.LAVA, null))
            .contains(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE, WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE, WorldGuardFlag.LAVA_FLOW);
        assertThat(WorldGuardProtectionHooks.bucketMutationFlags(Fluids.EMPTY, null))
            .contains(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_BREAK, WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE);
    }

    @Test
    void mobSpawningAppliesToMobCategoriesOnly() {
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.CREATURE)).isTrue();
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.MONSTER)).isTrue();
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.WATER_AMBIENT)).isTrue();
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.MISC)).isFalse();
    }

    @Test
    void nonLivingDamageUsesSpecificHangingEntityFlags() {
        assertThat(WorldGuardProtectionHooks.nonLivingDamageFlags(EntityType.PAINTING, null))
            .containsExactly(WorldGuardFlag.ENTITY_PAINTING_DESTROY);
        assertThat(WorldGuardProtectionHooks.nonLivingDamageFlags(EntityType.ITEM_FRAME, null))
            .containsExactly(WorldGuardFlag.ENTITY_ITEM_FRAME_DESTROY);
        assertThat(WorldGuardProtectionHooks.nonLivingDamageFlags(EntityType.GLOW_ITEM_FRAME, null))
            .containsExactly(WorldGuardFlag.ENTITY_ITEM_FRAME_DESTROY);
        assertThat(WorldGuardProtectionHooks.nonLivingDamageFlags(EntityType.MINECART, null))
            .isEmpty();
    }

    @Test
    void nonPlayerBuildMutationHonorsBuildDeny() {
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

        assertThat(WorldGuardProtectionHooks.deniesBuild(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5)
        )).isTrue();
    }

    @Test
    void specificNonPlayerFlagCanDenyMutation() {
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
            Map.of(WorldGuardFlag.FIRE_SPREAD, FlagState.DENY)
        );

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5),
            WorldGuardFlag.FIRE_SPREAD
        )).isTrue();
    }

    @Test
    void membersDoNotBypassNonPlayerBuildMutation() {
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
            Set.of(UUID.randomUUID()),
            Map.of(WorldGuardFlag.BUILD, FlagState.DENY)
        );

        assertThat(WorldGuardProtectionHooks.deniesBuild(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5)
        )).isTrue();
    }

    @Test
    void unsetEnvironmentalFlagAllowsNonPlayerMutation() {
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
            Map.of(WorldGuardFlag.INTERACT, FlagState.DENY)
        );

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(region),
            "minecraft:overworld",
            new BlockPos(5, 5, 5),
            WorldGuardFlag.FIRE_SPREAD
        )).isFalse();
    }
}
