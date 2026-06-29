package com.vantablack4.worldguard;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import com.vantablack4.worldguard.flag.WorldGuardFlagValue.LocationValue;
import com.vantablack4.worldguard.flag.WorldGuardFlagType;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;
import com.vantablack4.worldguard.model.RegionQueryEngine;
import com.vantablack4.worldguard.worldedit.WorldEditRegionSelection;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionResult;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionSource;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionWriteResult;

public final class WorldGuardCommands {
    private static final String ID_ARGUMENT = "region";
    private static final String PARENT_ARGUMENT = "parent";
    private static final String FLAG_ARGUMENT = "flag";
    private static final String VALUE_ARGUMENT = "value";
    private static final String GROUP_ARGUMENT = "group";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String DOMAIN_ARGUMENT = "domain";
    private static final String PRIORITY_ARGUMENT = "priority";
    private static final String BYPASS_ARGUMENT = "bypass";
    private static final List<String> REGION_GROUP_IDS = List.of(
        "members",
        "member",
        "owners",
        "owner",
        "nonmembers",
        "nonmember",
        "non-members",
        "non-member",
        "nonowners",
        "nonowner",
        "non-owners",
        "non-owner",
        "everyone",
        "anyone",
        "all",
        "none",
        "noone",
        "deny"
    );
    private static final SimpleCommandExceptionType PLAYER_NOT_FOUND =
        new SimpleCommandExceptionType(Component.translatable("argument.player.notfound"));

    private final WorldGuardConfig config;
    private final WorldGuardStorage storage;
    private final WorldEditSelectionSource worldEditSelectionSource;

    public WorldGuardCommands(WorldGuardConfig config, WorldGuardStorage storage) {
        this(config, storage, WorldEditSelectionSource.load());
    }

