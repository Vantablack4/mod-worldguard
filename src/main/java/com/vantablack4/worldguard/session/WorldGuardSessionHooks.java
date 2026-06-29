package com.vantablack4.worldguard.session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import com.vantablack4.worldguard.ProtectionDecision;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.WorldGuardService;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
import com.vantablack4.worldguard.model.RegionQueryEngine;

public final class WorldGuardSessionHooks {
    private static volatile WorldGuardSessionRuntime runtime;

    private WorldGuardSessionHooks() {
    }

    public static void configure(WorldGuardService service) {
        runtime = new WorldGuardSessionRuntime(service);
    }

    public static void clear() {
        runtime = null;
    }

    public static void evict(UUID playerUuid) {
        WorldGuardSessionRuntime active = runtime;
        if (active != null) {
            active.evict(playerUuid);
        }
    }

    public static void refresh(ServerPlayer player) {
        WorldGuardSessionRuntime active = runtime;
        if (active != null) {
            active.refresh(player);
        }
    }

    public static boolean allowMovement(ServerPlayer player, Vec3 previousPosition) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowMovement(player, previousPosition);
    }

    public static boolean allowTeleport(ServerPlayer player, ServerLevel targetLevel, double x, double y, double z) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowTeleport(player, targetLevel, x, y, z);
    }

    public static boolean denyAction(ServerPlayer player, BlockPos pos, WorldGuardFlag... flags) {
        WorldGuardSessionRuntime active = runtime;
        return active != null && active.denyAction(player, pos, flags);
    }

    public static boolean denyItemPickup(ServerPlayer player, Entity itemEntity) {
        return denyAction(player, itemEntity == null ? null : itemEntity.blockPosition(), WorldGuardFlag.ITEM_PICKUP);
    }

    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowDamage(entity, source);
    }

    public static boolean allowCommand(ServerPlayer player, String command) {
        WorldGuardSessionRuntime active = runtime;
        return active == null || active.allowCommand(player, command);
    }

    public static TeleportTransition respawnTransition(ServerPlayer player, TeleportTransition vanillaTransition) {
        WorldGuardSessionRuntime active = runtime;
        return active == null ? vanillaTransition : active.respawnTransition(player, vanillaTransition);
    }
}

final class WorldGuardSessionRuntime {
    private final WorldGuardService service;
    private final Map<UUID, WorldGuardSessionSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, GameType> previousGameModes = new HashMap<>();
    private final Map<UUID, String> activeTimeLocks = new HashMap<>();
    private final Map<UUID, String> activeWeatherLocks = new HashMap<>();

    WorldGuardSessionRuntime(WorldGuardService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    void evict(UUID playerUuid) {
        snapshots.remove(playerUuid);
        previousGameModes.remove(playerUuid);
        activeTimeLocks.remove(playerUuid);
        activeWeatherLocks.remove(playerUuid);
    }

    void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }

        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> groups = service.regionGroups(player, regions);
        WorldGuardSessionSnapshot current = snapshot(player);
        applyTypedTickEffects(player, regions, groups);
        WorldGuardSessionSnapshot previous = snapshots.put(player.getUUID(), current);
        if (previous == null) {
            return;
        }

