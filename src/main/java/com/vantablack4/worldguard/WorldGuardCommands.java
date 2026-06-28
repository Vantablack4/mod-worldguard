package com.vantablack4.worldguard;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.util.List;
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
    private static final String WORLD_ARGUMENT = "world";

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

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("wg"));
        dispatcher.register(root("worldguard"));
        dispatcher.register(root("region"));
        dispatcher.register(root("rg"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
            .executes(this::status)
            .then(Commands.literal("help").executes(this::help))
            .then(Commands.literal("status").executes(this::status))
            .then(Commands.literal("save")
                .requires(this::isAdmin)
                .executes(this::save))
            .then(Commands.literal("list").executes(this::list))
            .then(Commands.literal("here").executes(this::here))
            .then(Commands.literal("info")
                .then(regionArgument().executes(this::info)))
            .then(Commands.literal("global")
                .requires(this::isAdmin)
                .executes(context -> ensureGlobalRegion(context, WorldGuardRegion.ANY_WORLD))
                .then(Commands.literal("create")
                    .executes(context -> ensureGlobalRegion(context, WorldGuardRegion.ANY_WORLD))
                    .then(worldArgument()
                        .executes(context -> ensureGlobalRegion(context, getString(context, WORLD_ARGUMENT)))))
                .then(Commands.literal("flag")
                    .then(globalFlagArguments()))
                .then(worldArgument()
                    .executes(context -> ensureGlobalRegion(context, getString(context, WORLD_ARGUMENT)))))
            .then(Commands.literal("define")
                .requires(this::isAdmin)
                .then(defineArguments()))
            .then(Commands.literal("delete")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("remove")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("flags")
                .executes(this::flags))
            .then(Commands.literal("flag")
                .requires(this::isAdmin)
                .then(flagArguments()))
            .then(Commands.literal("setpriority")
                .requires(this::isAdmin)
                .then(setPriorityArguments()))
            .then(Commands.literal("setparent")
                .requires(this::isAdmin)
                .then(setParentArguments()))
            .then(Commands.literal("parent")
                .requires(this::isAdmin)
                .then(Commands.literal("set")
                    .then(regionArgument()
                        .then(parentArgument()
                            .executes(this::setParent))))
                .then(Commands.literal("clear")
                    .then(regionArgument()
                        .executes(this::clearParent)))
                .then(regionArgument()
                    .then(parentArgument()
                        .executes(this::setParent))))
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
            .then(Commands.literal("removeowner")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeOwner)))
            .then(Commands.literal("remowner")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeOwner)))
            .then(Commands.literal("addmember")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::addMember)))
            .then(Commands.literal("removemember")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)))
            .then(Commands.literal("remmember")
                .requires(this::isAdmin)
                .then(playerRegionArguments(this::removeMember)));
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(header("WorldGuard"));
        source.sendSystemMessage(line("Read", "/wg list | /wg here | /wg info <region>"));
        source.sendSystemMessage(line("Define", "/wg define <region> | /wg define <region> <x1> <y1> <z1> <x2> <y2> <z2> [priority]"));
        source.sendSystemMessage(line("WorldEdit", worldEditSelectionSource.description()));
        source.sendSystemMessage(line("Global", "/wg global [world] | /wg global flag <flag> [allow|deny|unset]"));
        source.sendSystemMessage(line("Flags", "/wg flags | /wg flag <region> <flag> [allow|deny|unset]"));
        source.sendSystemMessage(line("Regions", "/rg setpriority <region> <priority> | /rg setparent <region> [parent]"));
        source.sendSystemMessage(line("Owners", "/wg owner add|remove <region> <player> | /rg addowner <region> <player>"));
        source.sendSystemMessage(line("Members", "/wg member add|remove <region> <player> | /rg addmember <region> <player>"));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(header("WorldGuard status"));
        context.getSource().sendSystemMessage(line("Regions", Integer.toString(storage.regionIds().size())));
        context.getSource().sendSystemMessage(line(
            "Global region",
            storage.find(WorldGuardRegion.GLOBAL_REGION_ID)
                .map(WorldGuardRegion::world)
                .orElse("not defined")
        ));
        context.getSource().sendSystemMessage(line("Admin level", Integer.toString(config.adminPermissionLevel())));
        context.getSource().sendSystemMessage(line("WorldEdit", worldEditSelectionSource.description()));
        return 1;
    }

    private int save(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(header("WorldGuard save"));
        context.getSource().sendSystemMessage(line("Persistence", "changes are saved immediately"));
        context.getSource().sendSystemMessage(line("Regions", Integer.toString(storage.regionIds().size())));
        return 1;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        List<WorldGuardRegion> regions = storage.regions();
        context.getSource().sendSystemMessage(header("WorldGuard regions"));
        if (regions.isEmpty()) {
            context.getSource().sendSystemMessage(error("No regions are defined."));
            return 0;
        }
        for (WorldGuardRegion region : regions) {
            context.getSource().sendSystemMessage(line(
                region.id(),
                region.world() + " priority=" + region.priority() + " members=" + region.members().size()
            ));
        }
        return regions.size();
    }

    private int here(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<WorldGuardRegion> regions = storage.regionsAt(
            worldId(player),
            player.blockPosition().getX(),
            player.blockPosition().getY(),
            player.blockPosition().getZ()
        );
        context.getSource().sendSystemMessage(header("WorldGuard here"));
        if (regions.isEmpty()) {
            context.getSource().sendSystemMessage(error("No regions at your current position."));
            return 0;
        }
        for (WorldGuardRegion region : regions) {
            context.getSource().sendSystemMessage(line(region.id(), "priority=" + region.priority()));
        }
        return regions.size();
    }

    private int info(CommandContext<CommandSourceStack> context) {
        return storage.find(getString(context, ID_ARGUMENT))
            .map(region -> {
                context.getSource().sendSystemMessage(header(region.id()));
                context.getSource().sendSystemMessage(line("Bounds", region.boundsDisplay()));
                context.getSource().sendSystemMessage(line("Type", region.type().id()));
                context.getSource().sendSystemMessage(line("Priority", Integer.toString(region.priority())));
                context.getSource().sendSystemMessage(line("Parent", region.parentId().isBlank() ? "none" : region.parentId()));
                context.getSource().sendSystemMessage(line("Owners", Integer.toString(region.owners().size())));
                context.getSource().sendSystemMessage(line("Members", Integer.toString(region.members().size())));
                for (WorldGuardFlag flag : WorldGuardFlag.values()) {
                    FlagState state = region.flag(flag);
                    if (state != FlagState.UNSET) {
                        context.getSource().sendSystemMessage(line(flag.id(), state.id()));
                    }
                }
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int define(CommandContext<CommandSourceStack> context, int priority) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = WorldGuardStorage.normalizeId(getString(context, ID_ARGUMENT));
        if (id.isBlank()) {
            context.getSource().sendSystemMessage(error("Invalid region id."));
            return 0;
        }
        if (id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            context.getSource().sendSystemMessage(error("Global regions are not cuboids. Use /wg global [world]."));
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
        context.getSource().sendSystemMessage(success("Defined protected region " + id + "."));
        return 1;
    }

    private int defineFromWorldEditSelection(CommandContext<CommandSourceStack> context, int priority) throws CommandSyntaxException {
        String id = WorldGuardStorage.normalizeId(getString(context, ID_ARGUMENT));
        if (id.isBlank()) {
            context.getSource().sendSystemMessage(error("Invalid region id."));
            return 0;
        }
        if (id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            return ensureGlobalRegion(context, WorldGuardRegion.ANY_WORLD);
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
        context.getSource().sendSystemMessage(success("Defined protected region " + id + " from WorldEdit selection."));
        context.getSource().sendSystemMessage(line("Bounds", region.boundsDisplay()));
        context.getSource().sendSystemMessage(line("Volume", Long.toString(selection.volume())));
        return 1;
    }

    private int delete(CommandContext<CommandSourceStack> context) {
        String id = getString(context, ID_ARGUMENT);
        if (storage.delete(id)) {
            context.getSource().sendSystemMessage(success("Deleted region " + WorldGuardStorage.normalizeId(id) + "."));
            return 1;
        }
        context.getSource().sendSystemMessage(error("Unknown region."));
        return 0;
    }

    private int flags(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(header("WorldGuard flags"));
        for (WorldGuardFlag flag : WorldGuardFlag.values()) {
            context.getSource().sendSystemMessage(line(flag.id(), "allow | deny | unset"));
        }
        return WorldGuardFlag.values().length;
    }

    private int showFlag(CommandContext<CommandSourceStack> context) {
        return showRegionFlag(context, getString(context, ID_ARGUMENT));
    }

    private int showGlobalFlag(CommandContext<CommandSourceStack> context) {
        return showRegionFlag(context, WorldGuardRegion.GLOBAL_REGION_ID);
    }

    private int showRegionFlag(CommandContext<CommandSourceStack> context, String rawId) {
        WorldGuardFlag flag = WorldGuardFlag.parse(getString(context, FLAG_ARGUMENT)).orElse(null);
        if (flag == null) {
            context.getSource().sendSystemMessage(error("Invalid flag."));
            return 0;
        }
        return storage.find(rawId)
            .map(region -> {
                context.getSource().sendSystemMessage(line(region.id() + " " + flag.id(), region.flag(flag).id()));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int setFlag(CommandContext<CommandSourceStack> context) {
        return setRegionFlag(context, getString(context, ID_ARGUMENT));
    }

    private int setGlobalFlag(CommandContext<CommandSourceStack> context) {
        return setRegionFlag(context, WorldGuardRegion.GLOBAL_REGION_ID);
    }

    private int setRegionFlag(CommandContext<CommandSourceStack> context, String rawId) {
        WorldGuardFlag flag = WorldGuardFlag.parse(getString(context, FLAG_ARGUMENT)).orElse(null);
        FlagState state = FlagState.parse(getString(context, STATE_ARGUMENT)).orElse(null);
        if (flag == null || state == null) {
            context.getSource().sendSystemMessage(error("Invalid flag or state."));
            return 0;
        }
        return storage.setFlag(rawId, flag, state)
            .map(region -> {
                context.getSource().sendSystemMessage(success("Updated " + region.id() + " " + flag.id() + "=" + state.id() + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int setPriority(CommandContext<CommandSourceStack> context) {
        String id = getString(context, ID_ARGUMENT);
        int priority = getInteger(context, PRIORITY_ARGUMENT);
        return storage.find(id)
            .map(region -> {
                if (region.global()) {
                    context.getSource().sendSystemMessage(error("Global region priority is fixed."));
                    return 0;
                }
                WorldGuardRegion updated = copyWithPriority(region, priority);
                storage.save(updated);
                context.getSource().sendSystemMessage(success("Set " + updated.id() + " priority to " + priority + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int setParent(CommandContext<CommandSourceStack> context) {
        String id = getString(context, ID_ARGUMENT);
        String parentId = getString(context, PARENT_ARGUMENT);
        if (storage.find(id).isEmpty()) {
            context.getSource().sendSystemMessage(error("Unknown region."));
            return 0;
        }
        if (storage.find(parentId).isEmpty()) {
            context.getSource().sendSystemMessage(error("Unknown parent region."));
            return 0;
        }
        try {
            return storage.setParent(id, parentId)
                .map(region -> {
                    context.getSource().sendSystemMessage(success("Set " + region.id() + " parent to " + region.parentId() + "."));
                    return 1;
                })
                .orElseGet(() -> {
                    context.getSource().sendSystemMessage(error("Unknown region."));
                    return 0;
                });
        } catch (IllegalArgumentException exception) {
            context.getSource().sendSystemMessage(error(exception.getMessage()));
            return 0;
        }
    }

    private int clearParent(CommandContext<CommandSourceStack> context) {
        String id = getString(context, ID_ARGUMENT);
        return storage.setParent(id, "")
            .map(region -> {
                context.getSource().sendSystemMessage(success("Cleared parent for " + region.id() + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int ensureGlobalRegion(CommandContext<CommandSourceStack> context, String rawWorld) {
        String world = normalizeWorld(rawWorld);
        WorldGuardRegion region = storage.find(WorldGuardRegion.GLOBAL_REGION_ID)
            .map(existing -> copyWithWorld(existing, world))
            .orElseGet(() -> WorldGuardRegion.global(world));
        storage.save(region);
        context.getSource().sendSystemMessage(success("Global region is ready for " + region.world() + "."));
        context.getSource().sendSystemMessage(line("Edit flags", "/wg flag __global__ <flag> <allow|deny|unset>"));
        return 1;
    }

    private int defineGlobalFromRegionArgument(CommandContext<CommandSourceStack> context, String rawWorld) {
        String id = WorldGuardStorage.normalizeId(getString(context, ID_ARGUMENT));
        if (!id.equals(WorldGuardRegion.GLOBAL_REGION_ID)) {
            context.getSource().sendSystemMessage(error("Only __global__ can use the world-only define form."));
            return 0;
        }
        return ensureGlobalRegion(context, rawWorld);
    }

    private int addOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return storage.addOwner(getString(context, ID_ARGUMENT), player.getUUID())
            .map(region -> {
                context.getSource().sendSystemMessage(success("Added " + player.getName().getString() + " as owner of " + region.id() + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int removeOwner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return storage.removeOwner(getString(context, ID_ARGUMENT), player.getUUID())
            .map(region -> {
                context.getSource().sendSystemMessage(success("Removed " + player.getName().getString() + " as owner of " + region.id() + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int addMember(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return storage.addMember(getString(context, ID_ARGUMENT), player.getUUID())
            .map(region -> {
                context.getSource().sendSystemMessage(success("Added " + player.getName().getString() + " to " + region.id() + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
                return 0;
            });
    }

    private int removeMember(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);
        return storage.removeMember(getString(context, ID_ARGUMENT), player.getUUID())
            .map(region -> {
                context.getSource().sendSystemMessage(success("Removed " + player.getName().getString() + " from " + region.id() + "."));
                return 1;
            })
            .orElseGet(() -> {
                context.getSource().sendSystemMessage(error("Unknown region."));
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

    private RequiredArgumentBuilder<CommandSourceStack, String> worldArgument() {
        return Commands.argument(WORLD_ARGUMENT, StringArgumentType.word());
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
                .executes(this::showFlag)
                .then(Commands.argument(STATE_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestStates)
                    .executes(this::setFlag)));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> globalFlagArguments() {
        return Commands.argument(FLAG_ARGUMENT, StringArgumentType.word())
            .suggests(this::suggestFlags)
            .executes(this::showGlobalFlag)
            .then(Commands.argument(STATE_ARGUMENT, StringArgumentType.word())
                .suggests(this::suggestStates)
                .executes(this::setGlobalFlag));
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
            .then(worldArgument()
                .executes(context -> defineGlobalFromRegionArgument(context, getString(context, WORLD_ARGUMENT))))
            .then(x1);
    }

    private CompletableFuture<Suggestions> suggestRegions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(storage.regionIds(), builder);
    }

    private CompletableFuture<Suggestions> suggestFlags(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(WorldGuardFlag.ids(), builder);
    }

    private CompletableFuture<Suggestions> suggestStates(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(List.of("allow", "deny", "unset"), builder);
    }

    private static String worldId(ServerPlayer player) {
        return player.level().dimension().identifier().toString();
    }

    private static String normalizeWorld(String rawWorld) {
        if (rawWorld == null || rawWorld.isBlank()) {
            return WorldGuardRegion.ANY_WORLD;
        }
        return rawWorld.trim();
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
            region.flags()
        );
    }

    private static WorldGuardRegion copyWithWorld(WorldGuardRegion region, String world) {
        return new WorldGuardRegion(
            region.id(),
            world,
            region.minX(),
            region.minY(),
            region.minZ(),
            region.maxX(),
            region.maxY(),
            region.maxZ(),
            region.priority(),
            region.parentId(),
            region.type(),
            region.owners(),
            region.members(),
            region.ownerGroups(),
            region.memberGroups(),
            region.flags()
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
