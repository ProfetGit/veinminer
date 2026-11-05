# Veinminer 2.0 — PRD

Status: draft (2026-11-05). Mod id `veinminer`, package `io.github.profetgit.veinminer`. Tracker: `STATUS.md`.
Replaces the data pack in `packs/Veinminer` (1.3.3, frozen). That folder's `CLAUDE.md` ("Veinminer internals") and
`dev/gen.py` document the 1.3.x pipeline and the animation tables this port follows. Template for structure, config,
API, conflicts, migration and the loader split: `mods/Timber` (2.0.0).

## 1. Summary
- **Promise:** sneak, mine one ore, and the whole vein comes out, with any ore from any mod, no setup.
- **Moment:** the 1.3.3 chain unchanged: the vein pops block by block from the struck ore, accelerating, each block
  squashes, snaps and throws its loot in an arc to land at your feet. Now it works on a modded tin or zinc vein too.
- **Edge:**
  - Liteminer (606K) and Ore Excavation are instant breaks; OreVein reads `#c:ores` but only breaks.
  - Veinminer has the staged chain with real arcing loot, and works with the mod on the server only.
  - Modded ores need no list: a block in `#c:ores/<name>` is a vein of that name.
- **Platform:**
  - 26.3, 26.2, 1.21.1 and 1.20.1, each on Fabric, Quilt, NeoForge and Forge (user, 2026-11-05).
  - A combination the loader does not offer (no Quilt or NeoForge build for a version) is reported at M5, not faked.
  One jar per loader and version, server side only; a client install adds nothing.

## 2. Changes from the draft
The draft is the request "add modded block support to Veinminer" (`docs/draft-v1.md`). Defects found and fixed:
- **Data pack can't detect an unknown block.** The pack finds a break from one `minecraft.mined:<block>` objective per
  ore; the objective cannot be created for a block that is not installed (unknown criterion fails the function parse).
  Fix: a Java mod hooks the break event, so any block is seen.
- **Data pack can't break the way a mod expects.** `loot spawn` and `setblock` skip a modded ore's own drops, XP,
  block entity and events. Fix: the mod breaks through the real player break path.
- **"Modded blocks" is wider than ores.** Scope is ores only; other blocks stay out (1.1, §15).
- **"Like Timber" meant the 2.0 mod, not the pack.** The pack's only trace of mod support is an optional entry in a
  tag. Timber's modded trees live in `mods/Timber`; this follows that route.

## 3. Decisions
| Topic | Choice | Date |
|---|---|---|
| Form | New Java mod `mods/Veinminer` (not a data pack update) | 2026-11-05 |
| Variant | Sharpened: Java core, server-only works, chain animation kept as displays, modded ores via `#c:ores/*`, per-group toggles, claims, live config | 2026-11-05 |
| Scope | Ores only (modded ores in vanilla groups plus new groups such as tin, zinc, lead) | 2026-11-05 |
| Versions on day one | 26.3, 26.2, 1.21.1, 1.20.1 on Fabric, Quilt, NeoForge and Forge (wider than Timber's matrix; user asked for all) | 2026-11-05 |
| Build order | User said start all milestones: M0 to M6 are built in order, M7 is prepared but nothing is uploaded without a separate go | 2026-11-05 |

My calls, open to veto:
- Mod id `veinminer` and CurseForge project 1709829 are kept, so modpacks update in place.
- Data pack zip, plugin jar and mod-wrapped pack stay on 1.3.3 and get no more updates (as Timber did at 2.0).
- No new registry entries (blocks, items, entities, sounds, particles): vanilla clients can always join.
- The group of an ore is its `#c:ores/<name>` subtag, so stone, deepslate and netherrack variants are one vein, as in
  1.3.3. Vanilla nether gold ore is in `c:ores/gold` and joins gold; the two never touch in practice.
- A block in `#c:ores` but in no subtag is its own group, named by its id with stone-type prefixes removed.
- Blocks with a block entity are never veined (a machine tagged as an ore must not be mined by accident).
- The animation is on by default and holds nothing back: the vein only changes at each block's turn.
- If the chain's pickaxe is swapped for another item mid-chain, the chain ends and the rest stays as ore.
- Creative and adventure mode never vein (as 1.3.x).
- Messages use `translatableWithFallback`, so vanilla clients read English.
- The join hint stays off by default (1.3.3 rule) and is not rebuilt.

## 4. Pillars
1. **Same feel.** The chain's timeline equals 1.3.3: ring delays, squash/stretch keys, arcs, sounds, per tick, on a
   64-coal vein and an 80-coal blob. Measured with the lab probe (`packs/Veinminer/dev/lab/chain.py` curves, A/B on the
   same vein); the user's verdict at M2.
2. **Any ore, no list.** An ore from a mod that tags `#c:ores/<name>` veins at once with its vanilla break result.
   Checked by scene `modded` with fixture ores for three groups, and by hand with one real mod (§12).
3. **Nothing lost, nothing griefed.**
   - Loot, XP and tool wear equal breaking every block by hand with the same pickaxe (scene `fair`).
   - Claims, `#veinminer:api/cancel` and other mods' break events are asked for every block (scene `claims`).
   - A crash or chunk unload mid-chain leaves the unbroken blocks as ore (scene `crash`).
4. **Cheap.** Mining tick for a 64-block vein ≤ 3 ms (1.3.3: 12.8); chain tick ≤ 0.5 ms (1.3.3: 9.4); the whole chain
   ≤ 40 ms (1.3.3: 212). All guesses, measured at M1/M2 in scene `perf`.

## 5. Features (2.0)
### 5.1 Trigger and gates
- The mod hooks the player's block break on the server, after the vanilla break (the struck block is already gone and
  its drops exist).
