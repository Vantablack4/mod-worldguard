# WorldGuard Fabric Porting Plan

## Upstream Shape

`EngineHub/WorldGuard` does not ship a Fabric module. The reusable upstream
pieces live in `worldguard-core`: region containers, protected region types,
flag registries, query logic, storage, sessions, and command support. The
platform integration is concentrated in `worldguard-bukkit`, where Bukkit/Paper
listeners translate server events into WorldGuard's internal event model.

`EngineHub/WorldEdit` does ship first-party Fabric support. Treat WorldEdit as
the right selection/editing integration point, but keep the protection runtime
independent so Vantablack can boot without a hard WorldEdit coupling unless we
choose to add selection import.

## Current MVP

The first Vantablack Fabric slice is deliberately smaller than upstream
WorldGuard:

- Server-only Fabric mod metadata.
- Cuboid region model with per-dimension bounds.
- Region priority, members, and explicit allow/deny/unset flags.
- Local `regions.properties` persistence.
- `/wg` and `/worldguard` admin commands.
- Fabric event hooks for block break, block attack, block use/place attempts,
  item use, entity use, and entity attack.

## Next Parity Work

Full WorldGuard behavior is mostly event coverage and cache design:

- Add a section or chunk spatial index before evaluating high-frequency movement
  or entity rules.
- Add a WorldEdit Fabric bridge for `//wand` or selected-region import.
- Add mixins for explosions, fire spread, fluid flow, pistons, hoppers,
  pressure plates, crop trampling, portals, item pickup/drop, mob griefing, and
  entity spawning.
- Add more flags: `chest-access`, `pvp`, `entry`, `exit`, `tnt`, `creeper-
  explosion`, `fire-spread`, `water-flow`, `lava-flow`, `mob-spawning`, and
  `mob-grief`.
- Add migration tooling only after deciding whether to reuse LGPL
  `worldguard-core` or keep a Vantablack-native storage model.

## Reference Projects

- WorldGuard upstream: core behavior and storage reference, LGPL-3.0-or-later.
- WorldEdit Fabric: first-party Fabric command/lifecycle/network integration.
- Orbis: permissive multi-loader protection architecture reference.
- YAWP: useful Fabric mixin coverage reference, but AGPL-only, so reference
  concepts only.
- Leukocyte: compact Fabric protection-rule model, LGPL.
