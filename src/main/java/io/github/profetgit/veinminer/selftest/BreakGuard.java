package io.github.profetgit.veinminer.selftest;

import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A stand-in for a claim mod: the loader's break event asks {@link #allows} for every block a player breaks, and the
 * scene {@code claims} sets {@link #allow} to refuse regions. Registered only for a self-test run that names the scene.
 */
public final class BreakGuard {
    public static volatile BiPredicate<Level, BlockPos> allow;

    private BreakGuard() {}

    public static boolean wanted() {
        if (System.getProperty("veinminer.selftest") == null) return false;
        String scene = System.getProperty("veinminer.selftest.scene", "all");
        return scene.equals("all") || scene.contains("claims") || scene.contains("crash");
    }

    public static boolean allows(Level level, BlockPos pos) {
        BiPredicate<Level, BlockPos> a = allow;
        return a == null || a.test(level, pos);
    }
}
