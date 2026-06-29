package com.vantablack4.worldguard.worldedit;

import com.vantablack4.worldguard.VantablackWorldGuardMod;
import com.vantablack4.worldguard.WorldGuardRegion;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

public interface WorldEditSelectionSource {
    String WORLD_EDIT_MOD_ID = "worldedit";

    WorldEditSelectionResult selection(ServerPlayer player);

    WorldEditSelectionWriteResult selectRegion(ServerPlayer player, WorldGuardRegion region);

    String description();

    static WorldEditSelectionSource load() {
        if (!FabricLoader.getInstance().isModLoaded(WORLD_EDIT_MOD_ID)) {
            return new UnavailableWorldEditSelectionSource(
                "not installed; explicit coordinates remain available",
                "WorldEdit is not installed. Use explicit coordinates or install WorldEdit."
            );
        }

        try {
            Class<?> sourceType = Class.forName(
                "com.vantablack4.worldguard.worldedit.WorldEditFabricSelectionSource",
                true,
                WorldEditSelectionSource.class.getClassLoader()
            );
            return (WorldEditSelectionSource) sourceType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError exception) {
            VantablackWorldGuardMod.LOGGER.warn("WorldEdit is installed but the Fabric selection bridge could not load", exception);
            return new UnavailableWorldEditSelectionSource(
                "installed, but bridge failed to load",
                "WorldEdit is installed, but the selection bridge could not load. Use explicit coordinates."
            );
        }
    }
}
