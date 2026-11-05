package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.core.Delays;
import io.github.profetgit.veinminer.core.StandKeys;
import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Fx;
import io.github.profetgit.veinminer.server.Tune;
import io.github.profetgit.veinminer.server.Vein;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** PRD 13 M2 {@code chain}: the chain's timeline, loot, cleanup and stops. Manual sections step the chain a tick at a time; the real section waits on server ticks. */
final class ChainScene {
    private static final Identifier COAL_SEQUENCE = Identifier.withDefaultNamespace("blocks/coal_ore");
    private static Vec3 home;
    private static volatile boolean realStarted;
    private static List<BlockPos> realCells;

    private ChainScene() {}

    static void manual(MinecraftServer server) {
        Rig.begin(server);
        home = Rig.player.position();
        Config.use(Config.Values.DEFAULTS);
        Tune.reset();
        try {
            timeline();
            standCells();
            sameResult();
            landing();
            ownDrops();
            unsaved();
            toolSwap();
            animationOff();
            blob();
            claims();
            flush();
            tune();
        } finally {
            Chain.flushAll();
            Tune.reset();
            Config.use(Config.Values.DEFAULTS);
            Rig.clear();
            Rig.player.teleportTo(home.x, home.y, home.z);
        }
    }

    static void realStart(MinecraftServer server) {
        Rig.begin(server);
        home = Rig.player.position();
        Config.use(Config.Values.DEFAULTS);
        arena();
        Rig.hold(Rig.pick(1, 0));
        realCells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.check("real: chain started", Chain.count() == 1, "chains " + Chain.count());
        realStarted = true;
    }

    static boolean realDone() {
        return realStarted && Chain.count() == 0;
    }

    static void realCheck(MinecraftServer server) {
        Rig.eq("real ticks: all 64 broke", 64, Rig.air(realCells));
        Rig.Loot loot = Rig.grab();
        Rig.eq("real ticks: 64 coal_ore items", 64, loot.count(Items.COAL_ORE));
        Rig.eq("real ticks: no other items", 0, loot.total() - loot.count(Items.COAL_ORE));
        Rig.eq("real ticks: no displays left in the world", 0, Rig.level.getEntitiesOfClass(Display.class, Rig.box()).size());
        Rig.eq("real ticks: no tracked fx", 0, Fx.liveCount());
        Rig.player.teleportTo(home.x, home.y, home.z);
        Rig.clear();
    }

    /** A stone floor under the work volume and the player standing on it in front (south) of the vein area. */
    private static void arena() {
        Rig.clear();
        Rig.cube(0, 0, 0, Rig.SIZE, 1, Rig.SIZE, Blocks.STONE);
        Rig.player.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
        Config.use(Config.Values.DEFAULTS);
        Tune.reset();
    }

    private static Chain only(String what) {
        List<Chain> all = Chain.active();
        Rig.eq(what + ": one chain running", 1, all.size());
        return all.isEmpty() ? null : all.get(0);
    }

    private static int count(Class<? extends Entity> type) {
        return Rig.level.getEntitiesOfClass(type, Rig.box()).size();
    }

    private static void noLeak(String name) {
        Rig.eq(name + ": no chains left", 0, Chain.count());
        Rig.eq(name + ": no tracked displays", 0, Fx.liveCount());
        Rig.eq(name + ": no displays in the world", 0, count(Display.class));
        int other = 0;
        for (Entity e : Rig.level.getEntitiesOfClass(Entity.class, Rig.box())) {
            if (!(e instanceof ItemEntity) && !(e instanceof ExperienceOrb) && e != Rig.player) other++;
        }
        Rig.eq(name + ": no other entities", 0, other);
    }

