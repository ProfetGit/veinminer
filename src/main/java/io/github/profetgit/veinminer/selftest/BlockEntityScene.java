package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import io.github.profetgit.veinminer.server.Vein;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** PRD 13 M3 {@code blockentity}: block-entity ores are never entered or started from, and a block with its own break behaviour keeps it. */
final class BlockEntityScene {
    private BlockEntityScene() {}

    static void run(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Rig.PLAIN);
        Groups.invalidate();
        skipped();
        struck();
        directBreak();
        custom();
        customChain();
        Rig.clear();
    }

    private static void skipped() {
        Block be = TestBlocks.be;
        Rig.check("fixture: the block-entity ore is tagged #c:ores/tin", "tin".equals(Groups.of(be)), String.valueOf(Groups.of(be)));
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> row = Rig.cube(0, 1, 1, 9, 1, 1, TestBlocks.tin);
        BlockPos wall = row.get(4);
        Rig.set(wall, be.defaultBlockState());
        Rig.check("fixture: it has a block entity", Rig.level.getBlockEntity(wall) != null, "no block entity");
        Rig.strike(row.get(0), true);
        Rig.eq("skipped: the cells before it vein", 4, Rig.air(row.subList(0, 4)));
        Rig.check("skipped: the block-entity ore stays", Rig.level.getBlockState(wall).is(be), Rig.level.getBlockState(wall).toString());
        Rig.check("skipped: and keeps its block entity", Rig.level.getBlockEntity(wall) != null, "block entity gone");
        Rig.eq("skipped: the cells behind it stay", 0, Rig.air(row.subList(5, 9)));
        Rig.eq("skipped: items", 4, Rig.grab().count(TestBlocks.tin.asItem()));

        Rig.clear();
        List<BlockPos> ring = Rig.cube(1, 1, 1, 3, 3, 1, TestBlocks.tin);
        Rig.set(ring.get(4), be.defaultBlockState());
        Rig.strike(ring.get(0), true);
        Rig.eq("skipped: a block-entity ore in the middle of a plate does not stop the rest", 8, Rig.air(ring));
        Rig.grab();
    }

    private static void struck() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> row = Rig.cube(0, 1, 1, 5, 1, 1, TestBlocks.tin);
        Rig.set(row.get(2), TestBlocks.be.defaultBlockState());
        int before = Chain.count();
        Rig.strike(row.get(2), true);
        Rig.eq("struck block-entity ore: only itself breaks", 1, Rig.air(row));
        Rig.eq("struck block-entity ore: starts no chain", before, Chain.count());
        Rig.eq("struck block-entity ore: its item drops", 1, Rig.grab().count(TestBlocks.be.asItem()));
    }

    private static void directBreak() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> row = Rig.cube(0, 1, 1, 3, 1, 1, TestBlocks.tin);
        BlockPos at = row.get(1);
        Rig.set(at, TestBlocks.be.defaultBlockState());
        Vein.Result r = Vein.breakOne(Rig.player, Rig.player.getMainHandItem(), at, "tin");
        Rig.check("breakOne on a block-entity ore is skipped", r == Vein.Result.SKIPPED && Rig.level.getBlockState(at).is(TestBlocks.be), r + " " + Rig.level.getBlockState(at));
        Rig.check("scan does not enter it", !Vein.scan(Rig.level, Rig.player.position(), row.get(0), "tin", 10, 4096, true).contains(at), "scan entered the cell");
        Rig.grab();
    }

    private static void custom() {
        Rig.clear();
        Rig.hold(Rig.pick(0, 0));
        TestBlocks.customBreaks = 0;
        Rig.check("custom: group from #c:ores/custom", "custom".equals(Groups.of(TestBlocks.custom)), String.valueOf(Groups.of(TestBlocks.custom)));
        List<BlockPos> row = Rig.cube(0, 1, 1, 6, 1, 1, TestBlocks.custom);
        Rig.strike(row.get(0), true);
        Rig.eq("custom: vein of 6", 6, Rig.air(row));
        Rig.eq("custom: playerDestroy ran for every block, the struck one included", 6, TestBlocks.customBreaks);
        Rig.Loot loot = Rig.grab();
        Rig.eq("custom: the ore items", 6, loot.count(TestBlocks.custom.asItem()));
        Rig.eq("custom: the bonus items from its own behaviour", 6, loot.count(TestBlocks.CUSTOM_BONUS));
        Rig.eq("custom: nothing else", 12, loot.total());
    }

    private static void customChain() {
        Rig.clear();
        Rig.cube(0, 0, 0, Rig.SIZE, 1, Rig.SIZE, Blocks.STONE);
        Rig.player.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
        Config.use(Config.Values.DEFAULTS);
        Rig.hold(Rig.pick(0, 0));
        TestBlocks.customBreaks = 0;
        List<BlockPos> row = Rig.cube(0, 1, 1, 6, 1, 1, TestBlocks.custom);
        Rig.strike(row.get(0), true);
        Rig.settle();
        Rig.eq("custom chain: vein of 6", 6, Rig.air(row));
        Rig.eq("custom chain: playerDestroy ran for every block", 6, TestBlocks.customBreaks);
        Rig.Loot loot = Rig.grab();
        Rig.eq("custom chain: the bonus items", 6, loot.count(TestBlocks.CUSTOM_BONUS));
        Rig.eq("custom chain: the ore items", 6, loot.count(TestBlocks.custom.asItem()));
        Config.use(Rig.PLAIN);
    }
}
