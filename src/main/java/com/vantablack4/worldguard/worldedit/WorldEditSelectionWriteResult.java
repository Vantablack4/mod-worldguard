package com.vantablack4.worldguard.worldedit;

public record WorldEditSelectionWriteResult(
    boolean selected,
    String typeName,
    String message
) {
    public static WorldEditSelectionWriteResult success(String typeName) {
        return new WorldEditSelectionWriteResult(true, typeName == null ? "region" : typeName, "");
    }

    public static WorldEditSelectionWriteResult unavailable(String message) {
        return failed(message);
    }

    public static WorldEditSelectionWriteResult failed(String message) {
        return new WorldEditSelectionWriteResult(false, "", message);
    }
}
