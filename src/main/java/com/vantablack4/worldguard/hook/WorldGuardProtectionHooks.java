package com.vantablack4.worldguard.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.TeleportTransition;

import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardPolicy;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.WorldGuardService;
import com.vantablack4.worldguard.session.WorldGuardSessionHooks;

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

    public static boolean deniesBucketPlace(Level level, LivingEntity actor, BlockPos pos, Fluid content) {
        WorldGuardFlag[] flags = bucketMutationFlags(content, null);
        if (actor instanceof ServerPlayer serverPlayer) {
            return WorldGuardSessionHooks.denyAction(serverPlayer, pos, flags);
        }
        return deniesAny(level, pos, flags);
    }

    public static boolean deniesBucketPickup(LevelAccessor level, LivingEntity actor, BlockPos pos) {
        if (!(level instanceof Level concreteLevel)) {
            return false;
        }
        WorldGuardFlag[] flags = bucketMutationFlags(Fluids.EMPTY, concreteLevel.getFluidState(pos));
        if (actor instanceof ServerPlayer serverPlayer) {
            return WorldGuardSessionHooks.denyAction(serverPlayer, pos, flags);
        }
        return deniesAny(concreteLevel, pos, flags);
    }

    public static boolean deniesSolidBucketPlace(Level level, LivingEntity actor, BlockPos pos) {
        WorldGuardFlag[] flags = new WorldGuardFlag[] {
            WorldGuardFlag.BUILD,
            WorldGuardFlag.BLOCK_PLACE,
            WorldGuardFlag.ITEM_USE,
            WorldGuardFlag.USE
        };
        if (actor instanceof ServerPlayer serverPlayer) {
            return WorldGuardSessionHooks.denyAction(serverPlayer, pos, flags);
        }
        return deniesAny(level, pos, flags);
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

    public static boolean deniesMobSpawn(Level level, Entity entity) {
        if (!shouldCheck(level) || entity == null || !mobSpawningCategory(entity.getType().getCategory())) {
            return false;
        }
        return deniesAny(level, entity.blockPosition(), WorldGuardFlag.MOB_SPAWNING);
    }

    public static boolean deniesNonLivingDamage(Entity victim, DamageSource source) {
        if (victim == null || victim instanceof LivingEntity) {
            return false;
        }
        Level level = victim.level();
        if (!shouldCheck(level)) {
            return false;
        }
        Entity attacker = source == null ? null : source.getEntity();
        if (attacker == null && source != null) {
            attacker = source.getDirectEntity();
        }
        WorldGuardFlag[] flags = nonLivingDamageFlags(victim.getType(), attacker);
        if (flags.length == 0) {
            return false;
        }
        if (attacker instanceof ServerPlayer serverPlayer) {
            return WorldGuardSessionHooks.denyAction(serverPlayer, victim.blockPosition(), flags);
        }
        return deniesAny(level, victim.blockPosition(), flags);
    }

    public static boolean deniesEntityTeleport(Entity entity, TeleportTransition transition) {
        if (entity == null || transition == null) {
            return false;
        }
        Level sourceLevel = entity.level();
        if (!shouldCheck(sourceLevel)) {
            return false;
        }

        BlockPos from = entity.blockPosition();
        BlockPos to = BlockPos.containing(transition.position());
        Level targetLevel = transition.newLevel();
        if (worldId(sourceLevel).equals(worldId(targetLevel)) && from.equals(to)) {
            return false;
        }
        return deniesAny(sourceLevel, from, WorldGuardFlag.EXIT_VIA_TELEPORT, WorldGuardFlag.EXIT)
            || deniesAny(targetLevel, to, WorldGuardFlag.ENTRY);
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

    public static boolean isContainerAccess(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        return isContainerAccessCandidate(level.getBlockEntity(pos), level.getBlockState(pos).getBlock());
    }

    static boolean isContainerAccessCandidate(BlockEntity blockEntity, Block block) {
        return blockEntity instanceof Container
            || blockEntity instanceof MenuProvider
            || block instanceof AbstractChestBlock<?>;
    }

    public static boolean isEntityContainerAccess(Entity entity) {
        return entity != null && (entity instanceof ContainerEntity
            || entity instanceof HasCustomInventoryScreen
            || entity instanceof Container
            || entity instanceof MenuProvider);
    }

    static boolean mobSpawningCategory(MobCategory category) {
        return category != null && category != MobCategory.MISC;
    }

    static WorldGuardFlag[] nonLivingDamageFlags(EntityType<?> victimType, Entity attacker) {
        List<WorldGuardFlag> flags = new ArrayList<>();
        if (victimType == EntityType.PAINTING) {
            flags.add(WorldGuardFlag.ENTITY_PAINTING_DESTROY);
        }
        if (victimType == EntityType.ITEM_FRAME || victimType == EntityType.GLOW_ITEM_FRAME) {
            flags.add(WorldGuardFlag.ENTITY_ITEM_FRAME_DESTROY);
        }
        if (attacker instanceof ServerPlayer) {
            flags.add(WorldGuardFlag.ATTACK_ENTITY);
        } else if (attacker instanceof LivingEntity) {
            flags.add(WorldGuardFlag.MOB_DAMAGE);
        }
        return flags.toArray(WorldGuardFlag[]::new);
    }

    public static BlockPos bucketMutationTarget(ItemStack stack, BlockPos clicked, Direction direction) {
        return bucketMutationTarget(bucketContent(stack), clicked, direction);
    }

    static BlockPos bucketMutationTarget(Fluid content, BlockPos clicked, Direction direction) {
        if (content == null || clicked == null || direction == null) {
            return null;
        }
        return content.isSame(Fluids.EMPTY) ? clicked : clicked.relative(direction);
    }

    public static WorldGuardFlag[] bucketMutationFlags(Level level, ItemStack stack, BlockPos clicked) {
        Fluid content = bucketContent(stack);
        if (content == null) {
            return new WorldGuardFlag[0];
        }
        return bucketMutationFlags(content, level == null || clicked == null ? null : level.getFluidState(clicked));
    }

    static WorldGuardFlag[] bucketMutationFlags(Fluid content, FluidState clickedFluid) {
        List<WorldGuardFlag> flags = new ArrayList<>();
        if (content.isSame(Fluids.EMPTY)) {
            flags.add(WorldGuardFlag.BUILD);
            flags.add(WorldGuardFlag.BLOCK_BREAK);
            addIfPresent(flags, fluidFlag(clickedFluid));
        } else {
            flags.add(WorldGuardFlag.BUILD);
            flags.add(WorldGuardFlag.BLOCK_PLACE);
            addIfPresent(flags, fluidFlag(content));
        }
        flags.add(WorldGuardFlag.ITEM_USE);
        flags.add(WorldGuardFlag.USE);
        return flags.toArray(WorldGuardFlag[]::new);
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
        return fluidFlag(fluidState.getType());
    }

    private static WorldGuardFlag fluidFlag(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        if (fluid.isSame(Fluids.LAVA) || fluid.isSame(Fluids.FLOWING_LAVA)) {
            return WorldGuardFlag.LAVA_FLOW;
        }
        if (fluid.isSame(Fluids.WATER) || fluid.isSame(Fluids.FLOWING_WATER)) {
            return WorldGuardFlag.WATER_FLOW;
        }
        return null;
    }

    private static Fluid bucketContent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BucketItem bucket)) {
            return null;
        }
        return bucket.getContent();
    }

    private static void addIfPresent(List<WorldGuardFlag> flags, WorldGuardFlag flag) {
        if (flag != null) {
            flags.add(flag);
        }
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