    public WorldGuardCommands(WorldGuardConfig config, WorldGuardStorage storage, WorldEditSelectionSource worldEditSelectionSource) {
        this.config = config;
        this.storage = storage;
        this.worldEditSelectionSource = worldEditSelectionSource;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(worldGuardRoot("wg"));
        dispatcher.register(worldGuardRoot("worldguard"));
        dispatcher.register(regionRoot("region"));
        dispatcher.register(regionRoot("regions"));
        dispatcher.register(regionRoot("rg"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> worldGuardRoot(String name) {
        return Commands.literal(name)
            .executes(this::version)
            .then(Commands.literal("version").executes(this::version))
            .then(Commands.literal("reload")
                .requires(this::isAdmin)
                .executes(this::reloadConfiguration))
            .then(Commands.literal("help").executes(this::help));
    }

    private LiteralArgumentBuilder<CommandSourceStack> regionRoot(String name) {
        return Commands.literal(name)
            .executes(this::help)
            .then(Commands.literal("help").executes(this::help))
            .then(Commands.literal("save")
                .requires(source -> mayRegion(source, "save"))
                .executes(this::saveRegions))
            .then(Commands.literal("write")
                .requires(source -> mayRegion(source, "save"))
                .executes(this::saveRegions))
            .then(Commands.literal("load")
                .requires(source -> mayRegion(source, "load"))
                .executes(this::loadRegions))
            .then(Commands.literal("reload")
                .requires(source -> mayRegion(source, "load"))
                .executes(this::loadRegions))
            .then(Commands.literal("list").executes(this::list))
            .then(Commands.literal("info")
                .executes(this::infoHere)
                .then(regionArgument().executes(this::info)))
            .then(Commands.literal("i")
                .executes(this::infoHere)
                .then(regionArgument().executes(this::info)))
            .then(Commands.literal("define")
                .requires(source -> mayRegion(source, "define"))
                .then(defineArguments()))
            .then(Commands.literal("def")
                .requires(source -> mayRegion(source, "define"))
                .then(defineArguments()))
            .then(Commands.literal("d")
                .requires(source -> mayRegion(source, "define"))
                .then(defineArguments()))
            .then(Commands.literal("create")
                .requires(source -> mayRegion(source, "define"))
                .then(defineArguments()))
            .then(Commands.literal("claim")
                .requires(source -> mayRegion(source, "claim"))
                .then(regionArgument().executes(this::claimFromWorldEditSelection)))
            .then(Commands.literal("select")
                .requires(source -> mayRegion(source, "select"))
                .executes(this::selectHere)
                .then(regionArgument().executes(this::selectRegion)))
            .then(Commands.literal("sel")
                .requires(source -> mayRegion(source, "select"))
                .executes(this::selectHere)
                .then(regionArgument().executes(this::selectRegion)))
            .then(Commands.literal("s")
                .requires(source -> mayRegion(source, "select"))
                .executes(this::selectHere)
                .then(regionArgument().executes(this::selectRegion)))
            .then(Commands.literal("redefine")
                .requires(source -> mayRegion(source, "redefine"))
                .then(redefineArguments()))
            .then(Commands.literal("update")
                .requires(source -> mayRegion(source, "redefine"))
                .then(redefineArguments()))
            .then(Commands.literal("move")
                .requires(source -> mayRegion(source, "redefine"))
                .then(redefineArguments()))
            .then(Commands.literal("delete")
                .requires(source -> mayRegion(source, "remove"))
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("del")
                .requires(source -> mayRegion(source, "remove"))
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("remove")
                .requires(source -> mayRegion(source, "remove"))
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("rem")
                .requires(source -> mayRegion(source, "remove"))
                .then(regionArgument().executes(this::delete)))
            .then(teleportCommand("teleport"))
            .then(teleportCommand("tp"))
            .then(Commands.literal("flags")
                .executes(this::flagsHere)
                .then(regionArgument().executes(this::regionFlags)))
            .then(Commands.literal("flag")
                .requires(source -> mayRegion(source, "flag.regions"))
                .then(flagArguments()))
            .then(Commands.literal("f")
                .requires(source -> mayRegion(source, "flag.regions"))
                .then(flagArguments()))
            .then(Commands.literal("setpriority")
                .requires(source -> mayRegion(source, "setpriority"))
                .then(setPriorityArguments()))
            .then(Commands.literal("priority")
                .requires(source -> mayRegion(source, "setpriority"))
                .then(setPriorityArguments()))
            .then(Commands.literal("pri")
                .requires(source -> mayRegion(source, "setpriority"))
                .then(setPriorityArguments()))
            .then(Commands.literal("setparent")
                .requires(source -> mayRegion(source, "setparent"))
                .then(setParentArguments()))
            .then(Commands.literal("parent")
                .requires(source -> mayRegion(source, "setparent"))
                .then(setParentArguments()))
            .then(Commands.literal("par")
                .requires(source -> mayRegion(source, "setparent"))
                .then(setParentArguments()))
            .then(Commands.literal("addmem")
                .requires(source -> mayRegion(source, "addmember"))
                .then(domainRegionArguments(this::addMemberDomain)))
            .then(Commands.literal("am")
                .requires(source -> mayRegion(source, "addmember"))
                .then(domainRegionArguments(this::addMemberDomain)))
            .then(Commands.literal("addowner")
                .requires(source -> mayRegion(source, "addowner"))
                .then(domainRegionArguments(this::addOwnerDomain)))
            .then(Commands.literal("ao")
                .requires(source -> mayRegion(source, "addowner"))
                .then(domainRegionArguments(this::addOwnerDomain)))
            .then(Commands.literal("removeowner")
                .requires(source -> mayRegion(source, "removeowner"))
                .then(removeDomainRegionArguments(this::removeOwnerDomain, this::clearOwners)))
            .then(Commands.literal("remowner")
                .requires(source -> mayRegion(source, "removeowner"))
                .then(removeDomainRegionArguments(this::removeOwnerDomain, this::clearOwners)))
            .then(Commands.literal("ro")
                .requires(source -> mayRegion(source, "removeowner"))
                .then(removeDomainRegionArguments(this::removeOwnerDomain, this::clearOwners)))
            .then(Commands.literal("addmember")
                .requires(source -> mayRegion(source, "addmember"))
                .then(domainRegionArguments(this::addMemberDomain)))
            .then(Commands.literal("removemember")
                .requires(source -> mayRegion(source, "removemember"))
                .then(removeDomainRegionArguments(this::removeMemberDomain, this::clearMembers)))
            .then(Commands.literal("removemem")
                .requires(source -> mayRegion(source, "removemember"))
                .then(removeDomainRegionArguments(this::removeMemberDomain, this::clearMembers)))
            .then(Commands.literal("remmember")
                .requires(source -> mayRegion(source, "removemember"))
                .then(removeDomainRegionArguments(this::removeMemberDomain, this::clearMembers)))
            .then(Commands.literal("remmem")
                .requires(source -> mayRegion(source, "removemember"))
                .then(removeDomainRegionArguments(this::removeMemberDomain, this::clearMembers)))
            .then(Commands.literal("rm")
                .requires(source -> mayRegion(source, "removemember"))
                .then(removeDomainRegionArguments(this::removeMemberDomain, this::clearMembers)))
            .then(Commands.literal("toggle-bypass")
                .requires(source -> WorldGuardPermissions.toggleBypassPermission(source, config))
                .executes(this::toggleBypass)
                .then(Commands.argument(BYPASS_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestBypassStates)
                    .executes(this::setBypass)))
            .then(Commands.literal("bypass")
                .requires(source -> WorldGuardPermissions.toggleBypassPermission(source, config))
                .executes(this::toggleBypass)
                .then(Commands.argument(BYPASS_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestBypassStates)
                    .executes(this::setBypass)));
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(header("WorldGuard"));
        source.sendSystemMessage(line("Region root", "/region, /regions, /rg"));
        source.sendSystemMessage(line("Read", "/rg list | /rg info <region> | /rg flags <region>"));
        source.sendSystemMessage(line("Define", "/rg define <region> | /rg redefine <region>"));
        source.sendSystemMessage(line("WorldEdit", worldEditSelectionSource.description()));
        source.sendSystemMessage(line("Global", "/rg flag __global__ <flag> <allow|deny>"));
        source.sendSystemMessage(line("Flags", "/rg flag <region> <flag> [-g <group>] [value]"));
        source.sendSystemMessage(line("Regions", "/rg setpriority <region> <priority> | /rg setparent <region> [parent]"));
        source.sendSystemMessage(line("Owners", "/rg addowner <region> <player> | /rg removeowner <region> <player>"));
        source.sendSystemMessage(line("Members", "/rg addmember <region> <player> | /rg removemember <region> <player>"));
        return 1;
    }

    private int version(CommandContext<CommandSourceStack> context) {
        String version = FabricLoader.getInstance()
            .getModContainer(VantablackWorldGuardMod.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
        context.getSource().sendSystemMessage(Component.literal("WorldGuard " + version));
        context.getSource().sendSystemMessage(Component.literal("http://www.enginehub.org"));
        return 1;
    }

    private int reloadConfiguration(CommandContext<CommandSourceStack> context) {
        storage.reload();
        context.getSource().sendSystemMessage(success(WorldGuardText.configurationReloaded()));
        return 1;
    }

    private int saveRegions(CommandContext<CommandSourceStack> context) {
        storage.flush();
        context.getSource().sendSystemMessage(success(WorldGuardText.regionsSavedAllWorlds()));
        return 1;
    }

    private int loadRegions(CommandContext<CommandSourceStack> context) {
        storage.reload();
        context.getSource().sendSystemMessage(success(WorldGuardText.regionsLoadedAllWorlds()));
        return 1;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        String world = commandWorld(context);
        List<WorldGuardRegion> regions = listRegions(world);
        context.getSource().sendSystemMessage(header("Regions"));
        if (regions.isEmpty()) {
            context.getSource().sendSystemMessage(error("No regions are defined."));
            return 0;
        }
        for (int index = 0; index < regions.size(); index++) {
            WorldGuardRegion region = regions.get(index);
            context.getSource().sendSystemMessage(Component.literal((index + 1) + ". ")
                .withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(region.id()).withStyle(ChatFormatting.GOLD)));
        }
        return regions.size();
    }

    private int infoHere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = playerOrMessage(context, WorldGuardText.specifyInfoRegion());
        if (player == null) {
            return 0;
        }
        List<WorldGuardRegion> regions = storage.regionsAt(
            worldId(player),
            player.blockPosition().getX(),
            player.blockPosition().getY(),
            player.blockPosition().getZ()
        );
        if (regions.isEmpty()) {
            WorldGuardRegion global = storage.findOrCreateGlobal(worldId(player));
            return sendInfo(context, global);
        }
        if (regions.size() > 1) {
            context.getSource().sendSystemMessage(error(WorldGuardText.multipleStandingRegions()));
            return 0;
        }
        return sendInfo(context, regions.getFirst());
    }

    private int info(CommandContext<CommandSourceStack> context) {
        return findExistingRegion(context, getString(context, ID_ARGUMENT), true)
            .map(region -> sendInfo(context, region))
            .orElse(0);
    }

    private int sendInfo(CommandContext<CommandSourceStack> context, WorldGuardRegion region) {
        context.getSource().sendSystemMessage(header("Region Info"));
        context.getSource().sendSystemMessage(line(
            "Region",
            region.id() + " (type=" + region.type().id() + ", priority=" + region.priority() + ")"
        ));
        context.getSource().sendSystemMessage(line("Flags", flagsDisplay(region)));
        context.getSource().sendSystemMessage(line("Owners", Integer.toString(region.owners().size())));
        context.getSource().sendSystemMessage(line("Members", Integer.toString(region.members().size())));
        context.getSource().sendSystemMessage(line("Bounds", region.boundsDisplay()));
        if (!region.parentId().isBlank()) {
            context.getSource().sendSystemMessage(line("Parent", region.parentId()));
        }
        return 1;
    }

    private int define(CommandContext<CommandSourceStack> context, int priority) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }
        if (storage.find(id, worldId(player)).isPresent()) {
            context.getSource().sendSystemMessage(error(WorldGuardText.regionAlreadyExists(id)));
            return 0;
        }
        WorldGuardRegion region = WorldGuardRegion.defaultProtected(
            id,
            worldId(player),
            getInteger(context, "x1"),
            getInteger(context, "y1"),
            getInteger(context, "z1"),
            getInteger(context, "x2"),
            getInteger(context, "y2"),
            getInteger(context, "z2"),
            priority
        );
        storage.save(region);
        context.getSource().sendSystemMessage(success(WorldGuardText.createdRegion(id)));
        return 1;
    }

