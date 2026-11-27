package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.veinminer.core.Arc;
import io.github.profetgit.veinminer.core.StandKeys;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Tune;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

// A vein breaking in the preview box: a small floating patch of the player's own ore blocks in a pool of light, struck by
// the player's own tool, then the chain, the loot and what vanilla does instead, drawn from the mod's own functions
// (Delays, StandKeys, Arc) with the config as it is right now. Only vanilla's own physics (items, particles, falling
// blocks) is repeated here. The stage is drawn at half size, so every texture pixel is one GUI pixel, items and tool too.
abstract class VeinScene extends Preview {
    static final float MS = 50;
    static final float S = 0.5f;
    // ms: the struck block breaks (the payoff, inside 1.5 s), the swings before it, how long loot rests and shrinks at the end
    static final float BREAK = 1000, HIT1 = BREAK - 300, RAISE = 250, REST = 200, TAIL = 380;
    static final float PRE = 500;
    private static final float POP_LEN = 240, POP_STEP = 30, SQUASH = 120;
    private static final int[][] COPIES = {{-5, -3}, {5, -4}, {-3, -8}, {6, -9}};

    static Identifier tex(String block) {
        return Identifier.tryParse("textures/block/" + block + ".png");
    }

    static Identifier itemTex(String item) {
        return Identifier.tryParse("textures/item/" + item + ".png");
    }

    // a lang string, or this text when the player's language has none yet
    static String t(String key, String fallback, Object... args) {
        String full = "veinminer.preview." + key;
        return net.minecraft.locale.Language.getInstance().has(full) ? I18n.get(full, args) : String.format(fallback, args);
    }

    private Run run;
    private Run.Opts runOpts;
    private Subject runSubject;
    private int[] anchor;

    abstract Subject subject();

    // the options this scene plays with: the config as it is now
    Run.Opts opts(Config.Values c) {
        return new Run.Opts(true, c.animation(), Math.max(c.maxBlocks(), 12), c.diagonal(), c.drops() == 1, c.durability());
    }

    // the orange line saying why the scene shows vanilla, or null
    String off(Config.Values c) {
        return null;
    }

    // what else the scene draws (a key, the tool's icon, the hotbar), over the stage; time in ms, tau in ticks since the break
    void overlay(Gfx g, Ctx c, int x, int y, int w, int h, float time, double tau, Run run) {}

    // extra ms the loop lasts after the loot has gone
    float hold(Run run) {
        return 0;
    }

    // the band of the box the patch, its loot and the tool are centred in: top and height, in GUI pixels
    int[] area() {
        return new int[] {8, 104};
    }

    // how far below the struck block's floor the loot and the player reach, in stage pixels
    int lootBottom() {
        return 62;
    }

    // a number that restarts the loop when it changes
    int signature(Config.Values c) {
        return 0;
    }

    // where picked-up loot flies to, in box pixels (the centre of a hotbar slot), or null
    float[] slotTarget(int x, int y, int w) {
        return null;
    }

    float boxX, boxY, boxW, period, stageX, stageY;
    // set by a preset preview that plays two scenes one after the other: no tail of its own loot, no drop-in at the end,
    // the loot fades out instead; {@code blocksOnly} draws just the blocks (the next scene dropping in)
    boolean chained, blocksOnly, hideTool;

    // how long one play lasts with the options as they are now
    float loopMs() {
        Run r = run(opts(Previews.config()), subject());
        return length(r);
    }

    // a chained scene rests 100 ms after the last landing, fades its loot over 250 ms, then the next scene drops in over PRE
    private float length(Run r) {
        return chained ? BREAK + r.lastTick * MS + 100 + 250 + PRE + hold(r) : BREAK + r.lastTick * MS + REST + hold(r);
    }

