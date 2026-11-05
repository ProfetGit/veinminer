package io.github.profetgit.veinminer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ConfigTest {
    private static final Config.Values D = Config.Values.DEFAULTS;

    @Test
    void renderedDefaultsReadBackAsDefaults() {
        assertEquals(D, Config.parse(JsonParser.parseString(Config.render(D)).getAsJsonObject()));
    }

    @Test
    void everyKeyRoundTripsThroughTheFile() {
        Config.Values v = D;
        for (String[] kv : new String[][]{{"max_blocks", "17"}, {"require_sneak", "false"}, {"drops", "1"}, {"durability", "false"}, {"diagonal", "false"},
            {"animation", "false"}, {"feedback", "false"}, {"visit_cap", "100"}, {"groups.tin", "false"}, {"groups.veinminer:test_ore_odd", "false"}}) {
            v = Config.with(v, kv[0], kv[1]);
        }
        assertEquals(17, v.maxBlocks());
        assertFalse(v.groupOn("tin"));
        assertTrue(v.groupOn("coal"));
        assertEquals(v, Config.parse(JsonParser.parseString(Config.render(v)).getAsJsonObject()));
    }

    @Test
    void badKeysAndValuesAreRefusedWithAMessage() {
        for (String[] kv : new String[][]{{"max_blocks", "0"}, {"max_blocks", "1025"}, {"max_blocks", "x"}, {"drops", "2"}, {"visit_cap", "63"},
            {"require_sneak", "yes"}, {"groups.", "true"}, {"groups.tin", "1"}, {"nope", "1"}}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Config.with(D, kv[0], kv[1]), kv[0] + " " + kv[1]);
            assertFalse(e.getMessage().isBlank());
        }
    }

    @Test
    void valuesAreTrimmedAndBooleansIgnoreCase() {
        assertEquals(12, Config.with(D, "max_blocks", " 12 ").maxBlocks());
        assertFalse(Config.with(D, "diagonal", "FALSE").diagonal());
    }
}
