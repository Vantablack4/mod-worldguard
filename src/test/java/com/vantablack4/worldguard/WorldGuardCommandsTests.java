package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import com.vantablack4.worldguard.worldedit.WorldEditSelectionResult;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionSource;

import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;
import java.nio.file.Path;

final class WorldGuardCommandsTests {
    @TempDir
    Path tempDir;

    @Test
    void registersUpstreamStyleWorldGuardAndRegionRoots() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        WorldGuardCommands commands = new WorldGuardCommands(
            new WorldGuardConfig(tempDir, 2, 1000),
            storage,
            unavailableWorldEdit()
        );

        commands.register(dispatcher);

        assertThat(root(dispatcher, "wg").getChild("version")).isNotNull();
        assertThat(root(dispatcher, "wg").getChild("reload")).isNotNull();
        assertThat(root(dispatcher, "wg").getChild("define")).isNull();
        assertThat(root(dispatcher, "worldguard").getChild("version")).isNotNull();

        for (String rootName : List.of("region", "regions", "rg")) {
            CommandNode<CommandSourceStack> root = root(dispatcher, rootName);
            assertThat(root.getChild("define")).isNotNull();
            assertThat(root.getChild("def")).isNotNull();
            assertThat(root.getChild("d")).isNotNull();
            assertThat(root.getChild("create")).isNotNull();
            assertThat(root.getChild("claim")).isNotNull();
            assertThat(root.getChild("redefine")).isNotNull();
            assertThat(root.getChild("update")).isNotNull();
            assertThat(root.getChild("move")).isNotNull();
            assertThat(root.getChild("teleport")).isNotNull();
            assertThat(root.getChild("tp")).isNotNull();
            assertThat(root.getChild("flag")).isNotNull();
            assertThat(root.getChild("f")).isNotNull();
            assertThat(root.getChild("setpriority")).isNotNull();
            assertThat(root.getChild("priority")).isNotNull();
            assertThat(root.getChild("pri")).isNotNull();
            assertThat(root.getChild("setparent")).isNotNull();
            assertThat(root.getChild("parent")).isNotNull();
            assertThat(root.getChild("par")).isNotNull();
            assertThat(root.getChild("addmember")).isNotNull();
            assertThat(root.getChild("addmem")).isNotNull();
            assertThat(root.getChild("am")).isNotNull();
            assertThat(root.getChild("addowner")).isNotNull();
            assertThat(root.getChild("ao")).isNotNull();
            assertThat(root.getChild("removeowner")).isNotNull();
            assertThat(root.getChild("remowner")).isNotNull();
            assertThat(root.getChild("ro")).isNotNull();
            assertThat(root.getChild("removemember")).isNotNull();
            assertThat(root.getChild("removemem")).isNotNull();
            assertThat(root.getChild("remmember")).isNotNull();
            assertThat(root.getChild("remmem")).isNotNull();
            assertThat(root.getChild("rm")).isNotNull();
            assertThat(root.getChild("owner")).isNull();
            assertThat(root.getChild("member")).isNull();
        }
    }

    @Test
    void ownerAndMemberAliasesAcceptWorldGuardDomainArguments() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        WorldGuardStorage storage = WorldGuardStorage.load(tempDir);
        WorldGuardCommands commands = new WorldGuardCommands(
            new WorldGuardConfig(tempDir, 2, 1000),
            storage,
            unavailableWorldEdit()
        );

        commands.register(dispatcher);

        CommandNode<CommandSourceStack> rg = root(dispatcher, "rg");
        for (String alias : List.of(
            "addowner",
            "ao",
            "removeowner",
            "remowner",
            "ro",
            "addmember",
            "addmem",
            "am",
            "removemember",
            "removemem",
            "remmember",
            "remmem",
            "rm"
        )) {
            assertThat(rg.getChild(alias).getChild("region").getChild("domain"))
                .as(alias)
                .isNotNull();
        }
    }

    private static CommandNode<CommandSourceStack> root(
        CommandDispatcher<CommandSourceStack> dispatcher,
        String name
    ) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(name);
        assertThat(root).as(name).isNotNull();
        return root;
    }

    private static WorldEditSelectionSource unavailableWorldEdit() {
        return new WorldEditSelectionSource() {
            @Override
            public WorldEditSelectionResult selection(ServerPlayer player) {
                return WorldEditSelectionResult.unavailable("WorldEdit unavailable in tests.");
            }

            @Override
            public String description() {
                return "unavailable in tests";
            }
        };
    }
}
