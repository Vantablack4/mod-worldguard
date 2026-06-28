package com.vantablack4.worldguard;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import net.minecraft.server.level.ServerPlayer;

import com.vantablack4.worldguard.worldedit.WorldEditRegionSelection;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionResult;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionSource;

public final class WorldGuardCommands {
    private static final String ID_ARGUMENT = "region";
    private static final String PARENT_ARGUMENT = "parent";
    private static final String FLAG_ARGUMENT = "flag";
    private static final String STATE_ARGUMENT = "state";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String PRIORITY_ARGUMENT = "priority";

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
                .requires(this::isAdmin)
                .executes(this::saveRegions))
            .then(Commands.literal("write")
                .requires(this::isAdmin)
                .executes(this::saveRegions))
            .then(Commands.literal("load")
                .requires(this::isAdmin)
                .executes(this::loadRegions))
            .then(Commands.literal("reload")
                .requires(this::isAdmin)
                .executes(this::loadRegions))
            .then(Commands.literal("list").executes(this::list))
            .then(Commands.literal("info")
                .executes(this::infoHere)
                .then(regionArgument().executes(this::info)))
            .then(Commands.literal("i")
                .executes(this::infoHere)
                .then(regionArgument().executes(this::info)))
            .then(Commands.literal("define")
                .requires(this::isAdmin)
                .then(defineArguments()))
            .then(Commands.literal("def")
                .requires(this::isAdmin)
                .then(defineArguments()))
            .then(Commands.literal("d")
                .requires(this::isAdmin)
                .then(defineArguments()))
            .then(Commands.literal("create")
                .requires(this::isAdmin)
                .then(defineArguments()))
            .then(Commands.literal("redefine")
                .requires(this::isAdmin)
                .then(redefineArguments()))
            .then(Commands.literal("update")
                .requires(this::isAdmin)
                .then(redefineArguments()))
            .then(Commands.literal("move")
                .requires(this::isAdmin)
                .then(redefineArguments()))
            .then(Commands.literal("delete")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("del")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("remove")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("rem")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("flags")
                .executes(this::flagsHere)
                .then(regionArgument().executes(this::regionFlags)))
            .then(Commands.literal("flag")
                .requires(this::isAdmin)
                .then(flagArguments()))
            .then(Commands.literal("f")
                .requires(this::isAdmin)
                .then(flagArguments()))
            .then(Commands.literal("setpriority")
                .requires(this::isAdmin)
                .then(setPriorityArguments()))
            .then(Commands.literal("priority")
                .requires(this::isAdmin)
                .then(setPriorityArguments()))
            .then(Commands.literal("pri")
                .requires(this::isAdmin)
                .then(setPriorityArguments()))
            .then(Commands.literal("setparent")
                .requires(this::isAdmin)
                .then(setParentArguments()))
            .then(Commands.literal("parent")
                .requires(this::isAdmin)
                .then(setParentArguments()))
            .then(Commands.literal("par")
                .requires(this::isAdmin)
                .then(setParentArguments()))
            .then(Commands.literal("addmem")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::addMember)))
            .then(Commands.literal("am")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::addMember)))
            .then(Commands.literal("owner")
                .requires(this::isAdmin)
                .then(Commands.literal("add")
                    .then(playerRegionArguments(this::addOwner)))
                .then(Commands.literal("remove")
                    .then(playerRegionArguments(this::removeOwner))))
            .then(Commands.literal("member")
                .requires(this::isAdmin)
                .then(Commands.literal("add")
                    .then(playerRegionArguments(this::addMember)))
                .then(Commands.literal("remove")
                    .then(playerRegionArguments(this::removeMember))))
            .then(Commands.literal("addowner")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::addOwner)))
            .then(Commands.literal("ao")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::addOwner)))
            .then(Commands.literal("removeowner")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeOwner)))
            .then(Commands.literal("remowner")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeOwner)))
            .then(Commands.literal("ro")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeOwner)))
            .then(Commands.literal("addmember")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::addMember)))
            .then(Commands.literal("removemember")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)))
            .then(Commands.literal("removemem")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)))
            .then(Commands.literal("remmember")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)))
            .then(Commands.literal("remmem")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)))
            .then(Commands.literal("rm")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)));
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(header("WorldGuard"));
        source.sendSystemMessage(line("Region root", "/region, /regions, /rg"));
        source.sendSystemMessage(line("Read", "/rg list | /rg info <region> | /rg flags <region>"));
        source.sendSystemMessage(line("Define", "/rg define <region> | /rg redefine <region>"));
        source.sendSystemMessage(line("WorldEdit", worldEditSelectionSource.description()));
        source.sendSystemMessage(line("Global", "/rg flag __global__ <flag> <allow|deny>"));
        source.sendSystemMessage(line("Flags", "/rg flag <region> <flag> [allow|deny]"));
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
        return setRegionFlag(context, getString(context, ID_ARGUMENT));
    }

    private int setRegionFlag(CommandContext<CommandSourceStack> context, String rawId) {
        WorldGuardFlag flag = WorldGuardFlag.parse(getString(context, FLAG_ARGUMENT)).orElse(null);
        if (flag == null) {
            sendUnknownFlag(context, getString(context, FLAG_ARGUMENT));
            return 0;
        }
        String id = checkRegionId(context, rawId, true);
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
        FlagState state = FlagState.parse(getString(context, STATE_ARGUMENT)).orElse(null);
        if (state == null) {
            context.getSource().sendSystemMessage(error(WorldGuardText.invalidStateFlag(getString(context, STATE_ARGUMENT))));
            return 0;
        }
        return storage.setFlag(id, world, flag, state)
            .map(region -> {
                if (state == FlagState.UNSET) {
                    context.getSource().sendSystemMessage(success(WorldGuardText.flagRemoved(flag.id(), region.id())));
                } else {
                    context.getSource().sendSystemMessage(success(WorldGuardText.flagSet(flag.id(), region.id(), state.id())));
                }
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private int clearRegionFlag(CommandContext<CommandSourceStack> context) {
        WorldGuardFlag flag = WorldGuardFlag.parse(getString(context, FLAG_ARGUMENT)).orElse(null);
        if (flag == null) {
            sendUnknownFlag(context, getString(context, FLAG_ARGUMENT));
            return 0;
        }
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
        return storage.setFlag(id, world, flag, FlagState.UNSET)
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.flagRemoved(flag.id(), region.id())));
                return 1;
            })
            .orElse(0);
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
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.addOwner(id, world, player.getUUID())
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
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.removeOwner(id, world, player.getUUID())
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
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.addMember(id, world, player.getUUID())
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
        String id = regionIdForAllowedGlobalCommand(context);
        if (id.isBlank()) {
            return 0;
        }
        String world = commandWorld(context);
        return storage.removeMember(id, world, player.getUUID())
            .map(region -> {
                context.getSource().sendSystemMessage(success(WorldGuardText.membersRemoved(region.id())));
                return 1;
            })
            .orElseGet(() -> {
                noRegion(context, id);
                return 0;
            });
    }

    private boolean isAdmin(CommandSourceStack source) {
        return WorldGuardPermissions.admin(source, config);
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

    private RequiredArgumentBuilder<CommandSourceStack, String> playerRegionArguments(Command<CommandSourceStack> command) {
        return regionArgument()
            .then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                .executes(command));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> flagArguments() {
        return regionArgument()
            .then(Commands.argument(FLAG_ARGUMENT, StringArgumentType.word())
                .suggests(this::suggestFlags)
                .executes(this::clearRegionFlag)
                .then(Commands.argument(STATE_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestStates)
                    .executes(this::setFlag)));
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
        return SharedSuggestionProvider.suggest(WorldGuardFlag.ids(), builder);
    }

    private CompletableFuture<Suggestions> suggestStates(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("allow", "deny", "unset"), builder);
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
        String id = checkRegionId(context, getString(context, ID_ARGUMENT), true);
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

    private static WorldGuardRegion copyWithPriority(WorldGuardRegion region, int priority) {
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
            region.polygonPoints()
        );
    }

    private static WorldGuardRegion withShape(WorldGuardRegion existing, WorldEditRegionSelection selection) {
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
}
