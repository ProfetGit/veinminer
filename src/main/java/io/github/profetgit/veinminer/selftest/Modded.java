package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** PRD 13 M1 {@code modded}: ores from tags, not from an id list. */
final class Modded {
    private Modded() {}

    static void run(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Rig.PLAIN);
        Groups.invalidate();
        String report = Groups.report();
        SelfTest.record("c:ores tag present", Groups.oresTagSize() > 0, report);
        SelfTest.record("fixture groups", Groups.sizes().containsKey("tin") && Groups.sizes().containsKey("zinc")
            && Groups.sizes().containsKey("veinminer:test_ore_odd"), Groups.sizes().toString());
        Rig.check("coal group", "coal".equals(Groups.of(Blocks.COAL_ORE)) && "coal".equals(Groups.of(Blocks.DEEPSLATE_COAL_ORE)),
            String.valueOf(Groups.of(Blocks.COAL_ORE)));
        Rig.check("odd group name", "veinminer:test_ore_odd".equals(Groups.of(TestBlocks.odd)), String.valueOf(Groups.of(TestBlocks.odd)));
        Rig.check("stone is no ore", Groups.of(Blocks.STONE) == null, String.valueOf(Groups.of(Blocks.STONE)));

        solid("tin", TestBlocks.tin, TestBlocks.tin.asItem());
        solid("zinc", TestBlocks.zinc, TestBlocks.zinc.asItem());
        solid("odd", TestBlocks.odd, TestBlocks.odd.asItem());
        coal("coal via c:ores/coal");
        mixedCoal();
        Groups.ignoreCommon(true);
        Rig.check("vanilla only: coal group", "coal".equals(Groups.of(Blocks.COAL_ORE)) && Groups.of(TestBlocks.tin) == null,
            Groups.of(Blocks.COAL_ORE) + "/" + Groups.of(TestBlocks.tin));
        coal("coal via #minecraft:coal_ores only");
        Groups.ignoreCommon(false);
        adjacent();
        blockEntity();
        gates();
        Rig.clear();
    }

    private static void solid(String name, Block block, Item item) {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 5, 2, 2, block);
        Rig.strike(Rig.at(3, 2, 2), true);
        Rig.eq(name + " vein of 20: air", 20, Rig.air(cells));
        Rig.Loot loot = Rig.grab();
        Rig.eq(name + " vein of 20: items", 20, loot.count(item));
        Rig.eq(name + " vein of 20: no other items", 0, loot.total() - loot.count(item));
    }

    private static void coal(String name) {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 5, 2, 2, Blocks.COAL_ORE);
        Rig.strike(Rig.at(1, 1, 1), true);
        Rig.eq(name + ": air", 20, Rig.air(cells));
        Rig.eq(name + ": coal", 20, Rig.grab().count(Items.COAL));
    }

    private static void mixedCoal() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 5, 2, 2, Blocks.COAL_ORE);
        List<BlockPos> deep = Rig.cube(1, 3, 1, 5, 2, 2, Blocks.DEEPSLATE_COAL_ORE);
        cells.addAll(deep);
        Rig.strike(Rig.at(1, 1, 1), true);
        Rig.eq("stone and deepslate coal join: air", 40, Rig.air(cells));
        Rig.eq("stone and deepslate coal join: coal", 40, Rig.grab().count(Items.COAL));
    }

    private static void adjacent() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> tin = Rig.cube(1, 1, 1, 5, 2, 1, TestBlocks.tin);
        List<BlockPos> zinc = Rig.cube(1, 1, 2, 5, 2, 1, TestBlocks.zinc);
        Rig.strike(Rig.at(1, 1, 1), true);
        Rig.eq("adjacent groups: tin gone", 10, Rig.air(tin));
        Rig.eq("adjacent groups: zinc untouched", 0, Rig.air(zinc));
        Rig.Loot loot = Rig.grab();
        Rig.eq("adjacent groups: tin items", 10, loot.count(TestBlocks.tin.asItem()));
        Rig.eq("adjacent groups: no zinc items", 0, loot.count(TestBlocks.zinc.asItem()));
    }

    private static void blockEntity() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> row = Rig.cube(1, 1, 1, 7, 1, 1, TestBlocks.tin);
        BlockPos middle = row.get(3);
        Rig.set(middle, TestBlocks.be.defaultBlockState());
        Rig.check("block entity fixture has one", Rig.level.getBlockEntity(middle) != null, "no block entity at the fixture");
        Rig.strike(row.get(0), true);
        Rig.eq("block entity: near cells gone", 3, Rig.air(row.subList(0, 3)));
        Rig.check("block entity: the fixture stays", Rig.level.getBlockState(middle).is(TestBlocks.be), Rig.level.getBlockState(middle).toString());
        Rig.eq("block entity: cells behind it stay", 0, Rig.air(row.subList(4, 7)));
        Rig.eq("block entity: items", 3, Rig.grab().count(TestBlocks.tin.asItem()));

        Rig.clear();
        List<BlockPos> three = Rig.cube(1, 1, 1, 3, 1, 1, TestBlocks.tin);
        Rig.set(three.get(1), TestBlocks.be.defaultBlockState());
        Rig.strike(three.get(1), true);
        Rig.eq("struck block entity starts no vein", 1, Rig.air(three));
        Rig.grab();
    }

    private static void gates() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 5, 1, 1, TestBlocks.tin);
        Rig.strike(cells.get(0), false);
        Rig.eq("not sneaking: one block", 1, Rig.air(cells));

        Rig.clear();
        Rig.hold(new ItemStack(Items.IRON_AXE));
        cells = Rig.cube(1, 1, 1, 5, 1, 1, TestBlocks.tin);
        Rig.strike(cells.get(0), true);
        Rig.eq("axe: one block, no vein", 1, Rig.air(cells));

        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        Config.use(Config.get().group("tin", false));
        cells = Rig.cube(1, 1, 1, 5, 1, 1, TestBlocks.tin);
        Rig.strike(cells.get(0), true);
        Rig.eq("group switched off: one block", 1, Rig.air(cells));
        Config.use(Rig.PLAIN);

        for (boolean diagonal : new boolean[]{true, false}) {
            Rig.clear();
            Rig.hold(Rig.pick(0, 0));
            Config.use(Rig.PLAIN.diagonal(diagonal));
            Rig.set(Rig.at(1, 1, 1), TestBlocks.tin.defaultBlockState());
            Rig.set(Rig.at(2, 2, 1), TestBlocks.tin.defaultBlockState());
            Rig.strike(Rig.at(1, 1, 1), true);
            Rig.eq("diagonal " + (diagonal ? "on" : "off") + ": corner-touching block", diagonal ? 1 : 0, Rig.air(List.of(Rig.at(2, 2, 1))));
        }
        Config.use(Rig.PLAIN);
        Rig.grab();
        Rig.clear();
    }
}
