package com.vantablack4.worldguard;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public record WorldGuardConfig(
    Path configDirectory,
    int adminPermissionLevel,
    long denyCooldownMillis
) {
    private static final int DEFAULT_ADMIN_PERMISSION_LEVEL = 2;
    private static final long DEFAULT_DENY_COOLDOWN_MILLIS = 1000L;

    public static WorldGuardConfig load() {
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve(VantablackWorldGuardMod.MOD_ID);
        Path configFile = configDirectory.resolve("config.properties");
        Properties properties = new Properties();

        try {
            Files.createDirectories(configDirectory);
            if (Files.isRegularFile(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile)) {
                    properties.load(reader);
                }
            } else {
                writeDefaultConfig(configFile);
            }
        } catch (IOException exception) {
            VantablackWorldGuardMod.LOGGER.warn("Unable to read Vantablack WorldGuard config, using defaults", exception);
        }

        return new WorldGuardConfig(
            configDirectory,
            boundedInt(properties, "commands.admin-permission-level", DEFAULT_ADMIN_PERMISSION_LEVEL, 0, 4),
            boundedLong(properties, "messages.deny-cooldown-millis", DEFAULT_DENY_COOLDOWN_MILLIS, 0, 60_000)
        );
    }

    private static void writeDefaultConfig(Path configFile) throws IOException {
        Properties defaults = new Properties();
        defaults.setProperty("commands.admin-permission-level", Integer.toString(DEFAULT_ADMIN_PERMISSION_LEVEL));
        defaults.setProperty("messages.deny-cooldown-millis", Long.toString(DEFAULT_DENY_COOLDOWN_MILLIS));
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            defaults.store(writer, "Vantablack WorldGuard configuration");
        }
    }

    private static int boundedInt(Properties properties, String key, int fallback, int min, int max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long boundedLong(Properties properties, String key, long fallback, long min, long max) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
