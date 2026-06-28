package com.vantablack4.worldguard.worldedit;

public record WorldEditSelectionResult(
    WorldEditRegionSelection selection,
    String message
) {
    public static WorldEditSelectionResult success(WorldEditRegionSelection selection) {
        return new WorldEditSelectionResult(selection, "");
    }

    public static WorldEditSelectionResult unavailable(String message) {
        return new WorldEditSelectionResult(null, message);
    }

    public static WorldEditSelectionResult incomplete(String message) {
        return new WorldEditSelectionResult(null, message);
    }

    public static WorldEditSelectionResult unsupported(String message) {
        return new WorldEditSelectionResult(null, message);
    }

    public static WorldEditSelectionResult failed(String message) {
        return new WorldEditSelectionResult(null, message);
    }

    public boolean hasSelection() {
        return selection != null;
    }
}
