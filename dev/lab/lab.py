#!/usr/bin/env python3
"""Veinminer's chain lab (polish skill, lablib): ore veins in stone walls, mined by the player while sneaking, filmed in
the real client at 60 fps. Block stand-ins and loot ghosts are probe groups; real item drops are traced.

  python3 dev/lab/lab.py list | rec TAG [--shots small:side] | review TAG | compare A B | reel A B
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path.home() / ".claude/skills/polish/scripts"))
import lablib as L  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]

SNEAK_MINE = [{"t": -0.3, "key": "sneak", "down": True}, {"t": 0, "key": "attack", "down": True},
              {"t": 0.5, "key": "attack", "down": False}, {"t": 0.9, "key": "sneak", "down": False}]

# "at" = the ore the player mines, flush with the west face of its wall; "ores" = the rest of the vein relative to it
SUBJECTS = {
    # a small iron vein in daylight
    "small": {"at": [0, -59, 0], "h": 3, "len": 3.2, "ores": [(1, 0, 0), (1, 1, 0), (0, 1, 1), (1, 0, -1)]},
    # the 13-ore iron vein from dev/capture/scene in a torch-lit room: the typical case, and dim lighting
    "cave": {"at": [0, -59, 48], "h": 3, "len": 3.6, "room": True,
             "ores": [(0, 0, -1), (0, 0, 1), (0, 1, 0), (0, -1, 0), (0, 1, 1), (0, -1, -1), (0, 2, 1), (1, 0, 0),
                      (1, 1, 0), (1, 0, 1), (2, 0, 0), (1, -1, -1)]},
    # an 80-ore coal blob against the 64-block cap: the longest chain, the most loot, pitch range, cost
    "big": {"at": [0, -59, 96], "h": 5, "len": 4.8, "block": "coal_ore", "wall": (7, 6, 5),
            "ores": [(dx, dy, dz) for dx in range(5) for dy in range(4) for dz in range(-2, 2) if (dx, dy, dz) != (0, 0, 0)]},
    # deepslate diamond in deepslate: the "b" block variant, deepslate sounds, XP orbs
    "deep": {"at": [0, -59, 144], "h": 3, "len": 3.4, "block": "deepslate_diamond_ore", "stone": "deepslate",
             "ores": [(0, 1, 0), (1, 0, 0), (1, 1, 0), (0, 0, 1), (1, 0, -1), (1, 1, 1), (2, 0, 0)],
             "action": [dict(e, t=0.75) if e.get("key") == "attack" and not e["down"] else e for e in SNEAK_MINE]},
    # the small vein with drops set to the player's feet: the low arc
    "own": {"at": [0, -59, 192], "h": 3, "len": 3.2, "ores": [(1, 0, 0), (1, 1, 0), (0, 1, 1), (1, 0, -1)],
            "setup": ["scoreboard players set #drops veinminer.config 1"]},
    # the small vein mined with Silk Touch: the loot is ore blocks (cube models on the ground)
    "silk": {"at": [0, -59, 240], "h": 3, "len": 3.2, "ores": [(1, 0, 0), (1, 1, 0), (0, 1, 1), (1, 0, -1)],
             "setup": ['item replace entity @s weapon.mainhand with minecraft:diamond_pickaxe[minecraft:enchantments={"minecraft:efficiency":2,"minecraft:silk_touch":1}]']},
}


def world(lab) -> dict:
    cmds = ["forceload add -32 -32 47 271", "!sleep 12", "gamerule random_tick_speed 0", "gamerule spawn_mobs false",
            "kill @e[type=!player]"]
    for s in SUBJECTS.values():
        x, y, z = s["at"]
        wx, wy, wz = s.get("wall", (5, 5, 4))
        stone, ore = s.get("stone", "stone"), s.get("block", "iron_ore")
        if s.get("room"):
            cmds += [f"fill {x - 12} {y - 2} {z - 10} {x + 6} {y + 6} {z + 10} minecraft:stone",
                     f"fill {x - 11} {y - 1} {z - 9} {x - 1} {y + 4} {z + 9} minecraft:air",
                     f"setblock {x - 1} {y + 2} {z - 3} minecraft:wall_torch[facing=west]",
                     f"setblock {x - 1} {y + 2} {z + 4} minecraft:wall_torch[facing=west]",
                     f"setblock {x - 11} {y + 2} {z} minecraft:wall_torch[facing=east]",
                     f"setblock {x - 6} {y + 2} {z - 9} minecraft:wall_torch[facing=south]"]
        else:
            cmds.append(f"fill {x} {y - 1} {z - wz} {x + wx} {y + wy} {z + wz} minecraft:{stone}")
        cmds += [f"setblock {x + dx} {y + dy} {z + dz} minecraft:{ore}" for dx, dy, dz in [(0, 0, 0), *s["ores"]]]
    cmds += ["kill @e[type=item]", "!sleep 2"]
    return {"name": "Veinminer lab", "seed": "veinminer-lab", "type": "minecraft:flat", "structures": False, "commands": cmds}


LAB = L.Lab(
    name="Veinminer",
    root=ROOT,
    subjects=SUBJECTS,
    world=world,
    artifact="dist/Veinminer-*-fabric.jar",
    exclude_mods=["Veinminer-*.jar", "EnchantedVeinminer-*.jar"],
    direction=90,  # the loot flies back west, toward the player
    stand=(-2.5, -1, 0.5),  # on the ground, two blocks west of the wall
    facing=(-90, 10),
    aim=(0.02, 0.5, 0.5),  # the west face of the mined ore
    setup=['item replace entity @s weapon.mainhand with minecraft:diamond_pickaxe[minecraft:enchantments={"minecraft:efficiency":2}]',
           "tag @s add veinminer.welcomed", "recipe give @s *"],
    action=SNEAK_MINE,
    marks="break=.break>0.9,pop=entity.chicken.egg",
    align="break",
    key_marks=("pop",),
    groups={"standins": "^block:", "ghosts": "^item:"},
    probe_entities="minecraft:item,minecraft:experience_orb",
    window=(-3, 60),
    cams={
        "side": L.side(along=1.6, up=1.2, dist="1.4*h+5.5", fov=40),  # just west of the wall face: the loot flies to the left
        "hero": L.hero(pos=(4, 1.2, 3), look=(-0.5, 1, 0), fov=60),
        "fp": L.fp(keys=[{"t": "+0.6", "look": [1.5, 0.6, 0], "ease": "smooth"}, {"t": "end", "look": [1.8, -0.4, 0], "ease": "out"}]),
    },
)

if __name__ == "__main__":
    L.main(LAB)
