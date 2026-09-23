# Changelog

## 1.2.0 — 2026-09-23
- Chain animation. A veinmined vein now breaks block by block, spreading out from the ore you hit. The chain starts slow and speeds up. Each block squashes down, holds for a beat, then pops in a crack of block particles and a little puff, with the crack climbing in pitch ring by ring. Each popped block throws its own loot, which arcs out of the wall and lands in a row in front of it (or flies to you, with drops set to your feet). XP arrives with the loot. A typical vein takes about 1.5 seconds; the biggest take about 3.
- The blocks stay real, normal blocks until it's their turn, so nothing changes colour or looks out of place while you wait.
- Still fair: the vein, the tool tier and your durability are all settled when you break the first ore. Each block's loot is rolled when it pops, with the pickaxe you mined with, even if you switch items in between. A block that someone mines, pushes or blows up before its turn is simply skipped, so nothing drops twice.
- Big veins stay readable: only a handful of loot items fly at a time, and the rest land with them.
- The mining tick itself is lighter than in 1.1.0, because the loot is now rolled while the chain plays.
- New setting **Chain animation** (`#animation`), on by default. Turn it off and the whole vein breaks at once, exactly as in 1.1.0.
- Uninstalling while a chain plays lets the flying loot land; blocks still waiting stay as normal ore.
- For pack authors: the add-on hooks are unchanged. `veinminer:meta version_id` is now 10200.

## 1.1.0 — 2026-09-23
- Add-on support. Other data packs can now stop a vein from being mined, or add a line to the join hint and the settings menu. The first add-on is Enchanted Veinminer, which makes veinmining need a Veinminer enchantment on the pickaxe.
- Without add-ons, Veinminer works exactly as in 1.0.0.
- For pack authors: the function tags `#veinminer:api/cancel` and `#veinminer:api/loaded`, the `veinminer:meta requires` text list, and the `veinminer:meta version_id` number (10100 for 1.1.0). See the README.

## 1.0.0 — 2026-09-23
- First release for Minecraft Java 26.2 and 26.3.
- Sneak and mine an ore to break the whole connected vein, diagonals included. Stone and deepslate variants count as one vein.
- Fortune, Silk Touch, pickaxe tier and Unbreaking all apply. Every extra block costs durability, and mining stops before the pickaxe would break.
- Drops and XP appear at the block you mined, or at your feet.
- Players can turn it on or off for themselves with `/trigger veinminer`.
- Ops get a clickable settings menu with `/function veinminer:settings`: vein limit, sneaking, drop location, durability, diagonals, action bar message, join hint, and on/off per ore type.
- `/function veinminer:uninstall` removes all of the pack's data.
