#!/usr/bin/env bash
# grab.sh TAKE TICK... -> full-size frames (1280x720) at ticks after the break mark, into out/TAKE/full/, plus a strip
set -euo pipefail
T=$1; shift
D="$(dirname "$0")/out/$T"
B=$(awk -F'\t' 'NR>2 && $2 ~ /\.break$/ && $5 > 0.9 {print $1; exit}' "$D/$T.sounds.tsv")
mkdir -p "$D/full"
for k in "$@"; do
  f=$(python3 -c "print(int(round($B + 3*$k)))")
  ffmpeg -v error -y -i "$D/$T.mp4" -vf "select=eq(n\,$f)" -frames:v 1 "$D/full/t${k}.png"
done
echo "break frame $B -> $D/full"
