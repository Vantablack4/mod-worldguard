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
- Typed non-state flag metadata, command editing, storage, per-flag groups, and
  runtime effects for upstream-style string, set, location, numeric, boolean,
  and registry-valued flags where Fabric exposes a safe server hook.
- `/wg`, `/worldguard`, `/region`, `/regions`, and `/rg` commands.
- Protection hooks for block break, block attack, block use/place attempts,
  item use, bucket placement/pickup, chest/container access for blocks and
  entities, entity use, entity attack, non-player entity damage, mob spawning,
  explosions, fire spread, fluid flow, pistons, Enderman/Ravager grief,
  movement entry/exit, portal and teleport entry/exit, ender pearl and chorus
  teleport use, chat send, sleep, PvP, fall damage, invincibility, item drop,
  item pickup, item frame rotation, crop and block trampling, pressure plates,
  buttons, tripwire, hoppers, lightning, snow/ice weather, melt, plant growth,
  leaf decay, mushroom/grass/mycelium/vine/copper/coral/dripstone/sculk
  mutations.
- Runtime effects for `greeting`, `farewell`, `deny-message`,
  `entry-deny-message`, `exit-deny-message`, `blocked-cmds`, `allowed-cmds`,
  `game-mode`, and heal/feed flags.
- Region teleport command effects for `teleport`, `spawn`, and
  `teleport-message`, including upstream-style `/region teleport <region>`,
  `/region teleport -s <region>`, and `/region teleport -c <region>` forms.
- Fabric permission API integration for `mod_worldguard:admin` and
  `mod_worldguard:bypass`, upstream-style `worldguard:region.*` command/bypass
  permission aliases, plus region group matching through
  `mod_worldguard:region.group.<group>` permission nodes.
- Optional WorldEdit Fabric selection import and `/region select` export for
  cuboid and polygonal regions.
- Default protected regions deny `build`, `block-break`, `block-place`, `use`,
  `interact`, `item-use`, `use-entity`, `attack-entity`, explosions,
  mob grief, pistons, fire spread, and fluid flow for non-members.

Not included yet:

- Full WorldGuard Bukkit API compatibility.
- Runtime effects for `weather-lock`, `time-lock`, and respawn handling for the
  `spawn` location flag.
- LuckPerms-native group lookup beyond Fabric permission nodes.
- Upstream command options that are not yet represented by Brigadier flags:
  `-w`, `-n`, and list paging/filtering.
- Recipient chat filtering, sapling/tree feature growth, and dragon/wither
  non-explosion block damage.

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
/region select [region]
/region teleport <region>
/region teleport -s <region>
/region teleport -c <region>
/region tp <region>
/region tp -s <region>
/region tp -c <region>
/region remove <region>
/region flags <region>
/region flag <region> <flag> [allow|deny|unset]
/region flag <region> <flag> -g <members|owners|nonmembers|nonowners|all|none>
/region flag <region> <typed-flag> <value>
/region flag <region> <typed-flag> -g <group> <value>
/region setpriority <region> <priority>
/region setparent <region> [parent]
/region addowner <region> <player|uuid:<uuid>|g:<group>> [...]
/region removeowner <region> <player|uuid:<uuid>|g:<group>> [...]
/region removeowner <region> -a
/region addmember <region> <player|uuid:<uuid>|g:<group>> [...]
/region removemember <region> <player|uuid:<uuid>|g:<group>> [...]
/region removemember <region> -a
/region toggle-bypass [on|off]
/region load
/region save
```

`/worldguard` is an alias of `/wg`. `/regions` and `/rg` are aliases of
`/region`. Region command aliases follow upstream WorldGuard where implemented:
`define`, `def`, `d`, `create`; `claim`; `select`, `sel`, `s`; `redefine`,
`update`, `move`; `remove`, `delete`, `del`, `rem`; `flag`, `f`;
`setpriority`, `priority`, `pri`; `setparent`, `parent`, `par`; `addmember`,
`addmem`, `am`; `addowner`, `ao`; `removemember`, `remmember`, `removemem`,
`remmem`, `rm`; `removeowner`, `remowner`, `ro`; and `toggle-bypass`,
`bypass`.

The special `__global__` region is created lazily by commands that allow it,
including `info`, `flag`, `flags`, `remove`, and owner/member updates. It is
rejected by commands that define or reshape physical regions, set priority, or
set parents, matching upstream WorldGuard behavior.

Mutating commands accept upstream-style Fabric permission identifiers like
`worldguard:region.define`, `worldguard:region.claim`,
`worldguard:region.select`, `worldguard:region.flag.regions`,
`worldguard:region.addmember`, `worldguard:region.removeowner`, and
`worldguard:region.toggle-bypass`; `mod_worldguard:admin` remains the fallback
operator-level admin permission. Protection bypass checks `mod_worldguard:bypass`
or `worldguard:region.bypass` / `worldguard:region.bypass.<world>`, unless the
player has disabled bypass with `/region bypass off`. Operators do not bypass
protection solely because they can administer regions; grant a bypass permission
explicitly when that behavior is wanted. Region owner/member groups match
players that have `mod_worldguard:region.group.<group>`.

When WorldEdit is installed, `/region define <region>` imports the executing
player's complete WorldEdit cuboid or polygonal selection. `/region define <region>
selection [priority]` is the explicit selection form when a non-zero priority is
needed. `/region select [region]` writes an existing cuboid or polygonal region
back into the player's WorldEdit selection. WorldEdit is a soft integration: the
mod remains loadable without it, and explicit-coordinate definitions continue to
work.

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
item-frame-rotation
block-trampling
mob-spawning
lightning
snow-fall
snow-melt
ice-form
ice-melt
frosted-ice-melt
crop-growth
mushroom-growth
grass-growth
mycelium-spread
vine-growth
leaf-decay
rock-growth
sculk-growth
soil-dry
moisture-change
coral-fade
copper-fade
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
matching region when the flag has member-bypass semantics. Players with an
explicit bypass permission bypass all protection checks until they disable bypass
with `/region bypass off`.

The storage model also recognizes and round-trips typed upstream-style flags
such as `teleport`, `spawn`, `teleport-message`, `deny-message`, `greeting`,
`farewell`, `blocked-cmds`, `allowed-cmds`, `deny-spawn`, `weather-lock`,
`time-lock`, `game-mode`, heal/feed options, and per-flag region groups.
`/region flag` edits both state flags and typed flags. Clearing a flag with no
value also clears that flag's explicit `-g` group, matching upstream WorldGuard.
`deny-spawn` blocks matching namespaced or short entity ids, for example
`minecraft:zombie` or `zombie`.

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
