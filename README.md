# Veinminer

**Sneak, mine one ore, and the whole vein pops out in a chain, with any ore from any mod.**

Veinminer 2.0 is a server-side mod for **Minecraft Java 26.3, 26.2, 1.21.1 and 1.20.1** on **Fabric, Quilt, NeoForge and Forge**. Put it on the server and everyone who joins sees the chain, even with an unmodified game.

<!-- art (60 fps WebP showcase, banner, icon): release lane, see STATUS.md -->

## Features
- **A chain, not a blink.** The vein pops block by block from the ore you mined, faster and faster. Each block squashes and snaps, and its loot flies in an arc to land at your feet.
- **Any ore, no list.** An ore from a mod that tags `#c:ores/<name>` is a vein of that name. Tin next to zinc stays two veins.
- **Same loot as by hand.** Each block breaks as if you had mined it: Fortune, Silk Touch, XP and tool wear included. A vein never breaks your pickaxe.
- **Safe for servers.** Claims and protections are asked about every block. A crash mid-chain leaves the rest of the vein as ore. Machines and fake players never start a vein.
- **Cheap.** About 14 ms of server time for a whole vein of 64.

## How to use
1. Hold a pickaxe.
2. Sneak and mine an ore.

Creative mode never starts a vein. `/veinminer` turns it off or on for yourself.

## Settings
`config/veinminer.json` on the server is re-read while the game runs. You can set the biggest vein (`max_blocks`, default 64), whether sneaking is needed, where the loot lands, durability use, diagonal veins, the animation (off breaks the whole vein at once), the action-bar message, and a switch per ore group. Operators can use `/veinminer status`, `/veinminer set <key> <value>` and `/veinminer reload`.

Modpack makers can add or move ore groups with a data pack: `data/<namespace>/veinminer/group/<name>.json` with `{"blocks": "#<namespace>:<tag>"}`, and `#veinminer:never` removes blocks from every group.

## Add-ons and hooks
[Enchanted Veinminer](https://www.curseforge.com/minecraft/mc-mods/enchanted-veinminer) uses the data pack hooks, which 2.0 keeps (checked with a stand-in add-on; the real add-on is not yet re-tested on 2.0). The hooks (`#veinminer:api/cancel`, `#veinminer:api/loaded`) keep working, and mods get a Java hook, `VeinminerEvents.BEFORE_VEIN`.

## Installation
Put the jar in the `mods` folder of the server (or your singleplayer game). On Fabric and Quilt it needs Fabric API. Players can join with an unmodified game; a client install adds nothing.

**Conflicts.** FTB Ultimine, Liteminer, OreVein and Ore Excavation also mine veins. If one is installed, Veinminer warns the operators and the log. Use only one of them.
