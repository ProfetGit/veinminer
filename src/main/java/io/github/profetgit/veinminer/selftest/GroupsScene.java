package io.github.profetgit.veinminer.selftest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.GroupData;
import io.github.profetgit.veinminer.server.GroupDef;
import io.github.profetgit.veinminer.server.Groups;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** PRD 13 M3 {@code groups}: data groups from the real reload listener, the per-group cap, {@code #veinminer:never}. */
final class GroupsScene {
    private static int appliedBefore;

    private GroupsScene() {}

    static void run(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Rig.PLAIN);
        Groups.invalidate();
        loaded();
        extendsBuiltIn();
        newGroup();
        perGroupCap();
        never();
        empty();
        malformed();
        directLoad();
        Rig.clear();
    }

    static void startReload(MinecraftServer server) {
        appliedBefore = GroupData.applied();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
    }

    static boolean reloaded() {
        return GroupData.applied() > appliedBefore;
    }

    static void afterReload(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Rig.PLAIN);
        Rig.check("reload: the files are back", GroupData.groups().keySet().containsAll(List.of("coal", "test_new", "test_cap", "test_empty", "test_missing"))
            && !GroupData.groups().containsKey("direct"), GroupData.groups().keySet().toString());
        Rig.check("reload: the table follows", "coal".equals(Groups.of(TestBlocks.ext)) && "test_new".equals(Groups.of(TestBlocks.ga)), Groups.of(TestBlocks.ext) + "/" + Groups.of(TestBlocks.ga));
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(0, 1, 1, 6, 1, 1, TestBlocks.ga);
        Rig.strike(cells.get(0), true);
        Rig.eq("reload: a data group still veins", 6, Rig.air(cells));
        Rig.grab();
        Rig.clear();
    }

    private static TagKey<Block> tag(String ns, String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ns, path));
    }

    private static void loaded() {
        Rig.check("data groups loaded from the data files", GroupData.groups().keySet().containsAll(List.of("coal", "test_new", "test_cap", "test_empty", "test_missing")),
            GroupData.groups().keySet().toString());
        Rig.eq("test_cap file: max_blocks 5", 5, GroupData.cap("test_cap", 64));
        Rig.eq("no cap in the file: the config's", 64, GroupData.cap("test_new", 64));
        Rig.check("xp_toggle null in the file is accepted", GroupData.groups().containsKey("test_cap"), "test_cap missing");
    }

    private static void extendsBuiltIn() {
        Rig.check("extend: fixture block joins coal", "coal".equals(Groups.of(TestBlocks.ext)), String.valueOf(Groups.of(TestBlocks.ext)));
        Rig.check("extend: coal ore is still coal", "coal".equals(Groups.of(Blocks.COAL_ORE)), String.valueOf(Groups.of(Blocks.COAL_ORE)));
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(0, 1, 1, 3, 1, 1, Blocks.COAL_ORE);
        cells.addAll(Rig.cube(3, 1, 1, 2, 1, 1, TestBlocks.ext));
        cells.addAll(Rig.cube(5, 1, 1, 3, 1, 1, Blocks.DEEPSLATE_COAL_ORE));
        Rig.strike(cells.get(0), true);
        Rig.eq("extend: coal and the added block vein together", 8, Rig.air(cells));
        Rig.Loot loot = Rig.grab();
        Rig.eq("extend: coal items", 6, loot.count(Items.COAL));
        Rig.eq("extend: added block items", 2, loot.count(TestBlocks.ext.asItem()));
    }

    private static void newGroup() {
        Block tin = TestBlocks.win;
        Rig.check("rule 1: the fixture is also in #c:ores/tin", tin.defaultBlockState().is(tag("c", "ores/tin")), "not in the tin tag");
        Rig.check("new group: a block of it", "test_new".equals(Groups.of(TestBlocks.ga)), String.valueOf(Groups.of(TestBlocks.ga)));
        Rig.check("rule 1: the data group beats #c:ores/tin", "test_new".equals(Groups.of(tin)), String.valueOf(Groups.of(tin)));
        Rig.check("new group: listed", Groups.sizes().getOrDefault("test_new", 0) == 2, Groups.sizes().toString());
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(0, 1, 1, 3, 1, 1, TestBlocks.ga);
        cells.addAll(Rig.cube(3, 1, 1, 3, 1, 1, tin));
        List<BlockPos> other = Rig.cube(6, 1, 1, 2, 1, 1, TestBlocks.tin);
        Rig.strike(cells.get(0), true);
        Rig.eq("new group: both blocks vein", 6, Rig.air(cells));
        Rig.eq("new group: the tin next to it stays", 0, Rig.air(other));
        Rig.Loot loot = Rig.grab();
        Rig.eq("new group: items", 3, loot.count(TestBlocks.ga.asItem()));
        Rig.eq("new group: items of the second block", 3, loot.count(tin.asItem()));
    }

    private static void perGroupCap() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(0, 1, 1, 10, 1, 1, TestBlocks.cap);
        Rig.strike(cells.get(0), true);
        Rig.eq("cap 5 file: the struck block and 4 more", 5, Rig.air(cells));
        Rig.grab();

        Rig.clear();
        Config.use(Rig.PLAIN.maxBlocks(2));
        cells = Rig.cube(0, 1, 1, 10, 1, 1, TestBlocks.cap);
        Rig.strike(cells.get(0), true);
        Rig.eq("cap 5 file above the config's 2: the file wins", 5, Rig.air(cells));
        Rig.grab();

        Rig.clear();
        cells = Rig.cube(0, 1, 1, 10, 1, 1, Blocks.COAL_ORE);
        Rig.strike(cells.get(0), true);
        Rig.eq("no cap in the file: the config's 2", 2, Rig.air(cells));
        Rig.grab();
        Config.use(Rig.PLAIN);
        Rig.clear();
    }

    private static void never() {
        Block never = TestBlocks.never;
        Rig.check("never: the fixture is in #c:ores/tin", never.defaultBlockState().is(tag("c", "ores/tin")), "not in the tin tag");
        Rig.check("never: the fixture is in #veinminer:never", never.defaultBlockState().is(GroupData.NEVER), "not in the never tag");
        Rig.check("never: in no group", Groups.of(never) == null, String.valueOf(Groups.of(never)));
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> row = Rig.cube(0, 1, 1, 5, 1, 1, never);
        Rig.strike(row.get(0), true);
        Rig.eq("never: breaks alone, no vein", 1, Rig.air(row));
        Rig.grab();

        Rig.clear();
        List<BlockPos> tin = Rig.cube(0, 1, 1, 7, 1, 1, TestBlocks.tin);
        Rig.set(tin.get(3), never.defaultBlockState());
        Rig.strike(tin.get(0), true);
        Rig.eq("never: the tin before it veins", 3, Rig.air(tin.subList(0, 3)));
        Rig.eq("never: it and the tin behind it stay", 0, Rig.air(tin.subList(3, 7)));
        Rig.grab();
        Rig.clear();
    }

    private static void empty() {
        Rig.check("empty tag: the group file loaded", GroupData.groups().containsKey("test_empty"), GroupData.groups().keySet().toString());
        Rig.check("empty tag: no block in the group", !Groups.sizes().containsKey("test_empty"), Groups.sizes().toString());
        Rig.check("missing tag: no block, no error", GroupData.groups().containsKey("test_missing") && !Groups.sizes().containsKey("test_missing"), Groups.sizes().toString());
    }

    private static void malformed() {
        String[] bad = {
            "[]", "{}", "{\"blocks\": 5}", "{\"blocks\": \"c:ores/tin\"}", "{\"blocks\": \"#Bad Id\"}",
            "{\"blocks\": \"#veinminer:test/capped\", \"max_blocks\": 0}", "{\"blocks\": \"#veinminer:test/capped\", \"max_blocks\": 5000}",
            "{\"blocks\": \"#veinminer:test/capped\", \"max_blocks\": \"ten\"}", "{\"blocks\": \"#veinminer:test/capped\", \"max_blocks\": 2.5}"
        };
        int rejected = 0;
        for (String b : bad) if (GroupDef.parse(JsonParser.parseString(b)).isError()) rejected++;
        Rig.eq("malformed group files are rejected", bad.length, rejected);
        Rig.check("a good file parses", GroupDef.parse(JsonParser.parseString("{\"blocks\":\"#veinminer:test/capped\",\"max_blocks\":7,\"xp_toggle\":null}")).result()
            .map(d -> d.maxBlocks() == 7).orElse(false), "no");
    }

    /** Calls the loader-neutral function the listener ends in, with one good and one malformed file; the real reload comes after. */
    private static void directLoad() {
        Map<String, String> files = Map.of("veinminer:direct", "{\"blocks\":\"#veinminer:test/extend_coal\",\"max_blocks\":3}", "veinminer:broken", "{\"blocks\": 7}");
        Map<Identifier, GroupDef> defs = new HashMap<>();
        int ignored = 0;
        for (Map.Entry<String, String> e : files.entrySet()) {
            JsonElement json = JsonParser.parseString(e.getValue());
            var parsed = GroupDef.parse(json);
            if (parsed.isError()) ignored++;
            else defs.put(Identifier.parse(e.getKey()), parsed.getOrThrow());
        }
        Rig.eq("direct load: the malformed file is left out", 1, ignored);
        GroupData.apply(defs);
        Rig.check("direct load: only the loaded group exists", GroupData.groups().keySet().equals(java.util.Set.of("direct")), GroupData.groups().keySet().toString());
        Rig.check("direct load: the table follows at once", "direct".equals(Groups.of(TestBlocks.ext)) && Groups.of(TestBlocks.ga) == null,
            Groups.of(TestBlocks.ext) + "/" + Groups.of(TestBlocks.ga));
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(0, 1, 1, 8, 1, 1, TestBlocks.ext);
        Rig.strike(cells.get(0), true);
        Rig.eq("direct load: its cap 3 applies", 3, Rig.air(cells));
        Rig.grab();
        Rig.clear();
    }
}
