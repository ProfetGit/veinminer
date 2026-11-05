package io.github.profetgit.veinminer.server;

import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.api.VeinminerEvents;
import io.github.profetgit.veinminer.core.Arc;
import io.github.profetgit.veinminer.core.Delays;
import io.github.profetgit.veinminer.core.StandKeys;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * PRD 5.5, the 1.3.3 chain animation driven from Java: one controller per running vein. Blocks stay real until their turn.
 * A stand-in is summoned a tick before its block turns to air; at the turn the block breaks through {@link Vein#breakOne}
 * (claims, drops, XP and wear as by hand), the loot that break dropped is swapped for ghosts flying a 12-tick arc to the
 * landing spot, and the ghosts become the real items there. Nothing in the world changes before a block's turn, so a
 * chain that stops for any reason leaves the rest as ore.
 */
public final class Chain {
    private static final TagKey<Block> HOLLOW = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("veinminer", "hollow"));
    /** Most chains one player has running at once (PRD 7, a guess to tune). */
    public static final int MAX_PER_PLAYER = 3;
    private static final List<Chain> ACTIVE = new ArrayList<>();
    private static volatile int count;
    private static boolean quiet;

    /** One block of the vein. */
    public static final class Node {
        public final BlockPos pos;
        public final int delay;
        private BlockState state;
        private Display.BlockDisplay stand;
        private int[] offset = StandKeys.offset("0");
        private int turned = -1;

        private Node(BlockPos pos, int delay) {
            this.pos = pos;
            this.delay = delay;
        }

        public Display.BlockDisplay stand() {
            return stand;
        }

        public int[] standOffset() {
            return offset;
        }

        /** The chain age at which this block turned to air, or -1. */
        public int turned() {
            return turned;
        }
    }

    private static final class Ghost {
        ItemStack stack;
        int xp;
        boolean cube, lo, visible;
        Vec3 spot, start;
        Display.ItemDisplay display;
        int born;
        double amp;
    }

    private final ServerLevel level;
    private final ServerPlayer player;
    private final ItemStack tool;
    private final String group;
    private final BlockPos origin;
    private final List<Node> nodes = new ArrayList<>();
    private final List<List<Node>> byDelay = new ArrayList<>();
    private final List<Node> standing = new ArrayList<>();
    private final List<Ghost> ghosts = new ArrayList<>();
    private final LongOpenHashSet remaining = new LongOpenHashSet();
    private final Delays.Plan plan;
    private final String face;
    private final boolean own;
    private Vec3 landing;
    private int xpPool;
    private int age, ring, dir, shown, visTurn, maxVisTurn, mined;
    private Vein.Result stop;
    private boolean stopped, finished;

    private Chain(ServerLevel level, ServerPlayer player, ItemStack tool, String group, BlockPos origin, List<BlockPos> cells, boolean own) {
        this.level = level;
        this.player = player;
        this.tool = tool;
        this.group = group;
        this.origin = origin;
        this.own = own;
        double[] dist = new double[cells.size()];
        for (int i = 0; i < dist.length; i++) {
            BlockPos p = cells.get(i);
            dist[i] = Math.sqrt(p.distSqr(origin));
        }
        plan = Delays.plan(dist, Tune.delayK());
        for (int i = 0; i <= Delays.MAX + 1; i++) byDelay.add(new ArrayList<>());
        for (int i = 0; i < dist.length; i++) {
            Node n = new Node(cells.get(i), plan.delays()[i]);
            nodes.add(n);
            byDelay.get(n.delay).add(n);
            remaining.add(n.pos.asLong());
        }
        Vec3 center = Vec3.atCenterOf(origin);
        Vec3 eyes = player.getEyePosition();
        face = StandKeys.face(eyes.x - center.x, eyes.y - center.y, eyes.z - center.z);
        landing = landingSpot(center, eyes);
    }

    /** True while a chain runs: its blocks cannot start or join another vein. */
    public static boolean claimed(ServerLevel level, BlockPos pos) {
        if (ACTIVE.isEmpty()) return false;
        long key = pos.asLong();
        for (Chain c : ACTIVE) if (c.level == level && c.remaining.contains(key)) return true;
        return false;
    }

    /** True while the chain breaks a block: the per-block break effects are dropped, the chain plays its own. */
    public static boolean quiet() {
        return quiet;
    }

    public static int count() {
        return count;
    }

    public static int countFor(ServerPlayer player) {
        int n = 0;
        for (Chain c : ACTIVE) if (c.player == player) n++;
        return n;
    }

    public static List<Chain> active() {
        return List.copyOf(ACTIVE);
    }

    /** As {@link #start(ServerLevel, ServerPlayer, BlockPos, String, ItemStack, int)} for a struck block whose loot is left alone. */
    public static Chain start(ServerLevel level, ServerPlayer player, BlockPos origin, String group, ItemStack tool) {
        return start(level, player, origin, group, tool, Integer.MAX_VALUE);
    }

    /**
     * Scans from the struck cell and starts the chain; null when the vein has no other block. What the struck block dropped
     * (entities with an id above {@code mark}) joins the chain's loot and flies the same arc.
     */
    public static Chain start(ServerLevel level, ServerPlayer player, BlockPos origin, String group, ItemStack tool, int mark) {
        Config.Values cfg = Config.get();
        if (countFor(player) >= MAX_PER_PLAYER) return null;
        List<BlockPos> cells = Vein.scan(level, player.position(), origin, group, Lag.maxBlocks(GroupData.cap(group, cfg.maxBlocks())) - 1, cfg.visitCap(), cfg.diagonal());
        if (cells.isEmpty()) return null;
        Chain c = new Chain(level, player, tool, group, origin, cells, cfg.drops() == 1);
        ACTIVE.add(c);
        count = ACTIVE.size();
        c.loot(origin, mark);
        c.prepDue();
        return c;
    }

    /** One server tick of every running chain; nothing at all when none runs. */
    public static void tickAll() {
        if (ACTIVE.isEmpty()) return;
        for (int i = 0; i < ACTIVE.size();) {
            Chain c = ACTIVE.get(i);
            try {
                c.tick();
            } catch (RuntimeException e) {
                Veinminer.LOG.error("Veinminer: chain failed, landing its loot", e);
                c.end(false);
            }
            if (c.finished) {
                ACTIVE.remove(i);
                count = ACTIVE.size();
            } else {
                i++;
            }
        }
    }

    /** Lands every leftover now (a player leaves, the server stops). The blocks not yet turned stay ore. */
    public static void flushFor(ServerPlayer player) {
        for (Chain c : List.copyOf(ACTIVE)) if (c.player == player) c.end(false);
        ACTIVE.removeIf(c -> c.finished);
        count = ACTIVE.size();
    }

    public static void flushAll() {
        for (Chain c : List.copyOf(ACTIVE)) c.end(false);
        ACTIVE.clear();
        count = 0;
    }

    /** Server stopped: forget everything without touching the (closed) worlds. */
    public static void reset() {
        for (Chain c : ACTIVE) c.finished = true;
        ACTIVE.clear();
        count = 0;
        quiet = false;
        Fx.clear();
    }

    public List<Node> nodes() {
        return nodes;
    }

    public Delays.Plan plan() {
        return plan;
    }

    public Vec3 landing() {
        return landing;
    }

    public String face() {
        return face;
    }

    public int age() {
        return age;
    }

    public int mined() {
        return mined;
    }

    public int shown() {
        return shown;
    }

    public int maxVisiblePerTurn() {
        return maxVisTurn;
    }

    public boolean finished() {
        return finished;
    }

    public boolean stopped() {
        return stopped;
    }

    private void tick() {
        age++;
        visTurn = 0;
        if (player.isRemoved() || !player.isAlive() || player.hasDisconnected() || player.level() != level) {
            end(false);
            return;
        }
        if (!stopped && (player.getMainHandItem() != tool || tool.isEmpty())) halt(Vein.Result.TOOL_CHANGED);
        for (int i = 0; i < standing.size();) {
            Node n = standing.get(i);
            int o = age - n.turned;
            if (o >= StandKeys.KILL) {
                Fx.kill(n.stand);
                n.stand = null;
                standing.remove(i);
                continue;
            }
            for (StandKeys.Key k : StandKeys.KEYS) if (k.tick() == o) Fx.key(n.stand, n.offset, k, Tune.squash());
            i++;
        }
        for (Ghost g : ghosts) if (g.display != null) fly(g, age - g.born);
        if (!stopped) {
            if (age <= Delays.MAX) {
                for (Node n : byDelay.get(age)) {
                    turn(n);
                    if (stopped) break;
                }
            }
        }
        if (!stopped) prepDue();
        ring();
        for (int i = 0; i < ghosts.size();) {
            Ghost g = ghosts.get(i);
            if (age - g.born >= Arc.LAUNCH + Arc.LAND) {
                land(g);
                ghosts.remove(i);
            } else {
                i++;
            }
        }
        maxVisTurn = Math.max(maxVisTurn, visTurn);
        if (age >= plan.end() || (stopped && standing.isEmpty() && ghosts.isEmpty())) end(true);
    }

    private void prepDue() {
        if (age + 1 > Delays.MAX) return;
        for (Node n : byDelay.get(age + 1)) if (n.stand == null && n.turned < 0 && remaining.contains(n.pos.asLong())) prep(n);
    }

    private void prep(Node n) {
        if (!Vein.loaded(level, n.pos)) return;
        BlockState st = level.getBlockState(n.pos);
        if (!group.equals(Groups.of(st.getBlock()))) return;
        n.state = st;
        String cell = "0";
        int[] f = StandKeys.offset(face);
        if (hollow(n.pos.getX() + f[0], n.pos.getY() + f[1], n.pos.getZ() + f[2])) {
            cell = face;
        } else {
            for (String c : StandKeys.ORDER) {
                int[] o = StandKeys.offset(c);
                if (hollow(n.pos.getX() + o[0], n.pos.getY() + o[1], n.pos.getZ() + o[2])) {
                    cell = c;
                    break;
                }
            }
        }
        n.offset = StandKeys.offset(cell);
        n.stand = Fx.stand(this, level, st, n.pos, n.offset);
    }

    private boolean hollow(int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return !level.isOutsideBuildHeight(p) && level.hasChunkAt(p) && level.getBlockState(p).is(HOLLOW);
    }

    private void dropStand(Node n) {
        if (n.stand != null) {
            Fx.kill(n.stand);
            n.stand = null;
        }
    }

    private void turn(Node n) {
        remaining.remove(n.pos.asLong());
        if (!Vein.loaded(level, n.pos) || !group.equals(Groups.of(level.getBlockState(n.pos).getBlock()))) {
            dropStand(n);
            return;
        }
        if (n.stand == null) prep(n);
        if (n.state == null) n.state = level.getBlockState(n.pos);
        int mark = level.getNextEntityId();
        Vein.Result r;
        quiet = true;
        try {
            r = Vein.breakOne(player, tool, n.pos, group);
        } finally {
            quiet = false;
        }
        if (r == Vein.Result.BROKEN) {
            mined++;
            n.turned = age;
            if (n.stand != null) standing.add(n);
            loot(n.pos, mark);
        } else {
            dropStand(n);
            if (r != Vein.Result.SKIPPED) halt(r);
        }
    }

    /** The chain stops turning blocks: whatever is left stays ore; what already turned plays out. */
    private void halt(Vein.Result why) {
        stopped = true;
        stop = why;
        remaining.clear();
        for (Node n : nodes) if (n.turned < 0) dropStand(n);
    }

    /** Entity ids only grow, so what the break dropped is whatever near the block has an id above the mark taken before it. */
    private void loot(BlockPos pos, int mark) {
        AABB box = new AABB(pos).inflate(1.0);
        List<ItemEntity> items = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, box, e -> e.getId() > mark && !e.isRemoved()));
        items.sort(Comparator.comparingInt(ItemEntity::getId));
        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, box, e -> e.getId() > mark && !e.isRemoved());
        int xp = 0;
        for (ExperienceOrb o : orbs) {
            xp += o.getValue();
            o.discard();
        }
        for (ItemEntity e : items) {
            ghost(e.getItem().copy(), e.position(), xp);
            xp = 0;
            e.discard();
        }
        if (xp > 0) ghost(ItemStack.EMPTY, Vec3.atCenterOf(pos), xp);
    }

    private void ghost(ItemStack stack, Vec3 start, int xp) {
        boolean hide = stack.isEmpty() || visTurn >= Arc.MAX_VISIBLE_PER_TURN || shown >= Arc.MAX_VISIBLE_PER_CHAIN;
        if (!hide) {
            visTurn++;
            shown++;
        }
        Ghost g = new Ghost();
        g.stack = stack;
        g.xp = xp;
        g.cube = !stack.isEmpty() && isCube(stack);
        g.lo = own;
        g.visible = !hide;
        g.start = start;
        g.born = age;
        g.amp = (own ? Arc.BUMP_LO : Arc.BUMP_HI) * Tune.arc();
        g.spot = spotFor(start, hide, g.cube);
        if (!hide) g.display = Fx.ghost(this, level, stack.copy(), g.spot, start.subtract(g.spot));
        ghosts.add(g);
    }

    private static boolean isCube(ItemStack stack) {
        return stack.getItem() instanceof BlockItem b && b.getBlock().defaultBlockState().isSolidRender();
    }

    private static double yaw(double dx, double dz) {
        return Math.atan2(dz, dx) - Math.PI / 2;
    }

    /** Where a ghost rests: one of the 8 spread spots around the landing point (or in front of the miner's feet), as {@code anim/place}. */
    private Vec3 spotFor(Vec3 start, boolean hide, boolean cube) {
        Vec3 feet = player.position();
        Vec3 base;
        if (own) {
            double y = yaw(start.x - feet.x, start.z - feet.z);
            base = new Vec3(feet.x - Math.sin(y) * Arc.OWN_DISTANCE, feet.y, feet.z + Math.cos(y) * Arc.OWN_DISTANCE);
        } else {
            if (!hide && ++dir >= Arc.SPREAD.length) dir = 0;
            double y = yaw(feet.x - landing.x, feet.z - landing.z);
            double left = Arc.SPREAD[dir][0], fwd = Arc.SPREAD[dir][1];
            Vec3 c = new Vec3(landing.x + Math.cos(y) * left - Math.sin(y) * fwd, landing.y, landing.z + Math.sin(y) * left + Math.cos(y) * fwd);
            base = hollowAt(c.x, c.y + Arc.HOLLOW_PROBE, c.z) ? c : landing;
        }
        return base.add(0, Arc.rest(cube), 0);
    }

    private boolean hollowAt(double x, double y, double z) {
        return hollow(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    /** {@code anim/land_pos}: 1.3 blocks from the struck block toward the miner's eyes, dropped to the floor in 0.25 steps (max 4 blocks). */
    private Vec3 landingSpot(Vec3 center, Vec3 eyes) {
        Vec3 d = eyes.subtract(center);
        if (d.lengthSqr() < 1.0E-8) return center;
        Vec3 p = center.add(d.normalize().scale(Arc.LAND_DISTANCE));
        if (!hollowAt(p.x, p.y, p.z)) return center;
        double y = p.y;
        for (int i = 0; i < Arc.FLOOR_STEPS && airAt(p.x, y - Arc.FLOOR_PROBE, p.z); i++) y -= Arc.FLOOR_STEP;
        if (!hollowAt(p.x, y - Arc.FLOOR_PROBE, p.z)) y = Math.floor(y);
        return new Vec3(p.x, y, p.z);
    }

    private boolean airAt(double x, double y, double z) {
        BlockPos p = BlockPos.containing(x, y, z);
        return !level.isOutsideBuildHeight(p) && level.hasChunkAt(p) && level.getBlockState(p).isAir();
    }

    private void fly(Ghost g, int o) {
        int k = o - Arc.LAUNCH;
        if (k < 0 || k > Arc.FLIGHT) return;
        Display.ItemDisplay d = g.display;
        if (k == 0) {
            double w = Arc.growWeight();
            Vec3 from = g.start.subtract(g.spot);
            Fx.pose(d, from.x * w, from.y * w, from.z * w, new Quaternionf().rotationY(Arc.SPIN_GROW),
                Arc.GROW_SCALE, Arc.GROW_SCALE, Arc.GROW_SCALE, Arc.GROW);
            d.snapTo(g.spot.x, g.spot.y + Arc.bump(g.amp, 1), g.spot.z);
        } else if (k < Arc.FLIGHT) {
            if (k == Arc.GROW) Fx.pose(d, 0, 0, 0, new Quaternionf().rotationY(Arc.SPIN_END), 1, 1, 1, Arc.FLIGHT - Arc.GROW);
            d.snapTo(g.spot.x, g.spot.y + Arc.bump(g.amp, k + 1), g.spot.z);
        } else {
            Fx.pose(d, 0, Arc.squashTranslationY(g.cube), 0, new Quaternionf().rotationY(Arc.SPIN_END), Arc.SQUASH_XZ, Arc.SQUASH_Y, Arc.SQUASH_XZ, 1);
        }
    }

    private void land(Ghost g) {
        if (g.display != null) {
            Fx.kill(g.display);
            g.display = null;
        }
        Vec3 at = g.lo ? player.position() : g.spot.subtract(0, Arc.rest(g.cube), 0);
        if (!level.hasChunkAt(BlockPos.containing(at))) at = player.position();
        if (!g.stack.isEmpty()) {
            ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, g.stack, 0, g.lo ? 0 : Arc.HOP, 0);
            item.setPickUpDelay(g.lo ? 0 : Arc.PICKUP_DELAY);
            level.addFreshEntity(item);
        }
        xpPool += g.xp;
    }

    /** The chain's XP is paid as one lot at the landing spot, so a vein makes a few orbs, not one per block. */
    private void payXp() {
        if (xpPool <= 0) return;
        Vec3 at = own ? player.position() : landing;
        if (!level.hasChunkAt(BlockPos.containing(at))) at = player.position();
        ExperienceOrb.award(level, at, xpPool);
        xpPool = 0;
    }

    /** Crumbs and a gust per block, one break sound and one egg plop per ring, the pitch climbing ring by ring. */
    private void ring() {
        Node first = null;
        for (Node n : standing) {
            if (age - n.turned != StandKeys.VANISH) continue;
            Vec3 c = Vec3.atCenterOf(n.pos);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, n.state), c.x, c.y, c.z, 12, 0.25, 0.25, 0.25, 0);
            level.sendParticles(ParticleTypes.SMALL_GUST, c.x, c.y, c.z, 1, 0.15, 0.15, 0.15, 0);
            if (first == null) first = n;
        }
        if (first == null) return;
        Vec3 c = Vec3.atCenterOf(first.pos);
        int step = plan.pitchStep();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.45f, Delays.plop(ring, step) / 1000f);
        level.playSound(null, c.x, c.y, c.z, first.state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8f, Delays.pitch(ring, step) / 1000f);
        ring++;
    }

    /** Lands every ghost, removes every display, releases the blocks; the overlay message only when the chain ran its course. */
    private void end(boolean message) {
        if (finished) return;
        finished = true;
        remaining.clear();
        for (Ghost g : ghosts) land(g);
        ghosts.clear();
        payXp();
        for (Node n : nodes) dropStand(n);
        standing.clear();
        Config.Values cfg = Config.get();
        if (message && cfg.feedback()) Vein.feedback(player, mined, group, stop);
        for (VeinminerEvents.AfterVein a : VeinminerEvents.AFTER_VEIN.listeners()) a.after(level, player, origin, group, mined);
    }
}
