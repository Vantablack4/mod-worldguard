package com.vantablack4.worldguard;

import com.vantablack4.worldguard.model.RegionQueryEngine;
import com.vantablack4.worldguard.model.RegionType;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
import com.vantablack4.worldguard.storage.RegionStorageSchema;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

public final class WorldGuardStorage {
    private static final Pattern VALID_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_,'\\-\\+/]+$");
    private static final String ID_SUFFIX = ".id";
    private static final String WORLD_KEY_SEPARATOR = "~";

    private final Path regionsFile;
    private final Properties properties;

    private WorldGuardStorage(Path configDirectory, Properties properties) {
        this.regionsFile = configDirectory.resolve("regions.properties");
        this.properties = properties;
    }

    public static WorldGuardStorage load(Path configDirectory) {
        try {
            Files.createDirectories(configDirectory);
        } catch (IOException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Unable to create Vantablack WorldGuard config directory", exception);
        }
        return new WorldGuardStorage(configDirectory, loadProperties(configDirectory.resolve("regions.properties")));
    }

    public synchronized void reload() {
        properties.clear();
        properties.putAll(loadProperties(regionsFile));
    }

    public synchronized void flush() {
        saveProperties();
    }

    public synchronized List<WorldGuardRegion> regions() {
        List<WorldGuardRegion> regions = regionKeys().stream()
            .map(this::parseRegionKey)
            .flatMap(Optional::stream)
            .toList();
        return RegionQueryEngine.sort(regions);
    }

    public synchronized List<WorldGuardRegion> regions(String world) {
        String normalizedWorld = normalizeWorld(world);
        return RegionQueryEngine.sort(regions().stream()
            .filter(region -> region.appliesToWorld(normalizedWorld))
            .toList());
    }

