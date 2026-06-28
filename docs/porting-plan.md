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
- Region priority, owners, members, parent inheritance, and explicit
  allow/deny/unset flags.
- Local `regions.properties` persistence with schema versioning.
- `/wg`, `/worldguard`, `/region`, and `/rg` admin commands.
- Optional WorldEdit Fabric import for cuboid and polygonal selections.
- Fabric event hooks for block break, block attack, block use/place attempts,
  item use, entity use, entity attack, explosions, fire, fluids, pistons,
  practical Enderman/Ravager grief, movement entry/exit, chat send, sleep, PvP,
  fall damage, invincibility, item drop, and item pickup.

## Next Parity Work

Full WorldGuard behavior is mostly event coverage and cache design:

- Add a section or chunk spatial index before evaluating high-frequency movement
  or entity rules.
- Add group resolution against the configured permission provider.
- Add teleport-specific movement flags: `exit-via-teleport`, `enderpearl`, and
  `chorus-fruit-teleport`.
- Add recipient filtering for `receive-chat`.
- Add remaining environmental hooks for portals, crop trampling,
  hanging-entity destruction, lightning/weather, growth/decay, hoppers,
  pressure plates, entity spawning, and dragon/wither non-explosion block
  damage.
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
