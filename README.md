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
- `/wg`, `/worldguard`, `/region`, and `/rg` commands.
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
/wg status
/wg save
/wg list
/wg here
/wg info <region>
/wg global [world]
/wg global create [world]
/wg global flag <flag> [allow|deny|unset]
/wg define <region>
/wg define <region> selection [priority]
/wg define __global__ [world]
/wg define <region> <x1> <y1> <z1> <x2> <y2> <z2> [priority]
/wg delete <region>
/wg remove <region>
/wg flags
/wg flag <region> <flag> [allow|deny|unset]
/wg setpriority <region> <priority>
/wg setparent <region> [parent]
/wg parent set <region> <parent>
/wg parent clear <region>
/wg owner add <region> <player>
/wg owner remove <region> <player>
/wg member add <region> <player>
/wg member remove <region> <player>
/rg addowner <region> <player>
/rg removeowner <region> <player>
/rg remowner <region> <player>
/rg addmember <region> <player>
/rg removemember <region> <player>
/rg remmember <region> <player>
```

`/worldguard`, `/region`, and `/rg` are registered as aliases. Mutating commands
require `mod_worldguard:admin`, falling back to the configured admin permission
level. Protection bypass checks `mod_worldguard:bypass`, also falling back to the
configured admin permission level.

When WorldEdit is installed, `/wg define <region>` imports the executing
player's complete WorldEdit cuboid or polygonal selection. `/wg define <region>
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
