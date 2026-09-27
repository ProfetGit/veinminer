# Veinminer 1.2.0 → 1.3.0: polish log

Goal (the user's words): `/polish Veinminer` — the Timber 1.3.0 treatment for the chain animation.
Done means: checklist sections 1–8 on every subject (section 6 multiplayer/obstacles only where the lab or harness can
show it), user-approved look kept (real blocks until their turn, no brightness flash, small_gust puffs, loot arcs as the
payoff, ~1.5 s typical chain), every build and platform green.
Lab: `dev/lab/lab.py` — subjects small (5 iron, daylight), cave (13 iron, torch-lit room), big (80-coal blob against the
64 cap), deep (8 deepslate diamond), own (small with drops at the player's feet); cameras side/hero/fp. Baseline tag
`base` = build 1.2.0 (kept in `.work/artifacts/base/` and `dev/lab/baseline/`).
Tools added for this pack: `dev/lab/chain.py` (per stand-in / per ghost curves aligned on each member's own start,
hand-off heights, sound pitches), `strip.py` (every frame of a window at full size), `grab.sh`. Probe extended with
`<take>.items.tsv` (where the renderer draws each real item: bob lift and spin).
Pre-existing state: `validate.py --quick` on the baseline → build PASS, vanilla 26.2/26.3 2/2 PASS.

## Baseline findings (measured before any change)

| # | What reads off | Evidence | Cause | Plan |
|---|---|---|---|---|
| 1 | Big veins go silent and lose their crumbs and puffs halfway | big: 17 pop ticks, ring sounds stop after the 10th (sounds.tsv: last plop pitch 1.98 at t+23.3, pops continue to t+28.7) | `anim/ring` is a macro; plop pitch = ring pitch + 0.5 passes 2.0 at ring 10, `playsound` rejects it and the whole macro call dies (particles, gust, both sounds). The ring pitch is also capped at 1.6 (plateau). | pitch step scaled to the chain's ring count, ring ≤ 1.5, plop ≤ 2.0 |
| 2 | Stand-ins draw ~30 % darker than the ore they replace in a torch-lit cave | cave-hero luminance of one block: 54.6 (real ore) → 38.9 (stand-in) the frame it swaps; full-size frames | a display takes the light of its own cell, which is one level darker than the face cell in front of it (and has no smooth lighting) | spawn the display one block toward the miner (the lit face cell) and translate it back |
| 3 | Stand-in pops 10 % smaller the frame it appears (2 of 9 takes) | chain.py: first drawn scale 0.90/1.06 for whole rings in cave-side, own-side | the first squash key arrives in the same client tick as the add packet, so the client never draws the identity pose | first key one tick later |
| 4 | The snap stretch reads 1.21–1.40 depending on frame phase | chain.py: stretch peak per take 1.21, 1.22, 1.25, 1.27, 1.30, 1.35, 1.39, 1.40 | a 1-tick key immediately followed by the vanish key: the client draws 2/3 to 3/3 of it | hold the stretch one tick (most extreme pose held) |
| 5 | Loot arc is a tent, not a throw | ghosts: vertical speed is a 3-step staircase (+0.2..0.35, −0.05..−0.3, −0.6..−0.85 blocks/tick), parabola residual 0.20 blocks, accel jumps −0.5 at two joints | three linear translation keys (5, 4, 3 ticks) on a linear teleport lerp | one ballistic arc from per-tick keys |
| 6 | Loot vanishes in mid-air and reappears on the floor | hand-off: ghost's last drawn bottom 0.25–0.6 above the floor, falling 0.6–0.76 blocks/tick; the real item appears on the floor (jump −0.49..+0.14) with a random spin; strip frames f119→f120 | the ghost is killed the tick its last fall key would finish; the real item's bob/spin are client-random | land the ghost on the floor, hold a contact squash, swap into a small vanilla bounce |
| 7 | 40 % of a typical vein's loot pops into existence on the floor | cave: 7 of 12 ghosts drawn; a pile appears in one frame (f120) | only 2 ghosts visible per tick; hidden ones land at their own spread spot | 3 per tick, 12 per chain; hidden loot lands on the pile of the last visible ghost at its contact |
| 8 | Loot lighting is bucketed | ghosts use `brightness` in 5 buckets from the landing light | the ghost entity lerps through the wall, so it needed an override | keep the entity at the landing spot for the whole flight (exact light, no override) |

Also noted: `anim/pop.mcfunction` is unused (gen.py still writes it). Another session's Mob Reactions clients were
running during the baseline (load ~3): lockstep video is exact; perf is re-taken when idle.

## v1 — the pop: pitch over the whole chain, lit stand-ins, robust keys
- Changed: the ring count comes out of the delay tiers (`#rings`); the pitch step is 0.65/(rings−1) capped at 0.07, so
  the break sound climbs 0.85 → 1.49 and the plop 1.35 → 1.99 on any chain. Stand-ins stand in the lit cell in front of
  their block (toward the miner, when it is hollow) and are translated back. First squash key one tick later; the
  stretch is held one tick (the pop, ring effects and loot launch move one tick later). Dead `anim/pop` removed.
- Measured vs base: big chain 15/15 pop ticks with sound and crumbs (base 10/17), pitches rise every ring to 1.49;
  stretch peak 1.40 on every take (base 1.21–1.40); first drawn scale 1.00 on every take (base 0.90 on 2 of 9);
  stand-in luminance at the swap: cave 54.6 → 44.0 (−19 %, base −29 %), daylight 87.2 → 72.5 (−17 %, base −25 %).
- The remaining −17 % is vanilla: block displays use the entity diffuse lighting, whose east/west faces get 0.5 against
  0.6 for world block faces (0.497/0.6 = 0.83, exactly what the frames show). Z faces lose 7 %, tops nothing.
- Verdict: kept.

## v2–v3 — ballistic loot arc on the teleport lerp
- v2: straight teleport lerp (12 ticks) + a per-tick translation bump from a table; the ghost lands on the floor at its
  model's resting height, squashes (1.25/0.7, bottom kept on the floor) and swaps into the real item with a 0.12 hop;
  hidden loot lands on the last visible ghost's spot; 3 visible per tick, 12 per chain; the brightness override is
  removed on landing.
- Measured: per-tick gravity −0.089 constant; hand-off jump −0.14..+0.20 (base −0.49..+0.14); but tick 1 lagged (the
  bump keys were drawn a tick after the lerp started). v3 shifted the table a tick: fixed.
- Looked at per-frame steps: a [full, full, half] step pattern each tick on 2 of 3 frame phases.
- Verdict: superseded by v4–v6.

## v4 — whole flight in per-tick transformation keys (reverted)
- Entity parked at the landing spot (exact light, no override), translation D·(1−u) + bump every tick.
- Measured: gravity exact per tick, but the probe's progress column shows why the per-frame steps limp: frames at
  partial 0.19/0.52/0.85, and every new key restarts from the last drawn frame, so the 0.15 tick between the last
  frame and the tick boundary is lost every tick (steps 0.073/0.131/0.131). A per-tick key always judders at 20 Hz.
- Verdict: reverted to fewer keys.

## v5–v6 — straight line in two long keys, gravity bump as per-tick teleports
- The straight line is the translation (two keys: grow, then the rest); the bump is the entity's own position, a
  relative teleport per tick (position lerps interpolate between tick states, no restart loss). v5 spun the entity's
  yaw, which also turns its translation (the loot swung sideways): v6 moved the spin (half a turn) into the keys and pins
  the yaw to 0.
- Measured (v6): horizontal step 0.074 per frame, constant except one dip where the second key starts; vertical steps
  smooth; contact exact; hand-off +0.00..+0.20 (always upward, within the bob) on flat and cube loot.
- v7 moves the second key's start to tick 1, inside the pop (loot grows to 1.2 in one tick).

## v7–v8 — pop in one tick, floor snap, full set
- v7: the loot grows to 1.2 in one tick, so the second key's restart falls inside the pop. Full set 18/18, 0 frozen.
- The landing base came from a 0.25-step floor search, so it sat up to 0.25 above the floor (the loot landed in the air,
  then dropped). It is now snapped to the floor when the floor is solid (the water case keeps the old height).
- v8 full set: 18/18 shots, 0 frozen frames. Per ghost: gravity −0.089 blocks/tick² on every tick except the pop (tick
  1, −0.11..−0.16, inside the block) and the contact; horizontal step constant per frame; stretch peak 1.40 on every
  take; first drawn stand-in scale 1.00; hand-off +0.00..+0.22 (flat) / +0.13..+0.33 (cube) — always upward, i.e. the
  bob of the real item reads as the rebound; pitch 0.85 → 1.49 over 15 rings on the 64-block chain.
- Seen once (v8 big-side, machine load ~10 from other sessions): two rings' loot got bunched packets, so the per-tick
  gravity steps doubled and paused in their first 3 ticks. v7's take of the same shot was clean. Same class as Timber's
  delivery jitter; the heavy turn ticks were trimmed (hidden loot skips its position reads).

## Performance (26.3, harness arena, 100-coal blob against the 64 cap)

Machine load was 4–10 from other sessions the whole time (Mob Reactions clients, HavingABlast Gradle builds), so the
worst-tick windows of `perf.py` were noise (±5 ms between identical runs). Interleaved A/B of every tick instead
(`dev/lab/perfab.py`, base and current alternating, 3 runs each, per-tick medians):

| | 1.2.0 | 1.3.0 |
|---|---|---|
| mining tick | 12.3 ms | 12.8 ms |
| worst chain tick (median) | 8.6 ms | 9.4 ms |
| whole chain, 62 ticks | 207.9 ms | 211.8 ms (+2 %) |

Per ghost the work moved: the turn does more (position reads, a macro to place it, one merge), the flight less (one
relative teleport per tick instead of three keys and a teleport lerp). Same cost within the noise.

## Shaders (reel takes, Complementary)
- Stand-in luminance in the cave: real ore 91.7 → 1.2.0 stand-in 55.1, 1.3.0 stand-in 65.9. The rest of the gap is the
  shader: Complementary makes ore spots glow on blocks, and a block display is drawn as an entity without that glow.

## Rejected ideas (and why)
- A key per tick for the whole flight (v4): exact per tick, but a 20 Hz judder per frame (restart from the last drawn
  frame loses the slice before each tick boundary).
- Spinning the loot on the entity's yaw (v5): a display's translation is in its rotated frame, so the straight line
  swung sideways.
- A +1 light-level brightness override to cancel the display side-face shading: exact only in caves and x-facing faces,
  no headroom in daylight (sky 15), and overrides are what the user read as a colour change in 1.2.0's first version.
- A 2-tick teleport lerp for the gravity steps (robust to one late packet): it lags a tick and eases into the floor
  instead of hitting it.

## Weak spots / not done
- Sounds untested by ear (captures are muted); the sound log shows every ring with its crack and plop.
- Stand-ins stay 17 % darker on east/west faces (7 % north/south) than the real block: vanilla draws block displays with
  entity lighting. With Complementary the ore glow is lost on the stand-in.
- Per-tick gravity steps are sensitive to bunched packets (a heavy server or network jitter): a short hitch at most.
- Flying-loot positions are read ×1000 into scores; beyond ±2,147,483 blocks from the origin they saturate and the loot
  pops straight up at its landing spot instead of arcing.
- Floors that aren't full blocks (slabs, snow layers) still land the loot on the block grid, so it drops the rest.

## Validation incident: Spigot drops=1
- First full validation: matrix 8/10, Spigot 26.2 and 26.3 failed "drops=1 puts drops at player" (1.2.0 passes).
- Traced tick by tick in the Spigot harness: the loot does land at the player (t+26..t+38); at t+40 Spigot's item merge
  (`merge-radius.item` 2.5 there, 0.5 in vanilla/Paper/CraftBukkit) pulls the pile into the struck block's own drop
  1.8 blocks away. The 1.3.0 chain ends two ticks later than 1.2.0's, so the check now read the cell after that merge;
  1.2.0 passed by two ticks. The check now samples the most loot at the player cell while the chain lands.

## 1.3.1 — the black flicker at the swap (user report after the 1.3.0 release)
- Report: at high fps each block goes black for a moment when it turns into its animated stand-in.
- Cause (probe): a block's air update applies the frame its packet is handled, but a new display is drawn only after
  its first client tick. 1.3.0 (and 1.2.0) summoned the stand-in and set the block to air in the same tick, so the empty
  cell showed for 1–3 frames at 60 fps (up to 50 ms, many frames at high fps); where both were drawn it z-fought
  (hatching in the full-size v8 frames). The 60 fps lab showed it only as odd frames; nobody had looked frame by frame.
- Fix: the stand-in is summoned one tick before its block turns to air (`anim/prep`, `anim/prep_first` for the first
  ring), 0.6 % oversized while the real block is still there, in the first hollow neighbour cell (its own cell is solid
  that tick). First squash key back to 1.2.0's 3 ticks (the add packet is now a tick earlier).
- Measured (v10): every ring's air update arrives after its stand-in was drawn (big vein: stand-ins first drawn at frames
  65/71/74/77/80/86/89, air updates at 66/72/75/78/81/87/91); full-size frames show no dark cell and no hatching;
  curves, stretch, hand-off and pitches as in 1.3.0. 99 tests (the new one: the stand-in exists while its block is still
  ore).
