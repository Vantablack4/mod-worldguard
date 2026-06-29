package com.vantablack4.worldguard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
        if (decision.allowed()) {
            return false;
        }
        sendDenyMessage(player, decision);
        return true;
    }

    public void evict(UUID playerUuid) {
        lastDenyMessageMillis.remove(playerUuid);
    }

    public boolean isAdmin(ServerPlayer player) {
        return WorldGuardPermissions.bypass(player, player.createCommandSourceStack(), config);
    }

    private void sendDenyMessage(ServerPlayer player, ProtectionDecision decision) {
        long now = System.currentTimeMillis();
        long last = lastDenyMessageMillis.getOrDefault(player.getUUID(), 0L);
        if (config.denyCooldownMillis() > 0 && now - last < config.denyCooldownMillis()) {
            return;
        }
        lastDenyMessageMillis.put(player.getUUID(), now);
        player.sendSystemMessage(Component.literal(decision.message()).withStyle(ChatFormatting.RED));
    }
}