    private Run run(Run.Opts o, Subject s) {
        if (run == null || !o.equals(runOpts) || s != runSubject) {
            run = new Run(s, o);
            runOpts = o;
            runSubject = s;
            anchor = null;
        }
        return run;
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        Config.Values cfg = Previews.config();
        Subject sub = subject();
        Run.Opts o = opts(cfg);
        Run run = run(o, sub);
        period = length(run);
        boxX = x;
        boxY = y;
        boxW = w;
        float time = clock(c, Objects.hash(o, sub.name(), signature(cfg), Tune.delayK(), Tune.arc(), Tune.squash()), period);
        double tau = (time - BREAK) / MS;
        if (anchor == null) anchor = anchor(run, sub);
        int ox = x + (w - Diorama.W) / 2;
        Diorama.backdrop(g, ox, y);
        float x0 = Math.round(x + w / 2f + anchor[0]), y0 = Math.round(y + anchor[1]);
        stageX = x0;
        stageY = y0;
        g.push();
        g.translate(x0, y0);
        g.scale(S, S);
        stage(g, time, tau, run, sub);
        g.pop();
        if (blocksOnly) return;
        overlay(g, c, x, y, w, h, time, tau, run);
        String off = off(cfg);
        if (off != null) Diorama.note(g, x, y, w, h, off);
    }

    // where the struck block's floor centre goes in the box, so that the patch, its loot and the tool are centred
    private int[] anchor(Run run, Subject sub) {
        double minX = -40, maxX = 70, minY = -60, maxY = lootBottom();
        for (int[] c : sub.patch().at) {
            double sx = Diorama.sx(0, c[0], c[2]), sy = (c[0] + c[2]) * Diorama.HZ - c[1] * Diorama.TALL;
            minX = Math.min(minX, sx - 32);
            maxX = Math.max(maxX, sx + 32);
            minY = Math.min(minY, sy - 56);
            maxY = Math.max(maxY, sy + 16);
        }
        minY -= 20;
        double hh = (maxY - minY) * S;
        int[] band = area();
        double top = band[0] + (band[1] - hh) / 2;
        return new int[] {(int) Math.round(-(minX + maxX) / 2 * S), (int) Math.round(top - minY * S)};
    }

    // ---- the stage ----

    private record Part(double key, Runnable draw) {}

    // the loot of the last play fading out while this one starts (the box is never empty), as {tau, alpha}
    private double[] tail(float time) {
        if (time >= TAIL) return null;
        return new double[] {(time + period - BREAK) / MS, 1 - time / TAIL};
    }

    private void stage(Gfx g, float time, double tau, Run run, Subject sub) {
        Patch p = sub.patch();
        List<Part> parts = new ArrayList<>();
        double[] tail = chained || blocksOnly ? null : tail(time);
        // shadows first: the blocks stand in the pool of light, on nothing
        for (int i = 0; i < run.n; i++) {
            int[] c = p.at[i];
            if (c[1] != 0) continue;
            double[] sc = cellPose(run, i, time, tau);
            if (sc[0] > 0.01 && sc[4] <= 0.05) Diorama.shadow(g, Diorama.sx(0, c[0], c[2]), Diorama.sy(0, c[0], 0, c[2]), (float) sc[0], 0.9f);
        }
        for (int i = 0; i < run.n; i++) {
            int[] c = p.at[i];
            double[] sc = cellPose(run, i, time, tau);
            if (sc[0] > 0.001) {
                int cell = i;
                parts.add(new Part(c[0] + c[1] + c[2] + 0.5, () -> Diorama.box(g, Diorama.sx(0, c[0], c[2]), Diorama.sy(0, c[0], c[1] + sc[4], c[2]), sub.lookOf(cell), sc[0], sc[1], sc[2], (int) sc[3])));
            }
            if (!chained && !blocksOnly && time >= period - PRE && run.fallStart[i] >= 0 && time - (period - PRE) < 200) {
                int cell = i;
                double y = c[1] - run.fallDrop[i];
                float a = 1 - (time - (period - PRE)) / 200f;
                parts.add(new Part(c[0] + y + c[2] + 0.45, () -> {
                    float was = g.alpha;
                    g.alpha *= a;
                    Diorama.box(g, Diorama.sx(0, c[0], c[2]), Diorama.sy(0, c[0], y, c[2]), sub.lookOf(cell), 1, 0, 1, -1);
                    g.alpha = was;
                }));
            }
        }
        if (!blocksOnly) loot(parts, g, run, sub, tau, chained ? Math.max(0, Math.min(1, (period - PRE - time) / 250f)) : 1);
        if (tail != null) loot(parts, g, run, sub, tail[0], (float) tail[1]);
        parts.sort(Comparator.comparingDouble(Part::key));
        for (Part part : parts) part.draw().run();
        if (!hideTool) sub.tool().draw(g, -Diorama.AX, -Diorama.AY, swing(time));
        if (!blocksOnly) chips(g, run, sub, tau);
    }

