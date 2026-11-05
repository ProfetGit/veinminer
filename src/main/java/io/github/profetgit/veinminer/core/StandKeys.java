package io.github.profetgit.veinminer.core;

/**
 * The stand-in's squash, stretch and vanish keys ({@code KEYS} in gen.py), counted in ticks after its block turned to
 * air. The stand-in sits in the lit cell in front of its block (offset {@code n}) and is translated back by {@code n},
 * so it takes that cell's light; scales are about the block centre.
 */
public final class StandKeys {
    /** @param tick ticks after the turn; @param duration interpolation ticks */
    public record Key(int tick, int duration, float scaleXz, float scaleY) {}

    public static final Key[] KEYS = {
        new Key(1, 3, 1.06f, 0.9f),
        new Key(4, 2, 1.2f, 0.72f),
        new Key(8, 1, 0.8f, 1.4f),
        new Key(10, 2, 0.0f, 0.0f),
    };
    /** The vanish key is also the tick of the ring's crumbs, gust and sounds. */
    public static final int VANISH = 10;
    public static final int KILL = 12;
    /** A stand-in is this much larger per side while the real block is still there, so the two don't z-fight. */
    public static final double PAD = 0.003;

    /** Cell names and offsets: in front of the block toward the miner first, then this order, else the block's own cell. */
    public static final String[] ORDER = {"yp", "xp", "xn", "zp", "zn", "yn"};

    private StandKeys() {}

    public static int[] offset(String cell) {
        return switch (cell) {
            case "xp" -> new int[]{1, 0, 0};
            case "xn" -> new int[]{-1, 0, 0};
            case "yp" -> new int[]{0, 1, 0};
            case "yn" -> new int[]{0, -1, 0};
            case "zp" -> new int[]{0, 0, 1};
            case "zn" -> new int[]{0, 0, -1};
            default -> new int[]{0, 0, 0};
        };
    }

    /** Translation along one axis of a key: scales about the block centre, then back by the cell offset {@code n}. */
    public static float translation(float scale, int n) {
        return (float) (0.5 - 0.5 * scale - n);
    }

    /** {@code squash} scales how far a key departs from 1 (the {@code tune squash} knob); the vanish key is left alone. */
    public static float tuned(float scale, double squash) {
        return scale == 0.0f ? 0.0f : (float) (1 + (scale - 1) * squash);
    }

    /**
     * The cell toward the miner from the struck block: the dominant axis of the direction to the miner's eyes, by the
     * pack's yaw/pitch bands ({@code anim/face}: pitch beyond 45 degrees is up or down, else the yaw quadrant).
     */
    public static String face(double dx, double dy, double dz) {
        int pitch = (int) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        int yaw = (int) Math.toDegrees(Math.atan2(-dx, dz));
        if (pitch <= -45) return "yp";
        if (pitch >= 45) return "yn";
        if (yaw >= -45 && yaw <= 45) return "zp";
        if (yaw >= 46 && yaw <= 135) return "xn";
        if (yaw >= -135 && yaw <= -46) return "xp";
        return "zn";
    }
}