- The vein starts only when all hold, checked in this order (cheap first):
  1. survival mode and a real `ServerPlayer`, not a fake player (§7);
  2. the player's vein toggle is on (§5.7) and the struck block's group is on in the config;
  3. sneaking when `require_sneak` (default on);
  4. the held item is in `#minecraft:pickaxes` or `#c:tools/pickaxe`;
  5. the struck block belongs to a group (§5.2) and the pickaxe was correct for drops on it;
  6. `#veinminer:api/cancel` and the Java event `VeinminerEvents.BEFORE_VEIN` do not veto;
  7. no vein or chain of this player is running from the same block.
- A vein never starts from its own breaks: the mod's breaks carry a re-entrancy flag.
- The struck block can be at any position of the vein; the scan starts from it.

### 5.2 Groups and the scan
- A group is a name. Resolution of a block, first match wins:
  1. a data group (§5.3) whose `blocks` tag holds it;
  2. a `#c:ores/<name>` subtag: group `<name>` (`coal`, `iron`, `copper`, `gold`, `redstone`, `lapis`, `diamond`,
     `emerald`, `quartz`, `netherite_scrap`, and any modded name such as `tin`);
  3. the vanilla ore tags (`#minecraft:coal_ores` … `minecraft:ancient_debris`), so vanilla works when no common tags
     exist on a loader;
  4. `#c:ores` without a subtag: its own group, `<namespace>:<path>` with `deepslate_`, `nether_`, `end_`,
     `blackstone_`, `netherrack_` and a trailing `_ore` removed.
- The group is cached per block (invalidated on every data/tag reload).
- Scan: BFS from the struck cell over the 6 faces, plus the 20 diagonals when `diagonal` (default on), through cells
  whose block resolves to the same group. Cap `max_blocks` mined (default 64, the pack's value), and a visit cap of 4096
  cells so a huge connected ore body cannot stall a tick.
- The scan reads only loaded chunks; an unloaded neighbour ends that branch.
- Cells with a block entity are skipped and not entered.
- Order is BFS distance from the struck block (ties: nearer to the player), which is also the chain order.
- A world with no `#c:ores` at all (a loader without common tags): vanilla groups still work (rule 3), modded ores
  need the tag `data/veinminer/tags/block/groups/<name>.json` from a pack author (§5.3).

### 5.3 Data-driven groups
- `data/<ns>/veinminer/group/<id>.json`: `{"blocks": "#ns:tag", "max_blocks": 64?, "xp_toggle": null?}`. The id is the
  group name; same name as a built-in group extends it (the tag is added).
- Modpack makers add a group, or move an ore into another, with a data pack; no code. Reload listener like Timber's
  tree types.
- An empty tag is a group that never triggers (not an error). A malformed file logs once and is ignored.
- `data/veinminer/veinminer/group/never.json`: an empty-by-default tag `#veinminer:never` removes blocks from every group.

### 5.4 Breaking, tools and loot
- Each block is broken through the real player break path (`ServerPlayerGameMode.destroyBlock` on 26.3; the
  equivalent per loader), so the block's own drops, XP, block-state side effects and other mods' break events run and
  claims are asked per block.
- Tool: the block must be correct for the held pickaxe (`hasCorrectToolForDrops`), so wrong-tier and modded tiers
  follow the pickaxe's own rules; a block that fails is skipped, not removed.
- Fortune, Silk Touch, Unbreaking and modded enchantments act per block through vanilla.
- Durability: 1 per extra block through `hurtAndBreak` at the block's turn (Unbreaking applies; `durability` can be
  turned off). A vein never breaks the pickaxe: when the next block would use the last durability, the chain ends and
  the rest stays as ore.
