package com.vantablack4.worldguard;

import java.util.Arrays;
import java.util.Optional;

import com.vantablack4.worldguard.flag.WorldGuardFlagDefinition;
import com.vantablack4.worldguard.flag.WorldGuardFlagType;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;

public enum WorldGuardFlag {
    PASSTHROUGH(definition("passthrough", FlagState.UNSET, false, false, false)),
    BUILD(definition("build", FlagState.UNSET, true, true, true)),
    BLOCK_BREAK(memberBypass("block-break")),
    BLOCK_PLACE(memberBypass("block-place")),
    USE(memberBypass("use")),
    INTERACT(memberBypass("interact")),
    CHEST_ACCESS(memberBypass("chest-access")),
    VEHICLE_PLACE(memberBypass("vehicle-place")),
    VEHICLE_DESTROY(memberBypass("vehicle-destroy")),
    USE_ENTITY(memberBypass("use-entity", "entity-use")),
    ATTACK_ENTITY(memberBypass("attack-entity")),
    ITEM_USE(memberBypass("item-use")),
    PVP(allowDefault("pvp")),
    DAMAGE_ANIMALS(memberBypass("damage-animals", "attack-entity")),
    SLEEP(memberBypass("sleep")),
    RESPAWN_ANCHORS(memberBypass("respawn-anchors")),
    TNT(allowDefault("tnt")),
    LIGHTER(memberBypass("lighter")),
    RIDE(memberBypass("ride")),
    POTION_SPLASH(memberBypass("potion-splash")),
    ITEM_FRAME_ROTATE(memberBypass("item-frame-rotation")),
    TRAMPLE_BLOCKS(memberBypass("block-trampling")),
    FIREWORK_DAMAGE(memberBypass("firework-damage")),
    USE_ANVIL(memberBypass("use-anvil")),
    USE_DRIPLEAF(memberBypass("use-dripleaf")),
    WIND_CHARGE_BURST(memberBypass("wind-charge-burst")),
    ITEM_PICKUP(allowDefault("item-pickup")),
    ITEM_DROP(allowDefault("item-drop")),
    EXP_DROPS(allowDefault("exp-drops")),
    MOB_DAMAGE(allowDefault("mob-damage")),
    CREEPER_EXPLOSION(allowDefault("creeper-explosion")),
    ENDERDRAGON_BLOCK_DAMAGE(allowDefault("enderdragon-block-damage")),
    GHAST_FIREBALL(allowDefault("ghast-fireball")),
    OTHER_EXPLOSION(allowDefault("other-explosion")),
    BREEZE_WIND_CHARGE(allowDefault("breeze-charge-explosion")),
    WITHER_DAMAGE(allowDefault("wither-damage")),
    ENDER_BUILD(allowDefault("enderman-grief")),
    SNOWMAN_TRAILS(allowDefault("snowman-trails")),
    RAVAGER_RAVAGE(allowDefault("ravager-grief")),
    ENTITY_PAINTING_DESTROY(allowDefault("entity-painting-destroy")),
    ENTITY_ITEM_FRAME_DESTROY(allowDefault("entity-item-frame-destroy")),
    MOB_SPAWNING(allowDefault("mob-spawning")),
    MOB_GRIEF(allowDefault("mob-grief")),
    PISTONS(allowDefault("pistons")),
    FIRE_SPREAD(allowDefault("fire-spread")),
    LAVA_FIRE(allowDefault("lava-fire")),
    LIGHTNING(allowDefault("lightning")),
    SNOW_FALL(allowDefault("snow-fall")),
    SNOW_MELT(allowDefault("snow-melt")),
    ICE_FORM(allowDefault("ice-form")),
    ICE_MELT(allowDefault("ice-melt")),
    FROSTED_ICE_MELT(allowDefault("frosted-ice-melt")),
    FROSTED_ICE_FORM(memberBypass("frosted-ice-form")),
    MUSHROOMS(allowDefault("mushroom-growth")),
    LEAF_DECAY(allowDefault("leaf-decay")),
    GRASS_SPREAD(allowDefault("grass-growth")),
    MYCELIUM_SPREAD(allowDefault("mycelium-spread")),
    VINE_GROWTH(allowDefault("vine-growth")),
    ROCK_GROWTH(allowDefault("rock-growth")),
    SCULK_GROWTH(allowDefault("sculk-growth")),
    CROP_GROWTH(allowDefault("crop-growth")),
    SOIL_DRY(allowDefault("soil-dry")),
    CORAL_FADE(allowDefault("coral-fade")),
    COPPER_FADE(allowDefault("copper-fade")),
    WATER_FLOW(allowDefault("water-flow")),
    LAVA_FLOW(allowDefault("lava-flow")),
    MOISTURE_CHANGE(allowDefault("moisture-change")),
    SEND_CHAT(allowDefault("send-chat")),
    RECEIVE_CHAT(allowDefault("receive-chat")),
    INVINCIBILITY(memberBypass("invincible")),
    FALL_DAMAGE(allowDefault("fall-damage")),
    HEALTH_REGEN(allowDefault("natural-health-regen")),
    HUNGER_DRAIN(allowDefault("natural-hunger-drain")),
    ENTRY(allowDefault("entry", WorldGuardRegionGroup.NON_MEMBERS)),
    EXIT(allowDefault("exit", WorldGuardRegionGroup.NON_MEMBERS)),
    EXIT_VIA_TELEPORT(allowDefault("exit-via-teleport")),
    ENDERPEARL(allowDefault("enderpearl")),
    CHORUS_TELEPORT(allowDefault("chorus-fruit-teleport")),
    NOTIFY_ENTER(definition("notify-enter", FlagState.UNSET, false, false, false)),
    NOTIFY_LEAVE(definition("notify-leave", FlagState.UNSET, false, false, false));

