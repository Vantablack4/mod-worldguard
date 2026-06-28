package com.vantablack4.worldguard.session;

import java.util.List;

import net.minecraft.core.BlockPos;

public record WorldGuardSessionSnapshot(
    String world,
    BlockPos pos,
    List<String> regionIds
) {
    public WorldGuardSessionSnapshot {
        world = world == null ? "" : world;
        pos = pos == null ? BlockPos.ZERO : pos.immutable();
        regionIds = regionIds == null ? List.of() : List.copyOf(regionIds);
    }
}
