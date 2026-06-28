package com.vantablack4.worldguard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

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
        return WorldGuardPolicy.evaluate(
            storage.regions(),
            world,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            flag,
            player.getUUID(),
            isAdmin(player)
        );
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
        return player.createCommandSourceStack().permissions().hasPermission(
            new Permission.HasCommandLevel(PermissionLevel.byId(config.adminPermissionLevel()))
        );
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
