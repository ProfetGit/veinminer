#!/usr/bin/env bash
# Dev-only: watch the chain animation in the real client. Usage: dev/capture/run.sh [mc-version] [out-dir]
# Builds the pack, lets the test server create a flat world with it, adds the capture scene (dev/capture/scene),
# plays it off-screen via ../ClientCapture and writes frames plus dev/capture/sheet.png.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
VER=${1:-26.3}
OUT=${2:-$ROOT/dev/capture/frames}
WORK=$ROOT/dev/capture/.work
mkdir -p "$WORK"
# build the scene on the server first, so the saved world already has settled lighting when the client loads it
printf "forceload add -16 -16 32 32\n!tick 40\nfunction vmscene:build%s\n!tick 60\n" "${SCENE:+_$SCENE}" > "$WORK/build.txt"
[ -n "${DROPS:-}" ] && echo "scoreboard players set #drops veinminer.config $DROPS" >> "$WORK/build.txt"
(cd "$ROOT" && EXTRA_PACKS=dev/capture/scene dev/test/run.sh "$VER" "$WORK/server" explore "$WORK/build.txt") > "$WORK/server.log" 2>&1
"$ROOT/../ClientCapture/run.sh" "$VER" "$WORK/server/world" "$OUT" "${WARMUP:-40}" "${FRAMES:-90}"
python3 "$ROOT/dev/capture/sheet.py" "$OUT" "$ROOT/dev/capture/sheet.png"
