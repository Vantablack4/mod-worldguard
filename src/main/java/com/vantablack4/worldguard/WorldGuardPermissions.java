package com.vantablack4.worldguard;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import net.fabricmc.fabric.api.permission.v1.PermissionContextOwner;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

final class WorldGuardPermissions {
    static final Identifier ADMIN = Identifier.fromNamespaceAndPath(VantablackWorldGuardMod.MOD_ID, "admin");
    static final Identifier BYPASS = Identifier.fromNamespaceAndPath(VantablackWorldGuardMod.MOD_ID, "bypass");
    private static final Pattern UNSAFE_PERMISSION_PATH = Pattern.compile("[^a-z0-9/._-]");
    private static final Set<UUID> BYPASS_DISABLED = ConcurrentHashMap.newKeySet();

    private WorldGuardPermissions() {
    }

    static boolean admin(PermissionContextOwner source, WorldGuardConfig config) {
        return source.checkPermission(ADMIN, PermissionLevel.byId(config.adminPermissionLevel()));
    }

    static boolean bypass(PermissionContextOwner source, WorldGuardConfig config) {
        return source.checkPermission(BYPASS, false);
    }

    static boolean bypass(ServerPlayer player, PermissionContextOwner source, WorldGuardConfig config) {
        if (player == null || source == null) {
            return false;
        }
        return !BYPASS_DISABLED.contains(player.getUUID())
            && (
                source.checkPermission(BYPASS, false)
                    || source.checkPermission(upstreamRegionPermission("bypass"), false)
                    || source.checkPermission(upstreamRegionPermission("bypass." + safePermissionPath(player.level().dimension().identifier().toString())), false)
            );
    }

    static boolean regionCommand(PermissionContextOwner source, WorldGuardConfig config, String command) {
        return source.checkPermission(upstreamRegionPermission(command), false) || admin(source, config);
    }

    static boolean toggleBypassPermission(PermissionContextOwner source, WorldGuardConfig config) {
        return source.checkPermission(upstreamRegionPermission("toggle-bypass"), false) || admin(source, config);
    }

    static boolean hasBypassDisabled(UUID playerUuid) {
        return playerUuid != null && BYPASS_DISABLED.contains(playerUuid);
    }

    static void setBypassDisabled(UUID playerUuid, boolean disabled) {
        if (playerUuid == null) {
            return;
        }
        if (disabled) {
            BYPASS_DISABLED.add(playerUuid);
        } else {
            BYPASS_DISABLED.remove(playerUuid);
        }
    }

    static Set<String> regionGroups(PermissionContextOwner source, Collection<WorldGuardRegion> regions) {
        if (source == null || regions == null || regions.isEmpty()) {
            return Set.of();
        }
        Set<String> matching = new HashSet<>();
        for (WorldGuardRegion region : regions) {
            addMatchingGroups(source, matching, region.ownerGroups());
            addMatchingGroups(source, matching, region.memberGroups());
        }
        return Set.copyOf(matching);
    }

    private static void addMatchingGroups(PermissionContextOwner source, Set<String> matching, Collection<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        for (String group : groups) {
            String normalized = normalizeGroup(group);
            if (!normalized.isBlank() && source.checkPermission(regionGroupPermission(normalized), false)) {
                matching.add(normalized);
            }
        }
    }

    private static Identifier regionGroupPermission(String group) {
        return Identifier.fromNamespaceAndPath(
            VantablackWorldGuardMod.MOD_ID,
            "region.group." + safePermissionPath(group)
        );
    }

    private static Identifier upstreamRegionPermission(String path) {
        return Identifier.fromNamespaceAndPath("worldguard", "region." + safePermissionPath(path));
    }

    private static String safePermissionPath(String path) {
        return UNSAFE_PERMISSION_PATH.matcher(path.toLowerCase(Locale.ROOT)).replaceAll("_");
    }

    private static String normalizeGroup(String group) {
        if (group == null) {
            return "";
        }
        String trimmed = group.trim();
        if (trimmed.regionMatches(true, 0, "g:", 0, 2)) {
            trimmed = trimmed.substring(2).trim();
        } else if (trimmed.regionMatches(true, 0, "group:", 0, 6)) {
            trimmed = trimmed.substring(6).trim();
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
