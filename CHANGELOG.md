# Changelog

## 2.0.0 — 2026-11-05

Veinminer is now a mod. It replaces the data pack and keeps the same chain: the vein pops block by block from the ore you mined, faster and faster, and the loot flies in an arc to land at your feet. It runs on Minecraft **26.3, 26.2, 1.21.1 and 1.20.1**, with **Fabric** (needs Fabric API), **Quilt**, **NeoForge** and **Forge**. On 1.20.1 one jar serves Forge and NeoForge.

### Added
- **Any ore from any mod.** A block in `#c:ores/<name>` is a vein of that name (tin, zinc, lead, and so on). Ores without a subtag get a group of their own. Modded ores break exactly as if you mined each one yourself: their own drops, XP and events.
- **Data groups.** A data pack can add a group or move an ore into another one (`data/<namespace>/veinminer/group/<name>.json`), and `#veinminer:never` takes blocks out of every group. Ores with a block entity are never mined by a vein.
- **Claims and protections.** Every block's break is offered to the server's break events with your player, so claim mods refuse block by block. A vein across a claim border breaks only the allowed part. Fake players (machines, turtles) never start a vein.
- **Fair by construction.** Loot, XP and tool wear equal mining every block by hand with the same pickaxe, Fortune and Silk Touch included. A vein never breaks your pickaxe: it stops before the last point of durability.
- **Crash safety.** Nothing in the world changes until a block's turn, so a crash, a stopped server or a chunk unload leaves the rest of the vein as ore.
- **`/veinminer`** turns veining off or on for you (saved), `/veinminer status` shows the settings and the groups, and operators get `/veinminer set <key> <value>` and `/veinminer reload`.
- **Config file** `config/veinminer.json`, re-read while the game runs: `max_blocks`, `require_sneak`, `drops`, `durability`, `diagonal`, `animation`, `feedback`, `visit_cap` and a switch per group.
- **Java hooks** `VeinminerEvents` for mods; the data pack hooks `#veinminer:api/cancel` and `#veinminer:api/loaded` keep working (checked with a stand-in add-on; Enchanted Veinminer itself is not yet re-tested on 2.0). On 1.20.1 the function-tag cancel hook cannot run, the Java hook can.
- **Conflict warning** when FTB Ultimine, Liteminer, OreVein or Ore Excavation are installed too.
- **Lag guard:** while the server is overloaded, veins are halved until it recovers.
- **Cheap.** Starting a vein of 64 takes under a millisecond on the server, and a whole chain costs about 14 ms of server time (1.3.3: about 180).

### Changed
- The animation runs from the mod instead of command functions: the same timings, sounds, arcs and landing spots as 1.3.3. The chain's XP now arrives as one lot of orbs at the landing spot, and the loot of the block you mined joins the chain instead of waiting in the hole.
- Settings moved from the scoreboard menu to `config/veinminer.json`.

### Notes for 1.3.x users
- Remove the 1.3.x data pack or jar when you add 2.0. Don't run both.
- The first time a 1.3.x world loads, Veinminer imports your settings and each player's on/off choice, turns the old data pack off, and tells the operators once.
- Creative and adventure mode never start a vein, as before.
- The 1.3.x data pack zip and server plugin stay on 1.3.3 and get no more updates.
