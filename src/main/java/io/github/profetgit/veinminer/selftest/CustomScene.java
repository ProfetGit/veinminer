package io.github.profetgit.veinminer.selftest;

import com.google.gson.JsonParser;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Veinminer 2.1 {@code custom}: the player's own {@code custom_blocks} list. Each entry is a group of its own, ahead of every
 * other group; the tool is the correct one for the block, or anything for a block no tool is made for.
 */
final class CustomScene {
    private static final String NETHERRACK = "minecraft:netherrack";
    private static final String LOGS = "#minecraft:logs";

    private CustomScene() {}

    private static void reset() {
        Config.Values keep = Config.get();
        M4.arena();
        Config.use(keep);
        Rig.grab();
    }

    private static void use(String... entries) {
        Config.use(Rig.PLAIN.customBlocks(List.of(entries)));
    }

    private static CommandSourceStack source() {
        return Rig.player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput();
    }

    private static boolean cmd(MinecraftServer s, String command) {
        return M4.run(s, source(), command);
    }

    private static String read() {
        try {
            return Files.readString(Config.file());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run(MinecraftServer server) {
        Rig.begin(server);
        reset();
        netherrack();
        logs();
        noTool();
        dirt();
        chests();
        unknown();
        gates();
        names();
        commands(server);
        reset();
        Config.save(Config.Values.DEFAULTS);
        Config.use(Rig.PLAIN);
    }

    private static void netherrack() {
        use(NETHERRACK);
        Rig.hold(Rig.pick(0, 0));
        Rig.check("netherrack: the entry is a group of its own", NETHERRACK.equals(Groups.of(Blocks.NETHERRACK)) && Groups.isCustom(NETHERRACK) && Groups.unknown().isEmpty(),
            Groups.of(Blocks.NETHERRACK) + " unknown " + Groups.unknown());
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), true);
        Rig.eq("netherrack: a pickaxe veins the 3x3x3 blob", 27, Rig.air(blob));
        Rig.eq("netherrack: 27 netherrack dropped", 27, Rig.grab().count(Items.NETHERRACK));

        reset();
        Rig.hold(ItemStack.EMPTY);
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), true);
        Rig.eq("netherrack: an empty hand does not vein a pickaxe block", 1, Rig.air(blob));
    }

    private static void logs() {
        use(LOGS);
        Rig.hold(new ItemStack(Items.IRON_AXE));
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.OAK_LOG);
        int birch = 0;
        for (BlockPos p : blob) {
            if (((p.getX() + p.getY() + p.getZ()) & 1) == 0) {
                Rig.set(p, Blocks.BIRCH_LOG.defaultBlockState());
                birch++;
            }
        }
        Rig.check("logs: oak and birch are in the one tag group", LOGS.equals(Groups.of(Blocks.OAK_LOG)) && LOGS.equals(Groups.of(Blocks.BIRCH_LOG)),
            Groups.of(Blocks.OAK_LOG) + "/" + Groups.of(Blocks.BIRCH_LOG));
        Rig.strike(blob.get(0), true);
        Rig.eq("logs: an axe veins mixed oak and birch", 27, Rig.air(blob));
        Rig.Loot loot = Rig.grab();
        Rig.eq("logs: birch logs dropped", birch, loot.count(Items.BIRCH_LOG));
        Rig.eq("logs: oak logs dropped", 27 - birch, loot.count(Items.OAK_LOG));

        reset();
        Rig.hold(Rig.pick(0, 0));
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.OAK_LOG);
        Rig.strike(blob.get(0), true);
        Rig.eq("logs: a pickaxe does not vein logs", 1, Rig.air(blob));

        reset();
        use("minecraft:oak_log", LOGS);
        Rig.check("logs: a block in two entries goes to the first", "minecraft:oak_log".equals(Groups.of(Blocks.OAK_LOG)) && LOGS.equals(Groups.of(Blocks.BIRCH_LOG)),
            Groups.of(Blocks.OAK_LOG) + "/" + Groups.of(Blocks.BIRCH_LOG));
    }

    private static void noTool() {
        use("minecraft:glass");
        Rig.hold(ItemStack.EMPTY);
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.GLASS);
        Rig.strike(blob.get(0), true);
        Rig.eq("glass: an empty hand veins a block no tool is made for", 27, Rig.air(blob));

        reset();
        Rig.hold(new ItemStack(Items.IRON_SHOVEL));
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.GLASS);
        Rig.strike(blob.get(0), true);
        Rig.eq("glass: so does any item", 27, Rig.air(blob));
    }

    private static void dirt() {
        reset();
        Config.use(Rig.PLAIN.customBlocks(List.of("minecraft:dirt")));
        Rig.check("dirt: the shipped group is off", !Config.get().groupOn("dirt"), Config.get().toString());
        Rig.hold(new ItemStack(Items.IRON_SHOVEL));
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.DIRT);
        Rig.check("dirt: the block is in the custom group", "minecraft:dirt".equals(Groups.of(Blocks.DIRT)), String.valueOf(Groups.of(Blocks.DIRT)));
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt: a custom entry veins with a shovel while the dirt group is off", 27, Rig.air(blob));
        Rig.eq("dirt: 27 dirt dropped", 27, Rig.grab().count(Items.DIRT));

        reset();
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.DIRT);
        Rig.strike(blob.get(0), false);
        Rig.eq("dirt: not sneaking, no vein", 1, Rig.air(blob));

        reset();
        Config.use(Config.get().group("minecraft:dirt", false));
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.DIRT);
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt: the entry switched off in groups, no vein", 1, Rig.air(blob));

        reset();
        Config.use(Config.get().group("minecraft:dirt", true));
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.DIRT);
        Rig.strike(blob.get(0), true);
        Rig.eq("dirt: switched on again, a vein", 27, Rig.air(blob));
    }

    private static void chests() {
        reset();
        use("minecraft:chest");
        Rig.hold(new ItemStack(Items.IRON_AXE));
        List<BlockPos> chests = Rig.cube(3, 1, 2, 2, 2, 2, Blocks.CHEST);
        for (BlockPos p : chests) ((Container) Rig.level.getBlockEntity(p)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        Rig.check("chests: the entry resolves", "minecraft:chest".equals(Groups.of(Blocks.CHEST)), String.valueOf(Groups.of(Blocks.CHEST)));
        Rig.strike(chests.get(0), true);
        Rig.eq("chests: only the struck chest broke", 1, Rig.air(chests));
        Rig.grab();
        int full = 0;
        for (BlockPos p : chests.subList(1, chests.size())) {
            if (Rig.level.getBlockState(p).is(Blocks.CHEST) && Rig.level.getBlockEntity(p) instanceof Container c && c.getItem(0).is(Items.DIAMOND) && c.getItem(0).getCount() == 3) full++;
        }
        Rig.eq("chests: the others stay with their items", chests.size() - 1, full);
    }

    private static void unknown() {
        reset();
        use("minecraft:nope", "#nope:nothing", NETHERRACK, "minecraft:air", "minecraft:water", "Not A Block");
        Rig.hold(Rig.pick(0, 0));
        Rig.check("unknown: the missing block and tag are listed as skipped, the file keeps them", Groups.unknown().containsAll(List.of("minecraft:nope", "#nope:nothing", "Not A Block"))
            && Config.get().customBlocks().size() == 6, Groups.unknown() + " " + Config.get().customBlocks());
        Rig.check("unknown: air and fluids are in no group", Groups.of(Blocks.AIR) == null && Groups.of(Blocks.WATER) == null && Groups.of(Blocks.LAVA) == null, "air/water/lava");
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), true);
        Rig.eq("unknown: the other entries still work", 27, Rig.air(blob));
    }

    private static void gates() {
        reset();
        use(NETHERRACK);
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), false);
        Rig.eq("gates: not sneaking, no vein", 1, Rig.air(blob));

        reset();
        Config.use(Config.get().group(NETHERRACK, false));
        blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), true);
        Rig.eq("gates: the entry switched off in groups, no vein", 1, Rig.air(blob));
    }

    private static void names() {
        use(NETHERRACK, LOGS);
        Object block = Groups.label(NETHERRACK);
        String name = block instanceof Component c ? c.getString() : String.valueOf(block);
        Rig.check("names: a block entry is the block's own name", block instanceof Component && name.equalsIgnoreCase("netherrack") || name.toLowerCase().contains("netherrack"), name);
        Rig.check("names: a tag entry is a readable name", "Logs".equals(Groups.label(LOGS)) && "Ores Iron".equals(Groups.readable("#c:ores/iron")), String.valueOf(Groups.label(LOGS)));
        Rig.check("names: an ore group stays its name", "coal".equals(Groups.label("coal")), String.valueOf(Groups.label("coal")));
    }

    private static void commands(MinecraftServer s) {
        reset();
        Config.save(Config.Values.DEFAULTS.animation(false));
        Rig.hold(Rig.pick(0, 0));
        Rig.check("command: starts with no custom entry", Config.get().customBlocks().isEmpty() && Groups.of(Blocks.NETHERRACK) == null, Config.get().toString());
        Rig.check("command: add " + NETHERRACK, cmd(s, "veinminer add " + NETHERRACK) && Config.get().customBlocks().equals(List.of(NETHERRACK)), Config.get().toString());
        Rig.check("command: the table follows without a restart", NETHERRACK.equals(Groups.of(Blocks.NETHERRACK)), String.valueOf(Groups.of(Blocks.NETHERRACK)));
        String text = read();
        Rig.check("command: the file lists it and reads back to the live values", text.contains("\"custom_blocks\": [\"minecraft:netherrack\"]")
            && Config.parse(JsonParser.parseString(text).getAsJsonObject()).equals(Config.get()), text);
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), true);
        Rig.eq("command: the added block veins at once", 27, Rig.air(blob));
        Rig.grab();

        Rig.check("command: a second add of the same block (no namespace) is refused", !cmd(s, "veinminer add netherrack") && Config.get().customBlocks().size() == 1, Config.get().toString());
        Rig.check("command: add " + LOGS, cmd(s, "veinminer add " + LOGS) && Config.get().customBlocks().equals(List.of(NETHERRACK, LOGS)), Config.get().toString());
        Rig.check("command: the tag joins the table", LOGS.equals(Groups.of(Blocks.OAK_LOG)), String.valueOf(Groups.of(Blocks.OAK_LOG)));
        List<String> before = Config.get().customBlocks();
        for (String bad : new String[]{"minecraft:nope", "#nope:nothing", "Not A Block", "minecraft:air", "minecraft:water"}) {
            Rig.check("command: add " + bad + " is refused", !cmd(s, "veinminer add " + bad) && Config.get().customBlocks().equals(before), Config.get().toString());
        }

        reset();
        BlockPos target = Rig.at(4, 2, 5);
        Rig.set(target, Blocks.BRICKS.defaultBlockState());
        Rig.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(target));
        Rig.check("command: add with no argument takes the block in view", cmd(s, "veinminer add") && Config.get().customBlocks().contains("minecraft:bricks"), Config.get().toString());
        Vec3 eye = Rig.player.getEyePosition();
        Rig.player.lookAt(EntityAnchorArgument.Anchor.EYES, eye.add(0, 10, 0));
        int size = Config.get().customBlocks().size();
        Rig.check("command: add with no argument and no block in view is refused", !cmd(s, "veinminer add") && Config.get().customBlocks().size() == size, Config.get().toString());

        Rig.check("command: remove " + NETHERRACK, cmd(s, "veinminer remove " + NETHERRACK) && !Config.get().customBlocks().contains(NETHERRACK), Config.get().toString());
        Rig.check("command: the table follows the removal", Groups.of(Blocks.NETHERRACK) == null, String.valueOf(Groups.of(Blocks.NETHERRACK)));
        Rig.check("command: remove an entry that is not there is refused", !cmd(s, "veinminer remove " + NETHERRACK), Config.get().toString());
        text = read();
        Rig.check("command: the file reads back to the live values", !Config.parse(JsonParser.parseString(text).getAsJsonObject()).customBlocks().contains(NETHERRACK)
            && Config.parse(JsonParser.parseString(text).getAsJsonObject()).equals(Config.get()), text);

        Config.save(Config.Values.DEFAULTS.customBlocks(List.of("minecraft:nope", NETHERRACK)));
        Rig.check("command: status runs with an unknown entry", cmd(s, "veinminer status"), "status failed");
        Rig.check("command: an unknown entry can be removed", cmd(s, "veinminer remove minecraft:nope") && Config.get().customBlocks().equals(List.of(NETHERRACK)), Config.get().toString());

        var dispatcher = s.getCommands().getDispatcher();
        var low = Rig.player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.ALL);
        var high = Rig.player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
        for (String sub : new String[]{"add", "remove"}) {
            Rig.check("command: " + sub + " needs level 2", !dispatcher.findNode(List.of("veinminer", sub)).canUse(low) && dispatcher.findNode(List.of("veinminer", sub)).canUse(high), "permission");
        }
        Config.save(Config.Values.DEFAULTS);
        Config.use(Rig.PLAIN);
    }

    // ---- file edit: the poll picks up a new entry while the game runs ----

    static void edit(MinecraftServer s) {
        Rig.begin(s);
        M4.arena();
        Config.save(Config.Values.DEFAULTS);
        Rig.check("edit: no custom entry before the edit", Groups.of(Blocks.NETHERRACK) == null, String.valueOf(Groups.of(Blocks.NETHERRACK)));
        try {
            Path f = Config.file();
            Files.writeString(f, "{\n  \"custom_blocks\": [\"minecraft:netherrack\", \"minecraft:nope\"]\n}\n");
            Files.setLastModifiedTime(f, FileTime.fromMillis(System.currentTimeMillis() + 20_000L));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        M4.park();
    }

    static void checkEdit(MinecraftServer s) {
        Rig.check("edit: the poll read the entry", Config.get().customBlocks().equals(List.of(NETHERRACK, "minecraft:nope")), Config.get().toString());
        Rig.check("edit: the table follows the file", NETHERRACK.equals(Groups.of(Blocks.NETHERRACK)) && Groups.unknown().equals(List.of("minecraft:nope")),
            Groups.of(Blocks.NETHERRACK) + " unknown " + Groups.unknown());
        Config.Values keep = Config.get().animation(false);
        M4.arena();
        Config.use(keep);
        Rig.hold(Rig.pick(0, 0));
        List<BlockPos> blob = Rig.cube(3, 1, 2, 3, 3, 3, Blocks.NETHERRACK);
        Rig.strike(blob.get(0), true);
        Rig.eq("edit: the edited-in entry veins", 27, Rig.air(blob));
        Rig.grab();
        Config.save(Config.Values.DEFAULTS);
        Config.use(Rig.PLAIN);
        M4.arena();
    }
}