    public synchronized List<String> regionIds() {
        return regions().stream()
            .map(WorldGuardRegion::id)
            .filter(id -> !id.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public synchronized List<String> regionIds(String world) {
        return regions(world).stream()
            .map(WorldGuardRegion::id)
            .filter(id -> !id.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public synchronized Optional<WorldGuardRegion> find(String rawId) {
        String id = normalizeId(rawId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        return regions().stream()
            .filter(region -> region.id().equals(id))
            .sorted(Comparator.comparing(WorldGuardRegion::world, String.CASE_INSENSITIVE_ORDER))
            .findFirst();
    }

    public synchronized Optional<WorldGuardRegion> find(String rawId, String world) {
        String id = normalizeId(rawId);
        String normalizedWorld = normalizeWorld(world);
        if (id.isBlank()) {
            return Optional.empty();
        }
        return regions().stream()
            .filter(region -> region.id().equals(id))
            .filter(region -> region.appliesToWorld(normalizedWorld))
            .sorted(Comparator
                .comparing((WorldGuardRegion region) -> region.world().equals(normalizedWorld) ? 0 : 1)
                .thenComparing(WorldGuardRegion::world, String.CASE_INSENSITIVE_ORDER))
            .findFirst();
    }

    public synchronized List<WorldGuardRegion> regionsAt(String world, int x, int y, int z) {
        return RegionQueryEngine.applicableRegions(regions(world), normalizeWorld(world), x, y, z);
    }

    public synchronized void save(WorldGuardRegion region) {
        removeRegion(region.id(), region.world());
        writeRegion(region);
        saveProperties();
    }

    public synchronized boolean delete(String rawId) {
        String id = normalizeId(rawId);
        boolean removed = removeRegion(id);
        if (removed) {
            clearDanglingParents(id, null);
            saveProperties();
        }
        return removed;
    }

    public synchronized boolean delete(String rawId, String world) {
        String id = normalizeId(rawId);
        String normalizedWorld = normalizeWorld(world);
        boolean removed = removeRegion(id, normalizedWorld);
        if (removed) {
            clearDanglingParents(id, normalizedWorld);
            saveProperties();
        }
        return removed;
    }

    public synchronized Optional<WorldGuardRegion> setFlag(String rawId, WorldGuardFlag flag, FlagState state) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withFlag(flag, state));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setFlag(String rawId, String world, WorldGuardFlag flag, FlagState state) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withFlag(flag, state));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setValue(String rawId, WorldGuardValueFlag flag, WorldGuardFlagValue value) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withValue(flag, value));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setValue(
        String rawId,
        String world,
        WorldGuardValueFlag flag,
        WorldGuardFlagValue value
    ) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withValue(flag, value));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setFlagGroup(
        String rawId,
        WorldGuardFlag flag,
        WorldGuardRegionGroup group
    ) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withFlagGroup(flag, group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setFlagGroup(
        String rawId,
        String world,
        WorldGuardFlag flag,
        WorldGuardRegionGroup group
    ) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withFlagGroup(flag, group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setFlagGroup(
        String rawId,
        WorldGuardValueFlag flag,
        WorldGuardRegionGroup group
    ) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withFlagGroup(flag, group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setFlagGroup(
        String rawId,
        String world,
        WorldGuardValueFlag flag,
        WorldGuardRegionGroup group
    ) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withFlagGroup(flag, group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setParent(String rawId, String rawParentId) {
        String parentId = normalizeId(rawParentId);
        if (!parentId.isBlank() && find(parentId).isEmpty()) {
            return Optional.empty();
        }
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withParent(parentId));
        updated.ifPresent(region -> {
            if (RegionQueryEngine.circularParent(region, RegionQueryEngine.byId(regionsWith(region)))) {
                throw new IllegalArgumentException("Circular region parent relationship for " + region.id());
            }
            save(region);
        });
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> setParent(String rawId, String world, String rawParentId) {
        String parentId = normalizeId(rawParentId);
        String normalizedWorld = normalizeWorld(world);
        if (!parentId.isBlank() && find(parentId, normalizedWorld).isEmpty()) {
            return Optional.empty();
        }
        Optional<WorldGuardRegion> existing = find(rawId, normalizedWorld);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withParent(parentId));
        updated.ifPresent(region -> {
            if (RegionQueryEngine.circularParent(region, RegionQueryEngine.byId(regionsWith(region, normalizedWorld)))) {
                throw new IllegalArgumentException("Circular region parent relationship for " + region.id());
            }
            save(region);
        });
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addOwner(String rawId, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withOwner(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addOwner(String rawId, String world, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withOwner(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeOwner(String rawId, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutOwner(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeOwner(String rawId, String world, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutOwner(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> clearOwners(String rawId, String world) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(WorldGuardRegion::withoutOwners);
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addOwnerGroup(String rawId, String group) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withOwnerGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addOwnerGroup(String rawId, String world, String group) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withOwnerGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeOwnerGroup(String rawId, String group) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutOwnerGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeOwnerGroup(String rawId, String world, String group) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutOwnerGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addMember(String rawId, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withMember(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addMember(String rawId, String world, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withMember(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeMember(String rawId, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutMember(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeMember(String rawId, String world, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutMember(playerUuid));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> clearMembers(String rawId, String world) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(WorldGuardRegion::withoutMembers);
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addMemberGroup(String rawId, String group) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withMemberGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> addMemberGroup(String rawId, String world, String group) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withMemberGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeMemberGroup(String rawId, String group) {
        Optional<WorldGuardRegion> existing = find(rawId);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutMemberGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized Optional<WorldGuardRegion> removeMemberGroup(String rawId, String world, String group) {
        Optional<WorldGuardRegion> existing = find(rawId, world);
        Optional<WorldGuardRegion> updated = existing.map(region -> region.withoutMemberGroup(group));
        updated.ifPresent(this::save);
        return updated;
    }

    public synchronized WorldGuardRegion findOrCreateGlobal(String world) {
        String normalizedWorld = normalizeWorld(world);
        WorldGuardRegion region = regions().stream()
            .filter(candidate -> candidate.global())
            .filter(candidate -> candidate.world().equals(normalizedWorld))
            .findFirst()
            .orElseGet(() -> WorldGuardRegion.global(normalizedWorld));
        save(region);
        return region;
    }

    public static boolean validRegionId(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        return !trimmed.isBlank() && VALID_ID_PATTERN.matcher(trimmed).matches();
    }

    public static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase(WorldGuardRegion.GLOBAL_REGION_ID)) {
            return WorldGuardRegion.GLOBAL_REGION_ID;
        }
        if (!validRegionId(trimmed)) {
            return "";
        }
        return Normalizer.normalize(trimmed.toLowerCase(Locale.ROOT), Form.NFC);
    }

    public static String normalizeWorld(String rawWorld) {
        if (rawWorld == null || rawWorld.isBlank()) {
            return WorldGuardRegion.ANY_WORLD;
        }
        return rawWorld.trim();
    }

    private Optional<WorldGuardRegion> parseRegionKey(String regionKey) {
        String prefix = prefix(regionKey);
        String id = normalizeId(properties.getProperty(prefix + "id"));
        String world = properties.getProperty(prefix + "world");
        if (id.isBlank() || world == null || world.isBlank()) {
            return Optional.empty();
        }
        world = normalizeWorld(world);
        try {
            EnumMap<WorldGuardFlag, FlagState> flags = parseFlags(prefix);
            EnumMap<WorldGuardValueFlag, WorldGuardFlagValue> valueFlags = parseValueFlags(prefix);
            Map<String, WorldGuardRegionGroup> flagGroups = parseFlagGroups(prefix);
            RegionType type = RegionType.parse(properties.getProperty(prefix + "type")).orElse(RegionType.CUBOID);
            if (id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
                type = RegionType.GLOBAL;
            }
            List<WorldGuardRegion.PolygonPoint> polygonPoints = type == RegionType.POLYGON
                ? parsePolygonPoints(properties.getProperty(prefix + RegionStorageSchema.POLYGON_POINTS_KEY))
                : List.of();

            return Optional.of(new WorldGuardRegion(
                id,
                world,
                integer(prefix + "min-x", 0),
                integer(prefix + "min-y", 0),
                integer(prefix + "min-z", 0),
                integer(prefix + "max-x", 0),
                integer(prefix + "max-y", 0),
                integer(prefix + "max-z", 0),
                integer(prefix + "priority", 0),
                normalizeId(properties.getProperty(prefix + "parent")),
                type,
                parseUuidSet(properties.getProperty(prefix + "owners")),
                parseUuidSet(properties.getProperty(prefix + "members")),
                parseStringSet(properties.getProperty(prefix + "owner-groups")),
                parseStringSet(properties.getProperty(prefix + "member-groups")),
                flags,
                valueFlags,
                flagGroups,
                polygonPoints
            ));
        } catch (RuntimeException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Skipping malformed WorldGuard region: {}", id, exception);
            return Optional.empty();
        }
    }

    private EnumMap<WorldGuardFlag, FlagState> parseFlags(String prefix) {
        EnumMap<WorldGuardFlag, FlagState> flags = new EnumMap<>(WorldGuardFlag.class);
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            FlagState.parse(properties.getProperty(prefix + "flag." + flag.id()))
                .filter(state -> state != FlagState.UNSET)
                .ifPresent(state -> flags.put(flag, state));
        }
        return flags;
    }

    private EnumMap<WorldGuardValueFlag, WorldGuardFlagValue> parseValueFlags(String prefix) {
        EnumMap<WorldGuardValueFlag, WorldGuardFlagValue> valueFlags = new EnumMap<>(WorldGuardValueFlag.class);
        for (WorldGuardValueFlag flag : WorldGuardValueFlag.values()) {
            WorldGuardFlagValue.parse(flag, properties.getProperty(prefix + "flag." + flag.id()))
                .ifPresent(value -> valueFlags.put(flag, value));
        }
        return valueFlags;
    }

    private Map<String, WorldGuardRegionGroup> parseFlagGroups(String prefix) {
        HashMap<String, WorldGuardRegionGroup> groups = new HashMap<>();
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            if (flag.supportsRegionGroup()) {
                parseFlagGroup(prefix, flag.id()).ifPresent(group -> groups.put(flag.id(), group));
            }
        }
        for (WorldGuardValueFlag flag : WorldGuardValueFlag.values()) {
            if (flag.supportsRegionGroup()) {
                parseFlagGroup(prefix, flag.id()).ifPresent(group -> groups.put(flag.id(), group));
            }
        }
        return groups;
    }

    private Optional<WorldGuardRegionGroup> parseFlagGroup(String prefix, String flagId) {
        return WorldGuardRegionGroup.parse(properties.getProperty(prefix + "flag." + flagId + "-group"));
    }

    private void writeRegion(WorldGuardRegion region) {
        String prefix = prefix(regionKey(region));
        properties.setProperty(RegionStorageSchema.SCHEMA_VERSION_KEY, Integer.toString(RegionStorageSchema.CURRENT_VERSION));
        properties.setProperty(prefix + "id", region.id());
        properties.setProperty(prefix + "world", region.world());
        properties.setProperty(prefix + "type", region.type().id());
        if (region.parentId().isBlank()) {
            properties.remove(prefix + "parent");
        } else {
            properties.setProperty(prefix + "parent", region.parentId());
        }
        properties.setProperty(prefix + "min-x", Integer.toString(region.minX()));
        properties.setProperty(prefix + "min-y", Integer.toString(region.minY()));
        properties.setProperty(prefix + "min-z", Integer.toString(region.minZ()));
        properties.setProperty(prefix + "max-x", Integer.toString(region.maxX()));
        properties.setProperty(prefix + "max-y", Integer.toString(region.maxY()));
        properties.setProperty(prefix + "max-z", Integer.toString(region.maxZ()));
        properties.setProperty(prefix + "priority", Integer.toString(region.priority()));
        properties.setProperty(prefix + "owners", RegionStorageSchema.csv(region.owners()));
        properties.setProperty(prefix + "members", RegionStorageSchema.csv(region.members()));
        properties.setProperty(prefix + "owner-groups", RegionStorageSchema.csv(region.ownerGroups()));
        properties.setProperty(prefix + "member-groups", RegionStorageSchema.csv(region.memberGroups()));
        if (region.type() == RegionType.POLYGON) {
            properties.setProperty(
                prefix + RegionStorageSchema.POLYGON_POINTS_KEY,
                formatPolygonPoints(region.polygonPoints())
            );
        } else {
            properties.remove(prefix + RegionStorageSchema.POLYGON_POINTS_KEY);
        }
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            FlagState state = region.flag(flag);
            if (state == FlagState.UNSET) {
                properties.remove(prefix + "flag." + flag.id());
            } else {
                properties.setProperty(prefix + "flag." + flag.id(), state.id());
            }
            writeFlagGroup(prefix, flag.id(), flag.supportsRegionGroup(), region.explicitFlagGroup(flag.id()).orElse(null));
        }
        for (WorldGuardValueFlag flag : WorldGuardValueFlag.values()) {
            Optional<WorldGuardFlagValue> value = region.value(flag);
            if (value.isPresent()) {
                properties.setProperty(prefix + "flag." + flag.id(), value.get().serialized());
            } else {
                properties.remove(prefix + "flag." + flag.id());
            }
            writeFlagGroup(prefix, flag.id(), flag.supportsRegionGroup(), region.explicitFlagGroup(flag.id()).orElse(null));
        }
    }

    private void writeFlagGroup(
        String prefix,
        String flagId,
        boolean supportsRegionGroup,
        WorldGuardRegionGroup group
    ) {
        String key = prefix + "flag." + flagId + "-group";
        if (!supportsRegionGroup || group == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, group.id());
        }
    }

    private boolean removeRegion(String id) {
        if (id.isBlank()) {
            return false;
        }
        boolean removed = false;
        for (String regionKey : regionKeys()) {
            Optional<WorldGuardRegion> region = parseRegionKey(regionKey);
            if (region.isPresent() && region.get().id().equals(id)) {
                removed |= removeRegionKey(regionKey);
            }
        }
        return removed;
    }

    private boolean removeRegion(String id, String world) {
        if (id.isBlank()) {
            return false;
        }
        String normalizedWorld = normalizeWorld(world);
        boolean removed = false;
        for (String regionKey : regionKeys()) {
            Optional<WorldGuardRegion> region = parseRegionKey(regionKey);
            if (region.isPresent()
                && region.get().id().equals(id)
                && region.get().world().equals(normalizedWorld)) {
                removed |= removeRegionKey(regionKey);
            }
        }
        return removed;
    }

    private boolean removeRegionKey(String regionKey) {
        String prefix = prefix(regionKey);
        List<String> keys = new ArrayList<>(properties.stringPropertyNames().stream()
            .filter(key -> key.startsWith(prefix))
            .toList());
        keys.forEach(properties::remove);
        return !keys.isEmpty();
    }

    private void clearDanglingParents(String removedId, String world) {
        String normalizedWorld = world == null ? null : normalizeWorld(world);
        for (String regionKey : regionKeys()) {
            Optional<WorldGuardRegion> region = parseRegionKey(regionKey);
            if (region.isPresent()
                && (normalizedWorld == null || region.get().world().equals(normalizedWorld))
                && region.get().parentId().equals(removedId)) {
                properties.remove(prefix(regionKey) + "parent");
            }
        }
    }

    private List<WorldGuardRegion> regionsWith(WorldGuardRegion replacement) {
        List<WorldGuardRegion> updated = new ArrayList<>();
        for (WorldGuardRegion region : regions()) {
            updated.add(region.id().equals(replacement.id()) ? replacement : region);
        }
        return updated;
    }

    private List<WorldGuardRegion> regionsWith(WorldGuardRegion replacement, String world) {
        List<WorldGuardRegion> updated = new ArrayList<>();
        for (WorldGuardRegion region : regions(world)) {
            if (region.id().equals(replacement.id()) && region.world().equals(replacement.world())) {
                updated.add(replacement);
            } else {
                updated.add(region);
            }
        }
        return updated;
    }

    private List<String> regionKeys() {
        return properties.stringPropertyNames().stream()
            .filter(key -> key.startsWith(RegionStorageSchema.REGION_PREFIX) && key.endsWith(ID_SUFFIX))
            .map(key -> key.substring(
                RegionStorageSchema.REGION_PREFIX.length(),
                key.length() - ID_SUFFIX.length()
            ))
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    private static String regionKey(WorldGuardRegion region) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(region.world().getBytes(StandardCharsets.UTF_8))
            + WORLD_KEY_SEPARATOR
            + region.id();
    }

    private int integer(String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value);
    }

    private static Set<UUID> parseUuidSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<UUID> uniqueIds = new HashSet<>();
        for (String part : value.split(",")) {
            String trimmed = stripDomainPrefix(part.trim(), "uuid:");
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                uniqueIds.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException exception) {
                VantablackWorldGuardMod.LOGGER.warn("Skipping malformed WorldGuard domain UUID: {}", trimmed);
            }
        }
        return uniqueIds;
    }

    private static Set<String> parseStringSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> values = new HashSet<>();
        for (String part : value.split(",")) {
            String trimmed = com.vantablack4.worldguard.model.RegionDomain.normalizeGroup(part.trim());
            if (!trimmed.isBlank()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static List<WorldGuardRegion.PolygonPoint> parsePolygonPoints(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<WorldGuardRegion.PolygonPoint> points = new ArrayList<>();
        for (String part : value.split(";")) {
            String trimmed = part.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String[] coordinates = trimmed.split(",", -1);
            if (coordinates.length != 2) {
                throw new IllegalArgumentException("Malformed polygon point: " + trimmed);
            }
            points.add(new WorldGuardRegion.PolygonPoint(
                Integer.parseInt(coordinates[0].trim()),
                Integer.parseInt(coordinates[1].trim())
            ));
        }
        return points;
    }

    private static String formatPolygonPoints(List<WorldGuardRegion.PolygonPoint> points) {
        StringJoiner joiner = new StringJoiner(";");
        for (WorldGuardRegion.PolygonPoint point : points) {
            joiner.add(point.x() + "," + point.z());
        }
        return joiner.toString();
    }

    private static String stripDomainPrefix(String value, String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length()) ? value.substring(prefix.length()) : value;
    }

    private static String prefix(String id) {
        return RegionStorageSchema.prefix(id);
    }

    private static Properties loadProperties(Path file) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(file)) {
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Unable to load Vantablack WorldGuard regions: {}", file, exception);
        }
        return properties;
    }

    private void saveProperties() {
        try {
            Files.createDirectories(regionsFile.getParent());
            Path temporaryFile = Files.createTempFile(regionsFile.getParent(), regionsFile.getFileName().toString(), ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                properties.store(writer, "Vantablack WorldGuard regions");
            }
            try {
                Files.move(temporaryFile, regionsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, regionsFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save Vantablack WorldGuard regions: " + regionsFile, exception);
        }
    }
}
