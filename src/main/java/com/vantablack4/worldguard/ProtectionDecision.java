package com.vantablack4.worldguard;

public record ProtectionDecision(
    boolean allowed,
    String regionId,
    WorldGuardFlag flag
) {
    public static ProtectionDecision allow() {
        return new ProtectionDecision(true, "", null);
    }

    public static ProtectionDecision deny(WorldGuardRegion region, WorldGuardFlag flag) {
        return new ProtectionDecision(false, region.id(), flag);
    }

    public static ProtectionDecision deny(String regionId, WorldGuardFlag flag) {
        return new ProtectionDecision(false, regionId == null ? "" : regionId, flag);
    }

    public String message() {
        if (allowed) {
            return "";
        }
        return "Protected region '" + regionId + "' denies " + flag.id() + ".";
    }
}
