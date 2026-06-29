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
import java.util.UUID;
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
            CommandNode<CommandSourceStack> list = root.getChild("list");
            assertThat(list).isNotNull();
            assertThat(list.getChild("page")).isNotNull();
            assertThat(list.getChild("-i").getChild("idSearch")).isNotNull();
            assertThat(list.getChild("-i").getChild("idSearch").getChild("page")).isNotNull();
            assertThat(list.getChild("-i").getChild("idSearch").getChild("-w").getChild("world")).isNotNull();
            assertThat(list.getChild("-w").getChild("world")).isNotNull();
            assertThat(list.getChild("-w").getChild("world").getChild("page")).isNotNull();
            assertThat(list.getChild("-w").getChild("world").getChild("-i").getChild("idSearch")).isNotNull();
            assertThat(list.getChild("-p").getChild("owner")).isNotNull();
            assertThat(list.getChild("-p").getChild("owner").getChild("page")).isNotNull();
            assertThat(list.getChild("-p").getChild("owner").getChild("-n")).isNotNull();
            assertThat(list.getChild("-p").getChild("owner").getChild("-s")).isNotNull();
            assertThat(list.getChild("-p").getChild("owner").getChild("-i").getChild("idSearch")).isNotNull();
            assertThat(list.getChild("-p").getChild("owner").getChild("-w").getChild("world")).isNotNull();
            assertThat(list.getChild("-w").getChild("world").getChild("-p").getChild("owner")).isNotNull();
            assertThat(list.getChild("-n")).isNotNull();
            assertThat(list.getChild("-n").getChild("-p").getChild("owner")).isNotNull();
            assertThat(list.getChild("-s")).isNotNull();
            assertThat(list.getChild("-s").getChild("-i").getChild("idSearch")).isNotNull();
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
            assertThat(root.getChild("info").getChild("-w").getChild("world").getChild("region"))
                .isNotNull();
            assertThat(root.getChild("i").getChild("-w").getChild("world").getChild("region"))
                .isNotNull();
            assertThat(root.getChild("flags").getChild("-w").getChild("world").getChild("region"))
                .isNotNull();
            assertThat(root.getChild("flags").getChild("-p").getChild("page").getChild("region"))
                .isNotNull();
            assertThat(root.getChild("flags").getChild("-p").getChild("page").getChild("-w").getChild("world").getChild("region"))
                .isNotNull();
            assertThat(root.getChild("flags").getChild("-w").getChild("world").getChild("-p").getChild("page").getChild("region"))
                .isNotNull();
            assertThat(root.getChild("flag").getChild("-w").getChild("world").getChild("region").getChild("flag"))
                .isNotNull();
            assertThat(root.getChild("f").getChild("-w").getChild("world").getChild("region").getChild("flag"))
                .isNotNull();
            for (String alias : List.of("delete", "del", "remove", "rem")) {
                CommandNode<CommandSourceStack> remove = root.getChild(alias);
                assertThat(remove.getChild("-w").getChild("world").getChild("region"))
                    .as(alias + " -w")
                    .isNotNull();
                assertThat(remove.getChild("-f").getChild("region"))
                    .as(alias + " -f")
                    .isNotNull();
                assertThat(remove.getChild("-u").getChild("region"))
                    .as(alias + " -u")
                    .isNotNull();
                assertThat(remove.getChild("-f").getChild("-u").getChild("region"))
                    .as(alias + " -f -u")
                    .isNotNull();
                assertThat(remove.getChild("-u").getChild("-f").getChild("region"))
                    .as(alias + " -u -f")
                    .isNotNull();
                assertThat(remove.getChild("-w").getChild("world").getChild("-f").getChild("region"))
                    .as(alias + " -w -f")
                    .isNotNull();
                assertThat(remove.getChild("-f").getChild("-w").getChild("world").getChild("region"))
                    .as(alias + " -f -w")
                    .isNotNull();
            }
            for (String alias : List.of("select", "sel", "s", "redefine", "update", "move")) {
                assertThat(root.getChild(alias).getChild("-w").getChild("world").getChild("region"))
                    .as(alias + " -w")
                    .isNotNull();
            }
            for (String alias : List.of("setpriority", "priority", "pri")) {
                assertThat(root.getChild(alias).getChild("-w").getChild("world").getChild("region").getChild("priority"))
                    .as(alias + " -w")
                    .isNotNull();
            }
            for (String alias : List.of("setparent", "parent", "par")) {
                assertThat(root.getChild(alias).getChild("-w").getChild("world").getChild("region"))
                    .as(alias + " -w")
                    .isNotNull();
            }
        }
    }

    @Test
    void listHelpersFilterRegionIdsAndPageResults() {
        List<WorldGuardRegion> regions = List.of(
            WorldGuardRegion.global("minecraft:overworld"),
            region("spawn"),
            region("market"),
            region("spawn_west")
        );

        assertThat(WorldGuardCommands.filterListRegions(regions, "SPAWN"))
            .extracting(WorldGuardRegion::id)
            .containsExactly("spawn", "spawn_west");
        assertThat(WorldGuardCommands.filterListRegions(regions, "  "))
            .extracting(WorldGuardRegion::id)
            .containsExactly("__global__", "spawn", "market", "spawn_west");

        List<WorldGuardRegion> pagedRegions = List.of(
            region("r1"),
            region("r2"),
            region("r3"),
            region("r4"),
            region("r5"),
            region("r6"),
            region("r7"),
            region("r8"),
            region("r9")
        );
        assertThat(WorldGuardCommands.normalizeListPage(0)).isEqualTo(1);
        assertThat(WorldGuardCommands.listPageCount(pagedRegions.size(), WorldGuardCommands.LIST_PAGE_SIZE))
            .isEqualTo(2);
        assertThat(WorldGuardCommands.listPage(pagedRegions, 1, WorldGuardCommands.LIST_PAGE_SIZE))
            .extracting(WorldGuardRegion::id)
            .containsExactly("r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8");
        assertThat(WorldGuardCommands.listPage(pagedRegions, 2, WorldGuardCommands.LIST_PAGE_SIZE))
            .extracting(WorldGuardRegion::id)
            .containsExactly("r9");
        assertThat(WorldGuardCommands.listPage(pagedRegions, 3, WorldGuardCommands.LIST_PAGE_SIZE))
            .isEmpty();
    }

    @Test
    void listEntriesFilterPlayerDomainsAndSortOwnersBeforeMembers() {
        UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID other = UUID.fromString("22222222-2222-2222-2222-222222222222");
        List<WorldGuardRegion> regions = List.of(
            region("z_member").withMember(player),
            region("unrelated"),
            region("m_owner").withOwner(player),
            WorldGuardRegion.global("minecraft:overworld").withOwner(player),
            region("a_member").withMember(player),
            region("other_owner").withOwner(other)
        );

        List<WorldGuardCommands.ListEntry> entries = WorldGuardCommands.listEntries(regions, "", player, true, null);

        assertThat(entries)
            .extracting(entry -> entry.region().id())
            .containsExactly("__global__", "m_owner", "a_member", "z_member");
        assertThat(entries)
            .extracting(WorldGuardCommands.ListEntry::relationship)
            .containsExactly(
                WorldGuardCommands.ListRelationship.OWNER,
                WorldGuardCommands.ListRelationship.OWNER,
                WorldGuardCommands.ListRelationship.MEMBER,
                WorldGuardCommands.ListRelationship.MEMBER
            );

        assertThat(WorldGuardCommands.listEntries(regions, "member", player, true, null))
            .extracting(entry -> entry.region().id())
            .containsExactly("a_member", "z_member");
    }

    @Test
    void listEntriesTreatNameOnlyPlayerFilterAsUnmatchedWithoutLegacyNameDomains() {
        UUID player = UUID.fromString("11111111-1111-1111-1111-111111111111");
        List<WorldGuardRegion> regions = List.of(region("spawn").withOwner(player));

        assertThat(WorldGuardCommands.listEntries(regions, "", null, true, null))
            .isEmpty();
    }

    @Test
    void listEntriesFilterByWorldEditSelectionIntersectionAndKeepGlobal() {
        WorldEditRegionSelection selection = new WorldEditRegionSelection(
            "minecraft:overworld",
            5,
            0,
            5,
            15,
            10,
            15
        );
        List<WorldGuardRegion> regions = List.of(
            WorldGuardRegion.global("minecraft:overworld"),
            region("inside", 6, 0, 6, 8, 4, 8),
            region("touching", 15, 0, 15, 18, 4, 18),
            region("outside", 20, 0, 20, 24, 4, 24),
            region("other_world", "minecraft:the_nether", 6, 0, 6, 8, 4, 8)
        );

        assertThat(WorldGuardCommands.listEntries(regions, "", null, false, selection))
            .extracting(entry -> entry.region().id())
            .containsExactly("__global__", "inside", "touching");
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
            assertThat(teleport.getChild("-w").getChild("world").getChild("region"))
                .as(alias + " -w")
                .isNotNull();
            assertThat(teleport.getChild("-s").getChild("-w").getChild("world").getChild("region"))
                .as(alias + " -s -w")
                .isNotNull();
            assertThat(teleport.getChild("-c").getChild("-w").getChild("world").getChild("region"))
                .as(alias + " -c -w")
                .isNotNull();
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
    void flagListEntriesIncludeStateAndTypedFlags() {
        WorldGuardFlagValue greeting = WorldGuardFlagValue.parse(WorldGuardValueFlag.GREETING, "Welcome").orElseThrow();
        WorldGuardRegion region = region("spawn")
            .withFlag(WorldGuardFlag.PVP, FlagState.DENY)
            .withValue(WorldGuardValueFlag.GREETING, greeting);

        List<WorldGuardCommands.FlagListEntry> entries = WorldGuardCommands.flagListEntries(region);

        assertThat(entries)
            .contains(new WorldGuardCommands.FlagListEntry("pvp", "deny"))
            .contains(new WorldGuardCommands.FlagListEntry("greeting", "Welcome"))
            .contains(new WorldGuardCommands.FlagListEntry("teleport", "unset"));
        assertThat(WorldGuardCommands.flagListPage(entries, 2, 8)).hasSize(8);
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
            assertThat(rg.getChild(alias).getChild("-w").getChild("world").getChild("region").getChild("domain"))
                .as(alias + " -w")
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
            assertThat(rg.getChild(alias).getChild("-w").getChild("world").getChild("region").getChild("domain"))
                .as(alias + " -w")
                .isNotNull();
            assertThat(rg.getChild(alias).getChild("-w").getChild("world").getChild("region").getChild("-a"))
                .as(alias + " -w -a")
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
        assertThat(flag.getChild("-e")).isNotNull();
        assertThat(flag.getChild("-h").getChild("page")).isNotNull();

        CommandNode<CommandSourceStack> groupLiteral = flag.getChild("-g");
        assertThat(groupLiteral).isNotNull();
        CommandNode<CommandSourceStack> group = groupLiteral.getChild("group");
        assertThat(group).isNotNull();
        assertThat(group.getChild("value")).isNotNull();
        assertThat(group.getChild("-e")).isNotNull();

        assertThat(root(dispatcher, "rg")
            .getChild("flag")
            .getChild("region")
            .getChild("-g"))
            .isNull();

        CommandNode<CommandSourceStack> worldFlag = root(dispatcher, "rg")
            .getChild("flag")
            .getChild("-w")
            .getChild("world")
            .getChild("region")
            .getChild("flag");
        assertThat(worldFlag.getChild("value")).isNotNull();
        assertThat(worldFlag.getChild("-e")).isNotNull();
        assertThat(worldFlag.getChild("-h").getChild("page")).isNotNull();
        assertThat(worldFlag.getChild("-g").getChild("group").getChild("value")).isNotNull();
    }

    private static CommandNode<CommandSourceStack> root(
        CommandDispatcher<CommandSourceStack> dispatcher,
        String name
    ) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot().getChild(name);
        assertThat(root).as(name).isNotNull();
        return root;
    }

    private static WorldGuardRegion region(String id) {
        return WorldGuardRegion.defaultProtected(id, "minecraft:overworld", 0, 0, 0, 1, 1, 1, 0);
    }

    private static WorldGuardRegion region(String id, int x1, int y1, int z1, int x2, int y2, int z2) {
        return region(id, "minecraft:overworld", x1, y1, z1, x2, y2, z2);
    }

    private static WorldGuardRegion region(
        String id,
        String world,
        int x1,
        int y1,
        int z1,
        int x2,
        int y2,
        int z2
    ) {
        return WorldGuardRegion.defaultProtected(id, world, x1, y1, z1, x2, y2, z2, 0);
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
