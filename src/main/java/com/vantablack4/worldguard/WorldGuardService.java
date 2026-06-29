package com.vantablack4.worldguard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
import com.vantablack4.worldguard.model.RegionQueryEngine;

public final class WorldGuardService {
    private final WorldGuardConfig config;
    private final WorldGuardStorage storage;
    private final Map<UUID, Long> lastDenyMessageMillis = new HashMap<>();

    public WorldGuardService(WorldGuardConfig config, WorldGuardStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    public WorldGuardStorage storage() {
        return storage;
    }

    public ProtectionDecision check(ServerPlayer player, WorldGuardFlag flag, String world, BlockPos pos) {
        return checkAny(player, world, pos, flag);
    }

    public ProtectionDecision checkAny(ServerPlayer player, String world, BlockPos pos, WorldGuardFlag... flags) {
        boolean bypass = isAdmin(player);
        List<WorldGuardRegion> regions = storage.regions(world);
        Set<String> groups = WorldGuardPermissions.regionGroups(player.createCommandSourceStack(), regions);
        if (usesBuildOverride(flags)) {
            return WorldGuardPolicy.evaluateBuild(
                regions,
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                player.getUUID(),
                groups,
                bypass,
                flags
            );
        }
        for (WorldGuardFlag flag : flags) {
            ProtectionDecision decision = WorldGuardPolicy.evaluate(
                regions,
                world,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                flag,
                player.getUUID(),
                groups,
                bypass
            );
            if (!decision.allowed()) {
                return decision;
            }
        }
        return ProtectionDecision.allow();
    }

    public ProtectionDecision check(ServerPlayer player, WorldGuardFlag flag, String world, int x, int y, int z) {
        List<WorldGuardRegion> regions = storage.regions(world);
        return WorldGuardPolicy.evaluate(
            regions,
            world,
            x,
            y,
            z,
            flag,
            player.getUUID(),
            WorldGuardPermissions.regionGroups(player.createCommandSourceStack(), regions),
            isAdmin(player)
        );
    }

    public Set<String> regionGroups(ServerPlayer player, List<WorldGuardRegion> regions) {
        return WorldGuardPermissions.regionGroups(player.createCommandSourceStack(), regions);
    }

    public boolean deny(ServerPlayer player, ProtectionDecision decision) {
        return deny(player, decision, worldId(player), player == null ? null : player.blockPosition());
    }

    public boolean deny(ServerPlayer player, ProtectionDecision decision, String world, BlockPos pos) {
        if (decision.allowed()) {
            return false;
        }
        sendDenyMessage(player, decision, world, pos, "");
        return true;
    }

    public boolean deny(ServerPlayer player, ProtectionDecision decision, String world, BlockPos pos, String action) {
        if (decision.allowed()) {
            return false;
        }
        sendDenyMessage(player, decision, world, pos, action);
        return true;
    }

    public void denyCommand(ServerPlayer player) {
        sendDenyMessage(player, ProtectionDecision.deny("", null), worldId(player), player == null ? null : player.blockPosition(), "use that command");
    }

    public void evict(UUID playerUuid) {
        lastDenyMessageMillis.remove(playerUuid);
    }

    public boolean isAdmin(ServerPlayer player) {
        return WorldGuardPermissions.bypass(player, player.createCommandSourceStack(), config);
    }

    private void sendDenyMessage(ServerPlayer player, ProtectionDecision decision, String world, BlockPos pos, String action) {
        if (player == null || decision == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastDenyMessageMillis.getOrDefault(player.getUUID(), 0L);
        if (config.denyCooldownMillis() > 0 && now - last < config.denyCooldownMillis()) {
            return;
        }
        lastDenyMessageMillis.put(player.getUUID(), now);
        player.sendSystemMessage(Component.literal(denyMessage(player, decision, world, pos, action)).withStyle(ChatFormatting.RED));
    }

    private String denyMessage(ServerPlayer player, ProtectionDecision decision, String world, BlockPos pos, String action) {
        if (world == null || pos == null) {
            return action == null || action.isBlank() ? decision.message() : WorldGuardText.denyMessage("", action);
        }

        List<WorldGuardRegion> regions = storage.regions(world);
        Set<String> groups = regionGroups(player, regions);
        WorldGuardValueFlag valueFlag = denyMessageFlag(decision.flag());
        Optional<WorldGuardFlagValue> template = RegionQueryEngine.queryValue(
            regions,
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            valueFlag,
            player.getUUID(),
            groups
        ).value();
        if (action != null && !action.isBlank()) {
            return template.map(value -> WorldGuardText.denyMessage(value.serialized(), action))
                .orElseGet(() -> WorldGuardText.denyMessage("", action));
        }
        return template.map(value -> WorldGuardText.denyMessage(decision.flag(), value.serialized()))
            .orElseGet(decision::message);
    }

    private static WorldGuardValueFlag denyMessageFlag(WorldGuardFlag flag) {
        if (flag == WorldGuardFlag.ENTRY) {
            return WorldGuardValueFlag.ENTRY_DENY_MESSAGE;
        }
        if (flag == WorldGuardFlag.EXIT || flag == WorldGuardFlag.EXIT_VIA_TELEPORT) {
            return WorldGuardValueFlag.EXIT_DENY_MESSAGE;
        }
        return WorldGuardValueFlag.DENY_MESSAGE;
    }

    private static String worldId(ServerPlayer player) {
        return player == null ? "" : player.level().dimension().identifier().toString();
    }

    private static boolean usesBuildOverride(WorldGuardFlag... flags) {
        if (flags == null || flags.length < 2) {
            return false;
        }
        for (WorldGuardFlag flag : flags) {
            if (flag == WorldGuardFlag.BUILD) {
                return true;
            }
        }
        return false;
    }
}
