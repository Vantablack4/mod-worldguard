package com.vantablack4.worldguard;

import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;

final class WorldGuardPermissions {
    static final Identifier ADMIN = Identifier.fromNamespaceAndPath(VantablackWorldGuardMod.MOD_ID, "admin");
    static final Identifier BYPASS = Identifier.fromNamespaceAndPath(VantablackWorldGuardMod.MOD_ID, "bypass");

    private WorldGuardPermissions() {
    }

    static boolean admin(PermissionContextOwner source, WorldGuardConfig config) {
        return source.checkPermission(ADMIN, PermissionLevel.byId(config.adminPermissionLevel()));
    }

    static boolean bypass(PermissionContextOwner source, WorldGuardConfig config) {
        return source.checkPermission(BYPASS, PermissionLevel.byId(config.adminPermissionLevel()));
    }
}
