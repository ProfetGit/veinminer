package io.github.profetgit.veinminer.server;

import io.github.profetgit.veinminer.api.VeinminerEvents;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** PRD 5.2 and 5.4: the scan of a vein and the break of its blocks through the real player break path. */
public final class Vein {
    public enum Result {
        BROKEN,
        /** Not a block of the group any more, wrong tool for it, or another mod refused the break. */
        SKIPPED,
        /** The next block would use the pickaxe's last durability point: the chain ends. */
        WORN,
        /** The held item is no longer the one the vein started with: the chain ends. */
        TOOL_CHANGED
    }

    private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final int[][] ALL = neighbours();

    /** A vein reaches at most this many blocks from the struck block in each axis (PRD 7). */
    public static final int REACH = 16;
    /** Tests: cells this says are unloaded count as unloaded, so a chunk unload mid-chain can be staged. */
    public static volatile BiPredicate<ServerLevel, BlockPos> unloaded;

    private static boolean running;

    private Vein() {}

    private static int[][] neighbours() {
        int[][] o = new int[26][];
        int n = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) if (x != 0 || y != 0 || z != 0) o[n++] = new int[]{x, y, z};
            }
        }
        return o;
    }

    /** True when the cell is inside the build height and its chunk is loaded; never loads a chunk. */
    public static boolean loaded(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        BiPredicate<ServerLevel, BlockPos> u = unloaded;
        return u == null || !u.test(level, pos);
    }

    /** True while a vein is breaking blocks: those breaks never start another vein. */
    public static boolean running() {
        return running;
    }

    /**
     * The cells to mine after the struck cell {@code origin}: breadth-first over the faces (and the 20 diagonals), only
     * loaded cells of {@code group} without a block entity. Order is distance from {@code origin}, ties nearer to
     * {@code near}. At most {@code limit} cells, looking at no more than {@code visitCap} cells.
     */
    public static List<BlockPos> scan(ServerLevel level, Vec3 near, BlockPos origin, String group, int limit, int visitCap, boolean diagonal) {
        int[][] offsets = diagonal ? ALL : FACES;
        List<BlockPos> out = new ArrayList<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        visited.add(origin.asLong());
        int seen = 1;
        List<BlockPos> frontier = List.of(origin);
        while (!frontier.isEmpty() && out.size() < limit && seen < visitCap) {
            List<BlockPos> found = new ArrayList<>();
            scan:
            for (BlockPos p : frontier) {
                for (int[] o : offsets) {
                    if (seen >= visitCap) break scan;
                    BlockPos q = p.offset(o[0], o[1], o[2]);
                    if (!visited.add(q.asLong())) continue;
                    seen++;
                    if (Math.abs(q.getX() - origin.getX()) > REACH || Math.abs(q.getY() - origin.getY()) > REACH || Math.abs(q.getZ() - origin.getZ()) > REACH) continue;
                    if (!loaded(level, q)) continue;
                    BlockState s = level.getBlockState(q);
                    if (s.hasBlockEntity() || !group.equals(Groups.of(s.getBlock())) || Chain.claimed(level, q)) continue;
                    found.add(q);
                }
            }
            found.sort(Comparator.comparingDouble(q -> q.distToCenterSqr(near)));
            for (BlockPos q : found) {
                if (out.size() >= limit) break;
                out.add(q);
            }
            frontier = found;
        }
        return out;
    }

    /** Scans from the struck cell and breaks the whole vein now; returns the blocks broken, the struck one not counted. */
    public static int run(ServerLevel level, ServerPlayer player, BlockPos origin, String group, ItemStack tool) {
        Config.Values cfg = Config.get();
        int mined = 0;
        Result stop = null;
        running = true;
        try {
            for (BlockPos pos : scan(level, player.position(), origin, group, Lag.maxBlocks(GroupData.cap(group, cfg.maxBlocks())) - 1, cfg.visitCap(), cfg.diagonal())) {
                Result r = breakOne(player, tool, pos, group);
                if (r == Result.BROKEN) mined++;
                else if (r != Result.SKIPPED) {
                    stop = r;
                    break;
                }
            }
        } finally {
            running = false;
        }
        if (cfg.feedback()) feedback(player, mined, group, stop);
        for (VeinminerEvents.AfterVein a : VeinminerEvents.AFTER_VEIN.listeners()) a.after(level, player, origin, group, mined);
        return mined;
    }

    /** The action-bar message at the end of a vein or chain. */
    static void feedback(ServerPlayer player, int mined, String group, Result stop) {
        if (mined > 0) {
            player.sendOverlayMessage(Component.translatableWithFallback("veinminer.msg.veined", "Veined " + (mined + 1) + " " + group, mined + 1, group));
        }
        if (stop == Result.WORN) {
            player.sendOverlayMessage(Component.translatableWithFallback("veinminer.msg.worn", "Tool is too worn"));
        }
    }

    /**
     * Whether {@code tool} may mine {@code state} as a vein of {@code group}. Pickaxe groups (the ores): an item of
     * {@code #veinminer:tools} that is correct for the block. {@code "tool": "correct"} groups (dirt, sand...): the stack's own
     * tool rules make it correct for the block; the player-level check would say yes to any item for a block that needs no tool.
     */
    public static boolean toolFits(ServerPlayer player, ItemStack tool, BlockState state, String group) {
        if (GroupData.tool(group) == GroupDef.Tool.CORRECT) return tool.isCorrectToolForDrops(state);
        return tool.is(Hooks.TOOLS) && player.hasCorrectToolForDrops(state);
    }

    /**
     * One block of a vein through {@code ServerPlayerGameMode.destroyBlock}: drops, XP, stats, exhaustion, tool wear and
     * the loader's break events (claims) all run as for a block mined by hand. That path already charges the tool, so
     * with {@code durability} off the damage is put back instead of charging again.
     */
    public static Result breakOne(ServerPlayer player, ItemStack tool, BlockPos pos, String group) {
        if (player.getMainHandItem() != tool) return Result.TOOL_CHANGED;
        if (tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= 1) return Result.WORN;
        ServerLevel level = player.level();
        if (!loaded(level, pos) || !level.mayInteract(player, pos)) return Result.SKIPPED;
        BlockState state = level.getBlockState(pos);
        if (state.hasBlockEntity() || !group.equals(Groups.of(state.getBlock())) || !toolFits(player, tool, state, group)) return Result.SKIPPED;
        int damage = tool.getDamageValue();
        boolean was = running;
        running = true;
        boolean broke;
        try {
            broke = player.gameMode.destroyBlock(pos);
        } finally {
            running = was;
        }
        if (!Config.get().durability() && !tool.isEmpty()) tool.setDamageValue(damage);
        return broke ? Result.BROKEN : Result.SKIPPED;
    }
}
