package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Fx;
import io.github.profetgit.veinminer.server.Hooks;
import io.github.profetgit.veinminer.server.Vein;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** PRD 13 M4 scene {@code crash}: a chain that loses its chunk, its player or its server leaves ore, never a hole, a display or a lost item. */
final class Crash {
    private static BlockPos farOrigin;
    private static List<BlockPos> farCells;
    private static int farChunkX, farChunkZ;

    private Crash() {}

    static void crash(MinecraftServer s) {
        Rig.begin(s);
        try {
            unloadedCells();
            ServerPlayer fake = Platform.get().testFakePlayer(Rig.level);
            if (fake == null) Rig.check("crash: death, logout and dimension change untested: no fake player on this loader", true, "skipped");
            else {
                try {
                    death(fake);
                    logout(fake);
                    dimension(s, fake);
                } finally {
                    fake.setHealth(fake.getMaxHealth());
                    fake.setServerLevel(Rig.level);
                    fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
            serverStop(s);
        } finally {
            Vein.unloaded = null;
            Chain.flushAll();
            Config.use(Rig.PLAIN);
            Rig.player.setShiftKeyDown(false);
            Rig.clear();
        }
    }

    private static Chain start(ServerPlayer owner, List<BlockPos>[] cells) {
        M4.arena();
        Config.use(Config.Values.DEFAULTS);
        owner.setItemInHand(InteractionHand.MAIN_HAND, Rig.pick(1, 0));
        owner.teleportTo(Rig.level, Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5, java.util.Set.of(), 0, 0, false);
        cells[0] = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.set(Rig.at(4, 2, 5), Blocks.AIR.defaultBlockState());
        Chain c = Chain.start(Rig.level, owner, Rig.at(4, 2, 5), "coal", owner.getMainHandItem());
        Rig.check("crash: a chain started for " + owner.getName().getString(), c != null && Chain.count() == 1, "chains " + Chain.count());
        return c;
    }

    @SuppressWarnings("unchecked")
    private static List<BlockPos>[] box() {
        return new List[1];
    }

    private static void stops(String name, List<BlockPos> cells, int airBefore) {
        Rig.eq(name + ": no chain left", 0, Chain.count());
        Rig.eq(name + ": no block turned after it ended", airBefore, Rig.air(cells));
        Rig.check(name + ": the rest of the vein is still ore", Rig.air(cells) < cells.size() && Rig.level.getBlockState(Rig.at(4, 2, 2)).is(Blocks.COAL_ORE), "air " + Rig.air(cells));
        Rig.eq(name + ": the loot of the turned blocks landed", Rig.air(cells) - 1, Rig.grab().count(Items.COAL_ORE));
        M4.noLeak(name);
    }

    /** The cells of a chain say "unloaded" from tick 6 on: those blocks stay ore, the loaded ones finish. */
    private static void unloadedCells() {
        M4.arena();
        Config.use(Config.Values.DEFAULTS);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        Rig.eq("crash: unload: a chain started", 1, Chain.count());
        for (int t = 0; t < 6; t++) Chain.tickAll();
        List<BlockPos> oreHigh = new ArrayList<>(), oreLow = new ArrayList<>();
        int loadedBefore = Rig.level.getChunkSource().getLoadedChunksCount();
        for (BlockPos c : cells) {
            if (!Rig.level.getBlockState(c).is(Blocks.COAL_ORE)) continue;
            (c.getY() >= Rig.base.getY() + 3 ? oreHigh : oreLow).add(c);
        }
        Rig.check("crash: unload: both halves still have ore at tick 6", !oreHigh.isEmpty() && !oreLow.isEmpty(), oreHigh.size() + " high, " + oreLow.size() + " low");
        Vein.unloaded = (level, pos) -> pos.getY() >= Rig.base.getY() + 3;
        Rig.settle();
        Vein.unloaded = null;
        boolean highOre = true, lowAir = true;
        for (BlockPos c : oreHigh) highOre &= Rig.level.getBlockState(c).is(Blocks.COAL_ORE);
        for (BlockPos c : oreLow) lowAir &= Rig.level.getBlockState(c).isAir();
        Rig.check("crash: unload: blocks in the unloaded part were skipped, not broken", highOre, oreHigh.size() + " cells");
        Rig.check("crash: unload: the loaded part finished", lowAir, oreLow.size() + " cells");
        Rig.eq("crash: unload: no chunk was loaded for the chain", loadedBefore, Rig.level.getChunkSource().getLoadedChunksCount());
        Rig.grab();
        M4.noLeak("crash: unload");
    }

    private static void death(ServerPlayer fake) {
        List<BlockPos>[] box = box();
        Chain c = start(fake, box);
        if (c == null) return;
        for (int t = 0; t < 5; t++) Chain.tickAll();
        int air = Rig.air(box[0]);
        fake.setHealth(0f);
        Chain.tickAll();
        Rig.check("crash: death: the chain ended on the next tick", Chain.count() == 0 && c.finished(), "chains " + Chain.count());
        fake.setHealth(fake.getMaxHealth());
        stops("crash: death", box[0], air);
    }

    private static void logout(ServerPlayer fake) {
        List<BlockPos>[] box = box();
        Chain c = start(fake, box);
        if (c == null) return;
        for (int t = 0; t < 5; t++) Chain.tickAll();
        int air = Rig.air(box[0]);
        Hooks.leave(fake);
        Rig.check("crash: logout: the chain ended at once", Chain.count() == 0 && c.finished(), "chains " + Chain.count());
        stops("crash: logout", box[0], air);
    }

    private static void dimension(MinecraftServer s, ServerPlayer fake) {
        ServerLevel nether = s.getLevel(Level.NETHER);
        if (nether == null) {
            Rig.check("crash: dimension change (untested: no nether level here)", true, "skipped");
            return;
        }
        List<BlockPos>[] box = box();
        Chain c = start(fake, box);
        if (c == null) return;
        for (int t = 0; t < 5; t++) Chain.tickAll();
        int air = Rig.air(box[0]);
        fake.setServerLevel(nether);
        Chain.tickAll();
        Rig.check("crash: dimension change: the chain ended on the next tick", Chain.count() == 0 && c.finished(), "chains " + Chain.count());
        fake.setServerLevel(Rig.level);
        stops("crash: dimension", box[0], air);
    }

    /** Server stopping mid-chain: displays are never saved, and the stop lands the loot and leaves the rest ore. */
    private static void serverStop(MinecraftServer s) {
        M4.arena();
        Config.use(Config.Values.DEFAULTS);
        Rig.hold(Rig.pick(1, 0));
        List<BlockPos> cells = Rig.cube(3, 1, 2, 4, 4, 4, Blocks.COAL_ORE);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.grab();
        for (int t = 0; t < 9; t++) Chain.tickAll();
        int shown = Fx.liveCount();
        boolean unsaved = true;
        for (Display d : Fx.live()) unsaved &= !d.shouldBeSaved();
        Rig.check("crash: stop: displays are in the world and none would be saved", shown > 0 && unsaved, shown + " displays, unsaved " + unsaved);
        int air = Rig.air(cells);
        Hooks.serverStopping(s);
        Rig.eq("crash: stop: no turn on the way down", air, Rig.air(cells));
        Rig.check("crash: stop: the unturned blocks stay ore", air < 64 && Rig.level.getBlockState(Rig.at(4, 2, 2)).is(Blocks.COAL_ORE), "air " + air);
        Rig.eq("crash: stop: loot landed", air, Rig.grab().count(Items.COAL_ORE));
        M4.noLeak("crash: stop");
    }

    // ---- a real chunk unload while a chain runs (needs server ticks between the steps) ----

    static void unloadStart(MinecraftServer s) {
        Rig.begin(s);
        M4.park();
        Config.use(Config.Values.DEFAULTS);
        ServerLevel level = Rig.level;
        farChunkX = (Rig.base.getX() >> 4) + 24;
        farChunkZ = Rig.base.getZ() >> 4;
        level.getChunk(farChunkX, farChunkZ);
        level.setChunkForced(farChunkX, farChunkZ, true);
        int x = farChunkX * 16 + 2, y = Rig.base.getY(), z = farChunkZ * 16 + 2;
        farCells = new ArrayList<>();
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                for (int dz = 0; dz < 4; dz++) {
                    BlockPos p = new BlockPos(x + dx, y + dy, z + dz);
                    level.setBlock(p, Blocks.COAL_ORE.defaultBlockState(), 2 | 16);
                    farCells.add(p);
                }
            }
        }
        farOrigin = new BlockPos(x + 1, y + 1, z + 1);
        Rig.hold(Rig.pick(1, 0));
        Rig.player.setShiftKeyDown(true);
        Rig.player.gameMode.destroyBlock(farOrigin);
        Hooks.drain();
        Rig.player.setShiftKeyDown(false);
        Rig.check("crash: real unload: the chain started", Chain.count() == 1, "chains " + Chain.count());
        level.setChunkForced(farChunkX, farChunkZ, false);
    }

