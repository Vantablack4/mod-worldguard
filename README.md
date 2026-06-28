# Vantablack WorldGuard

Server-side Fabric port target for the WorldGuard protection surface used by the
Vantablack Minecraft server.

This repository is a Vantablack fork/port target for
[EngineHub/WorldGuard](https://github.com/EngineHub/WorldGuard). The Fabric
adaptation is intentionally Fabric-native instead of a Bukkit adapter: it
implements a region and flag runtime, then wires that runtime into Fabric server
events. No upstream WorldGuard core code is copied into the current Fabric
runtime; the repository uses WorldGuard's LGPL-3.0-or-later license so later
core reuse or migration can happen without changing the project license.

## Scope

Included in the Fabric port:

- Cuboid regions stored per dimension.
- Cuboid, polygonal 2D, and global regions stored per dimension.
- Region priority, owners, members, parent inheritance, and explicit state flags.
- `/wg`, `/worldguard`, `/region`, `/regions`, and `/rg` commands.
- Protection hooks for block break, block attack, block use/place attempts,
  item use, entity use, entity attack, explosions, fire spread, fluid flow,
  pistons, Enderman/Ravager grief, movement entry/exit, chat send, sleep, PvP,
  fall damage, invincibility, item drop, and item pickup.
- Fabric permission API integration for `mod_worldguard:admin` and
  `mod_worldguard:bypass`.
- Optional WorldEdit Fabric selection import for cuboid and polygonal region
  definition.
- Default protected regions deny `build`, `block-break`, `block-place`, `use`,
  `interact`, `item-use`, `use-entity`, `attack-entity`, explosions,
  mob grief, pistons, fire spread, and fluid flow for non-members.

Not included yet:

- Full WorldGuard Bukkit API compatibility.
- Typed non-state flags like string, set, location, and numeric flags.
- Group resolution against LuckPerms/Fabric permission groups.
- Teleport-specific movement flags, recipient chat filtering, portals,
  crop-trampling, hanging-entity destruction, lightning/weather, growth/decay,
  and full WorldGuard Bukkit API compatibility.

## Commands

```text
/wg
/wg version
/wg reload
/region list
/region info [region]
/region i [region]
/region define <region>
/region define <region> selection [priority]
/region define <region> <x1> <y1> <z1> <x2> <y2> <z2> [priority]
/region redefine <region>
/region remove <region>
/region flags <region>
/region flag <region> <flag> [allow|deny|unset]
/region setpriority <region> <priority>
/region setparent <region> [parent]
/region addowner <region> <player>
/region removeowner <region> <player>
/region addmember <region> <player>
/region removemember <region> <player>
/region load
/region save
```

`/worldguard` is an alias of `/wg`. `/regions` and `/rg` are aliases of
`/region`. Region command aliases follow upstream WorldGuard where implemented:
`define`, `def`, `d`, `create`; `redefine`, `update`, `move`; `remove`,
`delete`, `del`, `rem`; `flag`, `f`; `setpriority`, `priority`, `pri`;
`setparent`, `parent`, `par`; `addmember`, `addmem`, `am`; `addowner`, `ao`;
`removemember`, `remmember`, `removemem`, `remmem`, `rm`; and `removeowner`,
`remowner`, `ro`.

The special `__global__` region is created lazily by commands that allow it,
including `info`, `flag`, `flags`, `remove`, and owner/member updates. It is
rejected by commands that define or reshape physical regions, set priority, or
set parents, matching upstream WorldGuard behavior.

Mutating commands require `mod_worldguard:admin`, falling back to the configured
admin permission level. Protection bypass checks `mod_worldguard:bypass`, also
falling back to the configured admin permission level.

When WorldEdit is installed, `/region define <region>` imports the executing
player's complete WorldEdit cuboid or polygonal selection. `/region define <region>
selection [priority]` is the explicit selection form when a non-zero priority is
needed. WorldEdit is a soft integration: the mod remains loadable without it,
and explicit-coordinate definitions continue to work.

## Flags

```text
build
block-break
block-place
use
interact
chest-access
use-entity
attack-entity
item-use
pvp
tnt
fire-spread
water-flow
lava-flow
mob-grief
pistons
send-chat
sleep
item-drop
item-pickup
entry
exit
notify-enter
notify-leave
```

Flags resolve by region priority. The highest priority matching region with an
explicit flag value decides the action. Members bypass denies from their own
matching region; operators at the configured admin level bypass all checks.

## Configuration

The mod writes `config/mod_worldguard/config.properties` on first boot:

```properties
commands.admin-permission-level=2
messages.deny-cooldown-millis=1000
```

Region state is stored in:

```text
config/mod_worldguard/regions.properties
```

## Build

```bash
./gradlew build
```

The jar is produced under `build/libs/`.

## Serverpack Integration

This mod is server-only and should be registered in `mc-serverpack` after the
repo has a releasable commit and internal mod artifact:

```yaml
mod-worldguard:
  artifact: mod-worldguard
  repository: mod-worldguard
  sourcePath: ../mods/mod-worldguard
  side: server
```
