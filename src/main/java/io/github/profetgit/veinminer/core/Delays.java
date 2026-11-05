package io.github.profetgit.veinminer.core;

/**
 * The chain's pacing, ported from {@code packs/Veinminer/dev/gen.py}: each block turns {@code delay} ticks after the
 * start, {@code delay = floor(1 + K*(d^0.6 - 1) + 0.5)} clamped to 1..40 for its distance {@code d} from the struck block.
 * Bigger veins use a smaller K so the chain stays under 40 ticks.
 */
public final class Delays {
    public static final int MAX = 40;
    public static final double POWER = 0.6;
    public static final double[] TIER_K = {10.0, 5.0, 2.5};
    /** The controller outlives the last turn by this many ticks (stand-in 12, loot flight 24, with slack). */
    public static final int TAIL = 26;
    private static final double EDGE = 0.0005;
    public static final int MAX_PITCH_STEP = 70;
    public static final int BASE_PITCH = 850;
    public static final int MAX_PITCH = 1500;
    public static final int PLOP_ADD = 500;
    public static final int MAX_PLOP = 2000;

    private Delays() {}

    /** Distances up to this give a delay of at most {@code k} (the tier files' selector radius is this minus 0.0005). */
    public static double radius(int k, double kk) {
        return Math.pow(1 + (k - 0.5) / kk, 1 / POWER);
    }

    /** The tier switch: a block at least this far makes the next, smaller K necessary. */
    public static double far(double kk) {
        return Math.floor(radius(MAX, kk) * 100) / 100;
    }

    /**
     * {@code floor(1 + K*(d^0.6 - 1) + 0.5)} clamped to 1..40, settled against the tier files' bands (the smallest k whose
     * radius reaches d) so a distance that lands exactly on a band edge (d = 32 at K 2.5) rounds as the pack does.
     */
    public static int delay(double distance, double kk) {
        int k = Math.clamp((long) Math.floor(1 + kk * (Math.pow(distance, POWER) - 1) + 0.5), 1, MAX);
        while (k > 1 && distance <= radius(k - 1, kk) - EDGE) k--;
        while (k < MAX && distance > radius(k, kk) - EDGE) k++;
        return k;
    }

    /** The slowest tier whose predecessor's {@code far} radius some block reaches. */
    public static int tier(double[] distances) {
        for (int i = TIER_K.length - 1; i > 0; i--) {
            double limit = far(TIER_K[i - 1]);
            for (double d : distances) if (d >= limit) return i;
        }
        return 0;
    }

    public static int pitchStep(int rings) {
        return rings < 2 ? MAX_PITCH_STEP : Math.min(MAX_PITCH_STEP, 650 / (rings - 1));
    }

    /** Break-sound pitch of ring {@code ring} (0-based), in thousandths. */
    public static int pitch(int ring, int step) {
        return Math.min(MAX_PITCH, BASE_PITCH + ring * step);
    }

    /** The egg plop rides 0.5 above the break sound and never passes 2.0 ({@code playsound} rejects more). */
    public static int plop(int ring, int step) {
        return Math.min(MAX_PLOP, pitch(ring, step) + PLOP_ADD);
    }

    /**
     * @param delays    per block, in the order given
     * @param rings     distinct delays
     * @param maxDelay  the last turn tick
     * @param pitchStep thousandths of pitch per ring
     */
    public record Plan(int[] delays, int tier, double k, int rings, int maxDelay, int pitchStep) {
        public int end() {
            return maxDelay + TAIL;
        }
    }

    /** {@code kScale} multiplies the tier's K (the {@code /veinminer tune delay_k} knob; 1 is the pack's pacing). */
    public static Plan plan(double[] distances, double kScale) {
        int tier = tier(distances);
        double kk = TIER_K[tier] * kScale;
        int[] delays = new int[distances.length];
        boolean[] seen = new boolean[MAX + 1];
        int rings = 0, max = 1;
        for (int i = 0; i < delays.length; i++) {
            int d = delay(distances[i], kk);
            delays[i] = d;
            if (!seen[d]) {
                seen[d] = true;
                rings++;
            }
            max = Math.max(max, d);
        }
        return new Plan(delays, tier, kk, rings, max, pitchStep(rings));
    }
}
