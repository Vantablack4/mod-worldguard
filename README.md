# Vantablack WorldGuard

Server-side Fabric port target for the WorldGuard protection surface used by the
Vantablack Minecraft server.

This repository is a Vantablack fork/port target for
[EngineHub/WorldGuard](https://github.com/EngineHub/WorldGuard). The first
Fabric slice is intentionally Fabric-native instead of a direct Bukkit adapter:
it implements a small region and flag runtime, then wires that runtime into
Fabric server events. No upstream WorldGuard core code is copied into the MVP;
the repository uses WorldGuard's LGPL-3.0-or-later license so later core reuse
or migration can happen without changing the project license.

## Scope

Included in the first Fabric port:

- Cuboid regions stored per dimension.
- Region priority, members, and explicit flag states.
- `/wg` and `/worldguard` admin commands.
- Protection hooks for block break, block attack, block use/place attempts,
  item use, entity use, and entity attack.
- Default protected regions deny `build`, `interact`, `use-entity`, and
  `attack-entity` for non-members.

Not included yet:

- Full WorldGuard Bukkit API compatibility.
- WorldEdit selection import. Use explicit coordinates for now.
- Region polygons, parent regions, inheritance, greetings/farewells, sessions,
  or the full upstream flag set.
- Explosion, fire, fluid, piston, portal, crop-trample, or mob-grief protection.
  Those need dedicated mixins or additional Fabric API coverage.

## Commands

```text
/wg
/wg list
/wg here
/wg info <region>
/wg define <region> <x1> <y1> <z1> <x2> <y2> <z2> [priority]
/wg delete <region>
/wg flag <region> <flag> <allow|deny|unset>
/wg member add <region> <player>
/wg member remove <region> <player>
```

`/worldguard` is registered as an alias. Mutating commands require the
configured admin permission level, defaulting to operator level `2`.

## Flags

```text
build
interact
use-entity
attack-entity
item-use
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
