package com.vantablack4.worldguard.flag;

import java.util.Arrays;
import java.util.Optional;

import com.vantablack4.worldguard.FlagState;

public enum WorldGuardValueFlag {
    NONPLAYER_PROTECTION_DOMAINS(definition("nonplayer-protection-domains", WorldGuardFlagType.SET_STRING, false)),
    DENY_SPAWN(definition("deny-spawn", WorldGuardFlagType.SET_REGISTRY)),
    WEATHER_LOCK(definition("weather-lock", WorldGuardFlagType.REGISTRY)),
    TIME_LOCK(definition("time-lock", WorldGuardFlagType.STRING)),
    BLOCKED_CMDS(definition("blocked-cmds", WorldGuardFlagType.SET_STRING)),
    ALLOWED_CMDS(definition("allowed-cmds", WorldGuardFlagType.SET_STRING)),
    TELEPORT(definition("teleport", WorldGuardFlagType.LOCATION)),
    SPAWN(definition("spawn", WorldGuardFlagType.LOCATION, WorldGuardRegionGroup.MEMBERS)),
    TELEPORT_MESSAGE(definition(
        "teleport-message",
        WorldGuardFlagType.STRING,
        WorldGuardRegionGroup.ALL,
        true,
        "Teleported you to the region '%id%'."
    )),
    EXIT_OVERRIDE(definition("exit-override", WorldGuardFlagType.BOOLEAN)),
    GREETING(definition("greeting", WorldGuardFlagType.STRING)),
    FAREWELL(definition("farewell", WorldGuardFlagType.STRING)),
    GREETING_TITLE(definition("greeting-title", WorldGuardFlagType.STRING)),
    FAREWELL_TITLE(definition("farewell-title", WorldGuardFlagType.STRING)),
    GAME_MODE(definition("game-mode", WorldGuardFlagType.REGISTRY)),
    HEAL_DELAY(definition("heal-delay", WorldGuardFlagType.INTEGER)),
    HEAL_AMOUNT(definition("heal-amount", WorldGuardFlagType.INTEGER)),
    HEAL_MIN_HEALTH(definition("heal-min-health", WorldGuardFlagType.DOUBLE)),
    HEAL_MAX_HEALTH(definition("heal-max-health", WorldGuardFlagType.DOUBLE)),
    FEED_DELAY(definition("feed-delay", WorldGuardFlagType.INTEGER)),
    FEED_AMOUNT(definition("feed-amount", WorldGuardFlagType.INTEGER)),
    FEED_MIN_HUNGER(definition("feed-min-hunger", WorldGuardFlagType.INTEGER)),
    FEED_MAX_HUNGER(definition("feed-max-hunger", WorldGuardFlagType.INTEGER)),
    DENY_MESSAGE(definition(
        "deny-message",
        WorldGuardFlagType.STRING,
        WorldGuardRegionGroup.ALL,
        true,
        "Hey! Sorry, but you can't %what% here."
    )),
    ENTRY_DENY_MESSAGE(definition(
        "entry-deny-message",
        WorldGuardFlagType.STRING,
        WorldGuardRegionGroup.ALL,
        true,
        "Hey! You are not permitted to enter this area."
    )),
    EXIT_DENY_MESSAGE(definition(
        "exit-deny-message",
        WorldGuardFlagType.STRING,
        WorldGuardRegionGroup.ALL,
        true,
        "Hey! You are not permitted to leave this area."
    ));

    private final WorldGuardFlagDefinition definition;
    private final Optional<WorldGuardFlagValue> defaultValue;

    WorldGuardValueFlag(WorldGuardFlagDefinition definition) {
        this.definition = definition;
        this.defaultValue = definition.defaultValue().isBlank()
            ? Optional.empty()
            : WorldGuardFlagValue.parse(this, definition.defaultValue());
    }

    public String id() {
        return definition.id();
    }

    public WorldGuardFlagType type() {
        return definition.type();
    }

    public WorldGuardRegionGroup defaultGroup() {
        return definition.defaultGroup();
    }

    public boolean supportsRegionGroup() {
        return definition.supportsRegionGroup();
    }

    public Optional<WorldGuardFlagValue> defaultValue() {
        return defaultValue;
    }

    public static Optional<WorldGuardValueFlag> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (WorldGuardValueFlag flag : values()) {
            if (flag.definition.matches(raw)) {
                return Optional.of(flag);
            }
        }
        return Optional.empty();
    }

    public static Iterable<String> ids() {
        return Arrays.stream(values()).map(WorldGuardValueFlag::id).toList();
    }

    private static WorldGuardFlagDefinition definition(String id, WorldGuardFlagType type, String... aliases) {
        return definition(id, type, WorldGuardRegionGroup.ALL, true, null, aliases);
    }

    private static WorldGuardFlagDefinition definition(
        String id,
        WorldGuardFlagType type,
        WorldGuardRegionGroup defaultGroup,
        String... aliases
    ) {
        return definition(id, type, defaultGroup, true, null, aliases);
    }

    private static WorldGuardFlagDefinition definition(String id, WorldGuardFlagType type, boolean supportsGroup) {
        return definition(id, type, WorldGuardRegionGroup.ALL, supportsGroup, null);
    }

    private static WorldGuardFlagDefinition definition(
        String id,
        WorldGuardFlagType type,
        WorldGuardRegionGroup defaultGroup,
        boolean supportsGroup,
        String defaultValue,
        String... aliases
    ) {
        return new WorldGuardFlagDefinition(
            id,
            type,
            FlagState.UNSET,
            false,
            false,
            false,
            defaultGroup,
            supportsGroup,
            defaultValue,
            aliases
        );
    }
}
