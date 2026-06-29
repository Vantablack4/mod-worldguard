package com.vantablack4.worldguard;

import java.util.Arrays;
import java.util.stream.Collectors;

final class WorldGuardText {
    private WorldGuardText() {
    }

    static String invalidRegionId(String id) {
        return "The region name of '" + id + "' contains characters that are not allowed.";
    }

    static String globalNotAllowed() {
        return "Sorry, you can't use __global__ here.";
    }

    static String noRegion(String id) {
        return "No region could be found with the name of '" + id + "'.";
    }

    static String specifyInfoRegion() {
        return "Please specify the region with /region info -w world_name region_name.";
    }

    static String specifyFlagsRegion() {
        return "Please specify the region with /region flags -w world_name region_name.";
    }

    static String multipleStandingRegions() {
        return "You're standing in several regions (please pick one).";
    }

    static String noRegionsDefined() {
        return "No regions are defined.";
    }

    static String noRegionsMatched(String idFilter) {
        return "No regions matched the id search '" + idFilter + "'.";
    }

    static String invalidListPage(int page, int pageCount) {
        return "Page " + page + " is not valid. Available pages: 1-" + pageCount + ".";
    }

    static String createdRegion(String id) {
        return "A new region has been made named '" + id + "'.";
    }

    static String claimedRegion(String id) {
        return "A new region has been claimed named '" + id + "'.";
    }

    static String regionAlreadyExists(String id) {
        return "A region with that name already exists. Please choose another name."
            + " To change the shape, use /region redefine " + id + ".";
    }

    static String updatedRegionArea(String id) {
        return "Region '" + id + "' has been updated with a new area.";
    }

    static String removedRegions(String ids) {
        return "Successfully removed " + ids + ".";
    }

    static String teleportedToRegion(String id) {
        return "Teleported you to the region '" + id + "'.";
    }

    static String noTeleportPoint() {
        return "The region has no teleport point associated.";
    }

    static String noSpawnPoint() {
        return "The region has no spawn point associated.";
    }

    static String noCenterPoint() {
        return "The region has no center point.";
    }

    static String centerTeleportSpectatorOnly() {
        return "Center teleport is only available in Spectator gamemode.";
    }

    static String regionSelected(String typeName) {
        return "Region selected as " + typeName;
    }

    static String worldNotLoaded(String world) {
        return "World '" + world + "' is not loaded.";
    }

    static String unknownFlag(String flag) {
        return "Unknown flag specified: " + flag;
    }

    static String availableFlags() {
        return "Available flags: " + Arrays.stream(WorldGuardFlag.values())
            .map(WorldGuardFlag::id)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining(", "))
            + ", ";
    }

    static String invalidStateFlag(String value) {
        return "Expected none/allow/deny but got '" + value + "'";
    }

    static String flagSet(String flag, String regionId, String value) {
        return "Region flag " + flag + " set on '" + regionId + "' to '" + value + "'.";
    }

    static String flagRemoved(String flag, String regionId) {
        return "Region flag " + flag + " removed from '" + regionId
            + "'. (Any -g(roups) were also removed.)";
    }

    static String prioritySet(String regionId, int priority) {
        return "Priority of '" + regionId + "' set to " + priority + " (higher numbers override).";
    }

    static String inheritanceSet(String regionId) {
        return "Inheritance set for region '" + regionId + "'.";
    }

    static String inheritanceCleared(String regionId) {
        return inheritanceSet(regionId) + " Region is now orphaned.";
    }

    static String membersAdded(String regionId) {
        return "Region '" + regionId + "' updated with new members.";
    }

    static String membersRemoved(String regionId) {
        return "Region '" + regionId + "' updated with members removed.";
    }

    static String ownersAdded(String regionId) {
        return "Region '" + regionId + "' updated with new owners.";
    }

    static String ownersRemoved(String regionId) {
        return "Region '" + regionId + "' updated with owners removed.";
    }

    static String listNamesToRemoveOrAll() {
        return "List some names to remove, or use -a to remove all.";
    }

    static String invalidBypassArgument() {
        return "Allowed optional arguments are: on, off";
    }

    static String bypassEnabled() {
        return "You are now bypassing region protection (as long as you have permission).";
    }

    static String bypassDisabled() {
        return "You are no longer bypassing region protection.";
    }

    static String configurationReloaded() {
        return "WorldGuard configuration reloaded.";
    }

    static String regionsLoadedAllWorlds() {
        return "Successfully load the region data for all worlds.";
    }

    static String regionsSavedAllWorlds() {
        return "Successfully saved the region data for all worlds.";
    }

    static String denyMessage(WorldGuardFlag flag) {
        return "Hey! Sorry, but you can't " + action(flag) + " here.";
    }

    static String denyMessage(WorldGuardFlag flag, String template) {
        return denyMessage(template, action(flag));
    }

    static String denyMessage(String template, String action) {
        if (template == null || template.isBlank()) {
            return "Hey! Sorry, but you can't " + action + " here.";
        }
        return template.replace("%what%", action == null || action.isBlank() ? "do that" : action);
    }

    private static String action(WorldGuardFlag flag) {
        if (flag == null) {
            return "do that";
        }
        return switch (flag) {
            case BLOCK_BREAK -> "break that block";
            case BUILD, BLOCK_PLACE -> "place that block";
            case CHEST_ACCESS -> "open that";
            case ATTACK_ENTITY -> "hit that";
            case PVP -> "PvP";
            case DAMAGE_ANIMALS -> "harm that";
            case SLEEP -> "sleep";
            case RESPAWN_ANCHORS -> "use anchors";
            case TNT -> "use explosives";
            case ITEM_DROP -> "drop items";
            case ITEM_PICKUP -> "pick up items";
            case RIDE -> "ride that";
            case SEND_CHAT, RECEIVE_CHAT -> "chat";
            case USE, INTERACT, USE_ENTITY, ITEM_USE, LIGHTER, POTION_SPLASH, ITEM_FRAME_ROTATE,
                TRAMPLE_BLOCKS, FIREWORK_DAMAGE, USE_ANVIL, USE_DRIPLEAF, ENTRY, EXIT,
                EXIT_VIA_TELEPORT, ENDERPEARL, CHORUS_TELEPORT -> "use that";
            default -> "do that";
        };
    }
}
