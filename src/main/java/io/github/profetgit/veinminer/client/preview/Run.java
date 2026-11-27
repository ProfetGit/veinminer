package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.veinminer.core.Arc;
import io.github.profetgit.veinminer.core.Delays;
import io.github.profetgit.veinminer.core.StandKeys;
import io.github.profetgit.veinminer.server.Tune;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// One play of a vein as the mod runs it, worked out once per set of options: which blocks break, on which tick, where
// each one's loot flies and lands. The chain's pacing is Delays.plan, the loot's flight Arc, the landing rules those of
// server/Chain (landingSpot, spotFor, ghost limits); the scan is Vein.scan's breadth-first walk on the patch.
// Ticks are counted from the struck block's break, as the chain counts its age.
final class Run {
    record Opts(boolean veining, boolean chain, int max, boolean diagonal, boolean own, boolean durability) {}

    // the player: standing east of the struck block, eyes 1.62 up
    static final double[] FEET = {2.4, 0, 1.0};
    static final double EYE = 1.62;
    static final int FLOOR_STEPS = Arc.FLOOR_STEPS;

    final class Ghost {
        int cell, born;
        double[] start = new double[3], spot = new double[3];
        boolean cube, lo, visible;
        double amp;
    }

    // an item thrown out of a block that broke with the rest, by vanilla's rules (no chain)
    final class Pop {
        int cell;
        double[] start = new double[3], v = new double[3];
    }

    final Subject subject;
    final Patch patch;
    final Opts opts;
    final boolean chainMode;
    final int n;
    final boolean[] mined;
    final int[] delay;
    final List<Integer> order = new ArrayList<>();
    final List<Ghost> ghosts = new ArrayList<>();
    final List<Pop> pops = new ArrayList<>();
    final double[] landing;
    int maxDelay, lastTick;
    // falling cells (sand, gravel): the tick each starts to fall and how far down it ends
    final int[] fallStart;
    final int[] fallDrop;
    private int dir;

    Run(Subject subject, Opts opts) {
        this.subject = subject;
        this.patch = subject.patch();
        this.opts = opts;
        n = patch.size();
        mined = new boolean[n];
        delay = new int[n];
        fallStart = new int[n];
        fallDrop = new int[n];
        java.util.Arrays.fill(fallStart, -1);
        mined[0] = true;
        order.add(0);
        if (opts.veining()) for (int i : scan()) {
            mined[i] = true;
            order.add(i);
        }
        chainMode = opts.chain() && order.size() > 1;
        if (chainMode) {
            double[] dist = new double[order.size() - 1];
            for (int k = 1; k < order.size(); k++) {
                int[] c = patch.at[order.get(k)];
                dist[k - 1] = Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
            }
            Delays.Plan plan = Delays.plan(dist, Tune.delayK());
            for (int k = 1; k < order.size(); k++) delay[order.get(k)] = plan.delays()[k - 1];
            maxDelay = plan.maxDelay();
        }
        landing = landingSpot();
        if (chainMode) makeGhosts();
        else makePops();
        fall();
        lastTick = Math.max(lastTick, maxDelay + Arc.LAUNCH + Arc.LAND);
    }

    // ---- the vein ----

