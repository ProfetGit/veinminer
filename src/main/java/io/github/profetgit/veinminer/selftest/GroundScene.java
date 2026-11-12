package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.GroupData;
import io.github.profetgit.veinminer.server.GroupDef;
import io.github.profetgit.veinminer.server.Groups;
import java.util.Map;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Veinminer 2.1 {@code ground}: dirt, sand, gravel and clay veined with any shovel ({@code "tool": "correct"}), off by default,
 * and sand and gravel never falling out of a running chain. {@link #instant} breaks whole veins in one tick; the live cases run
 * real server ticks and watch for falling blocks every tick.
 */
final class GroundScene {
    static final List<String> LIVE = List.of("column", "blob", "column_instant", "suspicious", "halted");
    private static final int COLUMN = 10;

    private static volatile int gen;
    private static volatile boolean ended;
    private static int ticks, maxFalling, firstFall, haltAt, quiet, minedAtHalt, bare, leftAtHalt;
    private static boolean swapped;
    private static List<BlockPos> cells = List.of();

    private GroundScene() {}

    private static ItemStack shovel() {
        return new ItemStack(Items.IRON_SHOVEL);
    }

    private static void reset() {
        Config.Values keep = Config.get();
        M4.arena();
        Config.use(keep);
        Rig.grab();
    }

    private static List<BlockPos> dirtBlob() {
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.DIRT);
        for (BlockPos p : blob) if (p.getY() == Rig.at(0, 3, 0).getY()) Rig.set(p, Blocks.GRASS_BLOCK.defaultBlockState());
        return blob;
    }

    private static List<BlockPos> blocks(Block block) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = 0; x < Rig.SIZE; x++) {
            for (int y = 0; y < Rig.SIZE; y++) {
                for (int z = 0; z < Rig.SIZE; z++) if (Rig.level.getBlockState(Rig.at(x, y, z)).is(block)) out.add(Rig.at(x, y, z));
            }
        }
        return out;
    }

    // ---- instant: whole veins in one tick ----

    static void instant(MinecraftServer server) {
        Rig.begin(server);
        reset();
        data();
        merge();
        dirt();
        shovels();
        loot();
        command(server);
        reset();
    }

    private static void data() {
        for (String g : List.of("dirt", "sand", "gravel", "clay")) {
            Rig.check("data: group " + g + " is shipped as tool correct, default off", GroupData.groups().containsKey(g) && GroupData.tool(g) == GroupDef.Tool.CORRECT && !GroupData.defaultOn(g),
                GroupData.groups().keySet().toString());
        }
        Rig.check("data: the ores keep the pickaxe and are on", GroupData.tool("coal") == GroupDef.Tool.PICKAXE && GroupData.defaultOn("coal") && GroupData.defaultOn("tin"), "coal/tin");
        Block[][] members = {
            {Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM},
            {Blocks.SAND, Blocks.RED_SAND}, {Blocks.GRAVEL}, {Blocks.CLAY}};
        String[] names = {"dirt", "sand", "gravel", "clay"};
        for (int i = 0; i < names.length; i++) {
            for (Block b : members[i]) Rig.check("data: " + b.getDescriptionId() + " is in " + names[i], names[i].equals(Groups.of(b)), String.valueOf(Groups.of(b)));
        }
        Rig.check("data: groups are off with no config entry", !Config.Values.DEFAULTS.groupOn("dirt") && !Config.Values.DEFAULTS.groupOn("sand")
            && !Config.Values.DEFAULTS.groupOn("gravel") && !Config.Values.DEFAULTS.groupOn("clay") && Config.Values.DEFAULTS.groupOn("coal"), Config.Values.DEFAULTS.toString());
    }

    private static void merge() {
        Map<Identifier, GroupDef> before = GroupData.loaded();
        GroupDef plain = GroupDef.parse(com.google.gson.JsonParser.parseString("{\"blocks\":\"#a:b\"}")).result().orElseThrow();
        GroupDef strict = GroupDef.parse(com.google.gson.JsonParser.parseString("{\"blocks\":\"#a:c\",\"tool\":\"correct\",\"default\":false}")).result().orElseThrow();
        try {
            GroupData.apply(Map.of(Identifier.fromNamespaceAndPath("a", "mix"), plain, Identifier.fromNamespaceAndPath("b", "mix"), strict,
                Identifier.fromNamespaceAndPath("a", "solo"), plain));
            Rig.check("merge: one file saying correct makes the group correct", GroupData.tool("mix") == GroupDef.Tool.CORRECT && GroupData.tool("solo") == GroupDef.Tool.PICKAXE, "tools");
            Rig.check("merge: one file saying default false turns the group off", !GroupData.defaultOn("mix") && GroupData.defaultOn("solo") && GroupData.defaultOn("coal"), "defaults");
            Rig.check("merge: the config entry beats the default", Config.Values.DEFAULTS.group("mix", true).groupOn("mix") && !Config.Values.DEFAULTS.group("solo", false).groupOn("solo")
                && !Config.Values.DEFAULTS.groupOn("mix") && Config.Values.DEFAULTS.groupOn("solo"), "groupOn");
        } finally {
            GroupData.apply(before);
        }
        Rig.check("merge: the shipped groups are back", GroupData.tool("dirt") == GroupDef.Tool.CORRECT && !GroupData.defaultOn("sand"), GroupData.groups().keySet().toString());
    }

    private static void dirt() {
        Config.use(Rig.PLAIN);
        Rig.hold(shovel());
        List<BlockPos> blob = dirtBlob();
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt off: a shovel mines the struck block only", 1, Rig.air(blob));
        Rig.eq("dirt off: its dirt dropped", 1, Rig.grab().count(Items.DIRT));

        reset();
        Config.use(Rig.PLAIN.group("dirt", true));
        ItemStack tool = shovel();
        Rig.hold(tool);
        blob = dirtBlob();
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt on: the shovel veins the whole 3x3x3 blob, grass top included", 27, Rig.air(blob));
        Rig.eq("dirt on: 27 dirt items (grass drops dirt)", 27, Rig.grab().count(Items.DIRT));
        Rig.eq("dirt on: the shovel paid one durability per block", 27, tool.getDamageValue());

        reset();
        Config.use(Rig.PLAIN.group("dirt", true).durability(false));
        tool = shovel();
        Rig.hold(tool);
        blob = dirtBlob();
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt, durability off: the blob is veined", 27, Rig.air(blob));
        Rig.eq("dirt, durability off: only the struck block wore the shovel", 1, tool.getDamageValue());

        reset();
        Config.use(Rig.PLAIN.group("dirt", true));
        Rig.hold(Rig.pick(0, 0));
        blob = dirtBlob();
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt on, pickaxe: no vein", 1, Rig.air(blob));

        reset();
        Rig.hold(ItemStack.EMPTY);
        blob = dirtBlob();
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt on, bare hand: no vein", 1, Rig.air(blob));

        reset();
        Rig.hold(shovel());
        List<BlockPos> coal = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.COAL_ORE);
        Rig.strike(coal.get(0), true);
        Rig.eq("dirt on, shovel on coal ore: no vein", 1, Rig.air(coal));

        reset();
        Rig.hold(shovel());
        blob = dirtBlob();
        Rig.strike(blob.get(0), false);
        Rig.eq("dirt on, not sneaking: no vein", 1, Rig.air(blob));
    }

    private static void shovels() {
        Config.use(Rig.PLAIN.group("dirt", true));
        for (Item item : List.of(Items.WOODEN_SHOVEL, Items.NETHERITE_SHOVEL)) {
            reset();
            Rig.hold(new ItemStack(item));
            List<BlockPos> blob = dirtBlob();
            Rig.strike(blob.get(0), true);
            Rig.eq("shovels: " + item.getDescriptionId() + " veins the blob", 27, Rig.air(blob));
        }
    }

    private static void loot() {
        Config.use(Rig.PLAIN.group("gravel", true).group("clay", true).group("sand", true));
        reset();
        Rig.hold(shovel());
        List<BlockPos> cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.GRAVEL);
        Rig.strike(cells.get(0), true);
        Rig.eq("gravel: the blob is veined", 27, Rig.air(cells));
        Rig.Loot loot = Rig.grab();
        Rig.eq("gravel: gravel plus flint equals the cells", 27, loot.count(Items.GRAVEL) + loot.count(Items.FLINT));

        reset();
        Rig.hold(shovel());
        cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.CLAY);
        Rig.strike(cells.get(0), true);
        Rig.eq("clay: the blob is veined", 27, Rig.air(cells));
        Rig.eq("clay: 4 clay balls per cell", 27 * 4, Rig.grab().count(Items.CLAY_BALL));

        reset();
        Rig.hold(shovel());
        cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.RED_SAND);
        Rig.strike(cells.get(0), true);
        Rig.eq("red sand: the blob is veined", 27, Rig.air(cells));
        Rig.eq("red sand: 27 items", 27, Rig.grab().count(Items.RED_SAND));

        reset();
        Rig.hold(shovel());
        cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.DIRT);
        Config.use(Rig.PLAIN.group("gravel", true).group("clay", true).group("sand", true));
        Rig.strike(cells.get(0), true);
        Rig.eq("other groups on, dirt still off: no vein", 1, Rig.air(cells));
    }

    private static void command(MinecraftServer s) {
        Config.save(Config.Values.DEFAULTS);
        Rig.check("command: dirt is off in the default config", !Config.get().groupOn("dirt") && Config.get().groups().isEmpty(), Config.get().toString());
        Rig.check("command: set groups.dirt true", M4.run(s, "veinminer set groups.dirt true") && Config.get().groupOn("dirt"), Config.get().toString());
        String text = "";
        try {
            text = Files.readString(Config.file());
        } catch (java.io.IOException e) {
            text = e.toString();
        }
        Rig.check("command: the file says dirt true and explains the defaults", text.contains("\"dirt\": true") && text.contains("dirt, sand, gravel, clay off"), text);
        Rig.check("command: the file reads back to the live values", Config.parse(com.google.gson.JsonParser.parseString(text).getAsJsonObject()).equals(Config.get()), text);
        Rig.check("command: status runs", M4.run(s, "veinminer status"), "status failed");
        Rig.check("command: set groups.dirt false", M4.run(s, "veinminer set groups.dirt false") && !Config.get().groupOn("dirt"), Config.get().toString());
        Rig.check("command: set groups.coal false then true", M4.run(s, "veinminer set groups.coal false") && !Config.get().groupOn("coal")
            && M4.run(s, "veinminer set groups.coal true") && Config.get().groupOn("coal"), Config.get().toString());
        Config.save(Config.Values.DEFAULTS);
        Config.use(Rig.PLAIN);
    }

    // ---- live: real ticks, falling blocks counted every tick ----

    private static AABB wide() {
        return new AABB(Rig.base.getX() - 2, Rig.base.getY() - 2, Rig.base.getZ() - 2, Rig.base.getX() + Rig.SIZE + 2, Rig.base.getY() + Rig.SIZE + 2, Rig.base.getZ() + Rig.SIZE + 2);
    }

    private static void begin(boolean animation) {
        M4.arena();
        Rig.grab();
        Config.use(Config.Values.DEFAULTS.animation(animation).group("sand", true));
        Rig.hold(shovel());
        ticks = 0;
        maxFalling = 0;
        firstFall = -1;
        haltAt = -1;
        quiet = 0;
        minedAtHalt = -1;
        bare = 0;
        leftAtHalt = 0;
        swapped = false;
        ended = false;
        gen++;
    }

    static void start(MinecraftServer server, String name) {
        Rig.begin(server);
        switch (name) {
            case "column" -> {
                begin(true);
                cells = Rig.cube(4, 1, 5, 1, COLUMN, 1, Blocks.SAND);
                Rig.strike(cells.get(0), true);
                Rig.eq("column: the chain started", 1, Chain.count());
            }
            case "column_instant" -> {
                begin(false);
                cells = Rig.cube(4, 1, 5, 1, COLUMN, 1, Blocks.SAND);
                Rig.strike(cells.get(0), true);
                Rig.eq("column, no animation: no chain, the vein broke at once", 0, Chain.count());
                Rig.eq("column, no animation: all cells air right away", COLUMN, Rig.air(cells));
            }
            case "blob" -> {
                begin(true);
                cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.SAND);
                Rig.strike(Rig.at(4, 1, 3), true);
                Rig.eq("blob: the chain started", 1, Chain.count());
            }
            case "suspicious" -> {
                begin(true);
                cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.SAND);
                BlockPos be = Rig.at(4, 1, 3);
                Rig.set(be, Blocks.SUSPICIOUS_SAND.defaultBlockState());
                cells = new ArrayList<>(cells);
                cells.remove(be);
                Rig.check("suspicious: the fixture has a block entity", Rig.level.getBlockEntity(be) != null, "no block entity");
                Rig.strike(cells.get(0), true);
                Rig.eq("suspicious: the chain started", 1, Chain.count());
            }
            case "halted" -> {
                begin(true);
                cells = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.SAND);
                Rig.strike(Rig.at(4, 1, 3), true);
                Rig.eq("halted: the chain started", 1, Chain.count());
            }
            default -> throw new IllegalArgumentException(name);
        }
    }

    /** Client tick: queue one sample on the server thread. True once the case is over. */
    static boolean poll(Minecraft mc, String name) {
        int g = gen;
        MinecraftServer s = mc.getSingleplayerServer();
        s.execute(() -> sample(g, name));
        return ended;
    }

    private static void sample(int g, String name) {
        if (g != gen || ended) return;
        ticks++;
        int falling = Rig.level.getEntitiesOfClass(FallingBlockEntity.class, wide()).size();
        maxFalling = Math.max(maxFalling, falling);
        if (falling > 0 && firstFall < 0) firstFall = ticks;
        List<Chain> chains = Chain.active();
        if (haltAt < 0) {
            for (Chain c : chains) {
                if (c.stopped()) {
                    haltAt = ticks;
                    minedAtHalt = c.mined();
                    for (BlockPos p : cells) {
                        if (!Rig.level.getBlockState(p).is(Blocks.SAND)) continue;
                        leftAtHalt++;
                        if (!Rig.level.getBlockTicks().hasScheduledTick(p, Blocks.SAND)) bare++;
                    }
                }
            }
        }
        if (name.equals("halted") && !swapped && !chains.isEmpty() && chains.get(0).mined() >= 8) {
            swapped = true;
            Rig.hold(shovel());
        }
        quiet = Chain.count() == 0 && falling == 0 ? quiet + 1 : 0;
        ended = quiet >= 12;
    }

    static void finish(MinecraftServer server, String name) {
        gen++;
        Rig.eq(name + ": no chain left", 0, Chain.count());
        switch (name) {
            case "column", "column_instant" -> whole(name, COLUMN);
            case "blob" -> whole(name, 27);
            case "suspicious" -> {
                Rig.eq("suspicious: every other sand cell mined", 26, Rig.air(cells));
                Rig.check("suspicious: the suspicious sand stays, with its block entity", Rig.level.getBlockState(Rig.at(4, 1, 3)).is(Blocks.SUSPICIOUS_SAND)
                    && Rig.level.getBlockEntity(Rig.at(4, 1, 3)) != null, Rig.level.getBlockState(Rig.at(4, 1, 3)).toString());
                Rig.eq("suspicious: no falling block entity at any tick", 0, maxFalling);
                Rig.eq("suspicious: 26 sand items", 26, Rig.grab().count(Items.SAND));
            }
            case "halted" -> halted();
            default -> throw new IllegalArgumentException(name);
        }
        Rig.hold(ItemStack.EMPTY);
        M4.arena();
        Rig.grab();
    }

    private static void whole(String name, int count) {
        Rig.eq(name + ": no falling block entity at any tick of " + ticks + " ticks", 0, maxFalling);
        Rig.eq(name + ": every cell is air", count, Rig.air(cells));
        Rig.eq(name + ": no sand block anywhere", 0, blocks(Blocks.SAND).size());
        Rig.eq(name + ": sand items equal the cells", count, Rig.grab().count(Items.SAND));
    }

    private static void halted() {
        int left = 27 - 1 - minedAtHalt;
        Rig.check("halted: the chain stopped part-way", minedAtHalt >= 8 && left >= 1, "mined " + minedAtHalt + ", left " + left);
        Rig.eq("halted: the sand left at the stop was counted", left, leftAtHalt);
        Rig.eq("halted: every leftover sand cell has a scheduled tick right after the stop", 0, bare);
        Rig.check("halted: the leftover sand fell within 6 ticks of the stop", firstFall >= 0 && haltAt >= 0 && firstFall - haltAt <= 6, "halt " + haltAt + ", first fall " + firstFall);
        List<BlockPos> sand = blocks(Blocks.SAND);
        Rig.eq("halted: the leftover sand is all still sand blocks", left, sand.size());
        boolean hover = false;
        for (BlockPos p : sand) hover |= Rig.level.getBlockState(p.below()).isAir();
        Rig.check("halted: no sand hovers", !hover, sand.toString());
        Rig.eq("halted: the mined sand dropped", 27 - left, Rig.grab().count(Items.SAND));
    }
}
