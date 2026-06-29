package com.vantablack4.worldguard.hook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;

final class WorldGuardProtectionHooksTests {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void chestAccessTargetsInventoryBlocksOnly() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(
            new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState()),
            Blocks.CHEST
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.ENDER_CHEST)).isTrue();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.BARREL)).isTrue();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.SHULKER_BOX)).isTrue();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.CRAFTING_TABLE)).isFalse();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.ANVIL)).isFalse();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.BEACON)).isFalse();
        assertThat(WorldGuardProtectionHooks.isContainerAccessCandidate(null, Blocks.LECTERN)).isFalse();
        assertThat(WorldGuardProtectionHooks.isEntityContainerAccess(null)).isFalse();
    }

    @Test
    void doubleChestContainerAccessChecksBothHalves() {
        BlockPos pos = new BlockPos(5, 64, 5);
        var single = Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.SINGLE);
        var doubleChest = Blocks.CHEST.defaultBlockState()
            .setValue(ChestBlock.TYPE, ChestType.LEFT)
            .setValue(ChestBlock.FACING, Direction.NORTH);

        assertThat(WorldGuardProtectionHooks.connectedContainerPositions(pos, single))
            .containsExactly(pos);
        assertThat(WorldGuardProtectionHooks.connectedContainerPositions(pos, doubleChest))
            .hasSize(2)
            .contains(pos);
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
    void blockPlacementFlagsAddTntFlagForTntBlocksOnly() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertThat(WorldGuardProtectionHooks.blockPlacementFlags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE);
        assertThat(WorldGuardProtectionHooks.tntPlacementFlags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE, WorldGuardFlag.TNT);
        assertThat(WorldGuardProtectionHooks.lighterFlags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE, WorldGuardFlag.LIGHTER);
        assertThat(WorldGuardProtectionHooks.blockPlacementRule(Items.FLINT_AND_STEEL).flags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE, WorldGuardFlag.LIGHTER);
        assertThat(WorldGuardProtectionHooks.blockPlacementRule(Items.STICK))
            .isNull();
    }

    @Test
    void blockUseRulesMatchUpstreamSpecificInteractionFlags() {
        assertThat(WorldGuardProtectionHooks.blockUseRule(Blocks.ANVIL.defaultBlockState()).flags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.USE_ANVIL);
        assertThat(WorldGuardProtectionHooks.blockUseRule(Blocks.RED_BED.defaultBlockState()).flags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.INTERACT, WorldGuardFlag.SLEEP);
        assertThat(WorldGuardProtectionHooks.blockUseRule(Blocks.RESPAWN_ANCHOR.defaultBlockState()).flags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.INTERACT, WorldGuardFlag.RESPAWN_ANCHORS);
        assertThat(WorldGuardProtectionHooks.blockUseRule(Blocks.TNT.defaultBlockState()).flags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.INTERACT, WorldGuardFlag.TNT);
        assertThat(WorldGuardProtectionHooks.blockUseRule(Blocks.BIG_DRIPLEAF.defaultBlockState()).flags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.INTERACT, WorldGuardFlag.USE_DRIPLEAF);
        assertThat(WorldGuardProtectionHooks.blockUseRule(Blocks.CRAFTING_TABLE.defaultBlockState()))
            .isNull();
    }

    @Test
    void itemUseRulesMatchUpstreamSpecificItemFlags() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertThat(WorldGuardProtectionHooks.itemUseRule(Items.STICK).flags())
            .containsExactly(WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE);
        assertThat(WorldGuardProtectionHooks.itemUseRule(Items.SPLASH_POTION).flags())
            .containsExactly(WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE, WorldGuardFlag.POTION_SPLASH);
        assertThat(WorldGuardProtectionHooks.itemUseRule(Items.LINGERING_POTION).flags())
            .containsExactly(WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE, WorldGuardFlag.POTION_SPLASH);
        assertThat(WorldGuardProtectionHooks.itemUseRule(Items.WIND_CHARGE).flags())
            .containsExactly(WorldGuardFlag.ITEM_USE, WorldGuardFlag.USE, WorldGuardFlag.WIND_CHARGE_BURST);
    }

    @Test
    void projectileDamageFlagsMatchUpstreamProjectileSpecificFlags() {
        assertThat(WorldGuardProtectionHooks.projectileDamageFlags(EntityType.FIREWORK_ROCKET))
            .containsExactly(WorldGuardFlag.FIREWORK_DAMAGE);
        assertThat(WorldGuardProtectionHooks.projectileDamageFlags(EntityType.SPLASH_POTION))
            .containsExactly(WorldGuardFlag.POTION_SPLASH);
        assertThat(WorldGuardProtectionHooks.projectileDamageFlags(EntityType.LINGERING_POTION))
            .containsExactly(WorldGuardFlag.POTION_SPLASH);
        assertThat(WorldGuardProtectionHooks.projectileDamageFlags(EntityType.WIND_CHARGE))
            .containsExactly(WorldGuardFlag.WIND_CHARGE_BURST);
        assertThat(WorldGuardProtectionHooks.projectileDamageFlags(EntityType.BREEZE_WIND_CHARGE))
            .containsExactly(WorldGuardFlag.BREEZE_WIND_CHARGE);
        assertThat(WorldGuardProtectionHooks.projectileDamageFlags(EntityType.ARROW))
            .isEmpty();
    }

    @Test
    void mobSpawningAppliesToMobCategoriesOnly() {
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.CREATURE)).isTrue();
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.MONSTER)).isTrue();
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.WATER_AMBIENT)).isTrue();
        assertThat(WorldGuardProtectionHooks.mobSpawningCategory(MobCategory.MISC)).isFalse();
    }

    @Test
    void denySpawnMatchesNamespacedAndShortEntityIds() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertThat(WorldGuardProtectionHooks.denySpawnMatches(Set.of("minecraft:zombie"), EntityType.ZOMBIE))
            .isTrue();
        assertThat(WorldGuardProtectionHooks.denySpawnMatches(Set.of("zombie"), EntityType.ZOMBIE))
            .isTrue();
        assertThat(WorldGuardProtectionHooks.denySpawnMatches(Set.of("minecraft:cow"), EntityType.ZOMBIE))
            .isFalse();
    }

    @Test
    void globalDenySpawnBlocksMatchingEntityTypeOnly() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withValue(
                WorldGuardValueFlag.DENY_SPAWN,
                WorldGuardFlagValue.parse(WorldGuardValueFlag.DENY_SPAWN, "minecraft:zombie").orElseThrow()
            );

        assertThat(WorldGuardProtectionHooks.deniesMobSpawn(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            EntityType.ZOMBIE
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesMobSpawn(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            EntityType.COW
        )).isFalse();
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
            .containsExactly(WorldGuardFlag.VEHICLE_DESTROY);
        assertThat(WorldGuardProtectionHooks.nonLivingDamageFlags(EntityType.OAK_BOAT, null))
            .containsExactly(WorldGuardFlag.VEHICLE_DESTROY);
    }

    @Test
    void itemFrameRotationUsesSpecificFrameFlag() {
        assertThat(WorldGuardProtectionHooks.isItemFrame(null)).isFalse();
        assertThat(WorldGuardProtectionHooks.nonLivingDamageFlags(EntityType.ITEM_FRAME, null))
            .containsExactly(WorldGuardFlag.ENTITY_ITEM_FRAME_DESTROY);
    }

    @Test
    void tramplingSeparatesPlayerAndMobFallbackFlags() {
        assertThat(WorldGuardProtectionHooks.trampleFlags(true))
            .containsExactly(WorldGuardFlag.TRAMPLE_BLOCKS, WorldGuardFlag.BUILD);
        assertThat(WorldGuardProtectionHooks.trampleFlags(false))
            .containsExactly(WorldGuardFlag.TRAMPLE_BLOCKS, WorldGuardFlag.MOB_GRIEF);
    }

    @Test
    void redstoneAndPortalTriggersUseWorldGuardInteractionFlags() {
        assertThat(WorldGuardProtectionHooks.redstoneTriggerFlags())
            .containsExactly(WorldGuardFlag.INTERACT, WorldGuardFlag.USE);
        assertThat(WorldGuardProtectionHooks.portalEntryFlags())
            .containsExactly(WorldGuardFlag.EXIT_VIA_TELEPORT, WorldGuardFlag.EXIT);
    }

    @Test
    void vehiclePlacementUsesUpstreamBuildAndVehiclePlaceFlags() {
        assertThat(WorldGuardProtectionHooks.vehiclePlaceFlags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.VEHICLE_PLACE);
        assertThat(WorldGuardProtectionHooks.rideFlags())
            .containsExactly(WorldGuardFlag.RIDE, WorldGuardFlag.INTERACT);
        assertThat(WorldGuardProtectionHooks.entityPlaceFlags())
            .containsExactly(WorldGuardFlag.BUILD);
    }

    @Test
    void remainingEnvironmentalHelpersMapToSpecificUpstreamFlags() {
        assertThat(WorldGuardProtectionHooks.snowmanTrailFlags())
            .containsExactly(WorldGuardFlag.SNOWMAN_TRAILS, WorldGuardFlag.MOB_GRIEF);
        assertThat(WorldGuardProtectionHooks.experienceDropFlags())
            .containsExactly(WorldGuardFlag.EXP_DROPS);
        assertThat(WorldGuardProtectionHooks.frostedIceFormFlags())
            .containsExactly(WorldGuardFlag.FROSTED_ICE_FORM);
        assertThat(WorldGuardProtectionHooks.lavaFireFlags())
            .containsExactly(WorldGuardFlag.LAVA_FIRE);
    }

    @Test
    void growingPlantHelpersMapCaveVinesToCropAndVineFlags() {
        assertThat(WorldGuardProtectionHooks.growingPlantGrowthFlags(Blocks.CAVE_VINES.defaultBlockState()))
            .containsExactly(WorldGuardFlag.VINE_GROWTH, WorldGuardFlag.CROP_GROWTH);
        assertThat(WorldGuardProtectionHooks.growingPlantGrowthFlags(Blocks.CAVE_VINES_PLANT.defaultBlockState()))
            .containsExactly(WorldGuardFlag.VINE_GROWTH, WorldGuardFlag.CROP_GROWTH);
        assertThat(WorldGuardProtectionHooks.growingPlantGrowthFlags(Blocks.WEEPING_VINES.defaultBlockState()))
            .containsExactly(WorldGuardFlag.VINE_GROWTH);
    }

    @Test
    void vehicleTypesMatchUpstreamBoatsAndMinecartsOnly() {
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.OAK_BOAT)).isTrue();
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.BAMBOO_CHEST_RAFT)).isTrue();
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.MINECART)).isTrue();
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.TNT_MINECART)).isTrue();
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.ARMOR_STAND)).isFalse();
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.END_CRYSTAL)).isFalse();
        assertThat(WorldGuardProtectionHooks.vehicleType(EntityType.ZOMBIE)).isFalse();
    }

    @Test
    void treeFeatureGrowthUsesCropGrowthFlagAtOrigin() {
        assertThat(WorldGuardProtectionHooks.treeFeatureGrowthFlags())
            .containsExactly(WorldGuardFlag.CROP_GROWTH);
    }

    @Test
    void generatedTreeBlocksUseBuildPlacementFlagsAtTargets() {
        assertThat(WorldGuardProtectionHooks.generatedTreeBlockFlags())
            .containsExactly(WorldGuardFlag.BUILD, WorldGuardFlag.BLOCK_PLACE);
    }

    @Test
    void spreadingSnowyBlocksMapToGrassOrMyceliumFlags() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        assertThat(WorldGuardProtectionHooks.spreadingSnowyFlag(Blocks.GRASS_BLOCK.defaultBlockState()))
            .isEqualTo(WorldGuardFlag.GRASS_SPREAD);
        assertThat(WorldGuardProtectionHooks.spreadingSnowyFlag(Blocks.MYCELIUM.defaultBlockState()))
            .isEqualTo(WorldGuardFlag.MYCELIUM_SPREAD);
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
    void globalSoilDryDenyBlocksFarmlandDryMutation() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.SOIL_DRY, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            WorldGuardFlag.SOIL_DRY,
            WorldGuardFlag.MOISTURE_CHANGE
        )).isTrue();
    }

    @Test
    void globalMoistureChangeDenyBlocksFarmlandMoistureMutation() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.MOISTURE_CHANGE, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            WorldGuardFlag.SOIL_DRY,
            WorldGuardFlag.MOISTURE_CHANGE
        )).isTrue();
    }

    @Test
    void globalBlockTramplingDenyBlocksPlayerTrampleMutation() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.TRAMPLE_BLOCKS, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            WorldGuardProtectionHooks.trampleFlags(true)
        )).isTrue();
    }

    @Test
    void globalBlockTramplingDenyBlocksMobTrampleMutation() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.TRAMPLE_BLOCKS, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            WorldGuardProtectionHooks.trampleFlags(false)
        )).isTrue();
    }

    @Test
    void globalVehicleFlagsDenyPlaceAndDestroyMutations() {
        BlockPos pos = new BlockPos(5, 64, 5);
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.VEHICLE_PLACE, FlagState.DENY)
            .withFlag(WorldGuardFlag.VEHICLE_DESTROY, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.vehiclePlaceFlags()
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardFlag.VEHICLE_DESTROY
        )).isTrue();
    }

    @Test
    void globalCropGrowthDenyBlocksSaplingAndTreeFeatureOriginGrowth() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.CROP_GROWTH, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            new BlockPos(5, 64, 5),
            WorldGuardProtectionHooks.treeFeatureGrowthFlags()
        )).isTrue();
    }

    @Test
    void globalBuildOrBlockPlaceDenyBlocksGeneratedTreeBlocks() {
        BlockPos pos = new BlockPos(5, 64, 5);
        WorldGuardRegion buildDeny = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.BUILD, FlagState.DENY);
        WorldGuardRegion placeDeny = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.BLOCK_PLACE, FlagState.DENY);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(buildDeny),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.generatedTreeBlockFlags()
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(placeDeny),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.generatedTreeBlockFlags()
        )).isTrue();
    }

    @Test
    void dragonAndWitherBlockDamageFlagsDenyBossBlockMutation() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.ENDERDRAGON_BLOCK_DAMAGE, FlagState.DENY)
            .withFlag(WorldGuardFlag.WITHER_DAMAGE, FlagState.DENY);
        BlockPos pos = new BlockPos(5, 64, 5);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardFlag.ENDERDRAGON_BLOCK_DAMAGE
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardFlag.WITHER_DAMAGE
        )).isTrue();
    }

    @Test
    void globalSpecificEnvironmentFlagsDenySpecialMutations() {
        WorldGuardRegion global = WorldGuardRegion.global("minecraft:overworld")
            .withFlag(WorldGuardFlag.SNOWMAN_TRAILS, FlagState.DENY)
            .withFlag(WorldGuardFlag.EXP_DROPS, FlagState.DENY)
            .withFlag(WorldGuardFlag.FROSTED_ICE_FORM, FlagState.DENY)
            .withFlag(WorldGuardFlag.LAVA_FIRE, FlagState.DENY);
        BlockPos pos = new BlockPos(5, 64, 5);

        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.snowmanTrailFlags()
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.experienceDropFlags()
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.frostedIceFormFlags()
        )).isTrue();
        assertThat(WorldGuardProtectionHooks.deniesAny(
            List.of(global),
            "minecraft:overworld",
            pos,
            WorldGuardProtectionHooks.lavaFireFlags()
        )).isTrue();
    }

    @Test
    void mixinConfigurationRegistersEnvironmentProtectionHooks() throws Exception {
        String mixins = Files.readString(Path.of("src/main/resources/mod_worldguard.mixins.json"));

        assertThat(mixins)
            .contains(
                "BuddingAmethystBlockMixin",
                "CaveVinesBerryGrowthMixin",
                "FarmlandBlockMixin",
                "FrogspawnBlockMixin",
                "ExperienceOrbMixin",
                "LavaFluidMixin",
                "MangrovePropaguleBlockMixin",
                "ReplaceDiskMixin",
                "SculkChargeCursorMixin",
                "SnowGolemMixin",
                "TurtleEggBlockMixin"
            );
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
