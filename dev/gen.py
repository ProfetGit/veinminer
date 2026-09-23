#!/usr/bin/env python3
"""Write the chain-animation tables in pack/data/veinminer/function/anim/: rings*.mcfunction and pop.mcfunction.

Each broken block's display waits `delay` ticks before its pop starts. delay = floor(1 + K*(d**P - 1) + 0.5), clamped
to 1..KMAX, where d is its distance from the struck block. P < 1 makes the chain accelerate: the first rings are far
apart in time, later ones bunch up (pop... pop.. pop-pop-pop). Bigger veins use a smaller K so the chain stays under
KMAX ticks.
pop.mcfunction summons one poof drop with one of 8 fixed pop directions. Motion goes in the summon NBT so the spawn
packet already carries it; a static line per direction avoids a macro parse per drop.
Rerun after changing any table.
"""
import math
from pathlib import Path

FUNC = Path(__file__).resolve().parent.parent / "pack/data/veinminer/function/anim"
KMAX = 40
P = 0.6
# (suffix, K); a tier is used while every display is closer than far(K), where its delay would pass KMAX
TIERS = [("a", 10.0), ("b", 5.0), ("c", 2.5)]
SEL = "@e[type=marker,tag=veinminer.pnew{}]"


def radius(k: int, K: float) -> float:
    """delay <= k exactly for distances below this."""
    return (1 + (k - 0.5) / K) ** (1 / P)


def far(K: float) -> float:
    return math.floor(radius(KMAX, K) * 100) / 100


def tier(K: float) -> str:
    # ascending: each band takes the displays still tagged new within R_k; stop at the last band so #maxd is exact
    lines = []
    for k in range(1, KMAX):
        near = SEL.format(f",distance=..{radius(k, K) - 0.0005:.4f}")
        lines += [f"scoreboard players set {near} veinminer.t {k}",
                  f"tag {near} remove veinminer.pnew",
                  f"execute unless entity {SEL.format(',limit=1')} run return run scoreboard players set #maxd veinminer.data {k}"]
    lines += [f"scoreboard players set {SEL.format('')} veinminer.t {KMAX}",
              f"tag {SEL.format('')} remove veinminer.pnew",
              f"scoreboard players set #maxd veinminer.data {KMAX}"]
    return "\n".join(lines) + "\n"


def pop() -> str:
    lines = []
    for i in range(8):
        a = math.radians(i * 135 + 20)
        r = (0.055, 0.1, 0.075, 0.035)[i % 4]
        up = (0.2, 0.16, 0.24, 0.18)[i % 4]
        motion = f"[{math.cos(a) * r:.3f}d,{up}d,{math.sin(a) * r:.3f}d]"
        lines.append(f'execute if score #dir veinminer.data matches {i} run return run summon item ~ ~ ~ '
                     f'{{Item:{{id:"minecraft:stone"}},Age:1s,PickupDelay:10s,Tags:["veinminer.pop"],Motion:{motion}}}')
    return "\n".join(lines) + "\n"


def main() -> None:
    (FUNC / "pop.mcfunction").write_text(pop())
    for suffix, K in TIERS:
        (FUNC / f"rings_{suffix}.mcfunction").write_text(tier(K))
    # the slowest tier whose predecessor's radius limit no display exceeds
    pick = []
    for i in range(len(TIERS) - 1, 0, -1):
        pick.append(f"execute if entity {SEL.format(f',distance={far(TIERS[i - 1][1])}..,limit=1')} run return run function veinminer:anim/rings_{TIERS[i][0]}")
    pick.append(f"function veinminer:anim/rings_{TIERS[0][0]}")
    (FUNC / "rings.mcfunction").write_text("\n".join(pick) + "\n")
    print(f"wrote pop.mcfunction, rings.mcfunction + {len(TIERS)} tiers to {FUNC}")


if __name__ == "__main__":
    main()
