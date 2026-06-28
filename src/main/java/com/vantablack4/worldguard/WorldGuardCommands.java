package com.vantablack4.worldguard;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public final class WorldGuardCommands {
    private static final String ID_ARGUMENT = "region";
    private static final String FLAG_ARGUMENT = "flag";
    private static final String STATE_ARGUMENT = "state";
    private static final String PLAYER_ARGUMENT = "player";

    private final WorldGuardConfig config;
    private final WorldGuardStorage storage;

    public WorldGuardCommands(WorldGuardConfig config, WorldGuardStorage storage) {
        this.config = config;
        this.storage = storage;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("wg"));
        dispatcher.register(root("worldguard"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
            .executes(this::status)
            .then(Commands.literal("help").executes(this::help))
            .then(Commands.literal("status").executes(this::status))
            .then(Commands.literal("list").executes(this::list))
            .then(Commands.literal("here").executes(this::here))
            .then(Commands.literal("info")
                .then(regionArgument().executes(this::info)))
            .then(Commands.literal("define")
                .requires(this::isAdmin)
                .then(defineArguments()))
            .then(Commands.literal("delete")
                .requires(this::isAdmin)
                .then(regionArgument().executes(this::delete)))
            .then(Commands.literal("flag")
                .requires(this::isAdmin)
                .then(flagArguments()))
            .then(Commands.literal("member")
                .requires(this::isAdmin)
                .then(Commands.literal("add")
                    .then(regionArgument()
                        .then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                            .executes(this::addMember))))
                .then(Commands.literal("remove")
                    .then(regionArgument()
                        .then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                            .executes(this::removeMember)))));
    }

    private int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSystemMessage(header("WorldGuard"));
        source.sendSystemMessage(line("Read", "/wg list | /wg here | /wg info <region>"));
        source.sendSystemMessage(line("Define", "/wg define <region> <x1> <y1> <z1> <x2> <y2> <z2> [priority]"));
        source.sendSystemMessage(line("Flags", "/wg flag <region> <flag> <allow|deny|unset>"));
        source.sendSystemMessage(line("Members", "/wg member add|remove <region> <player>"));
        return 1;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(header("WorldGuard status"));
        context.getSource().sendSystemMessage(line("Regions", Integer.toString(storage.regionIds().size())));
        context.getSource().sendSystemMessage(line("Admin level", Integer.toString(config.adminPermissionLevel())));
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
                context.getSource().sendSystemMessage(line("Priority", Integer.toString(region.priority())));
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

    private int delete(CommandContext<CommandSourceStack> context) {
        String id = getString(context, ID_ARGUMENT);
        if (storage.delete(id)) {
            context.getSource().sendSystemMessage(success("Deleted region " + WorldGuardStorage.normalizeId(id) + "."));
            return 1;
        }
        context.getSource().sendSystemMessage(error("Unknown region."));
        return 0;
    }

    private int setFlag(CommandContext<CommandSourceStack> context) {
        String id = getString(context, ID_ARGUMENT);
        WorldGuardFlag flag = WorldGuardFlag.parse(getString(context, FLAG_ARGUMENT)).orElse(null);
        FlagState state = FlagState.parse(getString(context, STATE_ARGUMENT)).orElse(null);
        if (flag == null || state == null) {
            context.getSource().sendSystemMessage(error("Invalid flag or state."));
            return 0;
        }
        return storage.setFlag(id, flag, state)
            .map(region -> {
                context.getSource().sendSystemMessage(success("Updated " + region.id() + " " + flag.id() + "=" + state.id() + "."));
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
        return source.permissions().hasPermission(
            new Permission.HasCommandLevel(PermissionLevel.byId(config.adminPermissionLevel()))
        );
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> regionArgument() {
        return Commands.argument(ID_ARGUMENT, StringArgumentType.word()).suggests(this::suggestRegions);
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> flagArguments() {
        return regionArgument()
            .then(Commands.argument(FLAG_ARGUMENT, StringArgumentType.word())
                .suggests(this::suggestFlags)
                .then(Commands.argument(STATE_ARGUMENT, StringArgumentType.word())
                    .suggests(this::suggestStates)
                    .executes(this::setFlag)));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> defineArguments() {
        RequiredArgumentBuilder<CommandSourceStack, Integer> priority = Commands.argument("priority", IntegerArgumentType.integer())
            .executes(context -> define(context, getInteger(context, "priority")));
        RequiredArgumentBuilder<CommandSourceStack, Integer> z2 = Commands.argument("z2", IntegerArgumentType.integer())
            .executes(context -> define(context, 0))
            .then(priority);
        RequiredArgumentBuilder<CommandSourceStack, Integer> y2 = Commands.argument("y2", IntegerArgumentType.integer()).then(z2);
        RequiredArgumentBuilder<CommandSourceStack, Integer> x2 = Commands.argument("x2", IntegerArgumentType.integer()).then(y2);
        RequiredArgumentBuilder<CommandSourceStack, Integer> z1 = Commands.argument("z1", IntegerArgumentType.integer()).then(x2);
        RequiredArgumentBuilder<CommandSourceStack, Integer> y1 = Commands.argument("y1", IntegerArgumentType.integer()).then(z1);
        RequiredArgumentBuilder<CommandSourceStack, Integer> x1 = Commands.argument("x1", IntegerArgumentType.integer()).then(y1);
        return regionArgument().then(x1);
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
