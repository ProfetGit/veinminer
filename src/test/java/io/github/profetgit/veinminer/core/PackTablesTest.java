package io.github.profetgit.veinminer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The Java tables against the 1.3.3 data pack's generated functions ({@code packs/Veinminer/dev/gen.py} output): a typo fails here. */
class PackTablesTest {
    private static Path anim;

    @BeforeAll
    static void find() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            Path c = p.resolve("packs/Veinminer/pack/data/veinminer/function/anim");
            if (Files.isDirectory(c)) {
                anim = c;
                return;
            }
        }
        assumeTrue(false, "packs/Veinminer anim data not found from " + Path.of("").toAbsolutePath());
    }

    private static List<String> lines(String name) throws IOException {
        return Files.readAllLines(anim.resolve(name + ".mcfunction"));
    }

    private static double[] numbers(String text, Pattern p) {
        Matcher m = p.matcher(text);
        List<Double> out = new ArrayList<>();
        while (m.find()) out.add(Double.parseDouble(m.group(1)));
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private record Band(int k, double radius) {}

    private static List<Band> bands(String tier) throws IOException {
        Pattern set = Pattern.compile("players set @e\\[type=marker,tag=veinminer.pnew,distance=\\.\\.([0-9.]+)\\] veinminer.t (\\d+)");
        List<Band> out = new ArrayList<>();
        for (String l : lines("rings_" + tier)) {
            Matcher m = set.matcher(l);
            if (m.find()) out.add(new Band(Integer.parseInt(m.group(2)), Double.parseDouble(m.group(1))));
        }
        return out;
    }

    private static final String[] TIERS = {"a", "b", "c"};

    @Test
    void ringBandsMatchTheDelayFormula() throws IOException {
        for (int t = 0; t < TIERS.length; t++) {
            double kk = Delays.TIER_K[t];
            List<Band> bands = bands(TIERS[t]);
            assertEquals(Delays.MAX - 1, bands.size(), "bands of tier " + TIERS[t]);
            for (Band b : bands) {
                assertEquals(Delays.radius(b.k, kk) - 0.0005, b.radius, 0.00006, "tier " + TIERS[t] + " k " + b.k);
            }
            assertTrue(lines("rings_" + TIERS[t]).stream().anyMatch(l -> l.contains("veinminer.t " + Delays.MAX)), "last band is " + Delays.MAX);
        }
    }

    @Test
    void everyLatticeDistanceGetsThePacksDelay() throws IOException {
        for (int t = 0; t < TIERS.length; t++) {
            List<Band> bands = bands(TIERS[t]);
            for (int n2 = 1; n2 <= 6000; n2++) {
                double d = Math.sqrt(n2);
                int expect = Delays.MAX;
                for (Band b : bands) {
                    if (d <= b.radius) {
                        expect = b.k;
                        break;
                    }
                }
                assertEquals(expect, Delays.delay(d, Delays.TIER_K[t]), "tier " + TIERS[t] + " distance^2 " + n2);
            }
        }
    }

    @Test
    void tierPickMatchesTheSwitchFile() throws IOException {
        Pattern p = Pattern.compile("distance=([0-9.]+)\\.\\.,limit=1\\] run return run function veinminer:anim/rings_(\\w)");
        int seen = 0;
        for (String l : lines("rings")) {
            Matcher m = p.matcher(l);
            if (!m.find()) continue;
            int tier = "abc".indexOf(m.group(2));
            assertEquals(Double.parseDouble(m.group(1)), Delays.far(Delays.TIER_K[tier - 1]), 1e-9, "switch to tier " + m.group(2));
            seen++;
        }
        assertEquals(Delays.TIER_K.length - 1, seen);
    }

    /** The tier files' own bookkeeping (rings counted per band, #maxd) replayed on random blobs, against {@link Delays#plan}. */
    @Test
    void ringsAndLastTurnMatchTheTierFiles() throws IOException {
        Random rnd = new Random(7);
        for (int run = 0; run < 400; run++) {
            int size = 1 + rnd.nextInt(80);
            int spread = 1 + rnd.nextInt(4 + run % 12);
            double[] dist = new double[size];
            for (int i = 0; i < size; i++) {
                int x = rnd.nextInt(2 * spread + 1) - spread, y = rnd.nextInt(2 * spread + 1) - spread, z = rnd.nextInt(2 * spread + 1) - spread;
                dist[i] = Math.max(1, Math.sqrt(x * x + y * y + z * z));
            }
            Delays.Plan plan = Delays.plan(dist, 1.0);
            int rings = 0, maxd = 0;
            List<Band> bands = bands(TIERS[plan.tier()]);
            List<Double> left = new ArrayList<>();
            for (double d : dist) left.add(d);
            for (Band b : bands) {
                int before = left.size();
                left.removeIf(d -> d <= b.radius);
                if (left.size() < before) rings++;
                if (left.isEmpty()) {
                    maxd = b.k;
                    break;
                }
            }
            if (!left.isEmpty()) {
                rings++;
                maxd = Delays.MAX;
            }
            assertEquals(rings, plan.rings(), "rings of run " + run);
            assertEquals(maxd, plan.maxDelay(), "last turn of run " + run);
        }
    }

    @Test
    void pitchStepAndRingPitches() throws IOException {
        List<String> step = lines("pitch_step");
        assertTrue(step.stream().anyMatch(l -> l.contains("set #ps veinminer.data 650")));
        assertTrue(step.stream().anyMatch(l -> l.contains("matches 71.. run scoreboard players set #ps veinminer.data " + Delays.MAX_PITCH_STEP)));
        List<String> go = lines("ring_go");
        assertTrue(go.stream().anyMatch(l -> l.contains("add #p veinminer.data " + Delays.BASE_PITCH)));
        assertTrue(go.stream().anyMatch(l -> l.contains("matches " + (Delays.MAX_PITCH + 1) + ".. run scoreboard players set #p veinminer.data " + Delays.MAX_PITCH)));
        assertTrue(go.stream().anyMatch(l -> l.contains("add #p veinminer.data " + Delays.PLOP_ADD)));
        assertTrue(go.stream().anyMatch(l -> l.contains("set #ps veinminer.data " + Delays.MAX_PITCH_STEP)));
        for (int rings = 2; rings <= 40; rings++) assertEquals(Math.min(70, 650 / (rings - 1)), Delays.pitchStep(rings));
        assertEquals(70, Delays.pitchStep(1));
        for (int rings = 1; rings <= 40; rings++) {
            int s = Delays.pitchStep(rings);
            for (int r = 0; r < rings; r++) {
                assertTrue(Delays.pitch(r, s) <= Delays.MAX_PITCH, "pitch ring " + r);
                assertTrue(Delays.plop(r, s) <= Delays.MAX_PLOP, "plop ring " + r + " of " + rings);
            }
        }
        assertEquals(1500, Delays.pitch(39, 70));
        assertEquals(2000, Delays.plop(39, 70));
    }

    private static final Pattern KEY = Pattern.compile("tag=veinminer.at_(\\w+),scores=\\{veinminer.t=(-\\d+)\\}\\] run data merge entity @s \\{start_interpolation:0,interpolation_duration:(\\d+),"
        + "transformation:\\{translation:\\[([^\\]]+)\\],scale:\\[([^\\]]+)\\]\\}\\}");

    private static double[] floats(String csv) {
        String[] p = csv.split(",");
        double[] out = new double[p.length];
        for (int i = 0; i < p.length; i++) out[i] = Double.parseDouble(p[i].replace("f", ""));
        return out;
    }

    @Test
    void standKeysMatchTheKeyFiles() throws IOException {
        for (StandKeys.Key k : StandKeys.KEYS) {
            int seen = 0;
            for (String l : lines("key/k" + k.tick())) {
                Matcher m = KEY.matcher(l);
                if (!m.find()) continue;
                assertEquals(-k.tick(), Integer.parseInt(m.group(2)));
                assertEquals(k.duration(), Integer.parseInt(m.group(3)), "duration of key " + k.tick());
                int[] n = StandKeys.offset(m.group(1));
                double[] tr = floats(m.group(4)), sc = floats(m.group(5));
                assertEquals(k.scaleXz(), sc[0], 1e-4);
                assertEquals(k.scaleY(), sc[1], 1e-4);
                assertEquals(k.scaleXz(), sc[2], 1e-4);
                assertEquals(StandKeys.translation(k.scaleXz(), n[0]), tr[0], 1e-4, "x of key " + k.tick() + " in " + m.group(1));
                assertEquals(StandKeys.translation(k.scaleY(), n[1]), tr[1], 1e-4, "y of key " + k.tick() + " in " + m.group(1));
                assertEquals(StandKeys.translation(k.scaleXz(), n[2]), tr[2], 1e-4, "z of key " + k.tick() + " in " + m.group(1));
                seen++;
            }
            assertEquals(7, seen, "cells of key " + k.tick());
        }
        List<String> keys = lines("keys");
        assertEquals(StandKeys.KEYS.length, keys.size());
        for (StandKeys.Key k : StandKeys.KEYS) assertTrue(keys.stream().anyMatch(l -> l.contains("veinminer.t=-" + k.tick() + "}") && l.endsWith("key/k" + k.tick())), "keys " + k.tick());
        assertEquals(StandKeys.VANISH, StandKeys.KEYS[StandKeys.KEYS.length - 1].tick());
        assertTrue(lines("tick").stream().anyMatch(l -> l.contains("scores={veinminer.t=.." + -StandKeys.KILL + "}")), "kill tick");
        assertTrue(lines("op").stream().anyMatch(l -> l.contains("veinminer.t=-" + StandKeys.VANISH + "}") && l.contains("ring_go")), "ring tick");
    }

    @Test
    void standPlacementMatchesTheStandFiles() throws IOException {
        Pattern tr = Pattern.compile("translation:\\[([^\\]]+)\\],scale:\\[([^\\]]+)\\]");
        for (String cell : new String[]{"0", "xp", "xn", "yp", "yn", "zp", "zn"}) {
            int[] n = StandKeys.offset(cell);
            boolean any = false;
            for (String l : lines("stand/" + cell)) {
                Matcher m = tr.matcher(l);
                if (!m.find()) continue;
                any = true;
                double[] t = floats(m.group(1)), s = floats(m.group(2));
                for (int i = 0; i < 3; i++) {
                    assertEquals(-n[i] - StandKeys.PAD, t[i], 1e-4, "translation " + cell);
                    assertEquals(1 + 2 * StandKeys.PAD, s[i], 1e-4, "scale " + cell);
                }
            }
            assertTrue(any, "stand/" + cell);
        }
        List<String> order = new ArrayList<>();
        Pattern p = Pattern.compile("^execute if block [~0-9 -]+ #veinminer:hollow run return run function veinminer:anim/stand/(\\w+) with");
        for (String l : lines("prep")) {
            Matcher m = p.matcher(l);
            if (m.find()) order.add(m.group(1));
        }
        assertEquals(List.of(StandKeys.ORDER), order);
    }

    @Test
    void facingBands() {
        assertEquals("yp", StandKeys.face(0.1, 5, 0.1));
        assertEquals("yn", StandKeys.face(0.1, -5, 0.1));
        assertEquals("zp", StandKeys.face(0, 0.5, 4));
        assertEquals("zn", StandKeys.face(0, 0.5, -4));
        assertEquals("xp", StandKeys.face(4, 0.5, 0));
        assertEquals("xn", StandKeys.face(-4, 0.5, 0));
    }

    @Test
    void flightMatchesTheFlyFiles() throws IOException {
        Pattern step = Pattern.compile("matches -(\\d+) run return run tp @s ~ ~(-?[0-9.]+) ~");
        for (String[] kind : new String[][]{{"hi", "1.6"}, {"lo", "0.55"}}) {
            double amp = Double.parseDouble(kind[1]);
            assertEquals(amp, kind[0].equals("hi") ? Arc.BUMP_HI : Arc.BUMP_LO);
            int seen = 0;
            for (String l : lines("fly/" + kind[0])) {
                Matcher m = step.matcher(l);
                if (m.find()) {
                    int k = Integer.parseInt(m.group(1));
                    assertEquals(Double.parseDouble(m.group(2)), Arc.step(amp, k), 6e-5, kind[0] + " step " + k);
                    seen++;
                }
            }
            assertEquals(Arc.FLIGHT - 1, seen, kind[0] + " steps");
            List<String> all = lines("fly/" + kind[0]);
            String grow = all.stream().filter(l -> l.contains("matches -" + Arc.GROW + " run data merge")).findFirst().orElseThrow();
            assertTrue(grow.contains("interpolation_duration:" + (Arc.FLIGHT - Arc.GROW)), grow);
            assertTrue(grow.contains("angle:" + Arc.SPIN_END + "f"), grow);
            assertTrue(all.stream().anyMatch(l -> l.contains("matches -" + Arc.FLIGHT + " ")), "squash tick");
            for (String cube : new String[]{"cube", "flat"}) {
                String sq = all.stream().filter(l -> l.contains("matches -" + Arc.FLIGHT + " ") && l.contains(cube.equals("cube") ? "tag=veinminer.cube]" : "tag=!veinminer.cube]")).findFirst().orElseThrow();
                Matcher t = Pattern.compile("translation:\\[0f,([-0-9.]+)f,0f\\],scale:\\[([0-9.]+)f,([0-9.]+)f,").matcher(sq);
                assertTrue(t.find(), sq);
                assertEquals(Arc.squashTranslationY(cube.equals("cube")), Float.parseFloat(t.group(1)), 1e-4, "squash ty " + cube);
                assertEquals(Arc.SQUASH_XZ, Float.parseFloat(t.group(2)), 1e-4);
                assertEquals(Arc.SQUASH_Y, Float.parseFloat(t.group(3)), 1e-4);
            }
        }
        List<String> throwing = lines("throw");
        String first = throwing.get(0);
        assertTrue(first.contains("interpolation_duration:" + Arc.GROW), first);
        assertTrue(first.contains("scale:[" + Arc.GROW_SCALE + "f"), first);
        assertTrue(first.contains("angle:" + Arc.SPIN_GROW + "f"), first);
        assertEquals(Arc.growWeight() * 0.001, numbers(throwing.get(1), Pattern.compile("float ([0-9.]+) run"))[0], 1e-9);
        assertTrue(throwing.stream().anyMatch(l -> l.contains("tag=veinminer.lo] run return run tp @s ~ ~" + String.format("%.4f", Arc.bump(Arc.BUMP_LO, 1)).replaceAll("0+$", "") + " ~")));
        assertTrue(throwing.stream().anyMatch(l -> l.startsWith("tp @s ~ ~" + String.format("%.4f", Arc.bump(Arc.BUMP_HI, 1)).replaceAll("0+$", "") + " ~")));
        assertEquals(Arc.LAUNCH, 10);
        assertTrue(lines("ghost_init").stream().anyMatch(l -> l.contains("veinminer.t " + Arc.LAUNCH)));
        assertTrue(lines("op").stream().anyMatch(l -> l.contains("veinminer.t=..-" + Arc.LAND + "}") && l.contains("anim/land")));
    }

    @Test
    void landingAndSpreadMatchTheirFiles() throws IOException {
        Pattern sp = Pattern.compile("matches (\\d) positioned \\^(-?[0-9.]+) \\^ \\^(-?[0-9.]+) if block ~ ~([0-9.]+) ~ #veinminer:hollow");
        int seen = 0;
        for (String l : lines("spread")) {
            Matcher m = sp.matcher(l);
            if (!m.find()) continue;
            int i = Integer.parseInt(m.group(1));
            assertEquals(Arc.SPREAD[i][0], Double.parseDouble(m.group(2)), 1e-9, "left " + i);
            assertEquals(Arc.SPREAD[i][1], Double.parseDouble(m.group(3)), 1e-9, "forward " + i);
            assertEquals(Arc.HOLLOW_PROBE, Double.parseDouble(m.group(4)), 1e-9);
            seen++;
        }
        assertEquals(Arc.SPREAD.length, seen);
        assertTrue(lines("rest").stream().anyMatch(l -> l.contains("~0.1875 ~")) && Arc.REST_FLAT == 0.1875);
        List<String> floor = lines("floor");
        assertTrue(floor.get(0).contains("~-" + Arc.FLOOR_PROBE + " ~"));
        assertTrue(floor.stream().anyMatch(l -> l.contains("matches " + (Arc.FLOOR_STEPS + 1) + "..")));
        assertTrue(floor.stream().anyMatch(l -> l.contains("tp @s ~ ~-" + Arc.FLOOR_STEP + " ~")));
        assertTrue(lines("setup").stream().anyMatch(l -> l.contains("positioned ^ ^ ^" + Arc.LAND_DISTANCE)));
        assertTrue(lines("land_pos").stream().anyMatch(l -> l.contains("~-" + Arc.FLOOR_PROBE + " ~ #veinminer:hollow align y")));
        assertTrue(lines("tick").stream().anyMatch(l -> l.contains("pd:" + Arc.PICKUP_DELAY + ",hop:" + Arc.HOP + ",dy:0")));
        List<String> ghost = lines("ghost_init");
        assertTrue(ghost.stream().anyMatch(l -> l.contains("#vis veinminer.data matches " + Arc.MAX_VISIBLE_PER_TURN + "..")));
        assertTrue(ghost.stream().anyMatch(l -> l.contains("#shown veinminer.data matches " + Arc.MAX_VISIBLE_PER_CHAIN + "..")));
        assertTrue(lines("place").stream().anyMatch(l -> l.contains("^ ^ ^" + Arc.OWN_DISTANCE)));
        assertEquals(Arc.SPREAD.length, 8);
    }

    @Test
    void delayShape() {
        assertEquals(1, Delays.delay(1, 10));
        assertEquals(Delays.MAX, Delays.delay(1000, 10));
        int last = 0;
        for (int i = 1; i < 300; i++) {
            int d = Delays.delay(i / 10.0, 5);
            assertTrue(d >= last, "monotone at " + i);
            last = d;
        }
        assertEquals(0, Delays.plan(new double[]{1.0}, 1.0).tier());
        assertEquals(1, Delays.plan(new double[]{Delays.far(10) + 0.01}, 1.0).tier());
        assertEquals(2, Delays.plan(new double[]{Delays.far(5) + 0.01}, 1.0).tier());
    }
}
