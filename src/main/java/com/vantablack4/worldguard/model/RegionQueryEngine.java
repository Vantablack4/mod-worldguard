package com.vantablack4.worldguard.model;

import com.vantablack4.worldguard.FlagState;
import com.vantablack4.worldguard.WorldGuardFlag;
import com.vantablack4.worldguard.WorldGuardRegion;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup.RegionAssociation;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;

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
        return queryState(regions, world, x, y, z, flag, playerUuid, Set.of());
    }

    public static FlagEvaluation queryState(
        Collection<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        WorldGuardFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        List<WorldGuardRegion> scoped = regions == null ? List.of() : regions.stream()
            .filter(region -> region.appliesToWorld(world))
            .toList();
        Map<String, WorldGuardRegion> byId = byId(scoped);
        List<WorldGuardRegion> applicable = applicableWithGlobal(scoped, world, x, y, z);

        FlagEvaluation explicit = queryExplicitState(applicable, byId, flag, playerUuid, playerGroups);
        if (explicit.state() != FlagState.UNSET) {
            return explicit;
        }

        if (flag.usesMembershipDefault()) {
            FlagEvaluation membership = queryMembershipDefault(applicable, byId, flag, playerUuid, playerGroups);
            if (membership.state() != FlagState.UNSET) {
                return membership;
            }
        }

        if (flag.defaultState() != FlagState.UNSET) {
            return new FlagEvaluation(flag.defaultState(), "", "");
        }

        return FlagEvaluation.unset();
    }

    public static ValueEvaluation queryValue(
        Collection<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        WorldGuardValueFlag flag,
        UUID playerUuid
    ) {
        return queryValue(regions, world, x, y, z, flag, playerUuid, Set.of());
    }

    public static ValueEvaluation queryValue(
        Collection<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z,
        WorldGuardValueFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        if (flag == null) {
            return ValueEvaluation.unset();
        }
        List<WorldGuardRegion> scoped = regions == null ? List.of() : regions.stream()
            .filter(region -> region.appliesToWorld(world))
            .toList();
        Map<String, WorldGuardRegion> byId = byId(scoped);
        List<WorldGuardRegion> applicable = applicableWithGlobal(scoped, world, x, y, z);
        ValueEvaluation explicit = queryExplicitValue(applicable, byId, flag, playerUuid, playerGroups);
        if (explicit.value().isPresent()) {
            return explicit;
        }
        return flag.defaultValue()
            .map(value -> new ValueEvaluation(Optional.of(value), "", ""))
            .orElseGet(ValueEvaluation::unset);
    }

    public static boolean owner(WorldGuardRegion region, Map<String, WorldGuardRegion> byId, UUID playerUuid) {
        return owner(region, byId, playerUuid, Set.of());
    }

    public static boolean owner(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        if (!hasAssociation(playerUuid, playerGroups) || region == null) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            if (current.owner(playerUuid, playerGroups)) {
                return true;
            }
            current = parent(current, byId).orElse(null);
        }
        return false;
    }

    public static boolean member(WorldGuardRegion region, Map<String, WorldGuardRegion> byId, UUID playerUuid) {
        return member(region, byId, playerUuid, Set.of());
    }

    public static boolean member(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        if (!hasAssociation(playerUuid, playerGroups) || region == null) {
            return false;
        }
        if (owner(region, byId, playerUuid, playerGroups)) {
            return true;
        }
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            if (current.member(playerUuid, playerGroups)) {
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
        UUID playerUuid,
        Collection<String> playerGroups
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

            FlagEvaluation evaluation = effectiveFlag(region, byId, flag, playerUuid, playerGroups);
            if (evaluation.state() != FlagState.UNSET) {
                minimumPriority = priority;
                FlagState state = evaluation.state();
                if (state == FlagState.DENY
                    && flag.bypassesMemberDeny()
                    && member(region, byId, playerUuid, playerGroups)) {
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
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        if (!hasAssociation(playerUuid, playerGroups)) {
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
            if (passthroughAllows(region, byId, playerUuid, playerGroups)) {
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
            if (!member(region, byId, playerUuid, playerGroups)) {
                return new FlagEvaluation(FlagState.DENY, region.id(), region.id());
            }
        }
        return new FlagEvaluation(FlagState.ALLOW, counted.get(0).id(), counted.get(0).id());
    }

    private static ValueEvaluation queryExplicitValue(
        List<WorldGuardRegion> applicable,
        Map<String, WorldGuardRegion> byId,
        WorldGuardValueFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        int minimumPriority = Integer.MIN_VALUE;
        Set<String> ignoredParents = new HashSet<>();

        for (WorldGuardRegion region : applicable) {
            int priority = effectivePriority(region);
            if (priority < minimumPriority) {
                break;
            }
            if (ignoredParents.contains(region.id())) {
                continue;
            }

            ValueEvaluation evaluation = effectiveValue(region, byId, flag, playerUuid, playerGroups);
            if (evaluation.value().isPresent()) {
                return evaluation;
            }
            addParents(ignoredParents, region, byId);
        }

        return ValueEvaluation.unset();
    }

    private static boolean passthroughAllows(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        FlagEvaluation passthrough = effectiveFlag(region, byId, WorldGuardFlag.PASSTHROUGH, playerUuid, playerGroups);
        if (passthrough.state() != FlagState.UNSET) {
            return passthrough.state() == FlagState.ALLOW;
        }
        return region.global() && region.owners().isEmpty() && region.members().isEmpty()
            && region.ownerGroups().isEmpty() && region.memberGroups().isEmpty();
    }

    private static FlagEvaluation effectiveFlag(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        WorldGuardFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            FlagState state = current.flag(flag);
            if (current.global() && flag.preventsAllowOnGlobal() && state == FlagState.ALLOW) {
                state = FlagState.UNSET;
            }
            if (state != FlagState.UNSET && flagApplies(current, byId, current.flagGroup(flag), playerUuid, playerGroups)) {
                return new FlagEvaluation(state, region.id(), current.id());
            }
            current = parent(current, byId).orElse(null);
        }
        return FlagEvaluation.unset();
    }

    private static ValueEvaluation effectiveValue(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        WorldGuardValueFlag flag,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        Set<String> seen = new HashSet<>();
        WorldGuardRegion current = region;
        while (current != null && seen.add(current.id())) {
            Optional<WorldGuardFlagValue> value = current.value(flag);
            if (value.isPresent() && flagApplies(current, byId, current.flagGroup(flag), playerUuid, playerGroups)) {
                return new ValueEvaluation(value, region.id(), current.id());
            }
            current = parent(current, byId).orElse(null);
        }
        return ValueEvaluation.unset();
    }

    private static boolean flagApplies(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        WorldGuardRegionGroup group,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        WorldGuardRegionGroup effectiveGroup = group == null ? WorldGuardRegionGroup.ALL : group;
        return effectiveGroup.contains(association(region, byId, playerUuid, playerGroups));
    }

    private static RegionAssociation association(
        WorldGuardRegion region,
        Map<String, WorldGuardRegion> byId,
        UUID playerUuid,
        Collection<String> playerGroups
    ) {
        if (owner(region, byId, playerUuid, playerGroups)) {
            return RegionAssociation.OWNER;
        }
        if (member(region, byId, playerUuid, playerGroups)) {
            return RegionAssociation.MEMBER;
        }
        return RegionAssociation.NON_MEMBER;
    }

    private static List<WorldGuardRegion> applicableWithGlobal(
        Collection<WorldGuardRegion> regions,
        String world,
        int x,
        int y,
        int z
    ) {
        List<WorldGuardRegion> applicable = new ArrayList<>(applicableRegions(regions, world, x, y, z));
        globalRegion(regions, world).ifPresent(applicable::add);
        return applicable;
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

    private static boolean hasAssociation(UUID playerUuid, Collection<String> playerGroups) {
        return playerUuid != null || hasGroups(playerGroups);
    }

    private static boolean hasGroups(Collection<String> playerGroups) {
        return playerGroups != null && playerGroups.stream().anyMatch(group -> group != null && !group.isBlank());
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

    public record ValueEvaluation(
        Optional<WorldGuardFlagValue> value,
        String regionId,
        String sourceRegionId
    ) {
        public static ValueEvaluation unset() {
            return new ValueEvaluation(Optional.empty(), "", "");
        }
    }
}
