package io.github.profetgit.veinminer.server;

import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.core.LagGuard;
import java.util.function.LongSupplier;
import net.minecraft.server.MinecraftServer;

/**
 * PRD 7 lag guard: the time between two end-of-tick calls is the tick's length once the server is behind (a server that
 * is ahead sleeps up to 50 ms, never over 100). Ten ticks in a row over 100 ms halve {@code max_blocks} for new veins
 * until ten ticks in a row are back under it.
 */
public final class Lag {
    public static final double OVER_MS = 100;
    private static final LagGuard GUARD = new LagGuard(OVER_MS, 10, 10);
    /** Tests: a fake clock in nanoseconds. */
    public static volatile LongSupplier clock = System::nanoTime;
    private static long last;
    private static boolean warned;

    private Lag() {}

    /** The vein size to use for a configured {@code maxBlocks}. */
    public static int maxBlocks(int configured) {
        return GUARD.apply(configured);
    }

    public static boolean active() {
        return GUARD.active();
    }

    static void tick(MinecraftServer server) {
        if (!server.tickRateManager().runsNormally()) {
            last = 0;
            return;
        }
        long now = clock.getAsLong();
        if (last != 0) sample((now - last) / 1_000_000.0);
        last = now;
    }

    /** Feeds one tick length in milliseconds (tests call this directly). */
    public static void sample(double tickMs) {
        boolean was = GUARD.active();
        boolean now = GUARD.sample(tickMs);
        if (now != was) {
            Veinminer.LOG.warn(now ? "Veinminer: the server is behind (ticks over {} ms), veins are halved until it catches up" : "Veinminer: the server caught up, veins are back to full size", OVER_MS);
        }
    }

    public static void reset() {
        GUARD.reset();
        last = 0;
        clock = System::nanoTime;
    }
}
