package com.vantablack4.worldguard.hook;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardPolicy;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.WorldGuardService;

public final class WorldGuardProtectionHooks {
    private static volatile WorldGuardService service;

    private WorldGuardProtectionHooks() {
    }

    public static void configure(WorldGuardService configuredService) {
        service = Objects.requireNonNull(configuredService, "configuredService");
    }

    public static void removeExplosionBlockDamageDenied(Level level, Entity source, List<BlockPos> positions) {
        removeDeniedTargets(level, positions, explosionFlag(source));
    }

    public static void removeExplosionFireDenied(Level level, Entity source, List<BlockPos> positions) {
        removeDeniedTargets(level, positions, WorldGuardFlag.FIRE_SPREAD, explosionFlag(source));
    }

    public static boolean deniesFireMutation(Level level, BlockPos pos) {
        return deniesAny(level, pos, WorldGuardFlag.FIRE_SPREAD);
    }

    public static boolean deniesFluidFlow(LevelAccessor level, BlockPos pos, FluidState fluidState) {
        if (!(level instanceof Level concreteLevel)) {
            return false;
        }
        return deniesAny(concreteLevel, pos, fluidFlag(fluidState));
    }

    public static boolean deniesMobGrief(Level level, BlockPos pos) {
        return deniesAny(level, pos, WorldGuardFlag.MOB_GRIEF);
    }

    public static boolean deniesEndermanGrief(Level level, BlockPos pos) {
        return deniesAny(level, pos, WorldGuardFlag.ENDER_BUILD, WorldGuardFlag.MOB_GRIEF);
    }

    public static boolean deniesRavagerGrief(Level level, BlockPos pos) {
        return deniesAny(level, pos, WorldGuardFlag.RAVAGER_RAVAGE, WorldGuardFlag.MOB_GRIEF);
    }

    public static boolean deniesPistonAction(Level level, BlockPos pistonPos, Direction facing, boolean extending) {
        if (!shouldCheck(level) || pistonPos == null || facing == null) {
            return false;
        }

        BlockPos headPos = pistonPos.relative(facing);
        if (deniesAny(level, pistonPos, WorldGuardFlag.PISTONS) || deniesAny(level, headPos, WorldGuardFlag.PISTONS)) {
            return true;
        }

        PistonStructureResolver resolver = new PistonStructureResolver(level, pistonPos, facing, extending);
        if (!resolver.resolve()) {
            return false;
        }

        Direction pushDirection = resolver.getPushDirection();
        for (BlockPos source : resolver.getToPush()) {
            if (deniesAny(level, source, WorldGuardFlag.PISTONS)
                || deniesAny(level, source.relative(pushDirection), WorldGuardFlag.PISTONS)) {
                return true;
            }
        }

        for (BlockPos destroyed : resolver.getToDestroy()) {
            if (deniesAny(level, destroyed, WorldGuardFlag.PISTONS)) {
                return true;
            }
        }

        return false;
    }

    static boolean deniesBuild(List<WorldGuardRegion> regions, String world, BlockPos pos) {
        return deniesAny(regions, world, pos, WorldGuardFlag.BUILD);
    }

    static boolean deniesAny(List<WorldGuardRegion> regions, String world, BlockPos pos, WorldGuardFlag... flags) {
        if (regions == null || world == null || pos == null) {
            return false;
        }
        for (WorldGuardFlag flag : flags) {
            if (flag == null) {
                continue;
            }
            ProtectionDecision decision = WorldGuardPolicy.evaluate(
                regions,
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                flag,
                null,
                false
            );
            if (!decision.allowed()) {
                return true;
            }
        }
        return false;
    }

    private static void removeDeniedTargets(Level level, List<BlockPos> positions, WorldGuardFlag... flags) {
        if (!shouldCheck(level) || positions == null || positions.isEmpty()) {
            return;
        }
        positions.removeIf(pos -> deniesAny(level, pos, flags));
    }

    private static boolean deniesAny(Level level, BlockPos pos, WorldGuardFlag... flags) {
        WorldGuardService activeService = service;
        if (!shouldCheck(level) || pos == null || activeService == null) {
            return false;
        }
        return deniesAny(activeService.storage().regions(), worldId(level), pos, flags);
    }

    private static WorldGuardFlag fluidFlag(FluidState fluidState) {
        if (fluidState == null || fluidState.isEmpty()) {
            return null;
        }
        Fluid fluid = fluidState.getType();
        if (fluid.isSame(Fluids.LAVA) || fluid.isSame(Fluids.FLOWING_LAVA)) {
            return WorldGuardFlag.LAVA_FLOW;
        }
        if (fluid.isSame(Fluids.WATER) || fluid.isSame(Fluids.FLOWING_WATER)) {
            return WorldGuardFlag.WATER_FLOW;
        }
        return null;
    }

    private static WorldGuardFlag explosionFlag(Entity source) {
        if (source == null) {
            return WorldGuardFlag.OTHER_EXPLOSION;
        }
        if (source.is(EntityType.TNT) || source.is(EntityType.TNT_MINECART)) {
            return WorldGuardFlag.TNT;
        }
        if (source.is(EntityType.CREEPER)) {
            return WorldGuardFlag.CREEPER_EXPLOSION;
        }
        if (source.is(EntityType.GHAST) || source.is(EntityType.FIREBALL) || source.is(EntityType.SMALL_FIREBALL)) {
            return WorldGuardFlag.GHAST_FIREBALL;
        }
        if (source.is(EntityType.BREEZE_WIND_CHARGE) || source.is(EntityType.WIND_CHARGE)) {
            return WorldGuardFlag.BREEZE_WIND_CHARGE;
        }
        if (source.is(EntityType.WITHER) || source.is(EntityType.WITHER_SKULL)) {
            return WorldGuardFlag.WITHER_DAMAGE;
        }
        if (source.is(EntityType.ENDER_DRAGON) || source.is(EntityType.DRAGON_FIREBALL)) {
            return WorldGuardFlag.ENDERDRAGON_BLOCK_DAMAGE;
        }
        return WorldGuardFlag.OTHER_EXPLOSION;
    }

    private static boolean shouldCheck(Level level) {
        return service != null && level != null && !level.isClientSide();
    }

    private static String worldId(Level level) {
        return level.dimension().identifier().toString();
    }
}
