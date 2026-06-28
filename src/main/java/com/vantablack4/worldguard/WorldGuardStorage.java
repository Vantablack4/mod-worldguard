package com.vantablack4.worldguard;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WorldGuardStorage {
    private static final String REGION_PREFIX = "region.";

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

    public synchronized List<WorldGuardRegion> regions() {
        return regionIds().stream()
            .map(this::find)
            .flatMap(Optional::stream)
            .sorted(Comparator.comparing(WorldGuardRegion::id))
            .toList();
    }

    public synchronized List<String> regionIds() {
        return properties.stringPropertyNames().stream()
            .filter(key -> key.startsWith(REGION_PREFIX) && key.endsWith(".id"))
            .map(key -> properties.getProperty(key))
            .map(WorldGuardStorage::normalizeId)
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
        return parseRegion(id);
    }

    public synchronized List<WorldGuardRegion> regionsAt(String world, int x, int y, int z) {
        return regions().stream()
            .filter(region -> region.contains(world, x, y, z))
            .sorted(Comparator.comparingInt(WorldGuardRegion::priority).reversed())
            .toList();
    }

    public synchronized void save(WorldGuardRegion region) {
        removeRegion(region.id());
        writeRegion(region);
        saveProperties();
    }

    public synchronized boolean delete(String rawId) {
        boolean removed = removeRegion(normalizeId(rawId));
        if (removed) {
            saveProperties();
        }
        return removed;
    }

    public synchronized Optional<WorldGuardRegion> setFlag(String rawId, WorldGuardFlag flag, FlagState state) {
        Optional<WorldGuardRegion> existing = find(rawId);
        existing.ifPresent(region -> save(region.withFlag(flag, state)));
        return existing.map(region -> region.withFlag(flag, state));
    }

    public synchronized Optional<WorldGuardRegion> addMember(String rawId, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId);
        existing.ifPresent(region -> save(region.withMember(playerUuid)));
        return existing.map(region -> region.withMember(playerUuid));
    }

    public synchronized Optional<WorldGuardRegion> removeMember(String rawId, UUID playerUuid) {
        Optional<WorldGuardRegion> existing = find(rawId);
        existing.ifPresent(region -> save(region.withoutMember(playerUuid)));
        return existing.map(region -> region.withoutMember(playerUuid));
    }

    public static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        while (normalized.contains("__")) {
            normalized = normalized.replace("__", "_");
        }
        return normalized.replaceAll("^_+|_+$", "");
    }

    private Optional<WorldGuardRegion> parseRegion(String id) {
        String prefix = prefix(id);
        String world = properties.getProperty(prefix + "world");
        if (world == null || world.isBlank()) {
            return Optional.empty();
        }
        try {
            EnumMap<WorldGuardFlag, FlagState> flags = new EnumMap<>(WorldGuardFlag.class);
            for (WorldGuardFlag flag : WorldGuardFlag.values()) {
                FlagState.parse(properties.getProperty(prefix + "flag." + flag.id()))
                    .filter(state -> state != FlagState.UNSET)
                    .ifPresent(state -> flags.put(flag, state));
            }

            return Optional.of(new WorldGuardRegion(
                id,
                world,
                integer(prefix + "min-x"),
                integer(prefix + "min-y"),
                integer(prefix + "min-z"),
                integer(prefix + "max-x"),
                integer(prefix + "max-y"),
                integer(prefix + "max-z"),
                integer(prefix + "priority", 0),
                parseMembers(properties.getProperty(prefix + "members")),
                flags
            ));
        } catch (RuntimeException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Skipping malformed WorldGuard region: {}", id, exception);
            return Optional.empty();
        }
    }

    private void writeRegion(WorldGuardRegion region) {
        String prefix = prefix(region.id());
        properties.setProperty(prefix + "id", region.id());
        properties.setProperty(prefix + "world", region.world());
        properties.setProperty(prefix + "min-x", Integer.toString(region.minX()));
        properties.setProperty(prefix + "min-y", Integer.toString(region.minY()));
        properties.setProperty(prefix + "min-z", Integer.toString(region.minZ()));
        properties.setProperty(prefix + "max-x", Integer.toString(region.maxX()));
        properties.setProperty(prefix + "max-y", Integer.toString(region.maxY()));
        properties.setProperty(prefix + "max-z", Integer.toString(region.maxZ()));
        properties.setProperty(prefix + "priority", Integer.toString(region.priority()));
        properties.setProperty(prefix + "members", region.members().stream()
            .map(UUID::toString)
            .sorted()
            .collect(Collectors.joining(",")));
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            FlagState state = region.flag(flag);
            if (state == FlagState.UNSET) {
                properties.remove(prefix + "flag." + flag.id());
            } else {
                properties.setProperty(prefix + "flag." + flag.id(), state.id());
            }
        }
    }

    private boolean removeRegion(String id) {
        if (id.isBlank()) {
            return false;
        }
        String prefix = prefix(id);
        List<String> keys = new ArrayList<>(properties.stringPropertyNames().stream()
            .filter(key -> key.startsWith(prefix))
            .toList());
        keys.forEach(properties::remove);
        return !keys.isEmpty();
    }

    private int integer(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    private int integer(String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value);
    }

    private static Set<UUID> parseMembers(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<UUID> members = new HashSet<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) {
                members.add(UUID.fromString(trimmed));
            }
        }
        return members;
    }

    private static String prefix(String id) {
        return REGION_PREFIX + id + ".";
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