- Hunger and stats: exhaustion and `mined` stats count per block, as by hand.
- `gamerule block_drops` off: blocks break without loot, as by hand.
- XP: whatever the break path pays (vanilla ores, modded ores). XP orbs spawn at the block's landing spot.
- Loot placement `drops`: 0 = lands in front of the player's feet along the chain arc (default), 1 = at the player's
  feet with pickup delay 0.
- Inventory full: nothing special; items land on the floor.

### 5.5 Chain animation
- Same as 1.3.3 (`packs/Veinminer/CLAUDE.md`), driven from Java instead of macros:
  - blocks stay real until their turn; the delay is `1 + K·(d^0.6 − 1)`, K from the vein size (10 / 5 / 2.5), max 40 ticks;
  - a stand-in `block_display` is summoned one tick before its block turns to air, in the lit neighbour cell facing the
    miner, 0.6 % oversized, then the block turns to air;
  - keys: 3+2-tick squash, 2-tick hold, 1-tick stretch, 2-tick vanish; per ring crumbs, one `small_gust` per block, a
    break sound (0.85 → ≤ 1.5 pitch) and a chicken-egg plop (≤ 2.0);
  - loot is rolled at the turn, shown as item displays flying a 12-tick arc to the landing spot (up to 3 visible per turn
    tick, 12 per chain, the rest land with the last visible), swapped for the real item with a 0.12 hop.
- The break itself (§5.4) happens at the block's turn, not at the start; durability, tier and hooks are decided there.
- The turn is skipped if the block is no longer in the group (replaced by someone else), and its stand-in is removed.
- A running chain's blocks cannot start or join another vein.
- `animation` 0: the whole vein breaks in the start tick, loot at the landing spot, no displays.
- Displays are never saved (mixin as in Timber); they ride the existing entity packets, so vanilla clients draw them.
- Smoothness rules from `.claude/rules/datapack-engine-facts.md` apply unchanged (position lerps for per-tick motion,
  long keys for straight moves, `teleport_duration` set at summon).
- Whole-vein end: the controller ends at the last ring + 26 ticks; leftovers land immediately (`flush`).

### 5.6 Protection
- Every block's break is offered to the loader's break event with the real player, so claim mods (FTB Chunks, Open Parties
  and Claims, GriefPrevention-style) refuse per block. A refused block is skipped and the chain goes on.
- Spawn protection and `mayBuild` are asked per block through the same path.
- A claim boundary through a vein: the allowed part breaks, the rest stays.

### 5.7 Config and commands
- `config/veinminer.json`, live-reloaded (mtime poll + `Tune.DEFAULTS`), written with comments on first run.
- Keys (defaults = 1.3.3): `max_blocks` 64, `require_sneak` true, `drops` 0, `durability` true, `diagonal` true,
  `animation` true, `feedback` true, `welcome` false, `groups` {name: true} (new groups default on), `tools` (extra item
  tag), `visit_cap` 4096 (guess, tune at M1).
- `/veinminer` toggles the caller's own veining (permission level 0, remembered per player); `/veinminer status`
  shows the effective settings; `/veinminer set <key> <value>` and `/veinminer reload` need level 2.
- The 1.3.3 clickable menu is not rebuilt; `status` lists keys as clickable suggestions.
- `/veinminer tune <key> <value>` (dev) edits the live knobs of §5.5 (timing K, arc height, squash) without a build.

### 5.8 Migrating from 1.3.x
- On the first start with the old pack enabled in a world, the mod disables the data pack's `load`/`tick` tag entries by
  running its `uninstall` function and tells ops in chat and the log. It does not delete the zip.
