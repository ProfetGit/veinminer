package io.github.profetgit.veinminer.core;

/**
 * PRD 7 lag guard: after {@code trip} consecutive server ticks over {@code overMs} the vein size is halved, and it
 * goes back to full after {@code recover} consecutive ticks at or under the limit.
 */
public final class LagGuard {
    private final double overMs;
    private final int trip, recover;
    private int slow, fast;
    private boolean active;

    public LagGuard(double overMs, int trip, int recover) {
        this.overMs = overMs;
        this.trip = trip;
        this.recover = recover;
    }

    /** Feeds one tick's duration; returns whether the guard is active afterwards. */
    public boolean sample(double tickMs) {
        if (tickMs > overMs) {
            fast = 0;
            if (++slow >= trip) active = true;
        } else {
            slow = 0;
            if (active && ++fast >= recover) {
                active = false;
                fast = 0;
            }
        }
        return active;
    }

    public boolean active() {
        return active;
    }

    /** The vein size to use: half of {@code maxBlocks} (at least 1) while the guard is active. */
    public int apply(int maxBlocks) {
        return active ? Math.max(1, maxBlocks / 2) : maxBlocks;
    }

    public void reset() {
        slow = 0;
        fast = 0;
        active = false;
    }
}
