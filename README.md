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
- Region priority, owners, members, owner/member groups, parent inheritance, and
  explicit state flags.
- `/wg`, `/worldguard`, `/region`, `/regions`, and `/rg` commands.
- Protection hooks for block break, block attack, block use/place attempts,
  item use, bucket placement/pickup, chest/container access for blocks and
  entities, entity use, entity attack, non-player entity damage, mob spawning,
  explosions, fire spread, fluid flow, pistons, Enderman/Ravager grief,
  movement entry/exit, teleport entry/exit, ender pearl and chorus teleport
  use, chat send, sleep, PvP, fall damage, invincibility, item drop, and item
  pickup.
- Fabric permission API integration for `mod_worldguard:admin` and
  `mod_worldguard:bypass`, plus region group matching through
  `mod_worldguard:region.group.<group>` permission nodes.
- Optional WorldEdit Fabric selection import for cuboid and polygonal region
  definition.
- Default protected regions deny `build`, `block-break`, `block-place`, `use`,
  `interact`, `item-use`, `use-entity`, `attack-entity`, explosions,
  mob grief, pistons, fire spread, and fluid flow for non-members.

Not included yet:

- Full WorldGuard Bukkit API compatibility.
- Typed non-state flags like string, set, location, and numeric flags.
- LuckPerms-native group lookup beyond Fabric permission nodes.
- Upstream command options that are not yet represented by Brigadier flags:
  `-w`, `-g`, `-n`, `-a`, paging/filtering options, `/rg select`, and
  `/rg toggle-bypass`.
- Recipient chat filtering, portals, crop-trampling, pressure plates,
  lightning/weather, growth/decay, hoppers, and dragon/wither non-explosion
  block damage.

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
/region claim <region>
/region redefine <region>
/region teleport <region>
/region tp <region>
/region remove <region>
/region flags <region>
/region flag <region> <flag> [allow|deny|unset]
/region setpriority <region> <priority>
/region setparent <region> [parent]
/region addowner <region> <player|uuid:<uuid>|g:<group>> [...]
/region removeowner <region> <player|uuid:<uuid>|g:<group>> [...]
/region addmember <region> <player|uuid:<uuid>|g:<group>> [...]
/region removemember <region> <player|uuid:<uuid>|g:<group>> [...]
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
falling back to the configured admin permission level. Region owner/member
groups match players that have `mod_worldguard:region.group.<group>`.

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
mob-spawning
mob-damage
damage-animals
entity-painting-destroy
entity-item-frame-destroy
exit-via-teleport
enderpearl
chorus-fruit-teleport
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