- It imports the old `veinminer.config` scores (`max_blocks`, `require_sneak`, `drops`, `durability`, `diagonal`,
  `animation`, `feedback`, `ore.*` toggles) into `config/veinminer.json` once, and the per-player `veinminer.off`
  scores into the opt-out saved data. A chain mid-flight is flushed by the pack's own `anim/flush`.
- A world that never had the pack skips all of it.

### 5.9 Feedback
- With `feedback` on, the action bar shows `Veined N <group>` once the chain ends, and `Pickaxe is too worn` when the
  chain stopped for durability. Group names are translated with the vanilla `block.<ns>.<name>` of the first ore.
- No chat spam, no sound beyond §5.5.

## 6. Controls
| Input | Context | Action |
|---|---|---|
| Sneak (vanilla key) | Mining an ore with a pickaxe | Starts the vein |
| `/veinminer` | Any player | Toggles own veining |
| `/veinminer status` | Any player | Shows settings |
| `/veinminer set|reload` | Level 2 | Edits the config |

No new key binding, so the test profile's key collisions do not matter.

## 7. Multiplayer and security
- The server decides everything. Clients need nothing and never send a packet to Veinminer.
- Fake players (machines, turtles, deployers) never trigger a vein: the rule is `player.getClass() == ServerPlayer`
  plus the loader's fake-player check. A modded miner cannot mine 64 blocks by accident.
- Rate and reach caps: one running vein per player; a chain stays within 16 blocks of its struck block's chunk radius;
  a second vein by the same player while a chain runs ends the first at its next turn only if it started from a block of
  that chain, otherwise both run (max 3 per player, guess, tune at M4).
- Lag guard: if the server tick is over 100 ms for 10 ticks, `max_blocks` is halved until it recovers.
- All user text goes through components; no command ever interprets stored text.

## 8. Data and persistence
- Saved data `veinminer_players` (per overworld data storage): UUIDs that turned veining off. Survives uninstall as a
  tiny orphan file; removing the file resets everyone to on.
- Running chains are not saved. On stop or crash, unbroken blocks stay as ore (§5.5): nothing in the world changes until
  a block's turn, and the turn is atomic (stand-in, break, loot in one tick).