    private void loot(List<Part> parts, Gfx g, Run run, Subject sub, double tau, float alpha) {
        if (run.chainMode) {
            for (Run.Ghost gh : run.ghosts) if (gh.visible) addGhost(parts, g, sub, gh, tau, alpha);
        } else {
            for (Run.Pop pop : run.pops) addPop(parts, g, run, sub, pop, tau, alpha);
        }
    }

    // a block's pose now: {width, lowest, highest, crack stage, height above its place}; width 0 when it is not there
    private double[] cellPose(Run run, int i, float time, double tau) {
        double wide = 1, lo = 0, hi = 1, lift = 0;
        int crack = -1;
        boolean returns = run.mined[i] || run.fallStart[i] >= 0;
        // the vein comes back at the end of the loop, over the last PRE ms: each block drops in at full size with a squash
        boolean pre = returns && time >= period - PRE && !(chained && !blocksOnly);
        if (pre) {
            int[] c = run.patch.at[i];
            double dist = Math.sqrt(c[0] * c[0] + c[1] * c[1] + c[2] * c[2]);
            double u = (time - (period - PRE) - dist * POP_STEP) / POP_LEN;
            if (u < 0) return new double[] {0, 0, 0, -1, 0};
            if (u < 1) lift = 0.5 * (1 - u * u);
            else if (u < 1 + SQUASH / POP_LEN) {
                double k = Math.sin(Math.PI * (u - 1) * POP_LEN / SQUASH);
                hi = 1 - 0.12 * k;
                wide = 1 + 0.06 * k;
            }
            return new double[] {wide, lo, hi, -1, lift};
        }
        if (run.mined[i]) {
            if (i == 0) {
                if (tau >= 0) return new double[] {0, 0, 0, -1, 0};
                if (time >= HIT1) crack = (int) ((time - HIT1) / (BREAK - HIT1) * 10);
            } else if (run.chainMode) {
                double o = tau - run.delay[i];
                if (o >= StandKeys.KILL) return new double[] {0, 0, 0, -1, 0};
                if (o >= 0) {
                    double[] k = Run.stand(o, Tune.squash());
                    wide = k[0];
                    lo = 0.5 - 0.5 * k[1];
                    hi = 0.5 + 0.5 * k[1];
                }
            } else if (tau >= 0) {
                return new double[] {0, 0, 0, -1, 0};
            }
        }
        double fall = run.fallStart[i] < 0 || tau < run.fallStart[i] ? 0 : fallY(run, i, tau);
        return new double[] {wide, lo, hi, crack, lift + fall};
    }

    // how far a sand or gravel block that lost its support has dropped, in blocks (negative)
    private double fallY(Run run, int i, double tau) {
        double[] f = fall(0, run.fallDrop[i], 0, 0, tau - run.fallStart[i]);
        return f[1] - run.fallDrop[i];
    }

    // ---- the tool ----

    static int rest() {
        return Diorama.FOLLOW + Diorama.FOLLOW_FRAMES - 1;
    }

