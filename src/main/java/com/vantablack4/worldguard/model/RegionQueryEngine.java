package com.vantablack4.worldguard.model;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RegionQueryEngine {
    private RegionQueryEngine() {
    }

    public static List<WorldGuardRegion> applicableRegions(
        Collection<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z
    ) {
        Map<String, WorldGuardRegion> byId = byId(regions);
        return sort(regions == null ? List.of() : regions.stream()
            .filter(region -> region.contains(world, x, y, z))
            .toList(), byId);
    }

    public static List<WorldGuardRegion> sort(Collection<WorldGuardRegion> regions) {
        return sort(regions, byId(regions));
    }

    public static List<WorldGuardRegion> sort(Collection<WorldGuardRegion> regions, Map<String, WorldGuardRegion> byId) {
        List<WorldGuardRegion> sorted = new ArrayList<>(regions == null ? List.of() : regions);
        sorted.sort(Comparator
            .comparingInt(RegionQueryEngine::effectivePriority)
            .reversed()
            .thenComparing((left, right) -> {
                boolean leftChild = descendantOf(left, right, byId);
                boolean rightChild = descendantOf(right, left, byId);
                if (leftChild == rightChild) {
                    return 0;
                }
                return leftChild ? -1 : 1;
            })
            .thenComparing(WorldGuardRegion::id, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(sorted);
    }

    public static Map<String, WorldGuardRegion> byId(Collection<WorldGuardRegion> regions) {
        Map<String, WorldGuardRegion> byId = new HashMap<>();
        if (regions == null) {
            return byId;
        }
        for (WorldGuardRegion region : regions) {
            byId.put(region.id(), region);
        }
        return byId;
    }

    public static Optional<WorldGuardRegion> globalRegion(Collection<WorldGuardRegion> regions, String world) {
        if (regions == null) {
            return Optional.empty();
        }
        WorldGuardRegion wildcard = null;
        for (WorldGuardRegion region : regions) {
            if (!region.global()) {
                continue;
            }
            if (region.world().equals(world)) {
                return Optional.of(region);
            }
            if (region.appliesToWorld(world)) {
                wildcard = region;
            }
        }
        return Optional.ofNullable(wildcard);
    }

    public static FlagEvaluation queryState(
        Collection<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        WorldGuardFlag flag,
        UUID playerUuid
    ) {
        List<WorldGuardRegion> scoped = regions == null ? List.of() : regions.stream()
            .filter(region -> region.appliesToWorld(world))
            .toList();
        Map<String, WorldGuardRegion> byId = byId(scoped);
        List<WorldGuardRegion> applicable = new ArrayList<>(applicableRegions(scoped, world, x, y, z));
        globalRegion(scoped, world).ifPresent(applicable::add);

        FlagEvaluation explicit = queryExplicitState(applicable, byId, flag, playerUuid);
        if (explicit.state() != FlagState.UNSET) {
            return explicit;
        }

        if (flag.usesMembershipDefault()) {
            FlagEvaluation membership = queryMembershipDefault(applicable, byId, flag, playerUuid);
            if (membership.state() != FlagState.UNSET) {
                return membership;
            }
        }

        if (flag.defaultState() != FlagState.UNSET) {
            return new FlagEvaluation(flag.defaultState(), "", "");
        }

        return FlagEvaluation.unset();
    }

    public static boolean owner(WorldGuardRegion region, Map<String, WorldGuardRegion> byId, UUID playerUuid) {
        if (playerUuid == null || region == null) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            if (current.owner(playerUuid)) {
                return true;
            }
            current = parent(current, byId).orElse(null);
        }
        return false;
    }

    public static boolean member(WorldGuardRegion region, Map<String, WorldGuardRegion> byId, UUID playerUuid) {
        if (playerUuid == null || region == null) {
            return false;
        }
        if (owner(region, byId, playerUuid)) {
            return true;
        }
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            if (current.member(playerUuid)) {
                return true;
            }
            current = parent(current, byId).orElse(null);
        }
        return false;
    }

    public static boolean circularParent(WorldGuardRegion region, Map<String, WorldGuardRegion> byId) {
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null) {
            if (!seen.add(current.id())) {
                return true;
            }
            current = parent(current, byId).orElse(null);
        }
        return false;
    }

    private static FlagEvaluation queryExplicitState(
        List<WorldGuardRegion> applicable,
        Map<String, WorldGuardRegion> byId,
        WorldGuardFlag flag,
        UUID playerUuid
    ) {
        int minimumPriority = Integer.MIN_VALUE;
        Set<String> ignoredParents = new HashSet<>();
        List<FlagEvaluation> considered = new ArrayList<>();

        for (WorldGuardRegion region : applicable) {
            int priority = effectivePriority(region);
            if (priority < minimumPriority) {
                break;
            }
            if (ignoredParents.contains(region.id())) {
                continue;
            }

            FlagEvaluation evaluation = effectiveFlag(region, byId, flag);
            if (evaluation.state() != FlagState.UNSET) {
                minimumPriority = priority;
                FlagState state = evaluation.state();
                if (state == FlagState.DENY && flag.bypassesMemberDeny() && member(region, byId, playerUuid)) {
                    state = FlagState.ALLOW;
                }
                considered.add(new FlagEvaluation(state, region.id(), evaluation.sourceRegionId()));
            }
            addParents(ignoredParents, region, byId);
        }

        if (considered.isEmpty()) {
            return FlagEvaluation.unset();
        }

        boolean allow = false;
        FlagEvaluation allowedEvaluation = considered.get(0);
        for (FlagEvaluation evaluation : considered) {
            if (evaluation.state() == FlagState.DENY) {
                return evaluation;
            }
            if (evaluation.state() == FlagState.ALLOW) {
                allow = true;
                allowedEvaluation = evaluation;
            }
        }
        return allow ? allowedEvaluation : FlagEvaluation.unset();
    }

    private static FlagEvaluation queryMembershipDefault(
        List<WorldGuardRegion> applicable,
        Map<String, WorldGuardRegion> byId,
        WorldGuardFlag flag,
        UUID playerUuid
    ) {
        if (playerUuid == null) {
            return FlagEvaluation.unset();
        }

        int minimumPriority = Integer.MIN_VALUE;
        Set<String> ignoredParents = new HashSet<>();
        List<WorldGuardRegion> counted = new ArrayList<>();

        for (WorldGuardRegion region : applicable) {
            int priority = effectivePriority(region);
            if (priority < minimumPriority) {
                break;
            }
            if (ignoredParents.contains(region.id())) {
                continue;
            }
            if (passthroughAllows(region, byId)) {
                continue;
            }
            minimumPriority = priority;
            counted.add(region);
            addParents(ignoredParents, region, byId);
        }

        if (counted.isEmpty()) {
            return FlagEvaluation.unset();
        }

        for (WorldGuardRegion region : counted) {
            if (!member(region, byId, playerUuid)) {
                return new FlagEvaluation(FlagState.DENY, region.id(), region.id());
            }
        }
        return new FlagEvaluation(FlagState.ALLOW, counted.get(0).id(), counted.get(0).id());
    }

    private static boolean passthroughAllows(WorldGuardRegion region, Map<String, WorldGuardRegion> byId) {
        FlagEvaluation passthrough = effectiveFlag(region, byId, WorldGuardFlag.PASSTHROUGH);
        if (passthrough.state() != FlagState.UNSET) {
            return passthrough.state() == FlagState.ALLOW;
        }
        return region.global() && region.owners().isEmpty() && region.members().isEmpty()
            && region.ownerGroups().isEmpty() && region.memberGroups().isEmpty();
    }

    private static FlagEvaluation effectiveFlag(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        WorldGuardFlag flag
    ) {
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            FlagState state = current.flag(flag);
            if (current.global() && flag.preventsAllowOnGlobal() && state == FlagState.ALLOW) {
                state = FlagState.UNSET;
            }
            if (state != FlagState.UNSET) {
                return new FlagEvaluation(state, region.id(), current.id());
            }
            current = parent(current, byId).orElse(null);
        }
        return FlagEvaluation.unset();
    }

    private static Optional<WorldGuardRegion> parent(WorldGuardRegion region, Map<String, WorldGuardRegion> byId) {
        if (region == null || region.parentId().isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(region.parentId()));
    }

    private static int effectivePriority(WorldGuardRegion region) {
        return region.global() ? Integer.MIN_VALUE : region.priority();
    }

    private static boolean descendantOf(
        WorldGuardRegion candidate,
        WorldGuardRegion possibleParent,
        Map<String, WorldGuardRegion> byId
    ) {
        if (candidate == null || possibleParent == null) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        WorldGuardRegion parent = parent(candidate, byId).orElse(null);
        while (parent != null && seen.add(parent.id())) {
            if (parent.id().equals(possibleParent.id())) {
                return true;
            }
            parent = parent(parent, byId).orElse(null);
        }
        return false;
    }

    private static void addParents(Set<String> ignored, WorldGuardRegion region, Map<String, WorldGuardRegion> byId) {
        WorldGuardRegion parent = parent(region, byId).orElse(null);
        while (parent != null && ignored.add(parent.id())) {
            parent = parent(parent, byId).orElse(null);
        }
    }

    public record FlagEvaluation(
        FlagState state,
        String regionId,
        String sourceRegionId
    ) {
        public static FlagEvaluation unset() {
            return new FlagEvaluation(FlagState.UNSET, "", "");
        }
    }
}
