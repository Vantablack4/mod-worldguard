# WorldGuard Fabric Porting Plan

## Upstream Shape

`EngineHub/WorldGuard` does not ship a Fabric module. The reusable upstream
pieces live in `worldguard-core`: region containers, protected region types,
flag registries, query logic, storage, sessions, and command support. The
platform integration is concentrated in `worldguard-bukkit`, where Bukkit/Paper
listeners translate server events into WorldGuard's internal event model.

`EngineHub/WorldEdit` does ship first-party Fabric support. Treat WorldEdit as
the right selection/editing integration point, but keep the protection runtime
independent so Vantablack can boot without a hard WorldEdit coupling.

## Current Fabric Surface

The Vantablack Fabric adaptation is a server-side Fabric-native protection
runtime. It is not a Bukkit compatibility layer and does not load Bukkit
WorldGuard plugins, but it ports the region, command, flag, WorldEdit-selection,
and server-event behavior needed by the Vantablack server.

- Server-only Fabric mod metadata.
- Cuboid, polygonal 2D, and global region model with per-dimension bounds.
- Region priority, owners, members, owner/member groups, parent inheritance, and explicit
  allow/deny/unset flags.
- Local `regions.properties` persistence with schema versioning.
- `/wg`, `/worldguard`, `/region`, and `/rg` admin commands.
- Optional WorldEdit Fabric import for cuboid and polygonal selections.
- Fabric event hooks for block break, block attack, block use/place attempts,
  item use, bucket place/pickup internals, chest/container access on blocks and
  entities, entity use, entity attack, non-player entity damage, mob spawning,
  explosions, fire, fluids, pistons, practical Enderman/Ravager grief,
  movement entry/exit, teleport entry/exit, ender pearl and chorus use, chat
  send, sleep, PvP, fall damage, invincibility, item drop, and item pickup.

## Next Parity Work

Full WorldGuard behavior is mostly event coverage and cache design:

- Add a section or chunk spatial index before evaluating high-frequency movement
  or entity rules.
- Replace the Fabric-native group-permission bridge with direct LuckPerms group
  lookup if Vantablack adopts LuckPerms as a hard dependency.
- Add recipient filtering for `receive-chat`.
- Add remaining environmental hooks for portals, crop trampling,
  lightning/weather, growth/decay, hoppers, pressure plates, and dragon/wither
  non-explosion block damage.
- Continue command parity work for upstream flags/options that Brigadier does
  not expose yet: `-w`, `-g`, `-n`, `-a`, list paging/filtering, `/rg select`,
  and `/rg toggle-bypass`.
- Add typed non-state flags and message-bearing greeting/farewell equivalents.
- Add migration tooling if Vantablack later chooses to reuse LGPL
  `worldguard-core` storage directly instead of the Fabric-native storage model.

## Reference Projects

- WorldGuard upstream: core behavior and storage reference, LGPL-3.0-or-later.
- WorldEdit Fabric: first-party Fabric command/lifecycle/network integration.
- Orbis: permissive multi-loader protection architecture reference.
- YAWP: useful Fabric mixin coverage reference, but AGPL-only, so reference
  concepts only.
- Leukocyte: compact Fabric protection-rule model, LGPL.
