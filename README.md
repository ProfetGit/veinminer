![Veinminer](https://raw.githubusercontent.com/ProfetGit/veinminer/main/docs/banner.gif)

**Sneak, mine one ore, and the whole vein comes out.**

Veinminer is a lightweight, fully vanilla data pack for **Minecraft Java 26.2 and 26.3**. It installs as a single zip and runs only on the server, so players join with an unmodified game. It works in singleplayer, over LAN, and on dedicated servers.

## Why this Veinminer?

**Real vanilla drops, not a simulation.** Every extra block is mined with its own loot table and the pickaxe in your hand. Fortune, Silk Touch and XP behave exactly as they do in vanilla, because the game itself calculates them. Data packs that change ore drops keep working.

**Fair by design.** Veinmining is a convenience, not a cheat. A stone pickaxe still can't take a diamond vein. Each extra block costs durability, and Unbreaking works as normal. Veinminer stops before your pickaxe would break, leaving it at 1 durability. It never runs in Creative mode, and it follows the `block_drops` game rule.

**It only touches ores.** Veinminer reacts to vanilla ore blocks and nothing else. It will never chain-break stone, logs, or the house you just built.

**One zip, nothing else.** No mod loader, no companion resource pack, no client install and no experimental features. Drop it in, run `/reload`, and it works.

**Built to stay out of the way.** When nobody is mining, it only checks a few scoreboards and looks for running animations each tick. A 64-block vein costs about 4 ms in the tick you break it, then about 3 ms per tick while its animation plays, measured on a real server.

**Tested, not hoped.** Every release is checked by 94 automated tests on real 26.2 and 26.3 servers. A simulated player mines actual veins and checks drops, durability, tool tiers, enchantments and every setting.

**Server and modpack friendly.** Run it on any server, including monetized ones, and include it in any modpack with credit. See [License](#license).

## Features

- **The whole vein in one break.** Every connected ore of the same type is mined, including ores that only touch at an edge or a corner. Stone and deepslate variants count as the same vein.
- **A chain reaction you can watch.** The vein breaks block by block, spreading out from the ore you hit. It starts slow and speeds up: each block squashes, holds, and pops, with a crack that climbs in pitch ring by ring. Every popped block throws its own loot, which arcs out and lands in front of the vein or flies to you. The blocks stay normal blocks until their turn. Turn it off and the whole vein breaks at once.
- **Enchantments apply.** Fortune multiplies drops and Silk Touch gives you the ore blocks, on every block of the vein.
- **Tool tier matters.** Each ore needs the same pickaxe it needs in vanilla. Ancient debris still requires diamond or netherite.
- **Durability is used fairly.** Each extra block costs 1 durability, reduced by Unbreaking as usual. Unbreakable tools aren't affected.
- **Drops are gathered.** Items and XP land in front of the block you mined, or at your feet if you prefer. Nothing gets stuck in the walls.
- **Vanilla XP.** You get the same XP per ore as vanilla, and none with Silk Touch.
- **Players choose for themselves.** Anyone can turn Veinminer off or on with `/trigger veinminer`. No operator is needed.
- **A clickable settings menu.** Operators can change every setting from chat. Settings survive `/reload` and restarts.
- **Clean uninstall.** One command removes every scoreboard and all stored data.
- **Add-on support.** Other data packs can add their own rules on top. See [Add-ons](#add-ons).

## How to use

1. Hold a pickaxe.
2. **Sneak.**
3. Mine an ore.

A message above the hotbar tells you how many blocks were mined.

## Supported ores

| Vein | Blocks | Minimum pickaxe |
|---|---|---|
| Coal | coal ore, deepslate coal ore | wood |
| Copper | copper ore, deepslate copper ore | stone |
| Iron | iron ore, deepslate iron ore | stone |
| Lapis | lapis ore, deepslate lapis ore | stone |
| Gold | gold ore, deepslate gold ore | iron |
| Redstone | redstone ore, deepslate redstone ore | iron |
| Diamond | diamond ore, deepslate diamond ore | iron |
| Emerald | emerald ore, deepslate emerald ore | iron |
| Nether Gold | nether gold ore | wood |
| Quartz | nether quartz ore | wood |
| Ancient Debris | ancient debris | diamond |

Different ore types never mix. Mining iron won't take the coal next to it.

## Commands

| Command | Who | What it does |
|---|---|---|
| `/trigger veinminer` | everyone | Turns Veinminer off or on for yourself |
| `/trigger veinminer set 2` or `set 3` | everyone | Forces it on (`2`) or off (`3`) |
| `/function veinminer:settings` | operators | Opens the clickable settings menu |
| `/function veinminer:uninstall` | operators | Removes all Veinminer scoreboards and data |

## Settings

Open the menu with `/function veinminer:settings` and click to change a setting.

| Setting | Default | Options |
|---|---|---|
| Max blocks per vein | 64 | Menu: 16, 32, 64, 128, 256. Any value from 1 to 512 by command. |
| Require sneaking | ON | OFF means you veinmine whenever you break an ore with a pickaxe |
| Drops | At mined block | At mined block / At player |
| Chain animation | ON | OFF means the whole vein breaks at once and the drops appear right away, like 1.1.0 |
| Use tool durability | ON | OFF means extra blocks are free |
| Diagonal connections | ON | OFF means only face-touching ores count as one vein |
| Action bar message | ON | Shows how many blocks were mined |
| Join hint | ON | Explains Veinminer once to each new player |
| Ores | all ON | Turn each ore type on or off |

Every setting can also be changed by command, for example:
`/scoreboard players set #max_blocks veinminer.config 128`

Setting names: `#max_blocks`, `#require_sneak`, `#drops`, `#animation`, `#durability`, `#diagonal`, `#feedback`, `#welcome`, `#ore.coal`, `#ore.copper`, `#ore.iron`, `#ore.gold`, `#ore.redstone`, `#ore.lapis`, `#ore.diamond`, `#ore.emerald`, `#ore.nether_gold`, `#ore.quartz`, `#ore.ancient_debris`. For on/off settings, 1 is on and 0 is off.

## Add-ons

**[Enchanted Veinminer](https://modrinth.com/datapack/enchanted-veinminer)** makes veinmining depend on a new **Veinminer** enchantment. You find it like any other pickaxe enchantment: at the enchanting table, from librarians, or in loot. Pickaxes without it mine one block at a time. It needs Veinminer 1.1.0 or newer.

<details>
<summary>For data pack authors</summary>

Veinminer 1.1.0 and newer offer these hooks. They are safe to use when Veinminer isn't installed, because a tag your pack adds to is simply never called.

- `#veinminer:api/cancel` (function tag): runs as and at the player just before a vein is mined, after Veinminer's own checks (sneaking, pickaxe, ore type, game mode). Do `return 1` to cancel the vein. The block the player broke still breaks normally. To allow the vein, **don't return at all**: the first function in the tag that returns decides, so a `return 0` or `return fail` would skip the add-ons after yours.
- `#veinminer:api/loaded` (function tag): runs at the end of Veinminer's load function, every load and `/reload`. Use it to check that Veinminer is present.
- `storage veinminer:meta requires` (list of text components): cleared on every load, just before `#veinminer:api/loaded` runs. Append a sentence there, such as `{text:"Your pickaxe needs X. ",color:"gray"}`, and Veinminer shows it in the join hint and the settings menu.
- `storage veinminer:meta version_id` (int): `major × 10000 + minor × 100 + patch`, for example `10100` for 1.1.0.

</details>

## Installation

**Singleplayer**
- New world: under **More → Data Packs**, drag the `.zip` into the window.
- Existing world: open the world folder, put the `.zip` in `datapacks/`, then run `/reload` or reopen the world.

**Server:** put the `.zip` in `world/datapacks/` and run `/reload`, or restart the server.

Don't unzip the file.

## Compatibility

- One zip supports Minecraft Java **26.2 and 26.3**. 26.3 changed the data pack format, so the zip includes a small 26.3 overlay that the game selects automatically.
- Everything lives in the `veinminer` namespace. The only vanilla files it touches are the `#minecraft:load` and `#minecraft:tick` function tags, which it adds to, so other data packs are unaffected. Add-ons hook in through the tags in [Add-ons](#add-ons).
- Only vanilla ores are supported, because a data pack can't detect modded blocks being mined.

## Good to know

- Blocks broken by Veinminer don't count toward the "Mined" statistic and don't use hunger.
- Veinminer doesn't know about land-claim or protection plugins. On servers that use them, a vein that crosses into a claim will be mined too.
- The maximum vein size affects performance. A 64-block vein takes about 4 ms in the tick you break it (4–5 ms with the animation off), and its animation then costs about 3 ms per tick for a second or two, which is unnoticeable. A 512-block vein, the highest limit, takes about 40 ms, and its animation up to about 17 ms per tick. Keep the limit modest on busy servers. (Measured on a Ryzen 9 5900X.)
- While a chain plays, the waiting blocks are still normal blocks. If you mine one yourself before its turn, it drops as usual and the chain skips it. Loot from the chain is rolled with the pickaxe you started it with.
- The animation uses display entities (one per block while it pops, one per flying item) and removes them as soon as they're done. A typical vein is finished about 1.5 seconds after the break.

## Uninstall

1. Run `/function veinminer:uninstall`.
2. Remove the `.zip` from the `datapacks` folder, or run `/datapack disable "file/Veinminer-1.2.0.zip"`.
3. Run `/reload`.

## Support

Veinminer is free. If it saves you some time, a coffee helps fund the next update.

[![Support me on Ko-fi](https://raw.githubusercontent.com/ProfetGit/assets/main/kofi-banner.gif)](https://ko-fi.com/profetgit)

## License

© 2026 Profet. All rights reserved.

- **You can** use Veinminer on any server, including monetized ones, and include the unmodified zip in any modpack that credits Profet and links here. You can also feature it in videos and modify it for your own world or server.
- **Please don't** re-upload Veinminer or a modified version of it elsewhere, sell it, or present it as your own.

The full terms are in the `LICENSE` file inside the zip. For anything else, just ask.