    static boolean unloadDone() {
        return Chain.count() == 0;
    }

    static void unloadCheck(MinecraftServer s) {
        ServerLevel level = Rig.level;
        boolean gone = !level.hasChunkAt(farOrigin);
        Rig.check("crash: real unload: the chunk was unloaded and the chain never loaded it back", gone, "chunk loaded");
        Rig.eq("crash: real unload: no chain left", 0, Chain.count());
        Rig.eq("crash: real unload: no tracked displays", 0, Fx.liveCount());
        level.setChunkForced(farChunkX, farChunkZ, true);
        level.getChunk(farChunkX, farChunkZ);
        int ore = 0, air = 0, other = 0;
        for (BlockPos p : farCells) {
            var st = level.getBlockState(p);
            if (st.is(Blocks.COAL_ORE)) ore++;
            else if (st.isAir()) air++;
            else other++;
        }
        Rig.check("crash: real unload: every cell is ore or air, the unbroken ones stayed ore", other == 0 && ore + air == 64 && air >= 1, ore + " ore, " + air + " air, " + other + " other");
        int displays = level.getEntitiesOfClass(Display.class, new net.minecraft.world.phys.AABB(farOrigin).inflate(24)).size();
        Rig.eq("crash: real unload: no display came back with the chunk", 0, displays);
        for (BlockPos p : farCells) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        level.setChunkForced(farChunkX, farChunkZ, false);
        Rig.grab();
        Config.use(Rig.PLAIN);
    }
}
