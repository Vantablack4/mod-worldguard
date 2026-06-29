package com.vantablack4.worldguard.worldedit;

import com.vantablack4.worldguard.WorldGuardRegion;

import net.minecraft.server.level.ServerPlayer;

final class UnavailableWorldEditSelectionSource implements WorldEditSelectionSource {
    private final String description;
    private final String message;

    UnavailableWorldEditSelectionSource(String description, String message) {
        this.description = description;
        this.message = message;
    }

    @Override
    public WorldEditSelectionResult selection(ServerPlayer player) {
        return WorldEditSelectionResult.unavailable(message);
    }

    @Override
    public WorldEditSelectionWriteResult selectRegion(ServerPlayer player, WorldGuardRegion region) {
        return WorldEditSelectionWriteResult.unavailable(message);
    }

    @Override
    public String description() {
        return description;
    }
}
