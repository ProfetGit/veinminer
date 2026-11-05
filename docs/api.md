# Veinminer add-on API

Hooks for data packs that build on Veinminer, for example [Enchanted Veinminer](https://www.curseforge.com/minecraft/mc-mods/enchanted-veinminer).

Veinminer 1.1.0 and newer offer these hooks; 2.0 keeps them (`version_id` 20000, `version` `2.0.0`) and adds a Java hook for mods. They are safe to use when Veinminer isn't installed, because a tag your pack adds to is simply never called.

- `#veinminer:api/cancel` (function tag): runs as and at the player just before a vein is mined, after Veinminer's own checks (sneaking, pickaxe, ore type, game mode). Do `return 1` to cancel the vein. The block the player broke still breaks normally. To allow the vein, **don't return at all**: the first function in the tag that returns decides, so a `return 0` or `return fail` would skip the add-ons after yours.
- `#veinminer:api/loaded` (function tag): runs at the end of Veinminer's load function, every load and `/reload`. Use it to check that Veinminer is present.
- `storage veinminer:meta requires` (list of text components): cleared on every load, just before `#veinminer:api/loaded` runs. Append a sentence there, such as `{text:"Your pickaxe needs X. ",color:"gray"}`, and Veinminer shows it in the join hint and the settings menu.
- `storage veinminer:meta version_id` (int): `major × 10000 + minor × 100 + patch`, for example `10100` for 1.1.0.

## Java hooks (2.0, mods)
`io.github.profetgit.veinminer.api.VeinminerEvents`, loader-neutral, listeners run in registration order on the server thread:
- `BEFORE_VEIN.register((level, player, pos, state, group) -> boolean)`: asked once per vein that passed Veinminer's own checks, before the scan. Return `true` to veto (the struck block stays broken, nothing else happens). It runs before the `#veinminer:api/cancel` function tag.
- `AFTER_VEIN.register((level, player, pos, group, mined) -> ...)`: told when a vein ended (the whole vein with `animation` off, the end of the chain otherwise).

On Minecraft 1.20.1 the `#veinminer:api/cancel` function tag cannot veto (1.20.1 commands cannot `return`); the Java hook can.
