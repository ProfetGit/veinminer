package io.github.profetgit.veinminer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import org.junit.jupiter.api.Test;

class GroupDefTest {
//? if <1.21 {
/*    static {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

*///?}
    private static DataResult<GroupDef> parse(String json) {
        return GroupDef.parse(JsonParser.parseString(json));
    }

    @Test
    void readsTagAndOptionalCap() {
        GroupDef d = parse("{\"blocks\":\"#c:ores/tin\"}").result().get();
        assertEquals("c:ores/tin", d.blocks().location().toString());
        assertNull(d.maxBlocks());
        assertEquals(7, parse("{\"blocks\":\"#mymod:ruby\",\"max_blocks\":7}").result().get().maxBlocks());
    }

    @Test
    void acceptsNullCapAndIgnoresXpToggle() {
        GroupDef d = parse("{\"blocks\":\"#mymod:ruby\",\"max_blocks\":null,\"xp_toggle\":null}").result().get();
        assertNull(d.maxBlocks());
        assertTrue(parse("{\"blocks\":\"#mymod:ruby\",\"xp_toggle\":false}").result().isPresent());
    }

    @Test
    void rejectsMalformedFiles() {
        for (String bad : new String[]{"[]", "\"x\"", "{}", "{\"blocks\":5}", "{\"blocks\":\"c:ores/tin\"}", "{\"blocks\":\"#Bad Id\"}",
            "{\"blocks\":\"#a:b\",\"max_blocks\":0}", "{\"blocks\":\"#a:b\",\"max_blocks\":1025}", "{\"blocks\":\"#a:b\",\"max_blocks\":2.5}",
            "{\"blocks\":\"#a:b\",\"max_blocks\":\"9\"}"}) {
            assertTrue(parse(bad).result().isEmpty(), bad);
        }
    }

    @Test
    void capsAreInclusive() {
        assertEquals(1, parse("{\"blocks\":\"#a:b\",\"max_blocks\":1}").result().get().maxBlocks());
        assertEquals(1024, parse("{\"blocks\":\"#a:b\",\"max_blocks\":1024}").result().get().maxBlocks());
    }
}
