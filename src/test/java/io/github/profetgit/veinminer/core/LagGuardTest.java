package io.github.profetgit.veinminer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LagGuardTest {
    @Test
    void tripsAfterTenSlowTicksAndRecovers() {
        LagGuard g = new LagGuard(100, 10, 10);
        for (int i = 0; i < 9; i++) assertFalse(g.sample(150));
        assertEquals(64, g.apply(64));
        assertTrue(g.sample(150));
        assertEquals(32, g.apply(64));
        assertEquals(1, g.apply(1));
        for (int i = 0; i < 9; i++) assertTrue(g.sample(20));
        assertFalse(g.sample(20));
        assertEquals(64, g.apply(64));
    }

    @Test
    void oneFastTickBreaksTheRun() {
        LagGuard g = new LagGuard(100, 10, 10);
        for (int i = 0; i < 9; i++) g.sample(300);
        g.sample(30);
        for (int i = 0; i < 9; i++) assertFalse(g.sample(300));
        assertTrue(g.sample(300));
    }

    @Test
    void exactlyOverMsIsNotSlow() {
        LagGuard g = new LagGuard(100, 2, 2);
        g.sample(100);
        assertFalse(g.sample(100));
    }
}
