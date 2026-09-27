#!/usr/bin/env python3
"""Write the chain-animation tables in pack/data/veinminer/function/anim/: rings*.mcfunction, keys.mcfunction + key/.

Each broken block's display waits `delay` ticks before its pop starts. delay = floor(1 + K*(d**P - 1) + 0.5), clamped
to 1..KMAX, where d is its distance from the struck block. P < 1 makes the chain accelerate: the first rings are far
apart in time, later ones bunch up (pop... pop.. pop-pop-pop). Bigger veins use a smaller K so the chain stays under
KMAX ticks. The tiers also count the rings (#rings), which spreads the pop pitch over the whole chain.
keys.mcfunction + key/k<n>: the stand-ins' squash, stretch and vanish keys, one line per light-cell offset.
throw.mcfunction, fly.mcfunction + fly/: the loot's arc (keys and per-tick steps) and the contact squash.
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
                  f"execute if entity {SEL.format(f',distance=..{radius(k, K) - 0.0005:.4f},limit=1')} run scoreboard players add #rings veinminer.data 1",
                  f"tag {near} remove veinminer.pnew",
                  f"execute unless entity {SEL.format(',limit=1')} run return run scoreboard players set #maxd veinminer.data {k}"]
    lines += [f"scoreboard players set {SEL.format('')} veinminer.t {KMAX}",
              "scoreboard players add #rings veinminer.data 1",
              f"tag {SEL.format('')} remove veinminer.pnew",
              f"scoreboard players set #maxd veinminer.data {KMAX}"]
    return "\n".join(lines) + "\n"


# Stand-in keys (tick of the display's own countdown, interpolation ticks, xz scale, y scale); scaled about the block
# centre. The display stands in the lit cell in front of its block (offset n, tag veinminer.at_<k>) and is translated
# back, so it takes the light the block's visible face had.
KEYS = [(-2, 2, 1.06, 0.9), (-4, 2, 1.2, 0.72), (-8, 1, 0.8, 1.4), (-10, 2, 0.0, 0.0)]
CELLS = {"0": (0, 0, 0), "xp": (1, 0, 0), "xn": (-1, 0, 0), "yp": (0, 1, 0), "yn": (0, -1, 0), "zp": (0, 0, 1), "zn": (0, 0, -1)}


def f(v: float) -> str:
    return f"{round(v, 4):g}f"


def keys() -> dict:
    out = {"keys": []}
    for tick, dur, sxz, sy in KEYS:
        sel = f"@e[type=block_display,tag=veinminer.fx,scores={{veinminer.t={tick}}}"
        out["keys"].append(f"execute if entity {sel},limit=1] run function veinminer:anim/key/k{-tick}")
        lines = []
        for k, n in CELLS.items():
            tr = [0.5 - 0.5 * s - c for s, c in zip((sxz, sy, sxz), n)]
            lines.append(f"execute as @e[type=block_display,tag=veinminer.fx,tag=veinminer.at_{k},scores={{veinminer.t={tick}}}] run data merge entity @s "
                         f"{{start_interpolation:0,interpolation_duration:{dur},transformation:{{translation:[{','.join(f(v) for v in tr)}],"
                         f"scale:[{f(sxz)},{f(sy)},{f(sxz)}]}}}}")
        out[f"key/k{-tick}"] = lines
    return out


# Loot flight. The ghost's entity is parked at its landing spot from the turn on (anim/place), so it is lit like the
# spot it lands on. Two motions add up to one ballistic arc (a straight line plus a parabola has constant gravity):
# - the straight line from the popped block to the spot is its translation, D = start - spot (per ghost, x1000 in
#   veinminer.dx/dy/dz) going to 0, in two long keys (the pop out of the block to scale 1.2 in one tick, then 11 ticks
#   to scale 1; the second key's restart is hidden in the pop);
# - the parabolic bump is the entity's own position, one relative teleport per tick (teleport_duration 1).
# The spin (half a turn) rides on the two keys. It can't go on the entity's yaw: a display's translation is in its
# rotated frame, so a turning entity would swing the straight line around.
# Every transformation key restarts from the last drawn frame, so a key per tick loses the slice between the last frame
# and the tick boundary each tick (a 20 Hz judder at 60 fps); position lerps interpolate between tick states and don't.
# HI is the throw toward the landing spot, LO the low toss to the player's feet. On landing (countdown -FLIGHT) it
# squashes onto the floor (bottom kept on the floor) and holds that a tick; at -FLIGHT-2 anim/op swaps it for the real
# item (anim/land).
FLIGHT = 12
GROW = 1
BUMP = {"hi": 1.6, "lo": 0.55}
SPIN = (0.2618, 3.1416)  # radians at the end of the grow key and of the flight
SQUASH = (1.25, 0.7)
# ground-mode model bottom per display scale unit: flat items (generated models) -0.125, cube items (block models) +0.0625;
# a resting real item's model bottom is 1/16 above its position (ItemEntityRenderer, bob excluded). anim/rest parks the
# ghost this far above the floor so that translation 0 at scale 1 is exactly a resting item.
REST = {"flat": 0.1875, "cube": 0.0}
BOTTOM = {"flat": -0.125, "cube": 0.0625}


def bump(amp: float, k: int) -> float:
    u = k / FLIGHT
    return 4 * amp * u * (1 - u)


def fly() -> dict:
    """The launch tick (countdown 0, anim/throw) sends the first key and the first step; countdown -k sends step k+1,
    so the bump and the keys are drawn complete one tick after they are sent, together."""
    out = {}
    for name, amp in BUMP.items():
        lines = []
        for k in range(1, FLIGHT):
            if k == GROW:
                lines.append(f"execute if score @s veinminer.t matches {-k} run data merge entity @s {{start_interpolation:0,interpolation_duration:{FLIGHT - GROW},"
                             f"transformation:{{translation:[0f,0f,0f],scale:[1f,1f,1f],left_rotation:{{angle:{SPIN[1]}f,axis:[0f,1f,0f]}}}}}}")
            step = bump(amp, k + 1) - bump(amp, k)
            lines.append(f"execute if score @s veinminer.t matches {-k} run return run tp @s ~ ~{round(step, 4):g} ~")
        sxz, sy = SQUASH
        for kind in ("flat", "cube"):
            ty = (0.0625 - BOTTOM[kind] * sy) - REST[kind]
            tag = "veinminer.cube" if kind == "cube" else "!veinminer.cube"
            lines.append(f"execute if score @s veinminer.t matches {-FLIGHT} if entity @s[tag={tag}] run return run data merge entity @s {{start_interpolation:0,interpolation_duration:1,"
                         f"transformation:{{translation:[0f,{f(ty)},0f],scale:[{f(sxz)},{f(sy)},{f(sxz)}]}}}}")
        out[f"fly/{name}"] = lines
    out["fly"] = ["execute if entity @s[tag=veinminer.lo] run return run function veinminer:anim/fly/lo", "function veinminer:anim/fly/hi"]
    w = (FLIGHT - GROW) / FLIGHT
    out["throw"] = [f"data modify storage veinminer:anim g set value {{start_interpolation:0,interpolation_duration:{GROW},transformation:{{translation:[0f,0f,0f],scale:[1.2f,1.2f,1.2f],left_rotation:{{angle:{SPIN[0]}f,axis:[0f,1f,0f]}}}}}}"]
    out["throw"] += [f"execute store result storage veinminer:anim g.transformation.translation[{i}] float {w * 0.001:g} run scoreboard players get @s veinminer.d{ax}" for i, ax in enumerate("xyz")]
    out["throw"] += ["data modify entity @s {} merge from storage veinminer:anim g",
                     f"execute if entity @s[tag=veinminer.lo] run return run tp @s ~ ~{round(bump(BUMP['lo'], 1), 4):g} ~",
                     f"tp @s ~ ~{round(bump(BUMP['hi'], 1), 4):g} ~"]
    return out


def main() -> None:
    (FUNC / "key").mkdir(exist_ok=True)
    (FUNC / "fly").mkdir(exist_ok=True)
    for name, lines in {**keys(), **fly()}.items():
        (FUNC / f"{name}.mcfunction").write_text("\n".join(lines) + "\n")
    for suffix, K in TIERS:
        (FUNC / f"rings_{suffix}.mcfunction").write_text(tier(K))
    # the slowest tier whose predecessor's radius limit no display exceeds
    pick = []
    for i in range(len(TIERS) - 1, 0, -1):
        pick.append(f"execute if entity {SEL.format(f',distance={far(TIERS[i - 1][1])}..,limit=1')} run return run function veinminer:anim/rings_{TIERS[i][0]}")
    pick.append(f"function veinminer:anim/rings_{TIERS[0][0]}")
    (FUNC / "rings.mcfunction").write_text("\n".join(pick) + "\n")
    print(f"wrote keys.mcfunction, key/, fly.mcfunction, fly/, rings.mcfunction + {len(TIERS)} tiers to {FUNC}")


if __name__ == "__main__":
    main()