    private int defineFromWorldEditSelection(CommandContext<CommandSourceStack> context, int priority) throws CommandSyntaxException {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        if (storage.find(id, worldId(player)).isPresent()) {
            context.getSource().sendSystemMessage(error(WorldGuardText.regionAlreadyExists(id)));
            return 0;
        }
        WorldEditSelectionResult result = worldEditSelectionSource.selection(player);
        if (!result.hasSelection()) {
            context.getSource().sendSystemMessage(error(result.message()));
            return 0;
        }

        WorldEditRegionSelection selection = result.selection();
        WorldGuardRegion region = selection.toDefaultProtectedRegion(id, priority);
        storage.save(region);
        context.getSource().sendSystemMessage(success(WorldGuardText.createdRegion(id)));
        return 1;
    }

    private int claimFromWorldEditSelection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        if (storage.find(id, worldId(player)).isPresent()) {
            context.getSource().sendSystemMessage(error(WorldGuardText.regionAlreadyExists(id)));
            return 0;
        }
        WorldEditSelectionResult result = worldEditSelectionSource.selection(player);
        if (!result.hasSelection()) {
            context.getSource().sendSystemMessage(error(result.message()));
            return 0;
        }

        WorldGuardRegion region = result.selection().toDefaultProtectedRegion(id, 0)
            .withOwner(player.getUUID());
        storage.save(region);
        context.getSource().sendSystemMessage(success(WorldGuardText.claimedRegion(id)));
        return 1;
    }

    private int selectHere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = playerOrMessage(context, "Please specify a region name.");
        if (player == null) {
            return 0;
        }
        List<WorldGuardRegion> regions = storage.regionsAt(
            worldId(player),
            player.blockPosition().getX(),
            player.blockPosition().getY(),
            player.blockPosition().getZ()
        );
        if (regions.isEmpty()) {
            context.getSource().sendSystemMessage(error("Please specify a region name."));
            return 0;
        }
        if (regions.size() > 1) {
            context.getSource().sendSystemMessage(error(WorldGuardText.multipleStandingRegions()));
            return 0;
        }
        return selectRegion(context, regions.getFirst());
    }

    private int selectRegion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Optional<WorldGuardRegion> region = findExistingRegion(context, getString(context, ID_ARGUMENT), false);
        if (region.isEmpty()) {
            return 0;
        }
        return selectRegion(context, region.get());
    }

    private int selectRegion(CommandContext<CommandSourceStack> context, WorldGuardRegion region) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        WorldEditSelectionWriteResult result = worldEditSelectionSource.selectRegion(player, region);
        if (!result.selected()) {
            context.getSource().sendSystemMessage(error(result.message()));
            return 0;
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.regionSelected(result.typeName())));
        return 1;
    }

    private int redefineFromWorldEditSelection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        WorldGuardRegion existing = storage.find(id, world).orElse(null);
        if (existing == null) {
            noRegion(context, id);
            return 0;
        }

        ServerPlayer player = context.getSource().getPlayerOrException();
        WorldEditSelectionResult result = worldEditSelectionSource.selection(player);
        if (!result.hasSelection()) {
            context.getSource().sendSystemMessage(error(result.message()));
            return 0;
        }

        WorldGuardRegion replacement = withShape(existing, result.selection());
        storage.save(replacement);
        context.getSource().sendSystemMessage(success(WorldGuardText.updatedRegionArea(replacement.id())));
        return 1;
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), true);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        if (id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            storage.findOrCreateGlobal(world);
        } else if (storage.find(id, world).isEmpty()) {
            noRegion(context, id);
            return 0;
        }
        if (storage.delete(id, world)) {
            context.getSource().sendSystemMessage(success(WorldGuardText.removedRegions(id)));
            return 1;
        }
        noRegion(context, id);
        return 0;
    }

    private int teleport(CommandContext<CommandSourceStack> context, TeleportMode mode) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }
        Optional<WorldGuardRegion> existing = storage.find(id, commandWorld(context));
        if (existing.isEmpty()) {
            noRegion(context, id);
            return 0;
        }

        WorldGuardRegion region = existing.get();
        Optional<LocationValue> location = teleportLocation(context, player, region, mode);
        if (location.isEmpty()) {
            return 0;
        }

        LocationValue target = location.get();
        ServerLevel targetLevel = level(context, target.world());
        if (targetLevel == null) {
            context.getSource().sendSystemMessage(error(WorldGuardText.worldNotLoaded(target.world())));
            return 0;
        }

        if (!player.teleportTo(
            targetLevel,
            target.x(),
            target.y(),
            target.z(),
            Set.of(),
            target.yaw(),
            target.pitch(),
            true
        )) {
            return 0;
        }
        teleportMessage(player, region)
            .ifPresent(message -> context.getSource().sendSystemMessage(success(message)));
        return 1;
    }

    private int flagsHere(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = playerOrMessage(context, WorldGuardText.specifyFlagsRegion());
        if (player == null) {
            return 0;
        }
        List<WorldGuardRegion> regions = storage.regionsAt(
            worldId(player),
            player.blockPosition().getX(),
            player.blockPosition().getY(),
            player.blockPosition().getZ()
        );
        if (regions.isEmpty()) {
            return sendRegionFlags(context, storage.findOrCreateGlobal(worldId(player)));
        }
        if (regions.size() > 1) {
            context.getSource().sendSystemMessage(error(WorldGuardText.multipleStandingRegions()));
            return 0;
        }
        return sendRegionFlags(context, regions.getFirst());
    }

    private int regionFlags(CommandContext<CommandSourceStack> context) {
        return findExistingRegion(context, getString(context, ID_ARGUMENT), true)
            .map(region -> sendRegionFlags(context, region))
            .orElse(0);
    }

    private int sendRegionFlags(CommandContext<CommandSourceStack> context, WorldGuardRegion region) {
        context.getSource().sendSystemMessage(header("Flags for " + region.id()));
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            context.getSource().sendSystemMessage(line(flag.id(), region.flag(flag).id()));
        }
        return 1;
    }

    private int setFlag(CommandContext<CommandSourceStack> context) {
        return setRegionFlag(context, getString(context, ID_ARGUMENT), getString(context, VALUE_ARGUMENT));
    }

    private int setRegionFlag(CommandContext<CommandSourceStack> context, String rawId, String rawValue) {
        FlagTarget target = flagTarget(context);
        if (target == null) {
            return 0;
        }
        GroupTarget group = groupTarget(context, target);
        if (group.invalid()) {
            return 0;
        }
        String id = regionIdForAllowedGlobalCommand(context, rawId);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        if (target.stateFlag() != null) {
            return setStateFlag(context, id, world, target.stateFlag(), rawValue, group);
        }
        return setValueFlag(context, id, world, target.valueFlag(), rawValue, group);
    }

    private int clearRegionFlag(CommandContext<CommandSourceStack> context) {
        FlagTarget target = flagTarget(context);
        if (target == null) {
            return 0;
        }
        GroupTarget group = groupTarget(context, target);
        if (group.invalid()) {
            return 0;
        }
        String id = regionIdForAllowedGlobalCommand(context, getString(context, ID_ARGUMENT));
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        Optional<WorldGuardRegion> updated = target.stateFlag() != null
            ? storage.setFlag(id, world, target.stateFlag(), FlagState.UNSET)
            : storage.setValue(id, world, target.valueFlag(), null);
        if (updated.isEmpty()) {
            noRegion(context, id);
            return 0;
        }
        clearFlagGroup(id, world, target);
        context.getSource().sendSystemMessage(success(WorldGuardText.flagRemoved(target.id(), updated.get().id())));
        return 1;
    }

    private int setStateFlag(
        CommandContext<CommandSourceStack> context,
        String id,
        String world,
        WorldGuardFlag flag,
        String rawValue,
        GroupTarget group
    ) {
        FlagState state = FlagState.parse(rawValue).orElse(null);
        if (state == null) {
            context.getSource().sendSystemMessage(error(WorldGuardText.invalidStateFlag(rawValue)));
            return 0;
        }
        Optional<WorldGuardRegion> updated = storage.setFlag(id, world, flag, state);
        if (updated.isEmpty()) {
            noRegion(context, id);
            return 0;
        }
        if (state == FlagState.UNSET) {
            clearFlagGroup(id, world, new FlagTarget(flag, null, flag.id()));
            context.getSource().sendSystemMessage(success(WorldGuardText.flagRemoved(flag.id(), updated.get().id())));
            return 1;
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.flagSet(flag.id(), updated.get().id(), state.id())));
        return applyFlagGroup(context, id, world, new FlagTarget(flag, null, flag.id()), group) ? 1 : 0;
    }

    private int setValueFlag(
        CommandContext<CommandSourceStack> context,
        String id,
        String world,
        WorldGuardValueFlag flag,
        String rawValue,
        GroupTarget group
    ) {
        Optional<WorldGuardFlagValue> value = WorldGuardFlagValue.parse(flag, rawValue);
        if (value.isEmpty()) {
            context.getSource().sendSystemMessage(error("Invalid value for flag '" + flag.id() + "': " + rawValue));
            return 0;
        }
        Optional<WorldGuardRegion> updated = storage.setValue(id, world, flag, value.get());
        if (updated.isEmpty()) {
            noRegion(context, id);
            return 0;
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.flagSet(flag.id(), updated.get().id(), value.get().serialized())));
        return applyFlagGroup(context, id, world, new FlagTarget(null, flag, flag.id()), group) ? 1 : 0;
    }

    private int setFlagGroup(CommandContext<CommandSourceStack> context) {
        FlagTarget target = flagTarget(context);
        if (target == null) {
            return 0;
        }
        GroupTarget group = groupTarget(context, target);
        if (group.invalid()) {
            return 0;
        }
        String id = regionIdForAllowedGlobalCommand(context, getString(context, ID_ARGUMENT));
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return applyFlagGroup(context, id, world, target, group) ? 1 : 0;
    }

    private boolean applyFlagGroup(
        CommandContext<CommandSourceStack> context,
        String id,
        String world,
        FlagTarget target,
        GroupTarget group
    ) {
        if (!group.present()) {
            return true;
        }
        Optional<WorldGuardRegion> updated;
        if (group.group() == target.defaultGroup()) {
            updated = setExplicitFlagGroup(id, world, target, null);
            if (updated.isPresent()) {
                context.getSource().sendSystemMessage(success("Region group flag for '" + target.id() + "' reset to default."));
            }
        } else {
            updated = setExplicitFlagGroup(id, world, target, group.group());
            if (updated.isPresent()) {
                context.getSource().sendSystemMessage(success("Region group flag for '" + target.id() + "' set."));
            }
        }
        if (updated.isEmpty()) {
            noRegion(context, id);
            return false;
        }
        return true;
    }

    private Optional<WorldGuardRegion> setExplicitFlagGroup(
        String id,
        String world,
        FlagTarget target,
        WorldGuardRegionGroup group
    ) {
        if (target.stateFlag() != null) {
            return storage.setFlagGroup(id, world, target.stateFlag(), group);
        }
        return storage.setFlagGroup(id, world, target.valueFlag(), group);
    }

    private void clearFlagGroup(String id, String world, FlagTarget target) {
        if (!target.supportsRegionGroup()) {
            return;
        }
        if (target.stateFlag() != null) {
            storage.setFlagGroup(id, world, target.stateFlag(), null);
        } else {
            storage.setFlagGroup(id, world, target.valueFlag(), null);
        }
    }

    private int setPriority(CommandContext<CommandSourceStack> context) {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }
        int priority = getInteger(context, PRIORITY_ARGUMENT);
        String world = commandWorld(context);
        return storage.find(id, world)
            .map(region -> {
                WorldGuardRegion updated = copyWithPriority(region, priority);
                storage.save(updated);
                context.getSource().sendSystemMessage(success(WorldGuardText.prioritySet(updated.id(), priority)));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int setParent(CommandContext<CommandSourceStack> context) {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        String parentId = checkRegionId(context, getString(context, PARENT_ARGUMENT), false);
        if (id.isBlank() || parentId.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        if (storage.find(id, world).isEmpty()) {
            noRegion(context, id);
            return 0;
        }
        if (storage.find(parentId, world).isEmpty()) {
            noRegion(context, parentId);
            return 0;
        }
        try {
            return storage.setParent(id, world, parentId)
                .map(region -> {
                    context.getSource().sendSystemMessage(success(WorldGuardText.inheritanceSet(region.id())));
                    return 1;
                })
                .orElseGet(() -> {
                    noRegion(context, id);
                    return 0;
                });
        } catch (IllegalArgumentException exception) {
            context.getSource().sendSystemMessage(error(exception.getMessage()));
            return 0;
        }
    }

    private int clearParent(CommandContext<CommandSourceStack> context) {
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), false);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.setParent(id, world, "")
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.inheritanceCleared(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int addOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return addOwner(context, player.getUUID());
    }

    private int addOwner(CommandContext<CommandSourceStack> context, UUID playerUuid) {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.addOwner(id, world, playerUuid)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.ownersAdded(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int removeOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return removeOwner(context, player.getUUID());
    }

    private int removeOwner(CommandContext<CommandSourceStack> context, UUID playerUuid) {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.removeOwner(id, world, playerUuid)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.ownersRemoved(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int addMember(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return addMember(context, player.getUUID());
    }

    private int addMember(CommandContext<CommandSourceStack> context, UUID playerUuid) {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.addMember(id, world, playerUuid)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.membersAdded(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int removeMember(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return removeMember(context, player.getUUID());
    }

    private int removeMember(CommandContext<CommandSourceStack> context, UUID playerUuid) {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.removeMember(id, world, playerUuid)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.membersRemoved(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int addOwnerDomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        Optional<WorldGuardRegion> updated = Optional.empty();
        for (DomainArgument domain : domainArguments(context)) {
            updated = domain.playerUuid() != null
                ? storage.addOwner(id, world, domain.playerUuid())
                : storage.addOwnerGroup(id, world, domain.group());
            if (updated.isEmpty()) {
                noRegion(context, id);
                return 0;
            }
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.ownersAdded(updated.orElseThrow().id())));
        return 1;
    }

    private int removeOwnerDomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        Optional<WorldGuardRegion> updated = Optional.empty();
        for (DomainArgument domain : domainArguments(context)) {
            updated = domain.playerUuid() != null
                ? storage.removeOwner(id, world, domain.playerUuid())
                : storage.removeOwnerGroup(id, world, domain.group());
            if (updated.isEmpty()) {
                noRegion(context, id);
                return 0;
            }
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.ownersRemoved(updated.orElseThrow().id())));
        return 1;
    }

    private int clearOwners(CommandContext<CommandSourceStack> context) {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.clearOwners(id, world)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.ownersRemoved(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int addMemberDomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        Optional<WorldGuardRegion> updated = Optional.empty();
        for (DomainArgument domain : domainArguments(context)) {
            updated = domain.playerUuid() != null
                ? storage.addMember(id, world, domain.playerUuid())
                : storage.addMemberGroup(id, world, domain.group());
            if (updated.isEmpty()) {
                noRegion(context, id);
                return 0;
            }
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.membersAdded(updated.orElseThrow().id())));
        return 1;
    }

    private int removeMemberDomain(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        Optional<WorldGuardRegion> updated = Optional.empty();
        for (DomainArgument domain : domainArguments(context)) {
            updated = domain.playerUuid() != null
                ? storage.removeMember(id, world, domain.playerUuid())
                : storage.removeMemberGroup(id, world, domain.group());
            if (updated.isEmpty()) {
                noRegion(context, id);
                return 0;
            }
        }
        context.getSource().sendSystemMessage(success(WorldGuardText.membersRemoved(updated.orElseThrow().id())));
        return 1;
    }

    private int clearMembers(CommandContext<CommandSourceStack> context) {
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.clearMembers(id, world)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.membersRemoved(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int missingRemoveDomains(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(error(WorldGuardText.listNamesToRemoveOrAll()));
        return 0;
    }

    private int toggleBypass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return setBypass(context, player, WorldGuardPermissions.hasBypassDisabled(player.getUUID()));
    }

    private int setBypass(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String value = getString(context, BYPASS_ARGUMENT);
        if (!value.equalsIgnoreCase("on") && !value.equalsIgnoreCase("off")) {
            context.getSource().sendSystemMessage(error(WorldGuardText.invalidBypassArgument()));
            return 0;
        }
        return setBypass(context, context.getSource().getPlayerOrException(), value.equalsIgnoreCase("on"));
    }

    private int setBypass(CommandContext<CommandSourceStack> context, ServerPlayer player, boolean enabled) {
        WorldGuardPermissions.setBypassDisabled(player.getUUID(), !enabled);
        context.getSource().sendSystemMessage(Component.literal(
            enabled ? WorldGuardText.bypassEnabled() : WorldGuardText.bypassDisabled()
        ));
        return 1;
    }

    private boolean isAdmin(CommandSourceStack source) {
        return WorldGuardPermissions.admin(source, config);
    }

    private boolean mayRegion(CommandSourceStack source, String command) {
        return WorldGuardPermissions.regionCommand(source, config, command);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> regionArgument() {
        return regionArgument(ID_ARGUMENT);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> parentArgument() {
        return regionArgument(PARENT_ARGUMENT);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> regionArgument(String argumentName) {
        return Commands.argument(argumentName, StringArgumentType.word()).suggests(this::suggestRegions);
    }

    private LiteralArgumentBuilder<CommandSourceStack> teleportCommand(String name) {
        return Commands.literal(name)
            .then(regionArgument().executes(context -> teleport(context, TeleportMode.FLAG)))
            .then(Commands.literal("-s")
                .then(regionArgument().executes(context -> teleport(context, TeleportMode.SPAWN))))
            .then(Commands.literal("-c")
                .then(regionArgument().executes(context -> teleport(context, TeleportMode.CENTER))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> domainRegionArguments(Command<CommandSourceStack> command) {
        return regionArgument()
            .then(Commands.argument(DOMAIN_ARGUMENT, StringArgumentType.greedyString())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                    context.getSource().getServer().getPlayerNames(),
                    builder
                ))
                .executes(command));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> removeDomainRegionArguments(
        Command<CommandSourceStack> removeCommand,
        Command<CommandSourceStack> clearCommand
    ) {
        return regionArgument()
            .executes(this::missingRemoveDomains)
            .then(Commands.literal("-a").executes(clearCommand))
            .then(Commands.argument(DOMAIN_ARGUMENT, StringArgumentType.greedyString())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                    context.getSource().getServer().getPlayerNames(),
                    builder
                ))
                .executes(removeCommand));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> flagArguments() {
        return regionArgument()
            .then(flagValueArguments());
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> flagValueArguments() {
        return Commands.argument(FLAG_ARGUMENT, StringArgumentType.word())
            .suggests(this::suggestFlags)
            .executes(this::clearRegionFlag)
            .then(valueArgument().executes(this::setFlag))
            .then(Commands.literal("-g")
                .then(groupArgument()
                    .executes(this::setFlagGroup)
                    .then(valueArgument().executes(this::setFlag))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> valueArgument() {
        return Commands.argument(VALUE_ARGUMENT, StringArgumentType.greedyString())
            .suggests(this::suggestFlagValues);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> groupArgument() {
        return Commands.argument(GROUP_ARGUMENT, StringArgumentType.word())
            .suggests(this::suggestGroups);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> setPriorityArguments() {
        return regionArgument()
            .then(Commands.argument(PRIORITY_ARGUMENT, IntegerArgumentType.integer())
                .executes(this::setPriority));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> setParentArguments() {
        return regionArgument()
            .executes(this::clearParent)
            .then(parentArgument()
                .executes(this::setParent));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> defineArguments() {
        RequiredArgumentBuilder<CommandSourceStack, Integer> priority = Commands.argument(PRIORITY_ARGUMENT, IntegerArgumentType.integer())
            .executes(context -> define(context, getInteger(context, PRIORITY_ARGUMENT)));
        RequiredArgumentBuilder<CommandSourceStack, Integer> selectionPriority = Commands.argument(PRIORITY_ARGUMENT, IntegerArgumentType.integer())
            .executes(context -> defineFromWorldEditSelection(context, getInteger(context, PRIORITY_ARGUMENT)));
        RequiredArgumentBuilder<CommandSourceStack, Integer> z2 = Commands.argument("z2", IntegerArgumentType.integer())
            .executes(context -> define(context, 0))
            .then(priority);
        RequiredArgumentBuilder<CommandSourceStack, Integer> y2 = Commands.argument("y2", IntegerArgumentType.integer()).then(z2);
        RequiredArgumentBuilder<CommandSourceStack, Integer> x2 = Commands.argument("x2", IntegerArgumentType.integer()).then(y2);
        RequiredArgumentBuilder<CommandSourceStack, Integer> z1 = Commands.argument("z1", IntegerArgumentType.integer()).then(x2);
        RequiredArgumentBuilder<CommandSourceStack, Integer> y1 = Commands.argument("y1", IntegerArgumentType.integer()).then(z1);
        RequiredArgumentBuilder<CommandSourceStack, Integer> x1 = Commands.argument("x1", IntegerArgumentType.integer()).then(y1);
        return regionArgument()
            .executes(context -> defineFromWorldEditSelection(context, 0))
            .then(Commands.literal("selection")
                .executes(context -> defineFromWorldEditSelection(context, 0))
                .then(selectionPriority))
            .then(x1);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> redefineArguments() {
        return regionArgument().executes(this::redefineFromWorldEditSelection);
    }

    private CompletableFuture<Suggestions> suggestRegions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(storage.regionIds(commandWorld(context)), builder);
    }

    private CompletableFuture<Suggestions> suggestFlags(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> flags = new java.util.ArrayList<>();
        WorldGuardFlag.ids().forEach(flags::add);
        WorldGuardValueFlag.ids().forEach(flags::add);
        flags.sort(String.CASE_INSENSITIVE_ORDER);
        return SharedSuggestionProvider.suggest(flags, builder);
    }

    private CompletableFuture<Suggestions> suggestStates(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("allow", "deny", "unset"), builder);
    }

    private CompletableFuture<Suggestions> suggestFlagValues(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String rawFlag = optionalStringArgument(context, FLAG_ARGUMENT).orElse("");
        if (WorldGuardFlag.parse(rawFlag).isPresent()) {
            return suggestStates(context, builder);
        }
        Optional<WorldGuardValueFlag> flag = WorldGuardValueFlag.parse(rawFlag);
        if (flag.isPresent() && flag.get().type() == WorldGuardFlagType.BOOLEAN) {
            return SharedSuggestionProvider.suggest(List.of("true", "false", "on", "off"), builder);
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestGroups(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(REGION_GROUP_IDS, builder);
    }

    private CompletableFuture<Suggestions> suggestBypassStates(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("on", "off"), builder);
    }

    private String checkRegionId(CommandContext<CommandSourceStack> context, String rawId, boolean allowGlobal) {
        if (!WorldGuardStorage.validRegionId(rawId)) {
            context.getSource().sendSystemMessage(error(WorldGuardText.invalidRegionId(rawId)));
            return "";
        }
        String id = WorldGuardStorage.normalizeId(rawId);
        if (!allowGlobal && id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            context.getSource().sendSystemMessage(error(WorldGuardText.globalNotAllowed()));
            return "";
        }
        return id;
    }

    private Optional<WorldGuardRegion> findExistingRegion(
        CommandContext<CommandSourceStack> context,
        String rawId,
        boolean allowGlobal
    ) {
        String id = checkRegionId(context, rawId, allowGlobal);
        if (id.isBlank()) {
            return Optional.empty();
        }
        String world = commandWorld(context);
        if (id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            return Optional.of(storage.findOrCreateGlobal(world));
        }
        Optional<WorldGuardRegion> region = storage.find(id, world);
        if (region.isEmpty()) {
            noRegion(context, id);
        }
        return region;
    }

    private String regionIdForAllowedGlobalCommand(CommandContext<CommandSourceStack> context) {
        return regionIdForAllowedGlobalCommand(context, getString(context, ID_ARGUMENT));
    }

    private String regionIdForAllowedGlobalCommand(CommandContext<CommandSourceStack> context, String rawId) {
        String id = checkRegionId(context, rawId, true);
        if (id.isBlank()) {
            return "";
        }
        String world = commandWorld(context);
        if (id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            storage.findOrCreateGlobal(world);
            return id;
        }
        if (storage.find(id, world).isEmpty()) {
            noRegion(context, id);
            return "";
        }
        return id;
    }

    private FlagTarget flagTarget(CommandContext<CommandSourceStack> context) {
        String rawFlag = getString(context, FLAG_ARGUMENT);
        Optional<WorldGuardFlag> stateFlag = WorldGuardFlag.parse(rawFlag);
        if (stateFlag.isPresent()) {
            return new FlagTarget(stateFlag.get(), null, stateFlag.get().id());
        }
        Optional<WorldGuardValueFlag> valueFlag = WorldGuardValueFlag.parse(rawFlag);
        if (valueFlag.isPresent()) {
            return new FlagTarget(null, valueFlag.get(), valueFlag.get().id());
        }
        sendUnknownFlag(context, rawFlag);
        return null;
    }

    private GroupTarget groupTarget(CommandContext<CommandSourceStack> context, FlagTarget flag) {
        Optional<String> rawGroup = optionalStringArgument(context, GROUP_ARGUMENT);
        if (rawGroup.isEmpty()) {
            return new GroupTarget(false, null, false);
        }
        Optional<WorldGuardRegionGroup> group = parseRegionGroup(rawGroup.get());
        if (group.isEmpty()) {
            context.getSource().sendSystemMessage(error(
                "Unknown value '" + rawGroup.get() + "' in com.sk89q.worldguard.protection.flags.RegionGroup"
            ));
            return new GroupTarget(true, null, true);
        }
        if (!flag.supportsRegionGroup()) {
            context.getSource().sendSystemMessage(error("Region flag '" + flag.id() + "' does not have a group flag!"));
            return new GroupTarget(true, null, true);
        }
        return new GroupTarget(true, group.get(), false);
    }

    private Optional<WorldGuardRegionGroup> parseRegionGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "members", "member" -> Optional.of(WorldGuardRegionGroup.MEMBERS);
            case "owners", "owner" -> Optional.of(WorldGuardRegionGroup.OWNERS);
            case "nonmembers", "nonmember", "non-members", "non-member" -> Optional.of(WorldGuardRegionGroup.NON_MEMBERS);
            case "nonowners", "nonowner", "non-owners", "non-owner" -> Optional.of(WorldGuardRegionGroup.NON_OWNERS);
            case "everyone", "anyone", "all" -> Optional.of(WorldGuardRegionGroup.ALL);
            case "none", "noone", "deny" -> Optional.of(WorldGuardRegionGroup.NONE);
            default -> WorldGuardRegionGroup.parse(raw);
        };
    }

    private Optional<String> optionalStringArgument(CommandContext<CommandSourceStack> context, String argumentName) {
        try {
            return Optional.of(getString(context, argumentName));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void noRegion(CommandContext<CommandSourceStack> context, String id) {
        context.getSource().sendSystemMessage(error(WorldGuardText.noRegion(id)));
    }

    private String commandWorld(CommandContext<CommandSourceStack> context) {
        try {
            return worldId(context.getSource().getPlayerOrException());
        } catch (CommandSyntaxException exception) {
            return WorldGuardRegion.ANY_WORLD;
        }
    }

    private ServerLevel level(CommandContext<CommandSourceStack> context, String world) {
        Identifier identifier = Identifier.tryParse(world);
        if (identifier == null) {
            return null;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, identifier);
        return context.getSource().getServer().getLevel(key);
    }

    private List<WorldGuardRegion> listRegions(String world) {
        List<WorldGuardRegion> regions = storage.regions(world);
        List<WorldGuardRegion> global = regions.stream().filter(WorldGuardRegion::global).toList();
        List<WorldGuardRegion> local = regions.stream().filter(region -> !region.global()).toList();
        return java.util.stream.Stream.concat(global.stream(), local.stream()).toList();
    }

    private ServerPlayer playerOrMessage(CommandContext<CommandSourceStack> context, String message) throws CommandSyntaxException {
        if (context.getSource().isPlayer()) {
            return context.getSource().getPlayerOrException();
        }
        context.getSource().sendSystemMessage(error(message));
        return null;
    }

    private void sendUnknownFlag(CommandContext<CommandSourceStack> context, String flag) {
        context.getSource().sendSystemMessage(error(WorldGuardText.unknownFlag(flag)));
        context.getSource().sendSystemMessage(error(WorldGuardText.availableFlags()));
    }

    private List<DomainArgument> domainArguments(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<DomainArgument> domains = new java.util.ArrayList<>();
        for (String raw : getString(context, DOMAIN_ARGUMENT).split("\\s+")) {
            if (!raw.isBlank()) {
                domains.add(domainArgument(context, raw));
            }
        }
        if (domains.isEmpty()) {
            throw PLAYER_NOT_FOUND.create();
        }
        return List.copyOf(domains);
    }

    private DomainArgument domainArgument(CommandContext<CommandSourceStack> context, String raw) throws CommandSyntaxException {
        String group = groupName(raw);
        if (!group.isBlank()) {
            return new DomainArgument(null, group);
        }
        String uuid = uuidName(raw);
        if (!uuid.isBlank()) {
            try {
                return new DomainArgument(UUID.fromString(uuid), "");
            } catch (IllegalArgumentException exception) {
                throw PLAYER_NOT_FOUND.create();
            }
        }
        ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(raw);
        if (player == null) {
            throw PLAYER_NOT_FOUND.create();
        }
        return new DomainArgument(player.getUUID(), "");
    }

    private static String groupName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.regionMatches(true, 0, "g:", 0, 2)) {
            trimmed = trimmed.substring(2).trim();
        } else if (trimmed.regionMatches(true, 0, "group:", 0, 6)) {
            trimmed = trimmed.substring(6).trim();
        } else {
            return "";
        }
        return trimmed.isBlank() ? "" : com.vantablack4.worldguard.model.RegionDomain.normalizeGroup(trimmed);
    }

    private static String uuidName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.regionMatches(true, 0, "uuid:", 0, 5)) {
            trimmed = trimmed.substring(5).trim();
        }
        return trimmed.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            ? trimmed
            : "";
    }

    private static String flagsDisplay(WorldGuardRegion region) {
        StringJoiner flags = new StringJoiner(", ");
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            FlagState state = region.flag(flag);
            if (state != FlagState.UNSET) {
                flags.add(flag.id() + ": " + state.id());
            }
        }
        String display = flags.toString();
        return display.isBlank() ? "(none)" : display;
    }

    private static String worldId(ServerPlayer player) {
        return player.level().dimension().identifier().toString();
    }

    private static double center(int min, int max) {
        return ((double) min + (double) max) / 2.0D + 0.5D;
    }

    private Optional<LocationValue> teleportLocation(
        CommandContext<CommandSourceStack> context,
        ServerPlayer player,
        WorldGuardRegion region,
        TeleportMode mode
    ) {
        if (mode == TeleportMode.CENTER) {
            if (!region.type().physicalArea()) {
                context.getSource().sendSystemMessage(error(WorldGuardText.noCenterPoint()));
                return Optional.empty();
            }
            if (player.gameMode() != GameType.SPECTATOR) {
                context.getSource().sendSystemMessage(error(WorldGuardText.centerTeleportSpectatorOnly()));
                return Optional.empty();
            }
            return Optional.of(new LocationValue(
                region.world(),
                center(region.minX(), region.maxX()),
                center(region.minY(), region.maxY()),
                center(region.minZ(), region.maxZ()),
                0F,
                0F
            ));
        }

        WorldGuardValueFlag flag = mode == TeleportMode.SPAWN
            ? WorldGuardValueFlag.SPAWN
            : WorldGuardValueFlag.TELEPORT;
        List<WorldGuardRegion> regions = storage.regions(region.world());
        Set<String> groups = WorldGuardPermissions.regionGroups(player.createCommandSourceStack(), regions);
        Optional<LocationValue> location = RegionQueryEngine.queryRegionValue(
            regions,
            region,
            flag,
            player.getUUID(),
            groups
        ).value().flatMap(WorldGuardFlagValue::asLocation);
        if (location.isPresent()) {
            return location;
        }

        context.getSource().sendSystemMessage(error(
            mode == TeleportMode.SPAWN ? WorldGuardText.noSpawnPoint() : WorldGuardText.noTeleportPoint()
        ));
        return Optional.empty();
    }

    private Optional<String> teleportMessage(ServerPlayer player, WorldGuardRegion region) {
        List<WorldGuardRegion> regions = storage.regions(region.world());
        Set<String> groups = WorldGuardPermissions.regionGroups(player.createCommandSourceStack(), regions);
        String message = RegionQueryEngine.queryRegionValue(
            regions,
            region,
            WorldGuardValueFlag.TELEPORT_MESSAGE,
            player.getUUID(),
            groups
        ).value()
            .or(() -> WorldGuardValueFlag.TELEPORT_MESSAGE.defaultValue())
            .map(WorldGuardFlagValue::serialized)
            .orElseGet(() -> WorldGuardText.teleportedToRegion(region.id()));
        message = message.replace("%id%", region.id());
        return message.isEmpty() ? Optional.empty() : Optional.of(message);
    }

    static WorldGuardRegion copyWithPriority(WorldGuardRegion region, int priority) {
        return new WorldGuardRegion(
            region.id(),
            region.world(),
            region.minX(),
            region.minY(),
            region.minZ(),
            region.maxX(),
            region.maxY(),
            region.maxZ(),
            priority,
            region.parentId(),
            region.type(),
            region.owners(),
            region.members(),
            region.ownerGroups(),
            region.memberGroups(),
            region.flags(),
            region.valueFlags(),
            region.flagGroups(),
            region.polygonPoints()
        );
    }

    static WorldGuardRegion withShape(WorldGuardRegion existing, WorldEditRegionSelection selection) {
        return new WorldGuardRegion(
            existing.id(),
            selection.world(),
            selection.minX(),
            selection.minY(),
            selection.minZ(),
            selection.maxX(),
            selection.maxY(),
            selection.maxZ(),
            existing.priority(),
            existing.parentId(),
            selection.type(),
            existing.owners(),
            existing.members(),
            existing.ownerGroups(),
            existing.memberGroups(),
            existing.flags(),
            existing.valueFlags(),
            existing.flagGroups(),
            selection.polygonPoints()
        );
    }

    private static Component header(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GOLD);
    }

    private static Component line(String label, String value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static Component success(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    private static Component error(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }

    private record DomainArgument(UUID playerUuid, String group) {
    }

    private record FlagTarget(WorldGuardFlag stateFlag, WorldGuardValueFlag valueFlag, String id) {
        boolean supportsRegionGroup() {
            return stateFlag != null ? stateFlag.supportsRegionGroup() : valueFlag.supportsRegionGroup();
        }

        WorldGuardRegionGroup defaultGroup() {
            return stateFlag != null ? stateFlag.defaultGroup() : valueFlag.defaultGroup();
        }
    }

    private record GroupTarget(boolean present, WorldGuardRegionGroup group, boolean invalid) {
    }

    private enum TeleportMode {
        FLAG,
        SPAWN,
        CENTER
    }
}