    private static int swing(float time) {
        if (time < HIT1 - RAISE) return rest();
        if (time < HIT1) return Diorama.RAISE + frame(time - (HIT1 - RAISE), Diorama.RAISE_FRAMES);
        if (time >= BREAK) return Diorama.FOLLOW + frame(time - BREAK, Diorama.FOLLOW_FRAMES);
        return Diorama.SWING + (int) ((time - HIT1) * 60 / 1000 + 0.01f) % Diorama.SWING_FRAMES;
    }

    static int frame(float ms, int count) {
        return Math.max(0, Math.min(count - 1, (int) (ms * 60 / 1000)));
    }

    // ---- vanilla's physics ----

    static double bob(double t) {
        return 0.1 - 0.1 * Math.cos(t / 10);
    }

    // an item entity's fall after {@code t} ticks: {x, y, ticks since it touched down or -1}
    static double[] fall(double x, double y, double vx, double vy, double t) {
        double px = x, py = y, landed = -1;
        for (int i = 0; i <= (int) t; i++) {
            px = x;
            py = y;
            if (landed < 0) {
                vy -= 0.04;
                x += vx;
                y += vy;
                vx *= 0.98;
                vy *= 0.98;
                if (y <= 0) {
                    y = 0;
                    landed = i + 1;
                }
            }
        }
        double a = t - (int) t;
        return new double[] {px + (x - px) * a, py + (y - py) * a, landed < 0 || t < landed ? -1 : t - landed};
    }

    // ---- the loot ----

    // each dropped item turns from its own angle (the golden angle apart)
    static double phase(int cell) {
        return cell * 2.399963 + 0.7;
    }

    // ticks a picked-up stack takes to fly into the hotbar
    static final double PICKUP = 5;

    private void addGhost(List<Part> parts, Gfx g, Subject sub, Run.Ghost gh, double tau, float alpha) {
        double o = tau - gh.born - Arc.LAUNCH;
        if (o < 0) return;
        double[] at;
        double wide = 1, tall = 1, spin;
        boolean anchored = false;
        float[] target = null;
        if (o < Arc.FLIGHT) {
            double l = (Arc.FLIGHT - o) / Arc.FLIGHT;
            int k = (int) o;
            double bump = Arc.bump(gh.amp, k) + (Arc.bump(gh.amp, k + 1) - Arc.bump(gh.amp, k)) * (o - k);
            at = new double[] {gh.spot[0] + (gh.start[0] - gh.spot[0]) * l, gh.spot[1] + (gh.start[1] - gh.spot[1]) * l + bump,
                gh.spot[2] + (gh.start[2] - gh.spot[2]) * l};
            double s = o < Arc.GROW ? Arc.GROW_SCALE * o : Arc.GROW_SCALE + (1 - Arc.GROW_SCALE) * (o - Arc.GROW) / (Arc.FLIGHT - Arc.GROW);
            wide = tall = s;
            spin = o < Arc.GROW ? Arc.SPIN_GROW * o : Arc.SPIN_GROW + (Arc.SPIN_END - Arc.SPIN_GROW) * (o - Arc.GROW) / (Arc.FLIGHT - Arc.GROW);
        } else if (o < Arc.LAND) {
            at = new double[] {gh.spot[0], gh.spot[1], gh.spot[2]};
            wide = Arc.SQUASH_XZ;
            tall = Arc.SQUASH_Y;
            spin = Arc.SPIN_END;
            anchored = true;
        } else {
            double t = o - Arc.LAND;
            float[] slot = gh.lo ? slotTarget((int) boxX, (int) boxY, (int) boxW) : null;
            if (gh.lo && slot != null) {
                // picked up at once: the stack flies into the player's hotbar
                if (t >= PICKUP) return;
                double u = t / PICKUP;
                u = u * u * (3 - 2 * u);
                at = new double[] {gh.spot[0], gh.spot[1], gh.spot[2]};
                target = new float[] {(float) u, slot[0], slot[1]};
                spin = 0;
            } else if (gh.lo) {
                if (t >= 3.5) return;
                double u = Math.min(1, t / 3);
                at = new double[] {Run.FEET[0], 0.1875 + 0.9 * u * u, Run.FEET[2]};
                wide = tall = 1 - 0.3 * u;
                spin = 0;
            } else {
                double[] f = fall(0, 0, 0, Arc.HOP, t);
                at = new double[] {gh.spot[0], gh.spot[1] + f[1] + (f[2] < 0 ? 0 : bob(f[2])), gh.spot[2]};
                spin = t / 20 + phase(gh.cell);
            }
        }
        double[] fin = at;
        double w2 = wide, t2 = tall, sp = spin;
        boolean an = anchored;
        float[] tg = target;
        parts.add(new Part(fin[0] + fin[1] + fin[2] + 0.4 + (tg != null ? 50 : 0), () -> drop(g, sub, fin, w2, t2, sp, alpha, an, gh.cube, tg)));
    }

