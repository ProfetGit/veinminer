package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.api.VeinminerEvents;
import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.server.Api;
import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Conflicts;
import io.github.profetgit.veinminer.server.Hooks;
import io.github.profetgit.veinminer.server.Lag;
import io.github.profetgit.veinminer.server.Vein;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.Objective;
//? if >=1.20.3
import net.minecraft.world.scores.ScoreHolder;

/** PRD 13 M4 scenes {@code hooks}, {@code fake}, {@code claims}, {@code conflict} and {@code lag}: who may start a vein and what may stop it. */
final class Gates {
    private static int loadedBefore;
    private static final String PACK = "hookpack";

    private Gates() {}

    private static Path worldPack(MinecraftServer s) {
        return s.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK);
    }

    private static int score(MinecraftServer s, String holder) {
        Objective o = s.getScoreboard().getObjective("vmtest");
        if (o == null) return -1;
//? if >=1.20.3 {
        var info = s.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), o);
        return info == null ? 0 : info.value();
//?} else {
/*        return s.getScoreboard().hasPlayerScore(holder, o) ? s.getScoreboard().getOrCreatePlayerScore(holder, o).getScore() : 0;
*///?}
    }

    // ---- hooks: the add-on API through a real fixture data pack ----

    static void hooksInstall(MinecraftServer s) {
        Rig.begin(s);
        M4.park();
        Config.use(Rig.PLAIN);
        M4.copyTree(M4.workspace().resolve("packs/Veinminer/dev/test/hookpack"), worldPack(s));
        Rig.check("hooks: no add-on tag entries yet", s.getFunctions().getTag(Identifier.fromNamespaceAndPath("veinminer", "api/loaded")).isEmpty(), "tag not empty");
        loadedBefore = Api.loadedRuns();
        M4.ran(s, "reload");
        Rig.check("hooks: /reload found and enabled the add-on pack", s.getPackRepository().getSelectedIds().contains("file/" + PACK), s.getPackRepository().getSelectedIds().toString());
    }

    static boolean hooksLoaded() {
        return Api.loadedRuns() > loadedBefore;
    }

    static void hooksCheck(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        Rig.check("hooks: #veinminer:api/loaded ran after the reload", score(s, "#loaded") >= 1, "loaded " + score(s, "#loaded"));
        Rig.eq("hooks: meta version_id 20000 was set before loaded ran", 20000, score(s, "#version_id"));
        Rig.check("hooks: loaded appended to meta requires", s.getCommandStorage().get(Api.metaId()).get("requires") instanceof ListTag l && l.size() == 1, String.valueOf(s.getCommandStorage().get(Api.metaId())));
        Rig.check("hooks: meta version is 2.0.0", "2.0.0".equals(TestCompat.stringOr(s.getCommandStorage().get(Api.metaId()), "version", "")), String.valueOf(s.getCommandStorage().get(Api.metaId())));

        Rig.hold(Rig.pick(1, 0));
        int a = score(s, "#calls_a"), cancelRuns = Api.cancelRuns();
        vein("hooks: no veto", 27, 27);
        Rig.check("hooks: the cancel tag ran once for the vein", score(s, "#calls_a") == a + 1 && Api.cancelRuns() == cancelRuns + 1, "calls_a " + score(s, "#calls_a"));

        cancelRuns = Api.cancelRuns();
        Rig.player.setShiftKeyDown(false);
        M4.arena();
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), false);
        Rig.eq("hooks: not sneaking: no vein, cancel tag not asked", 0, Api.cancelRuns() - cancelRuns);
        Rig.eq("hooks: not sneaking: one block", 1, Rig.air(cells));

        Rig.player.addTag("vmtest.veto_a");
        vein("hooks: veto_a returns 1 (executor is the player)", 27, 1);
        Rig.player.removeTag("vmtest.veto_a");
        Rig.player.addTag("vmtest.veto_b");
        vein("hooks: veto_b returns 1", 27, 1);
        Rig.player.removeTag("vmtest.veto_b");
        vein("hooks: tags off again", 27, 27);

        VeinminerEvents.BeforeVein deny = (level, player, pos, state, group) -> group.equals("coal");
        VeinminerEvents.BEFORE_VEIN.register(deny);
        try {
            cancelRuns = Api.cancelRuns();
            vein("hooks: BEFORE_VEIN veto", 27, 1);
            Rig.eq("hooks: BEFORE_VEIN runs before the cancel tag, which is then not asked", cancelRuns, Api.cancelRuns());
        } finally {
            VeinminerEvents.BEFORE_VEIN.unregister(deny);
        }
        int[] after = {0, 0};
        VeinminerEvents.AfterVein seen = (level, player, pos, group, mined) -> {
            after[0]++;
            after[1] = mined;
        };
        VeinminerEvents.AFTER_VEIN.register(seen);
        try {
            vein("hooks: AFTER_VEIN run", 27, 27);
            Rig.check("hooks: AFTER_VEIN told once with 26 mined", after[0] == 1 && after[1] == 26, after[0] + " calls, mined " + after[1]);
        } finally {
            VeinminerEvents.AFTER_VEIN.unregister(seen);
        }
        VeinminerEvents.BeforeVein thrower = (level, player, pos, state, group) -> {
            throw new IllegalStateException("hostile listener");
        };
        VeinminerEvents.BEFORE_VEIN.register(thrower);
        try {
            M4.arena();
            cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
            Rig.strike(Rig.at(2, 2, 2), true);
            Rig.eq("hooks: a listener that throws costs only the vein", 1, Rig.air(cells));
        } finally {
            VeinminerEvents.BEFORE_VEIN.unregister(thrower);
        }
        Rig.grab();
        Rig.clear();
        M4.park();
        M4.ran(s, "datapack disable \"file/" + PACK + "\"");
    }

    static void hooksGone(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        Rig.check("hooks: requires cleared again after the add-on left", s.getCommandStorage().get(Api.metaId()).get("requires") instanceof ListTag l && l.isEmpty(),
            String.valueOf(s.getCommandStorage().get(Api.metaId())));
        int runs = Api.cancelRuns();
        Rig.hold(Rig.pick(1, 0));
        vein("hooks: add-on gone", 27, 27);
        Rig.eq("hooks: no function is run when the cancel tag is empty", runs, Api.cancelRuns());
        M4.park();
        M4.deleteTree(worldPack(s));
    }

    private static void vein(String name, int size, int expectedAir) {
        M4.arena();
        Rig.hold(Rig.pick(1, 0));
        int side = (int) Math.round(Math.cbrt(size));
        List<BlockPos> cells = Rig.cube(1, 1, 1, side, side, side, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.eq(name + ": air", expectedAir, Rig.air(cells));
        Rig.grab();
    }

    // ---- fake players ----

    static void fake(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        ServerPlayer fake = Platform.get().testFakePlayer(Rig.level);
        if (fake == null) {
            Rig.check("fake: untested, this loader has no fake player class here", true, "skipped");
            return;
        }
        Rig.check("fake: it is no plain ServerPlayer and the loader knows it", fake.getClass() != ServerPlayer.class && Platform.get().isFakePlayer(fake), fake.getClass().getName());
        Rig.check("fake: the test player is no fake player", Rig.player.getClass() == ServerPlayer.class && !Platform.get().isFakePlayer(Rig.player), Rig.player.getClass().getName());
        for (boolean animation : new boolean[]{false, true}) {
            M4.arena();
            Config.use(animation ? Config.Values.DEFAULTS : Rig.PLAIN);
            List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
            fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Rig.pick(1, 0));
            fake.setShiftKeyDown(true);
            fake.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
            boolean broke = fake.gameMode.destroyBlock(Rig.at(2, 2, 2));
            Hooks.drain();
            Rig.settle();
            fake.setShiftKeyDown(false);
            String mode = animation ? "chain" : "instant";
            Rig.check("fake (" + mode + "): the fake player broke its block", broke && Rig.air(cells) == 1, "broke " + broke + ", air " + Rig.air(cells));
            Rig.eq("fake (" + mode + "): no chain started", 0, Chain.count());
            Rig.eq("fake (" + mode + "): the vein stayed ore", 26, 27 - Rig.air(cells));
            Rig.grab();
        }
        M4.arena();
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.eq("fake: control, the real player's vein runs", 27, Rig.air(cells));
        Rig.grab();
        fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        Config.use(Rig.PLAIN);
        Rig.clear();
    }

    // ---- claims: a fixture break listener refuses regions (the loader's break event, as FTB Chunks would) ----

    static void claims(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        Rig.check("claims: the fixture break listener is registered", BreakGuard.wanted(), "scene not selected");
        try {
            for (boolean animation : new boolean[]{false, true}) {
                String mode = animation ? "chain" : "instant";
                region(mode, animation);
                hole(mode, animation);
                hostile(mode, animation);
                refusedStruck(mode, animation);
            }
            border();
        } finally {
            BreakGuard.allow = null;
            Config.use(Rig.PLAIN);
            Rig.player.setShiftKeyDown(false);
            Rig.clear();
        }
    }

    private static void run(boolean animation) {
        if (animation) Rig.settle();
    }

    private static int remaining(List<BlockPos> cells) {
        return cells.size() - Rig.air(cells);
    }

    /** A claim boundary through the vein: the allowed half breaks, the refused half stays ore. */
    private static void region(String mode, boolean animation) {
        M4.arena();
        Config.use(animation ? Config.Values.DEFAULTS : Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 6, 2, 2, Blocks.COAL_ORE);
        BreakGuard.allow = (level, pos) -> pos.getX() < Rig.base.getX() + 4;
        Rig.strike(Rig.at(1, 1, 1), true);
        run(animation);
        Rig.eq("claims (" + mode + ") region: the allowed 3 columns broke", 12, Rig.air(cells));
        Rig.eq("claims (" + mode + ") region: the refused 3 columns stay ore", 12, remaining(cells));
        boolean exact = true;
        for (BlockPos p : cells) exact &= Rig.level.getBlockState(p).isAir() == (p.getX() < Rig.base.getX() + 4);
        Rig.check("claims (" + mode + ") region: exactly the allowed blocks broke", exact, "mismatch");
        Rig.eq("claims (" + mode + ") region: loot is one coal_ore per allowed block", 12, Rig.grab().count(Items.COAL_ORE));
        Rig.eq("claims (" + mode + ") region: no chain left", 0, Chain.count());
        BreakGuard.allow = null;
    }

    /** One refused column in the middle: the chain skips it and goes on to the far side. */
    private static void hole(String mode, boolean animation) {
        M4.arena();
        Config.use(animation ? Config.Values.DEFAULTS : Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 5, 2, 2, Blocks.COAL_ORE);
        BreakGuard.allow = (level, pos) -> pos.getX() != Rig.base.getX() + 3;
        Rig.strike(Rig.at(1, 1, 1), true);
        run(animation);
        boolean exact = true;
        for (BlockPos p : cells) exact &= Rig.level.getBlockState(p).isAir() == (p.getX() != Rig.base.getX() + 3);
        Rig.check("claims (" + mode + ") hole: all but the refused column broke, the chain went on", exact && Rig.air(cells) == 16, "air " + Rig.air(cells));
        Rig.eq("claims (" + mode + ") hole: loot of the 16", 16, Rig.grab().count(Items.COAL_ORE));
        BreakGuard.allow = null;
    }

    /** A mod that starts refusing in the middle of the vein, and one that throws. */
    private static void hostile(String mode, boolean animation) {
        M4.arena();
        Config.use(animation ? Config.Values.DEFAULTS : Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 4, 2, 4, Blocks.COAL_ORE);
        AtomicInteger asked = new AtomicInteger();
        BreakGuard.allow = (level, pos) -> asked.incrementAndGet() <= 6;
        Rig.strike(Rig.at(1, 1, 1), true);
        run(animation);
        Rig.eq("claims (" + mode + ") hostile cancel: 6 blocks broke, then every break was refused", 6, Rig.air(cells));
        Rig.check("claims (" + mode + ") hostile cancel: the chain ended and the rest is ore", Chain.count() == 0 && remaining(cells) == 26 && !Vein.running(), "chains " + Chain.count());
        Rig.eq("claims (" + mode + ") hostile cancel: loot of the 6", 6, Rig.grab().count(Items.COAL_ORE));
        M4.noLeak("claims (" + mode + ") hostile cancel");

        M4.arena();
        Rig.hold(Rig.pick(1, 0));
        cells = Rig.cube(1, 1, 1, 4, 2, 4, Blocks.COAL_ORE);
        AtomicInteger calls = new AtomicInteger();
        BreakGuard.allow = (level, pos) -> {
            if (calls.incrementAndGet() == 4) throw new IllegalStateException("hostile claim mod");
            return true;
        };
        try {
            Rig.strike(Rig.at(1, 1, 1), true);
            run(animation);
        } catch (RuntimeException e) {
            Rig.check("claims (" + mode + ") hostile throw: the exception stays inside the vein", false, e.toString());
        }
        Rig.check("claims (" + mode + ") hostile throw: nothing running, the pickaxe and the world are fine", Chain.count() == 0 && !Vein.running() && Rig.air(cells) >= 3 && remaining(cells) > 0,
            "chains " + Chain.count() + ", air " + Rig.air(cells));
        Rig.grab();
        M4.noLeak("claims (" + mode + ") hostile throw");
        BreakGuard.allow = null;
    }

    private static void refusedStruck(String mode, boolean animation) {
        M4.arena();
        Config.use(animation ? Config.Values.DEFAULTS : Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
        BreakGuard.allow = (level, pos) -> false;
        boolean ok = Rig.strike(Rig.at(2, 2, 2), true);
        run(animation);
        Rig.check("claims (" + mode + ") struck block refused: nothing breaks and no chain starts", !ok && Rig.air(cells) == 0 && Chain.count() == 0, "ok " + ok + ", air " + Rig.air(cells));
        BreakGuard.allow = null;
    }

    /** {@code Level.mayInteract} (spawn protection, world border) is asked per block: a border through the vein. */
    private static void border() {
        M4.arena();
        Config.use(Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 8, 2, 2, Blocks.COAL_ORE);
        var wb = Rig.level.getWorldBorder();
        double cx = wb.getCenterX(), cz = wb.getCenterZ(), size = wb.getSize();
        try {
            wb.setCenter(Rig.base.getX() + 4, Rig.base.getZ() + 4);
            wb.setSize(8);
            Rig.strike(Rig.at(1, 1, 1), true);
            boolean exact = true;
            for (BlockPos p : cells) exact &= Rig.level.getBlockState(p).isAir() == wb.isWithinBounds(p);
            Rig.check("claims: blocks outside the world border stay ore, inside break", exact && Rig.air(cells) > 1 && remaining(cells) > 0, "air " + Rig.air(cells));
        } finally {
            wb.setSize(size);
            wb.setCenter(cx, cz);
        }
        Rig.grab();
    }

    // ---- conflicts ----

    static void conflict(MinecraftServer s) {
        Rig.begin(s);
        Rig.check("conflict: none of the known mods is loaded here", Conflicts.present().isEmpty(), Conflicts.present().toString());
        List<String> found = Conflicts.use(List.of("ftbultimine", "liteminer", "notamod", "oreexcavation", "ftbultimine"));
        Rig.check("conflict: three known mods found, unknown and duplicate ignored", found.equals(List.of("ftbultimine", "liteminer", "oreexcavation")), found.toString());
        Rig.eq("conflict: one warning each", 3, Conflicts.warnings());
        Conflicts.use(List.of());
        Rig.check("conflict: an empty list clears it", Conflicts.present().isEmpty(), Conflicts.present().toString());
        Rig.eq("conflict: and logs nothing", 3, Conflicts.warnings());
    }

    // ---- lag guard and rate caps ----

    static void lag(MinecraftServer s) {
        Rig.begin(s);
        Lag.reset();
        try {
            long[] now = {1_000_000_000L};
            Lag.clock = () -> now[0];
            for (int i = 0; i < 12; i++) {
                now[0] += 150_000_000L;
                Hooks.serverTickEnd(s);
            }
            Rig.check("lag: 10+ ticks of 150 ms trip the guard", Lag.active() && Lag.maxBlocks(64) == 32, "active " + Lag.active());
            sizeUnderLag("lag", false, 32);
            sizeUnderLag("lag (chain)", true, 32);
            for (int i = 0; i < 9; i++) {
                now[0] += 20_000_000L;
                Hooks.serverTickEnd(s);
            }
            Rig.check("lag: still halved after 9 good ticks", Lag.active(), "active " + Lag.active());
            now[0] += 20_000_000L;
            Hooks.serverTickEnd(s);
            Rig.check("lag: 10 good ticks lift it", !Lag.active() && Lag.maxBlocks(64) == 64, "active " + Lag.active());
            sizeUnderLag("lag recovered", false, 64);
        } finally {
            Lag.reset();
            Config.use(Rig.PLAIN);
        }
        rateCap();
        reach();
        Rig.clear();
    }

    private static void sizeUnderLag(String name, boolean animation, int expected) {
        M4.arena();
        Config.use(animation ? Config.Values.DEFAULTS : Rig.PLAIN);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(1, 1, 1, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.settle();
        Rig.eq(name + ": a vein of 64 coal breaks " + expected, expected, Rig.air(cells));
        Rig.grab();
        Config.use(Rig.PLAIN);
    }

    /** Four veins started by one player at once: three chains run, the fourth block is only the struck one. */
    private static void rateCap() {
        M4.arena();
        Config.use(Config.Values.DEFAULTS);
        Rig.hold(Rig.pick(1, 0));
        List<List<BlockPos>> blobs = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) blobs.add(Rig.cube(i * 3, 1, 2, 2, 2, 2, Blocks.COAL_ORE));
        for (int i = 0; i < 4; i++) Rig.strike(Rig.at(i * 3, 1, 2), true);
        Rig.eq("rate: three chains run", Chain.MAX_PER_PLAYER, Chain.count());
        Rig.eq("rate: they are all this player's", Chain.MAX_PER_PLAYER, Chain.countFor(Rig.player));
        Rig.eq("rate: the fourth vein gave only its struck block", 1, Rig.air(blobs.get(3)));
        Rig.settle();
        for (int i = 0; i < 3; i++) Rig.eq("rate: blob " + i + " finished", 8, Rig.air(blobs.get(i)));
        Rig.grab();
        M4.noLeak("rate");
        Config.use(Rig.PLAIN);
    }

    /** A long line of coal: the scan stays within 16 blocks of the struck block on each axis. */
    private static void reach() {
        M4.arena();
        BlockPos mid = Rig.at(6, 6, 6);
        List<BlockPos> line = new java.util.ArrayList<>();
        for (int dx = -20; dx <= 20; dx++) {
            BlockPos p = mid.offset(dx, 0, 0);
            Rig.set(p, Blocks.COAL_ORE.defaultBlockState());
            line.add(p);
        }
        List<BlockPos> found = Vein.scan(Rig.level, Rig.player.position(), mid, "coal", 500, 4096, true);
        int far = 0;
        for (BlockPos p : found) far = Math.max(far, Math.abs(p.getX() - mid.getX()));
        Rig.check("reach: a line of 41 gives 32 cells within 16 blocks", found.size() == 32 && far == Vein.REACH, found.size() + " cells, farthest " + far);
        for (BlockPos p : line) Rig.set(p, Blocks.AIR.defaultBlockState());
    }
}
