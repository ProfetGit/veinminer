package io.github.profetgit.veinminer.server;

/** Dev knobs for the chain's feel, set live with {@code /veinminer tune}; not saved, 1 is the 1.3.3 data pack's pacing. */
public final class Tune {
    private static volatile double delayK = 1.0;
    private static volatile double arc = 1.0;
    private static volatile double squash = 1.0;

    private Tune() {}

    /** Multiplies the tier's K of the delay table: below 1 a faster chain, above 1 a slower one (delays stay capped at 40). */
    public static double delayK() {
        return delayK;
    }

    /** Multiplies the loot arc's height. */
    public static double arc() {
        return arc;
    }

    /** Multiplies how far the stand-in's squash and stretch keys depart from full size. */
    public static double squash() {
        return squash;
    }

    public static boolean set(String name, double value) {
        switch (name) {
            case "delay_k" -> delayK = Math.clamp(value, 0.05, 20);
            case "arc" -> arc = Math.clamp(value, 0, 10);
            case "squash" -> squash = Math.clamp(value, 0, 5);
            default -> {
                return false;
            }
        }
        return true;
    }

    public static void reset() {
        delayK = 1.0;
        arc = 1.0;
        squash = 1.0;
    }

    public static String describe() {
        return "delay_k " + delayK + ", arc " + arc + ", squash " + squash;
    }
}
