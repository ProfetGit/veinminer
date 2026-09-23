# Changelog

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