    private final WorldGuardFlagDefinition definition;

    WorldGuardFlag(WorldGuardFlagDefinition definition) {
        this.definition = definition;
    }

    public String id() {
        return definition.id();
    }

    public FlagState defaultState() {
        return definition.defaultState();
    }

    public boolean usesMembershipDefault() {
        return definition.usesMembershipDefault();
    }

    public boolean bypassesMemberDeny() {
        return definition.bypassesMemberDeny();
    }

    public boolean preventsAllowOnGlobal() {
        return definition.preventsAllowOnGlobal();
    }

    public WorldGuardRegionGroup defaultGroup() {
        return definition.defaultGroup();
    }

    public boolean supportsRegionGroup() {
        return definition.supportsRegionGroup();
    }

    public static Optional<WorldGuardFlag> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        for (WorldGuardFlag flag : values()) {
            if (flag.definition.matches(raw)) {
                return Optional.of(flag);
            }
        }
        return Optional.empty();
    }

    public static Iterable<String> ids() {
        return Arrays.stream(values()).map(WorldGuardFlag::id).toList();
    }

    private static WorldGuardFlagDefinition memberBypass(String id, String... aliases) {
        return definition(id, FlagState.UNSET, false, true, false, aliases);
    }

    private static WorldGuardFlagDefinition allowDefault(String id, String... aliases) {
        return definition(id, FlagState.ALLOW, false, false, false, aliases);
    }

    private static WorldGuardFlagDefinition allowDefault(String id, WorldGuardRegionGroup defaultGroup, String... aliases) {
        return definition(id, FlagState.ALLOW, false, false, false, defaultGroup, aliases);
    }

    private static WorldGuardFlagDefinition definition(
        String id,
        FlagState defaultState,
        boolean usesMembershipDefault,
        boolean bypassesMemberDeny,
        boolean preventsAllowOnGlobal,
        String... aliases
    ) {
        return definition(
            id,
            defaultState,
            usesMembershipDefault,
            bypassesMemberDeny,
            preventsAllowOnGlobal,
            WorldGuardRegionGroup.ALL,
            aliases
        );
    }

    private static WorldGuardFlagDefinition definition(
        String id,
        FlagState defaultState,
        boolean usesMembershipDefault,
        boolean bypassesMemberDeny,
        boolean preventsAllowOnGlobal,
        WorldGuardRegionGroup defaultGroup,
        String... aliases
    ) {
        return new WorldGuardFlagDefinition(
            id,
            WorldGuardFlagType.STATE,
            defaultState,
            usesMembershipDefault,
            bypassesMemberDeny,
            preventsAllowOnGlobal,
            defaultGroup,
            true,
            "",
            aliases
        );
    }
}