- Displays are never saved; a sweep every 100 ticks kills tagged displays with no controller (as Timber's `sweep`).
- Config is the only user file; an invalid value logs once and the default applies.

## 9. Compatibility
| Mod | Expected | Checked by | Milestone |
|---|---|---|---|
| Enchanted Veinminer (add-on) | `#veinminer:api/cancel` and `loaded` still work; its pickaxe gate cancels the vein | scene `hooks` with the add-on pack | M4 |
| A mod with ores in `#c:ores/<name>` (Create zinc, Mekanism, Thermal, Immersive Engineering, AllTheOres) | veins with vanilla-equal loot | hand test with one real mod if a 26.x or 1.21.1 build exists; fixtures otherwise | M3 |
| FTB Ultimine, Liteminer, OreVein, Ore Excavation | double-fire risk; log a warning at start | `Conflicts` (Timber pattern), scene `conflict` | M4 |
| Claims (FTB Chunks, Open Parties and Claims) | per-block refusal | fixture claim listener in scene `claims`; real mod by hand if available | M4 |
| Tinkers-style tools with their own durability | `hurtAndBreak` honoured | fixture tool item in scene `fair` | M4 |
| Fortune / lucky modded enchantments | per block through loot context | scene `fair` | M1 |
| Sodium, Iris, Fresh Animations | displays look like blocks (lit by their cell) | `mt.py` render sheet | M2 |
| Timber 2.0, Snappy Blocks | no shared state; both hook the break event | boot together in the play profile | M4 |

## 10. Performance
- Budget: mining tick ≤ 3 ms for 64 blocks; chain tick ≤ 0.5 ms; whole chain ≤ 40 ms; no allocation per tick when idle
  (the break hook is a few comparisons for non-ores, ≤ 20 µs). Guesses, tuned at M2/M6.
- Idle server: 0 per-tick work when no chain runs.
- Worst case: 80-coal blob against the 64 cap, and a modded 4096-cell ore body (visit cap) at one spot.
- Measurement scene `perf`, interleaved A/B against the 1.3.3 pack (`packs/Veinminer/dev/lab/perfab.py` method).

## 11. Technical design
- Template: `mods/Timber` (Stonecutter, `build.<loader>.gradle.kts`, `platform/`, `server/Hooks`, `Api`, `Conflicts`,
  `Migrate`, `TreeTypes` reload listener → `Groups`), scaffold with `python3 tools/Studio/new_mod.py Veinminer`.
- Packages: `server/` (`Hooks` one-order server events, `Vein` scan, `Groups` resolver and cache, `Chain` timeline and
  controller, `Fx` displays, `Players` opt-out saved data, `Api`, `Conflicts`, `Migrate`, `Config`), `core/` (pure
  delay tables and keys ported from `dev/gen.py`, unit-tested), `platform/` (loader break event, fake-player check,
  config dir, mod list), `mixin/` (display never-saved, as Timber).
- Sides: server logic only; a dedicated-server jar works; the client has no code.
- Tags the mod ships: `data/veinminer/tags/item/tools.json` (`#minecraft:pickaxes`, optional `#c:tools/pickaxe`),
  `tags/block/hollow.json` (as the pack), `tags/function/api/{cancel,loaded}` consumers.
- Tag entries naming blocks or tags newer than the target must be `{"id": ..., "required": false}` (one missing id drops
  the whole tag).
- The Java event `VeinminerEvents` (before-vein veto, after-vein) mirrors Timber's `TimberEvents`.
- Vanilla paths to check at M1 with `wb src` (not checked yet): the break event order on each loader, the exact
  `ServerPlayerGameMode.destroyBlock` (26.3 `ServerPlayerGameMode.java:286`) side effects, and whether `#c:ores` and
  `#c:tools/pickaxe` exist at runtime on Fabric, NeoForge and Forge.
- The animation tables are generated Java constants (`dev/gen.py` kept as the source, rerun after changing a table).

## 12. Risks and spikes
| Risk | Spike | Milestone | Fallback |
|---|---|---|---|
| The real break path cannot be called for 64 blocks without recursion or event side effects | Scene `fair`: 64 silk-touched coal in one tick through the real path, count equals 64 | M1 | `Level.destroyBlock` with `LootParams` rolled by hand, claims asked explicitly |
| `#c:ores` is missing or incomplete on some loader | List the tag's size at boot on each loader | M1 (26.3 Fabric), M5 (rest) | Vanilla tags (§5.2 rule 3) plus a bundled `veinminer` mirror tag for the common names |
| Modded ores have block entities or custom break (tags but not plain) | Fixture ore with a block entity and one with a custom `playerDestroy` | M3 | Skip block-entity cells; honour custom `playerDestroy` through the real path |
| No real mod with ores exists for 26.3 | Search CurseForge for a 26.x ore mod; otherwise test on 1.21.1 after M5 | M3 | Fixtures prove the mechanism; real mod on 1.21.1 |
| The chain's displays look different from the pack (Java timing off by a tick) | Probe `chain.py` curves vs 1.3.3, same vein | M2 | Port macro order literally |
| Stonecutter build for four loaders and four versions is slow to settle | Timber already solved it; copy | M5 | Ship Fabric first, others as they pass |

## 13. Milestones
Each milestone: build green, version bump, installed in the Fabric 26.3 profile, committed.

| | Milestone | Exit check |
|---|---|---|
| M0 | Scaffold from Timber's template | builds; `mt.py Veinminer quick` boots on a dedicated server |
| M1 | Spike + ugly core: break hook, gates, scan, real-path break, groups from `#c:ores/*`, no animation, config | **go:** scene `fair` (64 silk coal = 64 items exactly; Fortune III mean within 3 % of hand-breaking over 500 veins; wear equal); scene `modded` (fixture ores in `c:ores/tin`, `zinc`, an untagged-subtag one; each veins). **stop:** the real path cannot break more than a handful without breaking events, or `#c:ores` is absent on Fabric 26.3: switch to the fallback column |
| M2 | Chain animation ported (stand-ins, keys, arcs, sounds, XP) | scene `chain` named asserts on delays and landing; probe curves vs 1.3.3 within 1 tick and 0.02 blocks; `wb img flicker` clean at each turn; **feel check** below |
| M3 | Breadth: data groups, never tag, block-entity skip, fixture block entity and custom break, one real mod by hand | scenes `groups`, `blockentity`; user OK to download the real mod |
| M4 | Server-grade: claims, fake players, hooks, conflicts, migration, crash, config reload, lag guard, opt-out saved data | scenes `claims`, `fake`, `hooks`, `conflict`, `migrate` (world saved with 1.3.3 and old config), `crash`, `config_reload`; `mp` with 2 vanilla clients, 0 kicks |
| M5 | Matrix: 26.2, 1.21.1, 1.20.1 on Fabric, Quilt, NeoForge and Forge (every combination the loader offers) | `port.py` one-pass compile; smoke boot per target; `fair` and `modded` per target |
| M6 | Polish and perf | `perf` budgets of §10 met; `polish` skill pass; user verdict on the chain |
| M7 | Release lane | full tier, matrix, mp, flicker, stills; art as 60 fps WebP; CurseForge 1709829 update and description (release skill) |

Feel check (M2, ~3 minutes, Fabric 26.3 profile, any world with a `/give` pickaxe and `/setblock` coal blobs):
1. Sneak and mine one block of a 30-coal blob: does the chain read as a chain, or as a blur?
2. Mine a vein with Fortune III: does the loot land where you stand, one pile per ring or scattered?
3. Do the same on a modded ore (fixture tin): is it indistinguishable from coal besides its texture?
4. In a torch-lit cave, watch the stand-ins: any colour change when a block starts to squash?
5. Listen to a 64-block vein: does the pitch climb to the end without going silent?
Judge: pacing, landing, sound. Tunables: `/veinminer tune delay_k|arc|squash <value>` (no rebuild).

## 14. Release
- Version 2.0.0 on CurseForge 1709829 (same project); one file per loader and version, tagged only with what was
  tested. Description from the description kit (Ko-fi, BisectHosting, promo tiles). The twin data pack project 1712827
  gets a "2.0 is a mod now" note.
- AI-generated content disclosure on every page. Changelog entry lists: modded ores, config file, `/veinminer`.
- The 1.3.x line is frozen, not removed.

## 15. Roadmap
- 2.1: any block (logs, stone, sand) via data groups with their own tool check; vein preview outline for modded
  clients; `shape` modes (tunnel, 3×3).
- 2.2: client mesh for modded viewers (Timber's drawing path), no display cap.

## 16. Cut
- Aim preview and shape modes (vs FTB Ultimine): not the promise; 2.1 at most.
- Non-ore blocks: scope decision; ores only.
- The 1.3.3 clickable settings menu: replaced by `/veinminer status` and the config file.
- A client install with extra behaviour: not needed for the promise.

## 17. Prior art
Checked 2026-11-05 by web search (download counts not all visible):
- Liteminer: multiloader, 605.8K downloads on CurseForge; instant break; excluded/whitelist tags.
- Ore Excavation: any tool-based action, key-driven, instant.
- OreVein: Fabric/NeoForge, default `c:ores`, adds mods' tags by command; instant.
- Veinminer - Highly Configurable, Smart VeinMiner, VeinMiner Plus, Ore Vein Miner: instant breaks, varying loaders.
- FTB Ultimine: shape modes, key-driven, the reference for preview.
- None is a staged chain with arcing loot or works with the mod only on the server.
Name/slug: `veinminer` is the existing project (kept). Not re-checked on Modrinth (withheld).

## 18. Coverage
| Dimension | Answered in |
|---|---|
| Sides | §1, §11 (server only) |
| Multiplayer | §7 |
| Persistence | §8 |
| Data safety | §4, §5.4, §8 |
| Uninstall | §8 (orphan file only), §5.8 |
| Updates | §5.8, §14 |
| Config | §5.7 |
| Performance | §4, §10 |
| Compatibility | §9 |
| Versions | §1, M5 |
| Controls | §6 |
| Discoverability | §5.9, §3 (hint off) |
| Accessibility | N/A: no flashing; `animation` off in §5.5 is the option |
| Localisation | §3 (`translatableWithFallback`) |
| Game modes | §5.1 (survival only) |
| Dimensions | §5.2 (any dimension; tags decide) |
| Command bypass | N/A: `/setblock`, `/fill`, `/clone` raise no player break |

## 19. Open questions
- Does a real mod with a 26.x or 1.21.1 build exist whose ores are tagged `#c:ores/<name>`? Default: fixtures plus one
  real mod after M5 on 1.21.1, with your OK for the download.
- Should the 1.3.3 mod-wrapped pack jar also be retired from CurseForge listings? Default: leave listed, note added.
- Is the lag guard (halve `max_blocks`) acceptable, or should an overloaded server just refuse veins? Default: halve.
