# Changelog

## 2.1.0 — 2026-11-12

### Added
- **Settings screen.** All settings in one menu, no file editing: on Fabric through Mod Menu's configure button (Mod Menu is optional), on NeoForge and Forge through the mod list's Config button. It edits your single-player settings, which apply right away in your world and on a LAN world you host. On a server, its operators still use `/veinminer set`.
- **Dirt, sand, gravel and clay.** Sneak and dig with any shovel, modded ones too, and the whole patch comes out in the same chain. Dirt includes grass, podzol and mycelium. These four are off by default: turn them on in the settings screen or with `/veinminer set groups.dirt true`. A pickaxe never veins them, and suspicious sand and gravel are left alone.
- **Your own blocks.** Add any block, or a whole block tag such as `#minecraft:logs`, to your own list, and it veins too. Use the settings screen's "Add or remove blocks..." (search every block, modded ones included, or "Add the block I'm looking at"), or, for operators on a server, `/veinminer add` and `/veinminer remove`. Each entry veins with the right tool for it; blocks no tool is made for, like glass, vein with anything. Your list wins over the built-in switches, but blocks that hold items (chests and the like) never vein.
- Sand and gravel stay put while the chain runs, so they don't fall away before their turn. If the chain stops early, what's left falls as normal.
- **Data groups** take `"tool": "correct"` (any tool that is right for the block, such as a shovel or an axe) and `"default": false` (the group starts off).

### Changed
- "Pickaxe is too worn" now says "Tool is too worn".
- `/veinminer status` lists the groups that are off.

### Not yet tested
- "Add the block I'm looking at" and adding a `#tag` through the block picker (both work as commands and in the self-test).
- The Mod Menu and Config buttons pressed by hand anywhere but Fabric 26.3 (the screen itself passes its self-test on every loader).
- Your own blocks in multiplayer.
- On 1.21.1 and 1.20.1 the settings screen has no section headings and no notice when you open it on a server; on 1.20.1 your own blocks sit between Clay and the ores.

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
