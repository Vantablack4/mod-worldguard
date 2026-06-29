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
- Region priority, owners, members, owner/member groups, parent inheritance,
  explicit allow/deny/unset flags, per-flag region groups, and typed non-state
  flag storage.
- Local `regions.properties` persistence with schema versioning.
- `/wg`, `/worldguard`, `/region`, and `/rg` admin commands.
- Optional WorldEdit Fabric import and export for cuboid and polygonal
  selections.
- Fabric event hooks for block break, block attack, block use/place attempts,
  item use, bucket place/pickup internals, chest/container access on blocks and
  entities, double-chest endpoint checks, entity use, direct ride interactions,
  entity attack, non-player entity damage, potion/firework/wind-charge projectile
  damage, mob spawning, explosions, TNT/lighter actions, anvil, bed, respawn
  anchor, and big-dripleaf interactions, fire, fluids, pistons, practical
  Enderman/Ravager grief,
  Ender Dragon and Wither block damage,
  movement entry/exit, portal and teleport entry/exit, ender pearl and chorus
  use, chat send/receive filtering, sleep, PvP, fall damage, natural health
  regeneration, natural hunger drain, invincibility, item drop, item
  pickup, vehicle and entity placement/destruction, item frame rotation,
  trampling, redstone triggers, hoppers, lightning,
  snow/ice weather, melt, growth, sapling/tree feature growth, spread, decay,
  and fade mutations.
- Typed `/rg flag` values and `-g` region-group targeting for state and typed
  flags.
- Upstream-style build action semantics for checked flag sets: specific action
  allows can pass through a broader `build deny`, while specific denies still
  win.
- Runtime typed flag effects for greeting/farewell messages, custom deny
  messages, blocked/allowed player commands, game mode, heal/feed, deny-spawn,
  player-scoped weather/time locks, respawn handling for the spawn location
  flag, and region teleport/spawn/teleport-message command targets.

## Next Parity Work

Full WorldGuard behavior is mostly event coverage and cache design:

- Add a section or chunk spatial index before evaluating high-frequency movement
  or entity rules.
- Replace the Fabric-native group-permission bridge with direct LuckPerms group
  lookup if Vantablack adopts LuckPerms as a hard dependency.
- Continue command parity work for remaining upstream flags/options, including
  `/region info -w/-u/-s` forms and remaining explicit `-w` world-targeted
  mutator forms.
- Add remaining high-friction event parity where Fabric needs targeted mixins:
  dispenser/dropper synthetic item/block actions, lectern book-take protection,
  entity-triggered dripleaf tilt, full non-damaging potion-effect paths, and
  mount/dismount edge cases beyond direct ride use.
- Continue command output parity for Bukkit WorldGuard's richer text/clickable
  pagination and exact formatting.
- Add migration tooling if Vantablack later chooses to reuse LGPL
  `worldguard-core` storage directly instead of the Fabric-native storage model.

## Reference Projects

- WorldGuard upstream: core behavior and storage reference, LGPL-3.0-or-later.
- WorldEdit Fabric: first-party Fabric command/lifecycle/network integration.
- Orbis: permissive multi-loader protection architecture reference.
- YAWP: useful Fabric mixin coverage reference, but AGPL-only, so reference
  concepts only.
- Leukocyte: compact Fabric protection-rule model, LGPL.
