# Veinminer add-on API

Hooks for data packs that build on Veinminer, for example [Enchanted Veinminer](https://modrinth.com/datapack/enchanted-veinminer).

Veinminer 1.1.0 and newer offer these hooks. They are safe to use when Veinminer isn't installed, because a tag your pack adds to is simply never called.

- `#veinminer:api/cancel` (function tag): runs as and at the player just before a vein is mined, after Veinminer's own checks (sneaking, pickaxe, ore type, game mode). Do `return 1` to cancel the vein. The block the player broke still breaks normally. To allow the vein, **don't return at all**: the first function in the tag that returns decides, so a `return 0` or `return fail` would skip the add-ons after yours.
- `#veinminer:api/loaded` (function tag): runs at the end of Veinminer's load function, every load and `/reload`. Use it to check that Veinminer is present.
- `storage veinminer:meta requires` (list of text components): cleared on every load, just before `#veinminer:api/loaded` runs. Append a sentence there, such as `{text:"Your pickaxe needs X. ",color:"gray"}`, and Veinminer shows it in the join hint and the settings menu.
- `storage veinminer:meta version_id` (int): `major × 10000 + minor × 100 + patch`, for example `10100` for 1.1.0.

