package com.vantablack4.worldguard.session;

import com.vantablack4.worldguard.WorldGuardFlag;

public record WorldGuardSessionMessage(
    WorldGuardFlag flag,
    String regionId,
    String message
) {
    public WorldGuardSessionMessage {
        regionId = regionId == null ? "" : regionId;
        message = message == null ? "" : message;
    }
}