    private void addPop(List<Part> parts, Gfx g, Run run, Subject sub, Run.Pop pop, double tau, float alpha) {
        if (tau < 0) return;
        if (run.toFeet()) {
            if (tau >= PICKUP) return;
            double u = tau / PICKUP;
            u = u * u * (3 - 2 * u);
            double[] from = {pop.start[0], pop.start[1] + 0.1875, pop.start[2]};
            float[] slot = slotTarget((int) boxX, (int) boxY, (int) boxW);
            float[] target = slot == null ? null : new float[] {(float) u, slot[0], slot[1]};
            if (slot == null) for (int k = 0; k < 3; k++) from[k] += (Run.FEET[k] + (k == 1 ? 0.1875 : 0) - from[k]) * u;
            parts.add(new Part(from[0] + from[1] + from[2] + 50.4, () -> drop(g, sub, from, 1, 1, phase(pop.cell), alpha, false, sub.item() == null, target)));
            return;
        }
        double[] at = run.pop(pop, tau);
        double[] fin = {at[0], at[1] + 0.1875, at[2]};
        double sp = tau / 20 + phase(pop.cell);
        parts.add(new Part(fin[0] + fin[1] + fin[2] + 0.4, () -> drop(g, sub, fin, 1, 1, sp, alpha, false, sub.item() == null, null)));
    }

    // one drop: a flat item turned by {@code spin} about its upright axis, or a small block, centred on {@code at}; with a
    // target {u, x, y} it is that far on its way to the box point (x, y)
    private void drop(Gfx g, Subject sub, double[] at, double wide, double tall, double spin, double alpha, boolean anchored, boolean cube, float[] target) {
        float sx = Diorama.sx(0, at[0], at[2]);
        float floorY = Diorama.sy(0, at[0], 0, at[2]);
        float cy = Diorama.sy(0, at[0], at[1], at[2]);
        float a = g.alpha;
        g.alpha *= (float) alpha;
        double height = Math.max(0, at[1] - 0.1875);
        if (target != null) {
            float tx = (target[1] - stageX) / S, ty = (target[2] - stageY) / S;
            sx += (tx - sx) * target[0];
            cy += (ty - cy) * target[0];
            height = 1;
        }
        if (cube) {
            double edge = 0.25 * wide, bottom = at[1] - 0.125 * wide;
            if (target == null) Diorama.groundShadow(g, sx, floorY, 1.1f, height);
            float bx = target == null ? sx : sx, by = target == null ? floorY : floorY + (cy - Diorama.sy(0, at[0], at[1], at[2]));
            Diorama.box(g, bx, by, sub.cube(), edge, bottom, bottom + 0.25 * tall, -1);
        } else {
            double face = Math.abs(Math.cos(Math.PI / 4 - spin));
            if (target == null) Diorama.groundShadow(g, sx, floorY, (float) (1.3 * (0.6 + 0.4 * Math.max(0.4, face))), height);
            double wd = Math.max(0.4, face) * wide;
            boolean back = Math.cos(Math.PI / 4 - spin) < 0;
            if (anchored) cy += (float) (16 * (1 - tall));
            int copies = sub.count() <= 1 ? 1 : sub.count() <= 16 ? 2 : 3;
            if (target != null) copies = 1;
            g.pushTranslate(sx, cy);
            g.scale(2, 2);
            for (int i = copies - 2; i >= 0; i--) {
                g.pushTranslate(COPIES[i][0] / 2f * (float) wd, COPIES[i][1] / 2f * (float) tall);
                sprite(g, sub.item(), wd, tall, back, 0xFFB4ACB4);
                g.pop();
            }
            sprite(g, sub.item(), wd, tall, back, 0xFFFFFFFF);
            g.pop();
        }
        g.alpha = a;
    }