    /** PRD pillar 1: every block turns at the tick the delay table says; its stand-in is there a tick before, the block still real. */
    private static void timeline() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        BlockPos origin = Rig.at(4, 2, 5);
        Rig.strike(origin, true);
        Rig.grab();
        Chain c = only("timeline 64");
        if (c == null) return;
        List<Chain.Node> nodes = c.nodes();
        Rig.eq("timeline 64: 63 blocks queued", 63, nodes.size());
        double[] dist = new double[nodes.size()];
        for (int i = 0; i < dist.length; i++) dist[i] = Math.sqrt(nodes.get(i).pos.distSqr(origin));
        Delays.Plan expect = Delays.plan(dist, 1.0);
        boolean same = true;
        for (int i = 0; i < dist.length; i++) same &= nodes.get(i).delay == expect.delays()[i];
        Rig.check("timeline 64: delays equal the table", same, "tier " + c.plan().tier() + " max " + c.plan().maxDelay());
        Rig.check("timeline 64: a real vein spans several rings", c.plan().rings() >= 4 && c.plan().maxDelay() <= Delays.MAX, "rings " + c.plan().rings() + " max " + c.plan().maxDelay());
        Rig.check("timeline 64: blocks are all still ore before tick 1", Rig.air(cells) == 1, "air " + Rig.air(cells));
        int[] airAt = new int[nodes.size()];
        java.util.Arrays.fill(airAt, -1);
        int early = 0, noStand = 0, enclosedFail = 0, ended = -1;
        for (int t = 1; t <= c.plan().end() + 2 && Chain.count() > 0; t++) {
            for (Chain.Node n : nodes) {
                if (n.delay == t && (n.stand() == null || Rig.level.getBlockState(n.pos).isAir())) noStand++;
            }
            Chain.tickAll();
            for (int i = 0; i < airAt.length; i++) {
                boolean air = Rig.level.getBlockState(nodes.get(i).pos).isAir();
                if (air && airAt[i] < 0) airAt[i] = t;
                if (!air && nodes.get(i).delay <= t) enclosedFail++;
                if (air && nodes.get(i).delay > t) early++;
            }
            if (Chain.count() == 0) ended = t;
        }
        int wrong = 0;
        for (int i = 0; i < airAt.length; i++) if (airAt[i] != nodes.get(i).delay) wrong++;
        Rig.eq("timeline 64: blocks that broke at a tick other than their delay", 0, wrong);
        Rig.eq("timeline 64: blocks broken early", 0, early);
        Rig.eq("timeline 64: blocks still ore after their turn", 0, enclosedFail);
        Rig.eq("timeline 64: stand-in missing a tick before the turn", 0, noStand);
        Rig.eq("timeline 64: all 64 air", 64, Rig.air(cells));
        Rig.eq("timeline 64: chain ends at last turn + 26", c.plan().maxDelay() + Delays.TAIL, ended);
        Rig.eq("timeline 64: blocks mined", 63, c.mined());
        Rig.eq("timeline 64: visible ghosts capped at 12", 12, c.shown());
        Rig.check("timeline 64: at most 3 visible ghosts per turn tick", c.maxVisiblePerTurn() <= 3 && c.maxVisiblePerTurn() >= 1, "max " + c.maxVisiblePerTurn());
        Rig.eq("timeline 64: loot is 63 coal_ore", 63, Rig.grab().count(Items.COAL_ORE));
        noLeak("timeline 64");
    }

    /** The stand-in stands in the neighbour toward the miner, else the first hollow one in the pack's order, else in place. */
    private static void standCells() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        Rig.set(Rig.at(4, 1, 5), Blocks.COAL_ORE.defaultBlockState());
        BlockPos a = Rig.at(5, 1, 4), b = Rig.at(3, 1, 4), blocker = Rig.at(5, 1, 5);
        Rig.set(a, Blocks.COAL_ORE.defaultBlockState());
        Rig.set(b, Blocks.COAL_ORE.defaultBlockState());
        Rig.set(blocker, Blocks.STONE.defaultBlockState());
        Rig.strike(Rig.at(4, 1, 5), true);
        Rig.grab();
        Chain c = only("stand cells");
        if (c == null) return;
        Rig.eq("stand cells: the miner is south of the vein", 0, "zp".equals(c.face()) ? 0 : 1);
        Chain.Node na = null, nb = null;
        for (Chain.Node n : c.nodes()) {
            if (n.pos.equals(a)) na = n;
            if (n.pos.equals(b)) nb = n;
        }
        Rig.check("stand cells: both neighbours queued", na != null && nb != null, "nodes " + c.nodes().size());
        if (na != null && nb != null) {
            for (int t = 1; t < Math.max(na.delay, nb.delay); t++) Chain.tickAll();
            Rig.check("stand cells: toward the miner when hollow", java.util.Arrays.equals(nb.standOffset(), StandKeys.offset("zp")) && nb.stand() != null,
                java.util.Arrays.toString(nb.standOffset()));
            Rig.check("stand cells: first hollow in the pack's order when blocked (up)", java.util.Arrays.equals(na.standOffset(), StandKeys.offset("yp")) && na.stand() != null,
                java.util.Arrays.toString(na.standOffset()));
            Vec3 pa = na.stand().position();
            Rig.check("stand cells: the display stands in that cell", pa.equals(new Vec3(a.getX(), a.getY() + 1, a.getZ())), pa.toString());
        }
        Rig.settle();
        Rig.grab();
        noLeak("stand cells");
    }

    /** A chain gives what Vein.run gives for the same vein and dice: items and wear exactly, XP within sampling noise. */
    private static void sameResult() {
        long chainItems = 0, plainItems = 0, chainXp = 0, plainXp = 0, chainWear = 0, plainWear = 0, chainAir = 0, plainAir = 0;
        int veins = 60;
        for (int i = 0; i < veins; i++) {
            long seed = 104729L * (i + 1);
            for (boolean chain : new boolean[]{true, false}) {
                arena();
                Config.use(chain ? Config.Values.DEFAULTS : Rig.PLAIN);
                ItemStack pick = Rig.pick(0, 3);
                Rig.hold(pick);
                List<BlockPos> cells = Rig.cube(1, 1, 1, 2, 2, 2, Blocks.COAL_ORE);
                Rig.server.getRandomSequences().reset(COAL_SEQUENCE, seed);
                Rig.level.getRandom().setSeed(seed);
                Rig.strike(Rig.at(1, 1, 1), true);
                if (chain) Rig.settle();
                Rig.Loot loot = Rig.grab();
                if (chain) {
                    chainItems += loot.count(Items.COAL);
                    chainXp += loot.xp();
                    chainWear += pick.getDamageValue();
                    chainAir += Rig.air(cells);
                } else {
                    plainItems += loot.count(Items.COAL);
                    plainXp += loot.xp();
                    plainWear += pick.getDamageValue();
                    plainAir += Rig.air(cells);
                }
            }
        }
        Rig.eq("same result: every chain vein broke all 8", 8L * veins, chainAir);
        Rig.eq("same result: items equal Vein.run", plainItems, chainItems);
        Rig.eq("same result: tool wear equals Vein.run", plainWear, chainWear);
        Rig.eq("same result: wear is 8 per vein", 8L * veins, chainWear);
        double ratio = plainXp == 0 ? 0 : (double) chainXp / plainXp;
        Rig.check("same result: xp within 10% of Vein.run", plainXp > 0 && Math.abs(ratio - 1) <= 0.10, "chain " + chainXp + " plain " + plainXp);
        Config.use(Config.Values.DEFAULTS);
    }

    /** The loot lands 1.3 blocks from the struck block toward the miner, on the floor, as real items, XP orbs with it. */
    private static void landing() {
        arena();
        Rig.hold(Rig.pick(0, 3));
        BlockPos origin = Rig.at(4, 2, 5);
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(origin, true);
        Chain c = only("landing");
        if (c == null) return;
        Vec3 center = Vec3.atCenterOf(origin);
        Vec3 p = center.add(Rig.player.getEyePosition().subtract(center).normalize().scale(1.3));
        Vec3 spot = c.landing();
        Rig.check("landing: 1.3 blocks from the struck block toward the eyes, on the floor",
            Math.abs(spot.x - p.x) < 1e-6 && Math.abs(spot.z - p.z) < 1e-6 && Math.abs(spot.y - (Rig.base.getY() + 1)) < 1e-6, spot + " vs " + p);
        Rig.Loot struck = Rig.grab();
        Rig.settle();
        Rig.eq("landing: all 64 broke", 64, Rig.air(cells));
        int items = 0, far = 0, delayed = 0, orbs = 0, orbsFar = 0;
        for (ItemEntity e : Rig.level.getEntitiesOfClass(ItemEntity.class, Rig.box())) {
            items += e.getItem().getCount();
            if (Math.hypot(e.getX() - spot.x, e.getZ() - spot.z) > 1.1 || Math.abs(e.getY() - spot.y) > 0.3) far++;
            if (!e.hasPickUpDelay()) delayed++;
        }
        int xp = 0;
        for (ExperienceOrb o : Rig.level.getEntitiesOfClass(ExperienceOrb.class, Rig.box())) {
            xp += o.getValue();
            orbs++;
            if (Math.hypot(o.getX() - spot.x, o.getZ() - spot.z) > 1.1) orbsFar++;
        }
        Rig.check("landing: loot of 63 blocks came out as real items", items >= 63, "items " + items + " struck " + struck.total());
        Rig.eq("landing: items away from the landing spot", 0, far);
        Rig.eq("landing: items without a pickup delay", 0, delayed);
        Rig.check("landing: xp orbs at the landing spot", orbs > 0 && xp > 0 && orbsFar == 0, "orbs " + orbs + " xp " + xp + " far " + orbsFar);
        Rig.grab();
        noLeak("landing");
    }

    /** {@code drops} 1: the loot lands at the miner's feet with no pickup delay, the same count. */
    private static void ownDrops() {
        arena();
        Config.use(Config.Values.DEFAULTS.drops(1));
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        Rig.settle();
        Rig.eq("drops 1: all 64 broke", 64, Rig.air(cells));
        int items = 0, far = 0, delayed = 0;
        Vec3 feet = Rig.player.position();
        for (ItemEntity e : Rig.level.getEntitiesOfClass(ItemEntity.class, Rig.box())) {
            items += e.getItem().getCount();
            if (e.position().distanceTo(feet) > 0.5) far++;
            if (e.hasPickUpDelay()) delayed++;
        }
        Rig.eq("drops 1: 63 items", 63, items);
        Rig.eq("drops 1: items away from the miner's feet", 0, far);
        Rig.eq("drops 1: items with a pickup delay", 0, delayed);
        Rig.grab();
        Config.use(Config.Values.DEFAULTS);
        noLeak("drops 1");
    }

    /** Stand-ins and ghosts exist mid-chain, are tracked, and none of them would be saved. */
    private static void unsaved() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        if (only("unsaved") == null) return;
        for (int t = 0; t < 14; t++) Chain.tickAll();
        int blocks = 0, ghosts = 0, saved = 0;
        for (Display d : Rig.level.getEntitiesOfClass(Display.class, Rig.box())) {
            if (d instanceof Display.BlockDisplay) blocks++;
            if (d instanceof Display.ItemDisplay) ghosts++;
            if (d.shouldBeSaved()) saved++;
        }
        Rig.check("unsaved: stand-ins and ghosts are in the world", blocks > 0 && ghosts > 0, "block displays " + blocks + " item displays " + ghosts);
        Rig.eq("unsaved: displays that would be saved", 0, saved);
        Rig.eq("unsaved: tracked displays equal the ones in the world", blocks + ghosts, Fx.liveCount());
        Rig.settle();
        Rig.grab();
        noLeak("unsaved");
    }

    private static void toolSwap() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        Chain c = only("tool swap");
        if (c == null) return;
        for (int t = 0; t < 6; t++) Chain.tickAll();
        int before = Rig.air(cells);
        Rig.check("tool swap: some blocks already turned", before > 1 && before < 64, "air " + before);
        Rig.hold(Rig.pick(1, 0));
        Chain.tickAll();
        Rig.check("tool swap: the chain stopped", c.stopped(), "stopped " + c.stopped());
        Rig.eq("tool swap: no block turned on the swap tick", before, Rig.air(cells));
        Rig.settle();
        Rig.eq("tool swap: no block turned after the swap", before, Rig.air(cells));
        Rig.check("tool swap: the rest stays ore", Rig.air(cells) < 64 && Rig.level.getBlockState(Rig.at(4, 2, 2)).is(Blocks.COAL_ORE), "air " + Rig.air(cells));
        Rig.eq("tool swap: loot of the turned blocks landed", before - 1, Rig.grab().count(Items.COAL_ORE));
        Rig.check("tool swap: claims released", !Chain.claimed(Rig.level, Rig.at(4, 2, 2)), "claimed");
        noLeak("tool swap");
    }

    private static void animationOff() {
        arena();
        Config.use(Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.eq("animation false: whole vein broke in the strike tick", 64, Rig.air(cells));
        Rig.eq("animation false: no chain", 0, Chain.count());
        Rig.eq("animation false: no displays", 0, Fx.liveCount() + count(Display.class));
        Rig.eq("animation false: 64 items", 64, Rig.grab().count(Items.COAL_ORE));
        Config.use(Config.Values.DEFAULTS);
    }

    /** 80 connected blocks against the cap of 64: the chain mines 64 (struck one included) and ends. */
    private static void blob() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(2, 1, 2, 5, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Chain c = only("blob 80");
        Rig.grab();
        int ticks = Rig.settle();
        Rig.eq("blob 80: 64 mined, the rest stays ore", 64, Rig.air(cells));
        Rig.check("blob 80: ends within the tail", c != null && ticks == c.plan().end(), "ticks " + ticks);
        Rig.eq("blob 80: 63 chain items", 63, Rig.grab().count(Items.COAL_ORE));
        noLeak("blob 80");
    }

    /** A running chain's blocks cannot join another vein, or start one when broken by hand. */
    private static void claims() {
        arena();
        Config.use(Config.Values.DEFAULTS.maxBlocks(4));
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 8, 1, 1, Blocks.COAL_ORE);
        Rig.strike(Rig.at(1, 1, 1), true);
        Rig.grab();
        Chain c = only("claims");
        if (c == null) return;
        Rig.eq("claims: the chain owns 3 blocks", 3, c.nodes().size());
        Rig.check("claims: owned blocks are claimed, the others are not", Chain.claimed(Rig.level, Rig.at(3, 1, 1)) && !Chain.claimed(Rig.level, Rig.at(5, 1, 1)), "claimed");
        List<BlockPos> scan = Vein.scan(Rig.level, Rig.player.position(), Rig.at(6, 1, 1), "coal", 10, 4096, true);
        boolean joined = scan.contains(Rig.at(2, 1, 1)) || scan.contains(Rig.at(3, 1, 1)) || scan.contains(Rig.at(4, 1, 1));
        Rig.check("claims: a scan does not join the chain's blocks", !joined && scan.contains(Rig.at(5, 1, 1)), scan.toString());
        Rig.strike(Rig.at(3, 1, 1), true);
        Rig.eq("claims: breaking a claimed block by hand starts no chain", 1, Chain.count());
        Rig.grab();
        Rig.settle();
        Rig.eq("claims: the hand-broken block stays air, the chain's others broke", 4, Rig.air(cells));
        Rig.eq("claims: the far blocks stay ore", 4, 8 - Rig.air(cells));
        Rig.grab();
        Config.use(Config.Values.DEFAULTS);
        noLeak("claims");
    }

    /** flush (player leaves, server stops): ghosts land at once, unturned blocks stay ore, nothing is lost. */
    private static void flush() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        if (only("flush") == null) return;
        for (int t = 0; t < 8; t++) Chain.tickAll();
        int air = Rig.air(cells);
        Chain.flushFor(Rig.player);
        Rig.eq("flush: no chain left", 0, Chain.count());
        Rig.eq("flush: no block turned by the flush", air, Rig.air(cells));
        Rig.check("flush: part of the vein stays ore", air < 64, "air " + air);
        Rig.eq("flush: every turned block's loot landed", air - 1, Rig.grab().count(Items.COAL_ORE));
        noLeak("flush");
    }

    private static void tune() {
        arena();
        Rig.hold(Rig.pick(1, 0));
        Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        Chain base = only("tune");
        int baseMax = base == null ? 0 : base.plan().maxDelay();
        Chain.flushAll();
        arena();
        Tune.set("delay_k", 0.5);
        Rig.hold(Rig.pick(1, 0));
        Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        Chain fast = only("tune fast");
        Rig.check("tune: delay_k 0.5 makes a shorter chain", fast != null && fast.plan().maxDelay() < baseMax, baseMax + " -> " + (fast == null ? -1 : fast.plan().maxDelay()));
        Chain.flushAll();
        Tune.reset();
        Rig.grab();
        Rig.eq("tune: the knob reset", 1, Tune.delayK() == 1.0 ? 1 : 0);
    }
}