    // Vein.scan: breadth-first from the struck cell over faces (and the 20 other neighbours), each ring's cells nearest the
    // player first, at most max - 1 cells
    private List<Integer> scan() {
        int limit = Math.max(0, opts.max() - 1);
        List<Integer> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        seen.add(0);
        List<Integer> frontier = List.of(0);
        while (!frontier.isEmpty() && out.size() < limit) {
            List<Integer> found = new ArrayList<>();
            for (int p : frontier) {
                int[] a = patch.at[p];
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (!opts.diagonal() && Math.abs(x) + Math.abs(y) + Math.abs(z) != 1) continue;
                    int q = patch.find(a[0] + x, a[1] + y, a[2] + z);
                    if (q < 0 || !seen.add(q)) continue;
                    found.add(q);
                }
            }
            found.sort(Comparator.comparingDouble(this::nearness));
            for (int q : found) {
                if (out.size() >= limit) break;
                out.add(q);
            }
            frontier = found;
        }
        return out;
    }

    private double nearness(int cell) {
        int[] c = patch.at[cell];
        double dx = c[0] - FEET[0], dy = c[1] + 0.5 - FEET[1], dz = c[2] - FEET[2];
        return dx * dx + dy * dy + dz * dz;
    }

    // a cell the loot can pass: nothing of the patch is there (yet), and it is above the floor
    boolean hollow(int cx, int cy, int cz, int tick) {
        if (cy < 0) return false;
        int i = patch.find(cx, cy, cz);
        return i < 0 || (mined[i] && delay[i] <= tick);
    }

    private static int cell(double v) {
        return (int) Math.floor(v + 0.5);
    }

    // Chain.landingSpot: 1.3 blocks from the struck block towards the player's eyes, dropped to the floor
    private double[] landingSpot() {
        double dx = FEET[0], dy = EYE - 0.5, dz = FEET[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double px = dx / len * Arc.LAND_DISTANCE, py = 0.5 + dy / len * Arc.LAND_DISTANCE, pz = dz / len * Arc.LAND_DISTANCE;
        if (!hollow(cell(px), (int) Math.floor(py), cell(pz), 0)) return new double[] {0, 0.5, 0};
        double y = py;
        for (int i = 0; i < FLOOR_STEPS && airAt(px, y - Arc.FLOOR_PROBE, pz); i++) y -= Arc.FLOOR_STEP;
        if (!hollow(cell(px), (int) Math.floor(y - Arc.FLOOR_PROBE), cell(pz), 0)) y = Math.floor(y);
        return new double[] {px, y, pz};
    }

    private boolean airAt(double x, double y, double z) {
        int cy = (int) Math.floor(y);
        return cy >= 0 && hollow(cell(x), cy, cell(z), 0);
    }

    // ---- the chain's loot ----

    // Chain.loot and ghost: one ghost per broken block, at most 3 shown per tick and 12 per chain
    private void makeGhosts() {
        int shown = 0;
        for (int age = 0; age <= maxDelay; age++) {
            int visTurn = 0;
            for (int cell : order) {
                if (delay[cell] != age) continue;
                Ghost g = new Ghost();
                boolean hide = visTurn >= Arc.MAX_VISIBLE_PER_TURN || shown >= Arc.MAX_VISIBLE_PER_CHAIN;
                if (!hide) {
                    visTurn++;
                    shown++;
                }
                int[] c = patch.at[cell];
                g.cell = cell;
                g.born = age;
                g.cube = subject.item() == null;
                g.lo = opts.own();
                g.visible = !hide;
                g.start = new double[] {c[0], c[1] + 0.5, c[2]};
                g.amp = (opts.own() ? Arc.BUMP_LO : Arc.BUMP_HI) * Tune.arc();
                spotFor(g, age);
                ghosts.add(g);
                lastTick = Math.max(lastTick, age + Arc.LAUNCH + Arc.LAND);
            }
        }
    }

    private static double yaw(double dx, double dz) {
        return Math.atan2(dz, dx) - Math.PI / 2;
    }

    // Chain.spotFor: one of the 8 spread spots round the landing point, or 0.9 in front of the player's feet
    private void spotFor(Ghost g, int age) {
        double rest = Arc.rest(g.cube);
        double[] base;
        if (g.lo) {
            double y = yaw(g.start[0] - FEET[0], g.start[2] - FEET[2]);
            base = new double[] {FEET[0] - Math.sin(y) * Arc.OWN_DISTANCE, FEET[1], FEET[2] + Math.cos(y) * Arc.OWN_DISTANCE};
        } else {
            if (g.visible && ++dir >= Arc.SPREAD.length) dir = 0;
            double y = yaw(FEET[0] - landing[0], FEET[2] - landing[2]);
            double left = Arc.SPREAD[dir][0], fwd = Arc.SPREAD[dir][1];
            double cx = landing[0] + Math.cos(y) * left - Math.sin(y) * fwd, cz = landing[2] + Math.sin(y) * left + Math.cos(y) * fwd;
            boolean free = hollow(cell(cx), (int) Math.floor(landing[1] + Arc.HOLLOW_PROBE), cell(cz), age);
            base = free ? new double[] {cx, landing[1], cz} : landing;
        }
        g.spot = new double[] {base[0], base[1] + rest, base[2]};
    }

    // ---- no chain: every block breaks as in vanilla, its drop popping out of it ----

    private void makePops() {
        Random r = new Random(5);
        for (int cell : order) {
            Pop p = new Pop();
            int[] c = patch.at[cell];
            p.cell = cell;
            p.start = new double[] {c[0], c[1] + 0.375, c[2]};
            p.v = new double[] {r.nextDouble() * 0.2 - 0.1, 0.2, r.nextDouble() * 0.2 - 0.1};
            // where sand or gravel falls into the hole, the drop is thrown out in front of it
            if (subject.falls() && cell == 0) p.v[0] = 0.16 + r.nextDouble() * 0.05;
            pops.add(p);
        }
        lastTick = Math.max(lastTick, 26);
    }

    // Vein.toFeet: a vein without the chain can send its drops straight to the player; a lone block's stays where it broke
    boolean toFeet() {
        return !opts.chain() && opts.own() && order.size() > 1;
    }

    // the height of the floor under (x, z): the top of what stays standing there
    double ground(double x, double z) {
        int cx = cell(x), cz = cell(z);
        double top = 0;
        for (int i = 0; i < n; i++) {
            int[] c = patch.at[i];
            if (c[0] == cx && c[2] == cz && !mined[i]) top = Math.max(top, c[1] + 1 - (fallStart[i] >= 0 ? fallDrop[i] : 0));
        }
        return top;
    }

    // ItemEntity's flight after popResource: {x, base y, z} {@code t} ticks after the break
    double[] pop(Pop p, double t) {
        double x = p.start[0], y = p.start[1], z = p.start[2], vx = p.v[0], vy = p.v[1], vz = p.v[2];
        double px = x, py = y, pz = z;
        for (int i = 0; i <= (int) t; i++) {
            px = x;
            py = y;
            pz = z;
            vy -= 0.04;
            x += vx;
            y += vy;
            z += vz;
            double g = ground(x, z);
            if (y <= g) {
                y = g;
                if (vy < 0) vy *= -0.5;
                vx *= 0.588;
                vz *= 0.588;
            } else {
                vx *= 0.98;
                vz *= 0.98;
            }
            vy *= 0.98;
        }
        double a = t - (int) t;
        return new double[] {px + (x - px) * a, py + (y - py) * a, pz + (z - pz) * a};
    }

    // ---- sand and gravel: what is left over falls once the block under it is gone ----

    private void fall() {
        if (!subject.falls()) return;
        for (int i = 0; i < n; i++) {
            if (mined[i]) continue;
            int[] c = patch.at[i];
            int below = 0, support = -1;
            for (int j = 0; j < n; j++) {
                int[] d = patch.at[j];
                if (j == i || d[0] != c[0] || d[2] != c[2] || d[1] >= c[1] || mined[j]) continue;
                below++;
            }
            int gone = 0;
            for (int j = 0; j < n; j++) {
                int[] d = patch.at[j];
                if (d[0] != c[0] || d[2] != c[2] || d[1] >= c[1] || !mined[j]) continue;
                gone++;
                support = Math.max(support, delay[j]);
            }
            if (gone == 0) continue;
            int rank = 0;
            for (int j = 0; j < n; j++) {
                int[] d = patch.at[j];
                if (!mined[j] && d[0] == c[0] && d[2] == c[2] && d[1] < c[1]) {
                    boolean falling = false;
                    for (int k = 0; k < n; k++) {
                        int[] e = patch.at[k];
                        if (mined[k] && e[0] == c[0] && e[2] == c[2] && e[1] < d[1]) falling = true;
                    }
                    if (falling) rank++;
                }
            }
            fallStart[i] = support + 2 * (rank + 1);
            fallDrop[i] = c[1] - below;
            lastTick = Math.max(lastTick, fallStart[i] + 10 + 3 * fallDrop[i]);
        }
    }

    // ---- the stand-in's squash ----

    // {width, height} of a block {@code o} ticks after it turned to air, as StandKeys keys it (about the block's centre)
    static double[] stand(double o, double squash) {
        double wx = 1, wy = 1;
        for (StandKeys.Key k : StandKeys.KEYS) {
            double tx = StandKeys.tuned(k.scaleXz(), squash), ty = StandKeys.tuned(k.scaleY(), squash);
            if (o < k.tick()) break;
            if (o < k.tick() + k.duration()) {
                double u = (o - k.tick()) / k.duration();
                return new double[] {wx + (tx - wx) * u, wy + (ty - wy) * u};
            }
            wx = tx;
            wy = ty;
        }
        return new double[] {wx, wy};
    }
}
