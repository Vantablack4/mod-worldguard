package com.vantablack4.worldguard.flag;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public record WorldGuardFlagValue(
    WorldGuardFlagType type,
    String serialized
) {
    public WorldGuardFlagValue {
        if (type == null) {
            throw new IllegalArgumentException("Flag value type is required");
        }
        serialized = serialized == null ? "" : serialized;
    }

    public static Optional<WorldGuardFlagValue> parse(WorldGuardValueFlag flag, String raw) {
        if (flag == null || raw == null) {
            return Optional.empty();
        }
        return parse(flag.type(), raw);
    }

    public static Optional<WorldGuardFlagValue> parse(WorldGuardFlagType type, String raw) {
        if (type == null || raw == null) {
            return Optional.empty();
        }
        return switch (type) {
            case STATE -> Optional.empty();
            case BOOLEAN -> parseBoolean(raw).map(value -> new WorldGuardFlagValue(type, Boolean.toString(value)));
            case STRING -> Optional.of(new WorldGuardFlagValue(type, raw));
            case INTEGER -> parseInteger(raw).map(value -> new WorldGuardFlagValue(type, Integer.toString(value)));
            case DOUBLE -> parseDouble(raw).map(value -> new WorldGuardFlagValue(type, Double.toString(value)));
            case LOCATION -> parseLocation(raw).map(value -> new WorldGuardFlagValue(type, value.serialized()));
            case REGISTRY -> canonicalIdentifier(raw).map(value -> new WorldGuardFlagValue(type, value));
            case SET_STRING -> Optional.of(new WorldGuardFlagValue(type, canonicalSet(raw, false)));
            case SET_REGISTRY -> Optional.of(new WorldGuardFlagValue(type, canonicalSet(raw, true)));
        };
    }

    public Optional<Boolean> asBoolean() {
        return type == WorldGuardFlagType.BOOLEAN ? parseBoolean(serialized) : Optional.empty();
    }

    public Optional<Integer> asInteger() {
        return type == WorldGuardFlagType.INTEGER ? parseInteger(serialized) : Optional.empty();
    }

    public Optional<Double> asDouble() {
        return type == WorldGuardFlagType.DOUBLE ? parseDouble(serialized) : Optional.empty();
    }

    public Optional<LocationValue> asLocation() {
        return type == WorldGuardFlagType.LOCATION ? parseLocation(serialized) : Optional.empty();
    }

    public Set<String> asSet() {
        if (type != WorldGuardFlagType.SET_STRING && type != WorldGuardFlagType.SET_REGISTRY) {
            return Set.of();
        }
        return splitSet(serialized, type == WorldGuardFlagType.SET_REGISTRY);
    }

    public static Optional<WorldGuardFlagValue> location(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        return parse(WorldGuardFlagType.LOCATION, new LocationValue(world, x, y, z, yaw, pitch).serialized());
    }

    private static Optional<Boolean> parseBoolean(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "yes", "on", "1" -> Optional.of(true);
            case "false", "no", "off", "0" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    private static Optional<Integer> parseInteger(String raw) {
        try {
            return Optional.of(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Double> parseDouble(String raw) {
        try {
            double value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> canonicalIdentifier(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? Optional.empty() : Optional.of(normalized);
    }

    private static String canonicalSet(String raw, boolean registryValues) {
        return splitSet(raw, registryValues).stream().collect(Collectors.joining(","));
    }

    private static Set<String> splitSet(String raw, boolean registryValues) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        TreeSet<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> registryValues ? value.toLowerCase(Locale.ROOT) : value)
            .forEach(values::add);
        return Collections.unmodifiableSet(values);
    }

    private static Optional<LocationValue> parseLocation(String raw) {
        String[] parts = raw.split(",");
        if (parts.length != 6) {
            return Optional.empty();
        }
        String world = parts[0].trim();
        if (world.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new LocationValue(
                world,
                Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim()),
                Float.parseFloat(parts[4].trim()),
                Float.parseFloat(parts[5].trim())
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public record LocationValue(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        public LocationValue {
            if (world == null || world.isBlank()) {
                throw new IllegalArgumentException("Location world is required");
            }
            world = world.trim();
        }

        String serialized() {
            return world + ","
                + Double.toString(x) + ","
                + Double.toString(y) + ","
                + Double.toString(z) + ","
                + Float.toString(yaw) + ","
                + Float.toString(pitch);
        }
    }
}