    // the item's 16 x 16 pixels centred on the origin, {@code wd} x {@code tall} of their size
    private static void sprite(Gfx g, Identifier item, double wd, double tall, boolean back, int tint) {
        g.push();
        g.scale((float) wd, (float) tall);
        if (back) g.blit(item, 8, -8, 16, 0, -16, 16, -16, 16, 16, 16, tint);
        else g.blit(item, -8, -8, 0, 0, 16, 16, 16, 16, tint);
        g.pop();
    }

    // ---- chips ----

    // vanilla's break particles of the struck block (and of every block, when there is no chain), the mod's crumbs where
    // the chain turns a block into its stand-in; they are gone after 14 ticks
    private void chips(Gfx g, Run run, Subject sub, double tau) {
        for (int i : run.order) {
            int[] c = run.patch.at[i];
            Diorama.Look look = sub.lookOf(i);
            boolean crumbs = i != 0 && run.chainMode;
            double at = crumbs ? run.delay[i] + StandKeys.VANISH + 1 : 0;
            double age = tau - at;
            if (age < 0 || age > 16) continue;
            Random r = new Random(31L * i + 7);
            int count = crumbs ? 12 : 10;
            double[] off = crumbs ? offset(run, i, (int) at) : new double[] {0, 0, 0};
            for (int k = 0; k < count; k++) {
                double x = c[0] + off[0] * 0.6 + (r.nextDouble() - 0.5) * (crumbs ? 0.4 : 0.8);
                double y = c[1] + 0.5 + off[1] * 0.6 + (r.nextDouble() - 0.5) * (crumbs ? 0.4 : 0.8);
                double z = c[2] + off[2] * 0.6 + (r.nextDouble() - 0.5) * (crumbs ? 0.4 : 0.8);
                double[] v = crumbs ? new double[] {0, 0, 0} : new double[] {(x - c[0]) * 0.3, (y - c[1] - 0.5) * 0.3 + 0.04, (z - c[2]) * 0.3};
                if (crumbs) for (int a = 0; a < 3; a++) v[a] = (r.nextDouble() - 0.5) * 0.12;
                Diorama.chip(g, r, 0, 0, look.left(), (float) age, x, Math.max(y, 0.05), z, v, 1.0, 14);
            }
        }
    }

    // the cell the stand-in stood in: toward the player if open, else the first open one (Chain.prep)
    private double[] offset(Run run, int i, int tick) {
        int[] c = run.patch.at[i];
        String face = StandKeys.face(Run.FEET[0], Run.EYE - 0.5, Run.FEET[2]);
        List<String> tries = new ArrayList<>();
        tries.add(face);
        for (String s : StandKeys.ORDER) tries.add(s);
        for (String s : tries) {
            int[] o = StandKeys.offset(s);
            if (run.hollow(c[0] + o[0], c[1] + o[1], c[2] + o[2], tick)) return new double[] {o[0], o[1], o[2]};
        }
        return new double[] {0, 0, 0};
    }
}
