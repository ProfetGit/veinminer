package io.github.profetgit.veinminer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GroupsTest {
//? if <1.21 {
/*    static {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

*///?}
    @Test
    void derivesNamesFromUntaggedOres() {
        assertEquals("mymod:tin", Groups.derive("mymod", "tin_ore"));
        assertEquals("mymod:tin", Groups.derive("mymod", "deepslate_tin_ore"));
        assertEquals("mymod:ruby", Groups.derive("mymod", "nether_ruby_ore"));
        assertEquals("mymod:glow", Groups.derive("mymod", "end_glow"));
        assertEquals("mymod:odd", Groups.derive("mymod", "blackstone_odd_ore"));
        assertEquals("veinminer:test_ore_odd", Groups.derive("veinminer", "test_ore_odd"));
        assertEquals("mymod:ore", Groups.derive("mymod", "ore"));
    }

    @Test
    void readableNamesForCustomEntries() {
        assertEquals("Netherrack", Groups.readable("minecraft:netherrack"));
        assertEquals("Polished Andesite", Groups.readable("minecraft:polished_andesite"));
        assertEquals("Logs", Groups.readable("#minecraft:logs"));
        assertEquals("Ores Iron", Groups.readable("#c:ores/iron"));
        assertEquals("Nothing", Groups.readable("nothing"));
    }
}
