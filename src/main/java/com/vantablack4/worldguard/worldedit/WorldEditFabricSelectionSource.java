package com.vantablack4.worldguard.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.world.World;
import com.vantablack4.worldguard.VantablackWorldGuardMod;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.WorldGuardRegion.PolygonPoint;
import com.vantablack4.worldguard.model.RegionType;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

final class WorldEditFabricSelectionSource implements WorldEditSelectionSource {
    private static final String POLYGONAL_2D_REGION_CLASS = "com.sk89q.worldedit.regions.Polygonal2DRegion";

    @Override
    public WorldEditSelectionResult selection(ServerPlayer player) {
        try {
            FabricAdapter adapter = FabricAdapter.get();
            com.sk89q.worldedit.entity.Player worldEditPlayer = adapter.fromNativePlayer(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            World selectionWorld = session.getSelectionWorld();
            if (selectionWorld == null) {
                return WorldEditSelectionResult.incomplete("Make a complete WorldEdit cuboid or polygonal selection first.");
            }

            Region region = session.getSelection(selectionWorld);
            ServerLevel nativeWorld = adapter.toNativeWorld(selectionWorld);
            return selectionFromRegion(nativeWorld.dimension().identifier().toString(), region);
        } catch (IncompleteRegionException exception) {
            return WorldEditSelectionResult.incomplete("Make a complete WorldEdit cuboid or polygonal selection first.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Unable to read WorldEdit selection", exception);
            return WorldEditSelectionResult.failed("WorldEdit selection could not be read. Check the server log.");
        }
    }

    @Override
    public WorldEditSelectionWriteResult selectRegion(ServerPlayer player, WorldGuardRegion region) {
        if (region == null || region.global()) {
            return WorldEditSelectionWriteResult.failed("Cannot select global region.");
        }
        try {
            FabricAdapter adapter = FabricAdapter.get();
            ServerLevel nativeWorld = nativeWorld(player, region.world());
            if (nativeWorld == null) {
                return WorldEditSelectionWriteResult.failed("World '" + region.world() + "' is not loaded.");
            }

            com.sk89q.worldedit.entity.Player worldEditPlayer = adapter.fromNativePlayer(player);
            World world = adapter.fromNativeWorld(nativeWorld);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(worldEditPlayer);
            RegionSelector selector = selector(region, world);
            session.setRegionSelector(world, selector);
            selector.explainRegionAdjust(worldEditPlayer, session);
            return WorldEditSelectionWriteResult.success(selector.getTypeName());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Unable to set WorldEdit selection", exception);
            return WorldEditSelectionWriteResult.failed("WorldEdit selection could not be set. Check the server log.");
        }
    }

    @Override
    public String description() {
        return "available; /wg define <region> imports the player's WorldEdit cuboid or polygonal selection";
    }

    private static WorldEditSelectionResult selectionFromRegion(String worldId, Region region)
        throws ReflectiveOperationException {
        if (region instanceof CuboidRegion cuboidRegion) {
            BlockVector3 minimum = cuboidRegion.getMinimumPoint();
            BlockVector3 maximum = cuboidRegion.getMaximumPoint();
            return WorldEditSelectionResult.success(new WorldEditRegionSelection(
                worldId,
                minimum.x(),
                minimum.y(),
                minimum.z(),
                maximum.x(),
                maximum.y(),
                maximum.z()
            ));
        }

        if (isPolygonal2DRegion(region)) {
            return polygonalSelection(worldId, region);
        }

        return WorldEditSelectionResult.unsupported(
            "WorldEdit selection must be cuboid or polygonal 2D. Use a supported selection or explicit coordinates."
        );
    }

    private static WorldEditSelectionResult polygonalSelection(String worldId, Region region)
        throws ReflectiveOperationException {
        Object rawPoints = region.getClass().getMethod("getPoints").invoke(region);
        if (!(rawPoints instanceof List<?> points)) {
            throw new IllegalStateException("WorldEdit polygonal selection returned an unexpected point list");
        }

        List<PolygonPoint> polygonPoints = new ArrayList<>();
        for (Object point : points) {
            if (!(point instanceof BlockVector2 vector)) {
                throw new IllegalStateException("WorldEdit polygonal selection returned an unexpected point");
            }
            polygonPoints.add(new PolygonPoint(vector.x(), vector.z()));
        }
        if (polygonPoints.stream().distinct().count() < 3) {
            return WorldEditSelectionResult.unsupported("WorldEdit polygonal selection must contain at least three points.");
        }

        int minY = (Integer) region.getClass().getMethod("getMinimumY").invoke(region);
        int maxY = (Integer) region.getClass().getMethod("getMaximumY").invoke(region);
        return WorldEditSelectionResult.success(WorldEditRegionSelection.polygonal(
            worldId,
            minY,
            maxY,
            polygonPoints,
            region.getVolume()
        ));
    }

    private static boolean isPolygonal2DRegion(Region region) {
        try {
            Class<?> polygonalType = Class.forName(POLYGONAL_2D_REGION_CLASS, false, region.getClass().getClassLoader());
            return polygonalType.isInstance(region);
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    private static ServerLevel nativeWorld(ServerPlayer player, String worldId) {
        Identifier identifier = Identifier.tryParse(worldId);
        if (identifier == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, identifier);
        return player.level().getServer().getLevel(key);
    }

    private static RegionSelector selector(WorldGuardRegion region, World world) throws ReflectiveOperationException {
        if (region.type() == RegionType.POLYGON) {
            List<BlockVector2> points = region.polygonPoints().stream()
                .map(point -> BlockVector2.at(point.x(), point.z()))
                .toList();
            return new Polygonal2DRegionSelector(world, points, region.minY(), region.maxY());
        }
        return new CuboidRegionSelector(
            world,
            BlockVector3.at(region.minX(), region.minY(), region.minZ()),
            BlockVector3.at(region.maxX(), region.maxY(), region.maxZ())
        );
    }
}