        for (WorldGuardSessionMessage message : WorldGuardSessionRules.messagesForTransition(
            regions,
            previous,
            current,
            player.getScoreboardName(),
            player.getUUID(),
            groups
        )) {
            player.sendSystemMessage(Component.literal(message.message()).withStyle(ChatFormatting.YELLOW));
        }
    }

    boolean allowMovement(ServerPlayer player, Vec3 previousPosition) {
        if (player == null || previousPosition == null) {
            return true;
        }

        String world = worldId(player.level());
        BlockPos from = BlockPos.containing(previousPosition);
        BlockPos to = player.blockPosition();
        List<WorldGuardRegion> regions = service.storage().regions();
        WorldGuardMovementDecision decision = WorldGuardSessionRules.movementDecision(
            regions,
            world,
            from,
            to,
            player.getUUID(),
            service.regionGroups(player, regions),
            service.isAdmin(player)
        );
        if (decision.allowed()) {
            return true;
        }

        BlockPos deniedPos = decision.decision().flag() == WorldGuardFlag.ENTRY ? to : from;
        service.deny(player, decision.decision(), world, deniedPos);
        return false;
    }

    boolean allowTeleport(ServerPlayer player, ServerLevel targetLevel, double x, double y, double z) {
        if (player == null || targetLevel == null) {
            return true;
        }

        String fromWorld = worldId(player.level());
        String targetWorld = worldId(targetLevel);
        BlockPos from = player.blockPosition();
        BlockPos to = BlockPos.containing(x, y, z);
        if (fromWorld.equals(targetWorld) && from.equals(to)) {
            return true;
        }

        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> groups = service.regionGroups(player, regions);
        boolean bypass = service.isAdmin(player);
        ProtectionDecision exitDecision = WorldGuardSessionRules.checkAny(
            regions,
            fromWorld,
            from,
            player.getUUID(),
            groups,
            bypass,
            WorldGuardFlag.EXIT_VIA_TELEPORT,
            WorldGuardFlag.EXIT
        );
        if (service.deny(player, exitDecision, fromWorld, from)) {
            return false;
        }

        ProtectionDecision entryDecision = WorldGuardSessionRules.checkAny(
            regions,
            targetWorld,
            to,
            player.getUUID(),
            groups,
            bypass,
            WorldGuardFlag.ENTRY
        );
        return !service.deny(player, entryDecision, targetWorld, to);
    }

    boolean denyAction(ServerPlayer player, BlockPos pos, WorldGuardFlag... flags) {
        if (player == null) {
            return false;
        }

        BlockPos checkPos = pos == null ? player.blockPosition() : pos;
        List<WorldGuardRegion> regions = service.storage().regions();
        ProtectionDecision decision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(player.level()),
            checkPos,
            player.getUUID(),
            service.regionGroups(player, regions),
            service.isAdmin(player),
            flags
        );
        return service.deny(player, decision, worldId(player.level()), checkPos);
    }

    boolean allowCommand(ServerPlayer player, String command) {
        if (player == null) {
            return true;
        }
        List<WorldGuardRegion> regions = service.storage().regions();
        boolean allowed = WorldGuardSessionRules.commandAllowed(
            regions,
            worldId(player.level()),
            player.blockPosition(),
            player.getUUID(),
            service.regionGroups(player, regions),
            service.isAdmin(player),
            command
        );
        if (!allowed) {
            service.denyCommand(player);
        }
        return allowed;
    }

    TeleportTransition respawnTransition(ServerPlayer player, TeleportTransition vanillaTransition) {
        if (player == null || vanillaTransition == null) {
            return vanillaTransition;
        }

        List<WorldGuardRegion> regions = service.storage().regions();
        Optional<WorldGuardFlagValue.LocationValue> location = WorldGuardSessionRules.respawnLocation(
            regions,
            worldId(player.level()),
            player.blockPosition(),
            player.getUUID(),
            service.regionGroups(player, regions)
        );
        if (location.isEmpty()) {
            return vanillaTransition;
        }

        ServerLevel level = level(player, location.get().world());
        if (level == null) {
            return vanillaTransition;
        }

        return new TeleportTransition(
            level,
            new Vec3(location.get().x(), location.get().y(), location.get().z()),
            vanillaTransition.deltaMovement(),
            location.get().yaw(),
            location.get().pitch(),
            false,
            vanillaTransition.asPassenger(),
            vanillaTransition.relatives(),
            vanillaTransition.postTeleportTransition()
        );
    }

    boolean allowDamage(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer victim)) {
            return allowNonPlayerDamage(entity, source);
        }

        Entity damageSource = damageSource(source);
        ServerPlayer attacker = damageSource instanceof ServerPlayer sourcePlayer
            ? sourcePlayer
            : null;
        boolean attackerBypass = attacker != null && service.isAdmin(attacker);
        boolean victimBypass = service.isAdmin(victim);
        boolean invincibilityBypassed = attackerBypass || victimBypass;
        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> victimGroups = service.regionGroups(victim, regions);
        if (WorldGuardSessionRules.enabledRegion(
            regions,
            worldId(victim.level()),
            victim.blockPosition(),
            victim.getUUID(),
            victimGroups,
            invincibilityBypassed,
            WorldGuardFlag.INVINCIBILITY
        ).isPresent()) {
            return false;
        }

        if (source != null && source.is(DamageTypes.FALL) && denyAction(victim, victim.blockPosition(), WorldGuardFlag.FALL_DAMAGE)) {
            victim.resetFallDistance();
            return false;
        }

        if (attacker != null && attacker != victim) {
            return !denyPvp(attacker, victim);
        }
        if (damageSource instanceof LivingEntity && damageSource != victim) {
            ProtectionDecision decision = WorldGuardSessionRules.checkAny(
                regions,
                worldId(victim.level()),
                victim.blockPosition(),
                null,
                false,
                WorldGuardFlag.MOB_DAMAGE
            );
            return decision.allowed();
        }
        return true;
    }

    private boolean allowNonPlayerDamage(LivingEntity victim, DamageSource source) {
        if (victim == null) {
            return true;
        }

        Entity damageSource = damageSource(source);
        ServerPlayer attacker = damageSource instanceof ServerPlayer sourcePlayer
            ? sourcePlayer
            : null;
        List<WorldGuardFlag> flags = WorldGuardSessionRules.nonPlayerDamageFlags(
            victim.getType().getCategory(),
            attacker != null,
            damageSource instanceof LivingEntity
        );
        if (flags.isEmpty()) {
            return true;
        }

        List<WorldGuardRegion> regions = service.storage().regions();
        if (attacker != null) {
            ProtectionDecision decision = WorldGuardSessionRules.checkAny(
                regions,
                worldId(victim.level()),
                victim.blockPosition(),
                attacker.getUUID(),
                service.regionGroups(attacker, regions),
                service.isAdmin(attacker),
                flags.toArray(WorldGuardFlag[]::new)
            );
            return !service.deny(attacker, decision);
        }

        ProtectionDecision decision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(victim.level()),
            victim.blockPosition(),
            null,
            false,
            flags.toArray(WorldGuardFlag[]::new)
        );
        return decision.allowed();
    }

    private boolean denyPvp(ServerPlayer attacker, ServerPlayer victim) {
        List<WorldGuardRegion> regions = service.storage().regions();
        Set<String> attackerGroups = service.regionGroups(attacker, regions);
        ProtectionDecision victimRegionDecision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(victim.level()),
            victim.blockPosition(),
            attacker.getUUID(),
            attackerGroups,
            service.isAdmin(attacker),
            WorldGuardFlag.PVP
        );
        if (service.deny(attacker, victimRegionDecision, worldId(victim.level()), victim.blockPosition())) {
            return true;
        }

        ProtectionDecision attackerRegionDecision = WorldGuardSessionRules.checkAny(
            regions,
            worldId(attacker.level()),
            attacker.blockPosition(),
            attacker.getUUID(),
            attackerGroups,
            service.isAdmin(attacker),
            WorldGuardFlag.PVP
        );
        return service.deny(attacker, attackerRegionDecision, worldId(attacker.level()), attacker.blockPosition());
    }

    private void applyTypedTickEffects(ServerPlayer player, List<WorldGuardRegion> regions, Set<String> groups) {
        String world = worldId(player.level());
        BlockPos pos = player.blockPosition();
        applyGameMode(player, regions, groups, world, pos);
        applyTimeLock(player, regions, groups, world, pos);
        applyWeatherLock(player, regions, groups, world, pos);
        applyHeal(player, regions, groups, world, pos);
        applyFeed(player, regions, groups, world, pos);
    }

    private void applyGameMode(ServerPlayer player, List<WorldGuardRegion> regions, Set<String> groups, String world, BlockPos pos) {
        RegionQueryEngine.queryValue(
            regions,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            WorldGuardValueFlag.GAME_MODE,
            player.getUUID(),
            groups
        ).value().map(WorldGuardFlagValue::serialized)
            .map(value -> GameType.byName(value, null))
            .ifPresentOrElse(gameType -> {
                previousGameModes.putIfAbsent(player.getUUID(), player.gameMode());
                if (player.gameMode() != gameType) {
                    player.setGameMode(gameType);
                }
            }, () -> {
                GameType previous = previousGameModes.remove(player.getUUID());
                if (previous != null && player.gameMode() != previous) {
                    player.setGameMode(previous);
                }
            });
    }

    private void applyTimeLock(ServerPlayer player, List<WorldGuardRegion> regions, Set<String> groups, String world, BlockPos pos) {
        Optional<String> raw = value(regions, groups, world, pos, player, WorldGuardValueFlag.TIME_LOCK)
            .map(WorldGuardFlagValue::serialized)
            .filter(value -> WorldGuardSessionRules.timeLock(value).isPresent());
        if (raw.isPresent()) {
            String current = activeTimeLocks.put(player.getUUID(), raw.get());
            if (!raw.get().equals(current) || shouldRefreshVisualLock(player)) {
                WorldGuardSessionRules.timeLock(raw.get()).ifPresent(lock -> sendPlayerTime(player, lock));
            }
            return;
        }

        if (activeTimeLocks.remove(player.getUUID()) != null) {
            player.connection.send(player.level().getServer().clockManager().createFullSyncPacket());
        }
    }

    private void applyWeatherLock(
        ServerPlayer player,
        List<WorldGuardRegion> regions,
        Set<String> groups,
        String world,
        BlockPos pos
    ) {
        Optional<String> raw = value(regions, groups, world, pos, player, WorldGuardValueFlag.WEATHER_LOCK)
            .map(WorldGuardFlagValue::serialized)
            .filter(value -> WorldGuardSessionRules.weatherLock(value).isPresent());
        if (raw.isPresent()) {
            String current = activeWeatherLocks.put(player.getUUID(), raw.get());
            if (!raw.get().equals(current) || shouldRefreshVisualLock(player)) {
                WorldGuardSessionRules.weatherLock(raw.get()).ifPresent(lock -> sendPlayerWeather(player, lock));
            }
            return;
        }

        if (activeWeatherLocks.remove(player.getUUID()) != null) {
            sendVanillaWeather(player);
        }
    }

    private void applyHeal(ServerPlayer player, List<WorldGuardRegion> regions, Set<String> groups, String world, BlockPos pos) {
        int delay = value(regions, groups, world, pos, player, WorldGuardValueFlag.HEAL_DELAY)
            .flatMap(WorldGuardFlagValue::asInteger)
            .orElse(0);
        int amount = value(regions, groups, world, pos, player, WorldGuardValueFlag.HEAL_AMOUNT)
            .flatMap(WorldGuardFlagValue::asInteger)
            .orElse(0);
        if (delay <= 0 || amount <= 0 || player.tickCount % Math.max(1, delay * 20) != 0) {
            return;
        }
        double min = value(regions, groups, world, pos, player, WorldGuardValueFlag.HEAL_MIN_HEALTH)
            .flatMap(WorldGuardFlagValue::asDouble)
            .orElse(0D);
        double max = value(regions, groups, world, pos, player, WorldGuardValueFlag.HEAL_MAX_HEALTH)
            .flatMap(WorldGuardFlagValue::asDouble)
            .orElse((double) player.getMaxHealth());
        if (player.getHealth() >= min && player.getHealth() < max) {
            player.heal((float) Math.min(amount, max - player.getHealth()));
        }
    }

    private void applyFeed(ServerPlayer player, List<WorldGuardRegion> regions, Set<String> groups, String world, BlockPos pos) {
        int delay = value(regions, groups, world, pos, player, WorldGuardValueFlag.FEED_DELAY)
            .flatMap(WorldGuardFlagValue::asInteger)
            .orElse(0);
        int amount = value(regions, groups, world, pos, player, WorldGuardValueFlag.FEED_AMOUNT)
            .flatMap(WorldGuardFlagValue::asInteger)
            .orElse(0);
        if (delay <= 0 || amount <= 0 || player.tickCount % Math.max(1, delay * 20) != 0) {
            return;
        }
        FoodData food = player.getFoodData();
        int min = value(regions, groups, world, pos, player, WorldGuardValueFlag.FEED_MIN_HUNGER)
            .flatMap(WorldGuardFlagValue::asInteger)
            .orElse(0);
        int max = value(regions, groups, world, pos, player, WorldGuardValueFlag.FEED_MAX_HUNGER)
            .flatMap(WorldGuardFlagValue::asInteger)
            .orElse(20);
        if (food.getFoodLevel() >= min && food.getFoodLevel() < max) {
            food.setFoodLevel(Math.min(max, food.getFoodLevel() + amount));
        }
    }

    private Optional<WorldGuardFlagValue> value(
        List<WorldGuardRegion> regions,
        Set<String> groups,
        String world,
        BlockPos pos,
        ServerPlayer player,
        WorldGuardValueFlag flag
    ) {
        return RegionQueryEngine.queryValue(
            regions,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            flag,
            player.getUUID(),
            groups
        ).value();
    }

    private WorldGuardSessionSnapshot snapshot(ServerPlayer player) {
        return WorldGuardSessionRules.snapshot(
            service.storage().regions(),
            worldId(player.level()),
            player.blockPosition()
        );
    }

    private static String worldId(Level world) {
        return world.dimension().identifier().toString();
    }

    private static boolean shouldRefreshVisualLock(ServerPlayer player) {
        return player.tickCount % 20 == 0;
    }

    private static ServerLevel level(ServerPlayer player, String world) {
        Identifier identifier = Identifier.tryParse(world);
        if (identifier == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, identifier);
        return player.level().getServer().getLevel(key);
    }

    private static void sendPlayerTime(ServerPlayer player, WorldGuardSessionRules.TimeLock lock) {
        Optional<Holder<WorldClock>> defaultClock = player.level().dimensionType().defaultClock();
        if (defaultClock.isEmpty()) {
            return;
        }

        long totalTicks = lock.relative()
            ? player.level().getDefaultClockTime() + lock.value()
            : lock.value();
        player.connection.send(new ClientboundSetTimePacket(
            player.level().getGameTime(),
            Map.of(defaultClock.get(), new ClockNetworkState(totalTicks, 0F, lock.relative() ? 1F : 0F))
        ));
    }

    private static void sendPlayerWeather(ServerPlayer player, WorldGuardSessionRules.WeatherLock lock) {
        switch (lock) {
            case CLEAR -> sendWeather(player, false, 0F, 0F);
            case RAIN -> sendWeather(player, true, 1F, 0F);
            case THUNDER_STORM -> sendWeather(player, true, 1F, 1F);
        }
    }

    private static void sendVanillaWeather(ServerPlayer player) {
        ServerLevel level = player.level();
        if (level.isRaining()) {
            sendWeather(player, true, level.getRainLevel(1F), level.getThunderLevel(1F));
        } else {
            sendWeather(player, false, 0F, 0F);
        }
    }

    private static void sendWeather(ServerPlayer player, boolean raining, float rainLevel, float thunderLevel) {
        player.connection.send(new ClientboundGameEventPacket(
            raining ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING,
            0F
        ));
        player.connection.send(new ClientboundGameEventPacket(
            ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
            rainLevel
        ));
        player.connection.send(new ClientboundGameEventPacket(
            ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
            thunderLevel
        ));
    }

    private static Entity damageSource(DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity entity = source.getEntity();
        return entity == null ? source.getDirectEntity() : entity;
    }
}
