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
  entities, double-chest endpoint checks, entity use, riding, entity attack,
  non-player entity damage, potion/firework/wind-charge projectile damage,
  mob spawning, explosions, TNT/lighter use, anvil, bed, respawn anchor, and
  big-dripleaf interactions, fire spread, fluid flow, pistons, Enderman/Ravager grief,
  Ender Dragon and Wither block damage,
  movement entry/exit, portal and teleport entry/exit, ender pearl and chorus
  teleport use, chat send/receive filtering, sleep, PvP, fall damage,
  natural health regeneration, natural hunger drain, invincibility, item drop,
  item pickup, vehicle and entity placement/destruction, item frame rotation,
  crop and block trampling, direct farmland-to-dirt conversion, pressure plates,
  buttons, tripwire, hoppers, lightning, snow/ice weather, melt, plant growth,
  sapling/tree feature growth, leaf decay,
  mushroom/grass/mycelium/vine/copper/coral/dripstone/sculk mutations,
  amethyst bud growth, hanging mangrove propagule growth, cave-vine crop/vine
  growth, generated huge mushroom target blocks, pointed-dripstone target
  growth, lava-created fire, frosted-ice formation, snowman snow trails, and
  experience orb drops.
- Runtime effects for `greeting`, `farewell`, `deny-message`,
  `entry-deny-message`, `exit-deny-message`, `blocked-cmds`, `allowed-cmds`,
  `game-mode`, and heal/feed flags.
- Player-scoped runtime effects for `time-lock` and `weather-lock`, plus
  region `spawn` location handling for respawns.
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
- Upstream-style build-related flag resolution: a specific allow such as
  `lighter allow` can permit that action through a broader `build deny`, while a
  specific deny such as `block-place deny` still wins.

Not included yet:

- Full WorldGuard Bukkit API compatibility.
- LuckPerms-native group lookup beyond Fabric permission nodes.
- Upstream command options that are not yet represented by Brigadier flags,
  including `/region info -u/-s`, WorldEdit define/claim `-w` forms, and
  free-order switch parsing.
- Dispenser/dropper synthetic action simulation, lectern book-take protection,
  entity-triggered dripleaf tilt hooks, full non-damaging potion-effect paths,
  and mount/dismount mixin parity beyond the current direct-use ride checks.
- Full rich-text/clickable command output parity with Bukkit WorldGuard.

## Commands

```text
/wg
/wg version
/wg reload
/region list [page]
/region list -i <id-search> [page]
/region list -w <world> [-i <id-search>] [-p <player>] [-n] [-s] [page]
/region list -p <player> [-n] [-s] [-i <id-search>] [-w <world>] [page]
/region info [region]
/region info -w <world> <region>
/region i [region]
/region define <region>
/region define <region> selection [priority]
/region define <region> <x1> <y1> <z1> <x2> <y2> <z2> [priority]
/region claim <region>
/region redefine <region>
/region redefine -w <world> <region>
/region select [region]
/region select -w <world> <region>
/region teleport <region>
/region teleport -w <world> <region>
/region teleport -s <region>
/region teleport -s -w <world> <region>
/region teleport -c <region>
/region teleport -c -w <world> <region>
/region tp <region>
/region tp -s <region>
/region tp -c <region>
/region remove <region>
/region remove -f <region>
/region remove -u <region>
/region remove -w <world> <region>
/region remove -w <world> -f <region>
/region remove -w <world> -u <region>
/region flags <region>
/region flags -p <page> <region>
/region flags -w <world> <region>
/region flags -w <world> -p <page> <region>
/region flag <region> <flag> [allow|deny|unset]
/region flag -w <world> <region> <flag> [allow|deny|unset]
/region flag <region> <flag> -g <members|owners|nonmembers|nonowners|all|none>
/region flag <region> <typed-flag> <value>
/region flag <region> <typed-flag> -g <group> <value>
/region setpriority <region> <priority>
/region setpriority -w <world> <region> <priority>
/region setparent <region> [parent]
/region setparent -w <world> <region> [parent]
/region addowner <region> <player|uuid:<uuid>|g:<group>> [...]
/region addowner -w <world> <region> <player|uuid:<uuid>|g:<group>> [...]
/region removeowner <region> <player|uuid:<uuid>|g:<group>> [...]
/region removeowner -w <world> <region> <player|uuid:<uuid>|g:<group>> [...]
/region removeowner <region> -a
/region removeowner -w <world> <region> -a
/region addmember <region> <player|uuid:<uuid>|g:<group>> [...]
/region addmember -w <world> <region> <player|uuid:<uuid>|g:<group>> [...]
/region removemember <region> <player|uuid:<uuid>|g:<group>> [...]
/region removemember -w <world> <region> <player|uuid:<uuid>|g:<group>> [...]
/region removemember <region> -a
/region removemember -w <world> <region> -a
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
Existing-region commands that support `-w <world>` expect the world switch
before the region argument, matching the Brigadier forms listed above.
`/region remove`, `/region delete`, `/region del`, and `/region rem` reject
parent regions with child regions unless `-f` is supplied to remove descendants
or `-u` is supplied to unset the direct children parent value; `-f` and `-u`
cannot be used together.
`/region flags` shows both state flags and typed value flags, with `-p <page>`
available for the paginated flag list.

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
vehicle-place
vehicle-destroy
use-entity
attack-entity
item-use
pvp
tnt
lighter
ride
potion-splash
fire-spread
water-flow
lava-flow
lava-fire
mob-grief
pistons
send-chat
receive-chat
sleep
respawn-anchors
item-drop
item-pickup
exp-drops
mob-spawning
mob-damage
damage-animals
creeper-explosion
enderdragon-block-damage
ghast-fireball
other-explosion
breeze-charge-explosion
wither-damage
enderman-grief
snowman-trails
ravager-grief
entity-painting-destroy
entity-item-frame-destroy
item-frame-rotation
block-trampling
firework-damage
use-anvil
use-dripleaf
wind-charge-burst
lightning
snow-fall
snow-melt
ice-form
ice-melt
frosted-ice-melt
frosted-ice-form
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

For build-related checks, the Fabric runtime follows WorldGuard's `testBuild`
shape rather than treating all checked flags as a flat deny list. A specific
state flag can allow its exact action through a broader `build deny`; a specific
deny still blocks the action. This matters for upstream UX such as permitting
`lighter` while denying general block placement.

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
