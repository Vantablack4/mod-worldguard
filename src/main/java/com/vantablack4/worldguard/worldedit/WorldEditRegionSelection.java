package com.vantablack4.worldguard.worldedit;

import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.WorldGuardRegion.PolygonPoint;
import com.vantablack4.worldguard.model.RegionType;

import java.util.List;

public record WorldEditRegionSelection(
    String world,
    int minX,
    int minY,
    int minZ,
    int maxX,
    int maxY,
    int maxZ,
    RegionType type,
    List<PolygonPoint> polygonPoints,
    long volume
) {
    public WorldEditRegionSelection {
        type = type == null ? RegionType.CUBOID : type;

        int originalMinX = minX;
        int originalMinY = minY;
        int originalMinZ = minZ;
        int originalMaxX = maxX;
        int originalMaxY = maxY;
        int originalMaxZ = maxZ;
        minX = Math.min(originalMinX, originalMaxX);
        minY = Math.min(originalMinY, originalMaxY);
        minZ = Math.min(originalMinZ, originalMaxZ);
        maxX = Math.max(originalMinX, originalMaxX);
        maxY = Math.max(originalMinY, originalMaxY);
        maxZ = Math.max(originalMinZ, originalMaxZ);

        if (type == RegionType.POLYGON) {
            polygonPoints = normalizePolygonPoints(polygonPoints);
            minX = polygonPoints.stream().mapToInt(PolygonPoint::x).min().orElseThrow();
            maxX = polygonPoints.stream().mapToInt(PolygonPoint::x).max().orElseThrow();
            minZ = polygonPoints.stream().mapToInt(PolygonPoint::z).min().orElseThrow();
            maxZ = polygonPoints.stream().mapToInt(PolygonPoint::z).max().orElseThrow();
        } else {
            polygonPoints = List.of();
        }

        if (volume < 0L) {
            volume = cuboidVolume(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }

    public WorldEditRegionSelection(
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
    ) {
        this(world, minX, minY, minZ, maxX, maxY, maxZ, RegionType.CUBOID, List.of(), -1L);
    }

    public static WorldEditRegionSelection polygonal(
        String world,
        int minY,
        int maxY,
        List<PolygonPoint> polygonPoints,
        long volume
    ) {
        return new WorldEditRegionSelection(
            world,
            0,
            minY,
            0,
            0,
            maxY,
            0,
            RegionType.POLYGON,
            polygonPoints,
            volume
        );
    }

    public static WorldEditRegionSelection polygonal(
        String world,
        int minY,
        int maxY,
        List<PolygonPoint> polygonPoints
    ) {
        return polygonal(world, minY, maxY, polygonPoints, -1L);
    }

    public WorldGuardRegion toDefaultProtectedRegion(String id, int priority) {
        if (type == RegionType.POLYGON) {
            return WorldGuardRegion.defaultProtectedPolygon(id, world, minY, maxY, polygonPoints, priority);
        }
        return WorldGuardRegion.defaultProtected(id, world, minX, minY, minZ, maxX, maxY, maxZ, priority);
    }

    private static long cuboidVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1);
    }

    private static List<PolygonPoint> normalizePolygonPoints(List<PolygonPoint> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("Polygon selections require at least three points");
        }
        List<PolygonPoint> copied = points.stream()
            .map(point -> {
                if (point == null) {
                    throw new IllegalArgumentException("Polygon points cannot be null");
                }
                return new PolygonPoint(point.x(), point.z());
            })
            .toList();
        if (copied.stream().distinct().count() < 3) {
            throw new IllegalArgumentException("Polygon selections require at least three unique points");
        }
        return List.copyOf(copied);
    }
}
