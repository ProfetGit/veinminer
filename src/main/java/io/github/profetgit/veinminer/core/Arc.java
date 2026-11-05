package io.github.profetgit.veinminer.core;

/**
 * The loot ghosts' flight and landing ({@code FLIGHT}, {@code GROW}, {@code BUMP}, {@code SPIN}, {@code SQUASH}, {@code REST},
 * {@code BOTTOM} in gen.py, and the landing rules of {@code anim/land_pos}, {@code spread}, {@code rest}). Ghost age 0 is the
 * tick its block turned.
 */
public final class Arc {
    public static final int FLIGHT = 12;
    public static final int GROW = 1;
    /** The ghost launches this many ticks after its block turned, as the stand-in vanishes. */
    public static final int LAUNCH = 10;
    /** Ticks after the launch at which the ghost is swapped for the real item. */
    public static final int LAND = 14;
    public static final double BUMP_HI = 1.6;
    public static final double BUMP_LO = 0.55;
    public static final float SPIN_GROW = 0.2618f;
    public static final float SPIN_END = 3.1416f;
    public static final float GROW_SCALE = 1.2f;
    public static final float SQUASH_XZ = 1.25f;
    public static final float SQUASH_Y = 0.7f;
    public static final double REST_FLAT = 0.1875;
    public static final double REST_CUBE = 0.0;
    public static final double BOTTOM_FLAT = -0.125;
    public static final double BOTTOM_CUBE = 0.0625;
    public static final double HOP = 0.12;
    public static final int PICKUP_DELAY = 10;
    public static final int MAX_VISIBLE_PER_TURN = 3;
    public static final int MAX_VISIBLE_PER_CHAIN = 12;
    public static final double LAND_DISTANCE = 1.3;
    public static final double FLOOR_STEP = 0.25;
    public static final int FLOOR_STEPS = 16;
    public static final double FLOOR_PROBE = 0.26;
    public static final double HOLLOW_PROBE = 0.2;
    public static final double OWN_DISTANCE = 0.9;
    /** Left, forward (blocks) of the 8 spread spots around the landing point, seen from the miner. */
    public static final double[][] SPREAD = {
        {0.75, 0.15}, {-0.6, 0.3}, {0.3, -0.25}, {-0.85, -0.1}, {0.55, 0.35}, {-0.3, -0.3}, {0.95, -0.05}, {-0.1, 0.2},
    };

    private Arc() {}

    public static double bump(double amp, int k) {
        double u = (double) k / FLIGHT;
        return 4 * amp * u * (1 - u);
    }

    /** The relative move of flight tick {@code k}: the bump after it minus the bump before it. */
    public static double step(double amp, int k) {
        return bump(amp, k + 1) - bump(amp, k);
    }

    /** Share of the straight line done by the end of the grow key. */
    public static double growWeight() {
        return (double) (FLIGHT - GROW) / FLIGHT;
    }

    public static double rest(boolean cube) {
        return cube ? REST_CUBE : REST_FLAT;
    }

    /** Contact squash: the model's bottom stays on the floor. */
    public static float squashTranslationY(boolean cube) {
        double bottom = cube ? BOTTOM_CUBE : BOTTOM_FLAT;
        return (float) ((0.0625 - bottom * SQUASH_Y) - rest(cube));
    }
}
