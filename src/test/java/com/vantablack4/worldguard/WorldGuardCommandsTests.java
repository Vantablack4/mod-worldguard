package com.vantablack4.worldguard;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;

import com.vantablack4.worldguard.worldedit.WorldEditSelectionResult;
import com.vantablack4.worldguard.worldedit.WorldEditSelectionSource;
import com.vantablack4.worldguard.worldedit.WorldEditRegionSelection;
import com.vantablack4.worldguard.flag.WorldGuardFlagValue;
import com.vantablack4.worldguard.flag.WorldGuardRegionGroup;
import com.vantablack4.worldguard.flag.WorldGuardValueFlag;

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
            assertThat(root.getChild("select")).isNotNull();
            assertThat(root.getChild("sel")).isNotNull();
            assertThat(root.getChild("s")).isNotNull();
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
            assertThat(root.getChild("toggle-bypass")).isNotNull();
            assertThat(root.getChild("bypass")).isNotNull();
            assertThat(root.getChild("owner")).isNull();
            assertThat(root.getChild("member")).isNull();
        }
    }

    @Test
    void teleportCommandRegistersUpstreamFlagSpawnAndCenterForms() {
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

        for (String alias : List.of("teleport", "tp")) {
            CommandNode<CommandSourceStack> teleport = root(dispatcher, "rg").getChild(alias);
            assertThat(teleport.getChild("region")).as(alias + " region").isNotNull();
            assertThat(teleport.getChild("-s").getChild("region")).as(alias + " -s").isNotNull();
            assertThat(teleport.getChild("-c").getChild("region")).as(alias + " -c").isNotNull();
            assertThat(teleport.getChild("region").getChild("-s")).as(alias + " misplaced -s").isNull();
            assertThat(teleport.getChild("region").getChild("-c")).as(alias + " misplaced -c").isNull();
        }
    }

    @Test
    void regionCommandCopiesPreserveTypedFlagsAndGroups() {
        WorldGuardFlagValue teleport = WorldGuardFlagValue.location(
            "minecraft:overworld",
            1,
            64,
            2,
            90,
            0
        ).orElseThrow();
        WorldGuardRegion region = WorldGuardRegion.defaultProtected(
            "spawn",
            "minecraft:overworld",
            0,
            0,
            0,
            5,
            5,
            5,
            0
        ).withValue(WorldGuardValueFlag.TELEPORT, teleport)
            .withFlagGroup(WorldGuardValueFlag.TELEPORT, WorldGuardRegionGroup.OWNERS);

        WorldGuardRegion reprioritized = WorldGuardCommands.copyWithPriority(region, 7);
        assertThat(reprioritized.priority()).isEqualTo(7);
        assertThat(reprioritized.value(WorldGuardValueFlag.TELEPORT)).contains(teleport);
        assertThat(reprioritized.flagGroup(WorldGuardValueFlag.TELEPORT)).isEqualTo(WorldGuardRegionGroup.OWNERS);

        WorldGuardRegion reshaped = WorldGuardCommands.withShape(
            region,
            new WorldEditRegionSelection("minecraft:overworld", 10, 11, 12, 20, 21, 22)
        );
        assertThat(reshaped.minX()).isEqualTo(10);
        assertThat(reshaped.maxZ()).isEqualTo(22);
        assertThat(reshaped.value(WorldGuardValueFlag.TELEPORT)).contains(teleport);
        assertThat(reshaped.flagGroup(WorldGuardValueFlag.TELEPORT)).isEqualTo(WorldGuardRegionGroup.OWNERS);
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
            "addmember",
            "addmem",
            "am"
        )) {
            assertThat(rg.getChild(alias).getChild("region").getChild("domain"))
                .as(alias)
                .isNotNull();
        }

        for (String alias : List.of(
            "removeowner",
            "remowner",
            "ro",
            "removemember",
            "removemem",
            "remmember",
            "remmem",
            "rm"
        )) {
            assertThat(rg.getChild(alias).getChild("region").getChild("domain"))
                .as(alias)
                .isNotNull();
            assertThat(rg.getChild(alias).getChild("region").getChild("-a"))
                .as(alias + " -a")
                .isNotNull();
        }
    }

    @Test
    void flagCommandRegistersTypedValuesAndGroupTargetingAfterFlag() {
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

        CommandNode<CommandSourceStack> flag = root(dispatcher, "rg")
            .getChild("flag")
            .getChild("region")
            .getChild("flag");
        assertThat(flag.getChild("value")).isNotNull();

        CommandNode<CommandSourceStack> groupLiteral = flag.getChild("-g");
        assertThat(groupLiteral).isNotNull();
        CommandNode<CommandSourceStack> group = groupLiteral.getChild("group");
        assertThat(group).isNotNull();
        assertThat(group.getChild("value")).isNotNull();

        assertThat(root(dispatcher, "rg")
            .getChild("flag")
            .getChild("region")
            .getChild("-g"))
            .isNull();
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
            public com.vantablack4.worldguard.worldedit.WorldEditSelectionWriteResult selectRegion(
                ServerPlayer player,
                WorldGuardRegion region
            ) {
                return com.vantablack4.worldguard.worldedit.WorldEditSelectionWriteResult.unavailable(
                    "WorldEdit unavailable in tests."
                );
            }

            @Override
            public String description() {
                return "unavailable in tests";
            }
        };
    }
}
