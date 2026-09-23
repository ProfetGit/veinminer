import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class VeinminerTest {
    static MinecraftServer server;
    static ServerLevel level;
    static ServerPlayer player;
    static EmbeddedChannel channel;
    static int passed, failed;
    static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        net.minecraft.server.Main.main(new String[] {"--nogui"});
        server = findServer();
        long deadline = System.currentTimeMillis() + 180_000;
        while (!server.isReady()) {
            if (System.currentTimeMillis() > deadline) throw new IllegalStateException("server never became ready");
            Thread.sleep(50);
        }
        ticks(20);
        level = server.overworld();
        try {
            if (args.length >= 2 && args[0].equals("explore")) {
                explore(Path.of(args[1]));
            } else {
                Scenarios.run();
                System.out.println("SUMMARY " + passed + " passed, " + failed + " failed");
                for (String f : failures) System.out.println("  - " + f);
            }
        } catch (Throwable t) {
            t.printStackTrace(System.out);
            failed++;
        } finally {
            on(() -> { server.halt(false); return null; });
            Thread.sleep(3000);
            System.exit(failed == 0 ? 0 : 1);
        }
    }

    @SuppressWarnings("unchecked")
    static MinecraftServer findServer() throws Exception {
        Class<?> hooksClass = Class.forName("java.lang.ApplicationShutdownHooks");
        Field hooksField = hooksClass.getDeclaredField("hooks");
        hooksField.setAccessible(true);
        Map<Thread, Thread> hooks = (Map<Thread, Thread>) hooksField.get(null);
        for (Thread t : hooks.keySet()) {
            for (Field f : t.getClass().getDeclaredFields()) {
                if (MinecraftServer.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (MinecraftServer) f.get(t);
                }
            }
        }
        throw new IllegalStateException("server instance not found in shutdown hooks");
    }

    static <T> T on(Callable<T> task) {
        AtomicReference<T> out = new AtomicReference<>();
        AtomicReference<Throwable> err = new AtomicReference<>();
        server.submit(() -> {
            try {
                out.set(task.call());
            } catch (Throwable t) {
                err.set(t);
            }
        }).join();
        if (err.get() != null) throw new RuntimeException(err.get());
        return out.get();
    }

    static void ticks(int n) throws InterruptedException {
        int target = server.getTickCount() + n;
        while (server.getTickCount() < target) Thread.sleep(2);
    }

    /** Runs a console command on the server thread and returns its chat output. */
    static List<String> cmd(String command) {
        return on(() -> {
            List<String> out = new ArrayList<>();
            CommandSource capture = new CommandSource() {
                public void sendSystemMessage(Component c) { out.add(c.getString()); }
                public boolean acceptsSuccess() { return true; }
                public boolean acceptsFailure() { return true; }
                public boolean shouldInformAdmins() { return false; }
            };
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(capture), command);
            return out;
        });
    }

    static void explore(Path file) throws Exception {
        for (String line : Files.readAllLines(file)) {
            if (line.isBlank() || line.startsWith("//")) continue;
            if (line.startsWith("!tick ")) { ticks(Integer.parseInt(line.substring(6).trim())); continue; }
            if (line.equals("!player")) { spawnPlayer(); continue; }
            if (line.startsWith("!sneak ")) { sneak(Boolean.parseBoolean(line.substring(7).trim())); continue; }
            if (line.equals("!chat")) { for (String m : chat()) System.out.println("[EXPLORE] chat| " + m); continue; }
            if (line.startsWith("!destroy ")) {
                String[] p = line.substring(9).trim().split(" ");
                BlockPos pos = new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                System.out.println("[EXPLORE] destroy " + pos + " -> " + destroy(pos) + " items " + itemsNear(pos, 3));
                continue;
            }
            List<String> out = cmd(line);
            System.out.println("[EXPLORE] > " + line);
            for (String o : out) System.out.println("[EXPLORE]     " + o.replace("\n", "\n[EXPLORE]     "));
        }
    }

    static void spawnPlayer() {
        if (player != null) return;
        on(() -> {
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("VeinTester".getBytes()), "VeinTester");
            CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
            ServerPlayer p = new ServerPlayer(server, level, profile, cookie.clientInformation());
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(new ChannelHandler[] {connection});
            server.getPlayerList().placeNewPlayer(connection, p, cookie);
            player = p;
            return null;
        });
    }

    /** Drains chat/action-bar packets sent to the mock player: "text {clicks=n}". */
    static List<String> chat() {
        return on(() -> {
            List<String> out = new ArrayList<>();
            Object o;
            while ((o = channel.readOutbound()) != null) {
                if (o instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket p)
                    out.add((p.overlay() ? "[actionbar] " : "") + p.content().getString().replace("\n", "⏎") + " {clicks=" + clicks(p.content()) + "}");
                else if (o instanceof net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket p)
                    out.add("[actionbar] " + p.text().getString());
            }
            return out;
        });
    }

    static int clicks(Component c) {
        int n = c.getStyle().getClickEvent() != null ? 1 : 0;
        for (Component s : c.getSiblings()) n += clicks(s);
        return n;
    }

    static void sneak(boolean on) {
        on(() -> {
            player.setShiftKeyDown(on);
            player.setPose(on ? Pose.CROUCHING : Pose.STANDING);
            return null;
        });
    }

    static boolean destroy(BlockPos pos) {
        return on(() -> player.gameMode.destroyBlock(pos));
    }

    static String blockId(BlockPos pos) {
        return on(() -> {
            BlockState s = level.getBlockState(pos);
            return BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
        });
    }

    static Map<String, Integer> itemsNear(BlockPos pos, double r) {
        return on(() -> {
            Map<String, Integer> m = new java.util.TreeMap<>();
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(r))) {
                ItemStack s = e.getItem();
                m.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
            }
            return m;
        });
    }

    static int xpNear(BlockPos pos, double r) {
        return on(() -> {
            int total = 0;
            for (ExperienceOrb o : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(r))) total += o.getValue();
            return total;
        });
    }

    static ItemStack mainhand() {
        return on(() -> player.getMainHandItem().copy());
    }

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name + (detail.isEmpty() ? "" : "  (" + detail + ")"));
        } else {
            failed++;
            failures.add(name + ": " + detail);
            System.out.println("[FAIL] " + name + "  (" + detail + ")");
        }
    }

    static void info(String msg) {
        System.out.println("[INFO] " + msg);
    }
}

class Scenarios {
    static final int OX = 111, OY = 6, OZ = 110;
    static final BlockPos O = new BlockPos(OX, OY, OZ);

    static void cmd(String c) { VeinminerTest.cmd(c); }

    static List<String> say(String c) {
        List<String> out = VeinminerTest.cmd(c);
        for (String o : out) VeinminerTest.info("  " + c + " -> " + o);
        return out;
    }

    static BlockPos at(int dx, int dy, int dz) { return new BlockPos(OX + dx, OY + dy, OZ + dz); }

    static void arena() throws Exception {
        cmd("fill 100 0 100 131 15 131 minecraft:stone");
        cmd("fill 110 5 110 110 6 110 minecraft:air");
        cmd("kill @e[type=item]");
        cmd("kill @e[type=experience_orb]");
        cmd("gamemode survival VeinTester");
        cmd("tp VeinTester 110.5 5 110.5 -90 -10");
        cmd("gamerule block_drops true");
        cmd("scoreboard players reset * veinminer.config");
        cmd("function veinminer:config/defaults");
        cmd("scoreboard players set VeinTester veinminer.off 0");
        VeinminerTest.sneak(true);
        VeinminerTest.ticks(2);
    }

    static void place(String block, int[][] offs) {
        for (int[] o : offs) cmd("setblock " + (OX + o[0]) + " " + (OY + o[1]) + " " + (OZ + o[2]) + " " + block);
    }

    static void tool(String spec) { cmd("item replace entity VeinTester weapon.mainhand with " + spec); }

    static int remaining(String blockId, int[][] offs) {
        int n = 0;
        for (int[] o : offs) if (VeinminerTest.blockId(at(o[0], o[1], o[2])).equals(blockId)) n++;
        return n;
    }

    static int remainingInBox(String blockId, int x1, int y1, int z1, int x2, int y2, int z2) {
        return VeinminerTest.on(() -> {
            int n = 0;
            for (BlockPos p : BlockPos.betweenClosed(x1, y1, z1, x2, y2, z2))
                if (BuiltInRegistries.BLOCK.getKey(VeinminerTest.level.getBlockState(p).getBlock()).toString().equals(blockId)) n++;
            return n;
        });
    }

    static int count(Map<String, Integer> items, String id) { return items.getOrDefault(id, 0); }

    static int score(String holder, String objective) {
        String out = VeinminerTest.cmd("scoreboard players get " + holder + " " + objective).toString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(" has (-?\\d+) ").matcher(out);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MIN_VALUE;
    }

    static int requiresCount() {
        String out = VeinminerTest.cmd("data get storage veinminer:meta requires").toString();
        return out.contains("Found no elements") ? -1 : out.split("Test requirement", -1).length - 1;
    }

    static Map<String, Integer> itemsInCell(BlockPos cell) {
        return VeinminerTest.on(() -> {
            Map<String, Integer> m = new java.util.TreeMap<>();
            for (ItemEntity e : VeinminerTest.level.getEntitiesOfClass(ItemEntity.class, new AABB(cell).inflate(0.2))) {
                if (!e.blockPosition().equals(cell)) continue;
                ItemStack s = e.getItem();
                m.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
            }
            return m;
        });
    }

    static boolean mine() throws Exception {
        boolean ok = VeinminerTest.destroy(O);
        VeinminerTest.ticks(3);
        return ok;
    }

    static String held() {
        ItemStack s = VeinminerTest.mainhand();
        return BuiltInRegistries.ITEM.getKey(s.getItem()) + " dmg=" + s.getDamageValue();
    }

    static int damage() { return VeinminerTest.mainhand().getDamageValue(); }

    static final int[][] IRON = {{0,0,0},{1,0,0},{2,0,0},{2,1,0},{1,0,1},{3,2,1}};

    static void run() throws Exception {
        VeinminerTest.spawnPlayer();
        cmd("forceload add 96 96 143 143");
        cmd("gamemode survival VeinTester");
        VeinminerTest.ticks(80);
        // the hook fixture (dev/test/hookpack) is off for everything except section 25
        VeinminerTest.cmd("datapack disable \"file/hookpack\"");
        VeinminerTest.ticks(5);
        List<String> packs = VeinminerTest.cmd("datapack list enabled");
        VeinminerTest.check("base pack runs alone", !packs.toString().contains("hookpack") && packs.toString().contains("Veinminer-"), packs.toString());
        VeinminerTest.check("version_id stored for add-ons", VeinminerTest.cmd("data get storage veinminer:meta version_id").toString().contains("10100"),
            VeinminerTest.cmd("data get storage veinminer:meta version_id").toString());
        VeinminerTest.check("no add-on requirement text without add-ons", requiresCount() == -1, "requires=" + requiresCount());
        List<String> ver = VeinminerTest.cmd("data get storage veinminer:meta version");
        VeinminerTest.info("pack version: " + ver);
        VeinminerTest.check("pack loaded (config defaults present)",
            VeinminerTest.cmd("scoreboard players get #max_blocks veinminer.config").toString().contains("64"), ver.toString());

        // 1. basic mixed stone/deepslate iron vein incl. a diagonal-only link
        arena();
        place("minecraft:iron_ore", IRON);
        cmd("setblock " + (OX + 1) + " " + OY + " " + OZ + " minecraft:deepslate_iron_ore");
        tool("minecraft:iron_pickaxe");
        mine();
        Map<String, Integer> it = VeinminerTest.itemsNear(O, 1.5);
        VeinminerTest.check("iron vein fully mined", remaining("minecraft:iron_ore", IRON) + remaining("minecraft:deepslate_iron_ore", IRON) == 0,
            "left=" + (remaining("minecraft:iron_ore", IRON) + remaining("minecraft:deepslate_iron_ore", IRON)));
        VeinminerTest.check("drops gathered at mined block", count(it, "minecraft:raw_iron") == 6, "items near origin " + it + ", all " + VeinminerTest.itemsNear(O, 12));
        VeinminerTest.check("durability 1 per block", damage() == 6, held());
        VeinminerTest.check("no xp from iron", VeinminerTest.xpNear(O, 4) == 0, "xp=" + VeinminerTest.xpNear(O, 4));

        List<String> bar = VeinminerTest.chat();
        VeinminerTest.info("chat after basic vein: " + bar);
        VeinminerTest.check("action bar reports 6 blocks", bar.stream().anyMatch(m -> m.startsWith("[actionbar]") && m.contains("6 blocks mined")), bar.toString());

        // 2. not sneaking
        arena();
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        VeinminerTest.sneak(false);
        mine();
        VeinminerTest.check("not sneaking -> single block", remaining("minecraft:iron_ore", IRON) == 5, "left=" + remaining("minecraft:iron_ore", IRON));

        // 3. require_sneak off
        arena();
        cmd("scoreboard players set #require_sneak veinminer.config 0");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        VeinminerTest.sneak(false);
        mine();
        VeinminerTest.check("require_sneak=0 -> works standing", remaining("minecraft:iron_ore", IRON) == 0, "left=" + remaining("minecraft:iron_ore", IRON));

        // 4. diagonal off
        arena();
        cmd("scoreboard players set #diagonal veinminer.config 0");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("diagonal=0 leaves corner-linked ore", remaining("minecraft:iron_ore", IRON) == 1
            && VeinminerTest.blockId(at(3, 2, 1)).equals("minecraft:iron_ore"), "left=" + remaining("minecraft:iron_ore", IRON));

        // 5. tier checks
        int[][] three = {{0,0,0},{1,0,0},{0,1,0}};
        arena();
        place("minecraft:diamond_ore", three);
        tool("minecraft:stone_pickaxe");
        mine();
        VeinminerTest.check("diamond + stone pickaxe -> no veinmine", remaining("minecraft:diamond_ore", three) == 2, "left=" + remaining("minecraft:diamond_ore", three));
        arena();
        place("minecraft:ancient_debris", three);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("debris + iron pickaxe -> no veinmine", remaining("minecraft:ancient_debris", three) == 2, "left=" + remaining("minecraft:ancient_debris", three));
        arena();
        place("minecraft:ancient_debris", three);
        tool("minecraft:diamond_pickaxe");
        mine();
        it = VeinminerTest.itemsNear(O, 1.5);
        VeinminerTest.check("debris + diamond pickaxe -> vein", remaining("minecraft:ancient_debris", three) == 0 && count(it, "minecraft:ancient_debris") == 3, "items " + it);
        arena();
        place("minecraft:iron_ore", three);
        tool("minecraft:copper_pickaxe");
        mine();
        VeinminerTest.check("iron + copper pickaxe -> vein", remaining("minecraft:iron_ore", three) == 0, "left=" + remaining("minecraft:iron_ore", three));
        arena();
        place("minecraft:coal_ore", three);
        tool("minecraft:wooden_pickaxe");
        mine();
        VeinminerTest.check("coal + wooden pickaxe -> vein", remaining("minecraft:coal_ore", three) == 0, "left=" + remaining("minecraft:coal_ore", three));
        arena();
        place("minecraft:coal_ore", three);
        tool("minecraft:golden_pickaxe");
        mine();
        VeinminerTest.check("coal + golden pickaxe -> vein", remaining("minecraft:coal_ore", three) == 0, "left=" + remaining("minecraft:coal_ore", three));
        arena();
        place("minecraft:iron_ore", three);
        tool("minecraft:diamond_shovel");
        mine();
        VeinminerTest.check("non-pickaxe -> no veinmine", remaining("minecraft:iron_ore", three) == 2, "left=" + remaining("minecraft:iron_ore", three));

        // 6. other ores untouched, nether gold separate from gold
        arena();
        place("minecraft:iron_ore", new int[][] {{0,0,0},{1,0,0}});
        place("minecraft:coal_ore", new int[][] {{0,1,0},{1,1,0},{0,-1,0}});
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("adjacent coal untouched", remaining("minecraft:coal_ore", new int[][] {{0,1,0},{1,1,0},{0,-1,0}}) == 3
            && remaining("minecraft:iron_ore", new int[][] {{0,0,0},{1,0,0}}) == 0, "");
        arena();
        place("minecraft:gold_ore", new int[][] {{0,0,0},{1,0,0}});
        place("minecraft:nether_gold_ore", new int[][] {{0,1,0}});
        cmd("setblock " + (OX + 2) + " " + OY + " " + OZ + " minecraft:deepslate_gold_ore");
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("gold vein mined, nether gold kept", VeinminerTest.blockId(at(0, 1, 0)).equals("minecraft:nether_gold_ore")
            && VeinminerTest.blockId(at(2, 0, 0)).equals("minecraft:air") && VeinminerTest.blockId(at(1, 0, 0)).equals("minecraft:air"), "");

        // 7. cap 64 on a 100-block vein
        arena();
        cmd("fill " + OX + " " + OY + " " + OZ + " " + (OX + 4) + " " + (OY + 3) + " " + (OZ + 4) + " minecraft:coal_ore");
        tool("minecraft:diamond_pickaxe");
        long[] t0 = VeinminerTest.on(() -> VeinminerTest.server.getTickTimesNanos().clone());
        mine();
        long[] t1 = VeinminerTest.on(() -> VeinminerTest.server.getTickTimesNanos().clone());
        long worst64 = 0;
        for (int i = 0; i < t1.length; i++) if (t1[i] != t0[i]) worst64 = Math.max(worst64, t1[i]);
        int left = remainingInBox("minecraft:coal_ore", OX, OY, OZ, OX + 4, OY + 3, OZ + 4);
        VeinminerTest.check("max_blocks=64 caps the vein", left == 36, "left=" + left + " of 100, worst tick " + worst64 / 100_000 / 10.0 + " ms");
        VeinminerTest.check("coal xp dropped", VeinminerTest.xpNear(O, 3) > 0, "xp=" + VeinminerTest.xpNear(O, 3));

        // 8. cap 512 + timing
        arena();
        cmd("scoreboard players set #max_blocks veinminer.config 512");
        cmd("fill " + OX + " " + (OY - 5) + " " + OZ + " " + (OX + 9) + " " + (OY + 5) + " " + (OZ + 9) + " minecraft:deepslate_iron_ore");
        tool("minecraft:netherite_pickaxe");
        long[] before = VeinminerTest.on(() -> VeinminerTest.server.getTickTimesNanos().clone());
        mine();
        long[] after = VeinminerTest.on(() -> VeinminerTest.server.getTickTimesNanos().clone());
        long worst = 0;
        for (int i = 0; i < after.length; i++) if (after[i] != before[i]) worst = Math.max(worst, after[i]);
        left = remainingInBox("minecraft:deepslate_iron_ore", OX, OY - 5, OZ, OX + 9, OY + 5, OZ + 9);
        VeinminerTest.check("max_blocks=512 caps a 1100-block vein", left == 1100 - 512, "left=" + left + ", worst tick " + worst / 1_000_000 + " ms");
        VeinminerTest.check("512 blocks -> 512 durability", damage() == 512, held());

        // 9. silk touch
        int[][] four = {{0,0,0},{1,0,0},{0,1,0},{1,1,0}};
        arena();
        place("minecraft:diamond_ore", four);
        tool("minecraft:iron_pickaxe[minecraft:enchantments={\"minecraft:silk_touch\":1}]");
        mine();
        it = VeinminerTest.itemsNear(O, 1.5);
        VeinminerTest.check("silk touch drops ore blocks", count(it, "minecraft:diamond_ore") == 4 && count(it, "minecraft:diamond") == 0, "items " + it);
        VeinminerTest.check("silk touch -> no xp", VeinminerTest.xpNear(O, 4) == 0, "xp=" + VeinminerTest.xpNear(O, 4));

        // 10. fortune III
        arena();
        place("minecraft:deepslate_diamond_ore", four);
        tool("minecraft:diamond_pickaxe[minecraft:enchantments={\"minecraft:fortune\":3}]");
        mine();
        it = VeinminerTest.itemsNear(O, 1.5);
        int xp = VeinminerTest.xpNear(O, 4);
        VeinminerTest.check("fortune applies to extra blocks", count(it, "minecraft:diamond") >= 4, "items " + it);
        VeinminerTest.check("diamond xp in vanilla range", xp >= 12 && xp <= 28, "xp=" + xp);

        // 11. durability protection
        int[][] ten = {{0,0,0},{1,0,0},{2,0,0},{3,0,0},{4,0,0},{0,1,0},{1,1,0},{2,1,0},{3,1,0},{4,1,0}};
        arena();
        place("minecraft:iron_ore", ten);
        tool("minecraft:iron_pickaxe[minecraft:damage=247]");
        mine();
        bar = VeinminerTest.chat();
        VeinminerTest.check("worn-tool warning shown", bar.stream().anyMatch(m -> m.contains("almost broken")), bar.toString());
        VeinminerTest.check("stops at 1 durability, tool survives", damage() == 249 && held().startsWith("minecraft:iron_pickaxe")
            && remaining("minecraft:iron_ore", ten) == 8, held() + " left=" + remaining("minecraft:iron_ore", ten));

        // 12. unbreakable
        arena();
        place("minecraft:iron_ore", ten);
        tool("minecraft:iron_pickaxe[minecraft:unbreakable={}]");
        mine();
        VeinminerTest.check("unbreakable -> no damage", damage() == 0 && remaining("minecraft:iron_ore", ten) == 0, held());

        // 13. unbreaking III
        arena();
        cmd("fill " + OX + " " + OY + " " + OZ + " " + (OX + 4) + " " + (OY + 1) + " " + (OZ + 3) + " minecraft:coal_ore");
        tool("minecraft:diamond_pickaxe[minecraft:enchantments={\"minecraft:unbreaking\":3}]");
        mine();
        VeinminerTest.check("unbreaking reduces wear", damage() > 0 && damage() < 25, held() + " for 40 blocks");

        // 14. custom max_damage component
        arena();
        place("minecraft:iron_ore", ten);
        tool("minecraft:iron_pickaxe[minecraft:max_damage=5]");
        mine();
        VeinminerTest.check("custom max_damage respected", damage() == 4 && held().startsWith("minecraft:iron_pickaxe"), held() + " left=" + remaining("minecraft:iron_ore", ten));

        // 15. per-player toggle
        arena();
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        VeinminerTest.chat();
        cmd("execute as VeinTester run trigger veinminer");
        VeinminerTest.ticks(2);
        bar = VeinminerTest.chat();
        VeinminerTest.info("toggle chat: " + bar);
        VeinminerTest.check("toggle message says disabled", bar.stream().anyMatch(m -> m.contains("disabled") && m.contains("clicks=1")), bar.toString());
        mine();
        VeinminerTest.check("/trigger veinminer turns it off", remaining("minecraft:iron_ore", IRON) == 5, "left=" + remaining("minecraft:iron_ore", IRON));
        arena();
        cmd("scoreboard players set VeinTester veinminer.off 1");
        cmd("execute as VeinTester run trigger veinminer");
        VeinminerTest.ticks(2);
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("/trigger veinminer turns it back on", remaining("minecraft:iron_ore", IRON) == 0, "left=" + remaining("minecraft:iron_ore", IRON));

        cmd("execute as VeinTester run trigger veinminer set 3");
        VeinminerTest.ticks(2);
        boolean offSet = VeinminerTest.cmd("scoreboard players get VeinTester veinminer.off").toString().contains("has 1");
        cmd("execute as VeinTester run trigger veinminer set 2");
        VeinminerTest.ticks(2);
        boolean onSet = VeinminerTest.cmd("scoreboard players get VeinTester veinminer.off").toString().contains("has 0");
        VeinminerTest.check("/trigger veinminer set 3 / set 2 force off / on", offSet && onSet, "off=" + offSet + " on=" + onSet);

        // 16. per-ore toggle
        arena();
        cmd("scoreboard players set #ore.iron veinminer.config 0");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("disabled ore type ignored", remaining("minecraft:iron_ore", IRON) == 5, "left=" + remaining("minecraft:iron_ore", IRON));

        // 17. creative
        arena();
        cmd("gamemode creative VeinTester");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("creative -> no veinmine", remaining("minecraft:iron_ore", IRON) == 5, "left=" + remaining("minecraft:iron_ore", IRON));

        // 18. block_drops off -> ray fallback, no drops
        arena();
        cmd("gamerule block_drops false");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        it = VeinminerTest.itemsNear(O, 3);
        VeinminerTest.check("block_drops=false: ray fallback mines vein", remaining("minecraft:iron_ore", IRON) == 0, "left=" + remaining("minecraft:iron_ore", IRON));
        VeinminerTest.check("block_drops=false: no drops created", it.isEmpty(), "items " + it);

        // 19. drops at player
        arena();
        cmd("scoreboard players set #drops veinminer.config 1");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        Map<String, Integer> atPlayer = itemsInCell(new BlockPos(110, 5, 110));
        VeinminerTest.check("drops=1 puts drops at player", count(atPlayer, "minecraft:raw_iron") == 5, "player cell " + atPlayer + ", origin " + itemsInCell(O));

        // 20. lapis / redstone / quartz / emerald / copper / nether gold sanity
        String[][] kinds = {{"minecraft:lapis_ore","minecraft:lapis_lazuli","minecraft:stone_pickaxe"},{"minecraft:deepslate_redstone_ore","minecraft:redstone","minecraft:iron_pickaxe"},
            {"minecraft:nether_quartz_ore","minecraft:quartz","minecraft:wooden_pickaxe"},{"minecraft:emerald_ore","minecraft:emerald","minecraft:iron_pickaxe"},
            {"minecraft:copper_ore","minecraft:raw_copper","minecraft:stone_pickaxe"},{"minecraft:nether_gold_ore","minecraft:gold_nugget","minecraft:wooden_pickaxe"},
            {"minecraft:deepslate_coal_ore","minecraft:coal","minecraft:wooden_pickaxe"},{"minecraft:deepslate_emerald_ore","minecraft:emerald","minecraft:diamond_pickaxe"}};
        for (String[] k : kinds) {
            arena();
            place(k[0], four);
            tool(k[2]);
            mine();
            it = VeinminerTest.itemsNear(O, 1.5);
            VeinminerTest.check(k[0] + " vein", remaining(k[0], four) == 0 && count(it, k[1]) >= 4, "items " + it + " xp=" + VeinminerTest.xpNear(O, 4));
        }

        // 21. lone ore (no vein) must not break anything else
        arena();
        place("minecraft:iron_ore", new int[][] {{0,0,0}});
        place("minecraft:coal_ore", new int[][] {{1,0,0}});
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("lone ore: nothing extra mined", VeinminerTest.blockId(at(1, 0, 0)).equals("minecraft:coal_ore") && damage() == 1, held());

        // 22. settings: set, clamp, menu, reload persistence
        arena();
        say("function veinminer:settings/set {key:max_blocks,value:16}");
        VeinminerTest.check("settings/set applies", VeinminerTest.cmd("scoreboard players get #max_blocks veinminer.config").toString().contains("16"), "");
        cmd("function veinminer:settings/set {key:max_blocks,value:99999}");
        VeinminerTest.check("max_blocks clamped to 512", VeinminerTest.cmd("scoreboard players get #max_blocks veinminer.config").toString().contains("512"), "");
        VeinminerTest.chat();
        List<String> menu = VeinminerTest.cmd("execute as VeinTester run function veinminer:settings");
        List<String> shown = VeinminerTest.chat();
        for (String m : shown) VeinminerTest.info("menu| " + m);
        int totalClicks = shown.stream().mapToInt(m -> Integer.parseInt(m.replaceAll(".*\\{clicks=(\\d+)\\}$", "$1"))).sum();
        VeinminerTest.check("menu has 10 lines and all buttons", shown.size() == 10 && totalClicks == 24, shown.size() + " lines, " + totalClicks + " clickable");
        VeinminerTest.check("settings menu runs without errors", menu.stream().noneMatch(m -> m.contains("rror") || m.contains("nknown") || m.contains("<--")), menu.toString());
        say("data get storage veinminer:menu row");
        cmd("scoreboard players set #max_blocks veinminer.config 100");
        say("reload");
        VeinminerTest.ticks(5);
        VeinminerTest.check("config survives /reload", VeinminerTest.cmd("scoreboard players get #max_blocks veinminer.config").toString().contains("100"), "");
        List<String> reset = VeinminerTest.cmd("execute as VeinTester run function veinminer:settings/reset");
        VeinminerTest.check("reset restores defaults", VeinminerTest.cmd("scoreboard players get #max_blocks veinminer.config").toString().contains("64"), reset.toString());
        VeinminerTest.chat();
        List<String> tog = VeinminerTest.cmd("execute as VeinTester run function veinminer:player/welcome");
        List<String> hello = VeinminerTest.chat();
        VeinminerTest.info("welcome: " + hello);
        VeinminerTest.check("welcome text shown, same as 1.0.0", hello.contains("⛏ Veinminer: sneak while mining an ore with a pickaxe to break the whole vein. [Toggle] {clicks=1}"), hello.toString());
        VeinminerTest.check("welcome hint runs", tog.stream().noneMatch(m -> m.contains("<--")), tog.toString());

        // 25. add-on hooks, via the fixture pack dev/test/hookpack
        cmd("scoreboard players reset * vmtest");
        VeinminerTest.cmd("datapack enable \"file/hookpack\"");
        VeinminerTest.ticks(5);
        VeinminerTest.check("api/loaded runs after version_id is set", score("#loaded", "vmtest") == 1 && score("#version_id", "vmtest") == 10100,
            "loaded=" + score("#loaded", "vmtest") + " version_id=" + score("#version_id", "vmtest"));
        say("reload");
        VeinminerTest.ticks(5);
        VeinminerTest.check("requires text rebuilt on /reload, not duplicated", requiresCount() == 1 && score("#loaded", "vmtest") == 2, "requires=" + requiresCount());

        arena();
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        cmd("scoreboard players set #calls_a vmtest 0");
        cmd("scoreboard players set #calls_b vmtest 0");
        mine();
        VeinminerTest.check("hooks that don't return allow the vein", remaining("minecraft:iron_ore", IRON) == 0
            && score("#calls_a", "vmtest") == 1 && score("#calls_b", "vmtest") == 1,
            "left=" + remaining("minecraft:iron_ore", IRON) + " a=" + score("#calls_a", "vmtest") + " b=" + score("#calls_b", "vmtest"));

        arena();
        cmd("tag VeinTester add vmtest.veto_a");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        cmd("scoreboard players set #calls_b vmtest 0");
        VeinminerTest.chat();
        mine();
        it = VeinminerTest.itemsNear(O, 3);
        VeinminerTest.check("return 1 cancels the vein, single block still breaks", remaining("minecraft:iron_ore", IRON) == 5
            && VeinminerTest.blockId(O).equals("minecraft:air") && count(it, "minecraft:raw_iron") == 1 && damage() == 1,
            "left=" + remaining("minecraft:iron_ore", IRON) + " items " + it + " " + held());
        VeinminerTest.check("first returning hook ends the chain", score("#calls_b", "vmtest") == 0, "b=" + score("#calls_b", "vmtest"));
        bar = VeinminerTest.chat();
        VeinminerTest.check("cancelled vein shows no action bar", bar.stream().noneMatch(m -> m.startsWith("[actionbar]")), bar.toString());

        arena();
        cmd("tag VeinTester remove vmtest.veto_a");
        cmd("tag VeinTester add vmtest.veto_b");
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("a later hook can cancel after an earlier one falls through", remaining("minecraft:iron_ore", IRON) == 5,
            "left=" + remaining("minecraft:iron_ore", IRON));

        arena();
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        VeinminerTest.sneak(false);
        cmd("scoreboard players set #calls_a vmtest 0");
        mine();
        VeinminerTest.check("hooks run only after Veinminer's own checks pass", score("#calls_a", "vmtest") == 0, "a=" + score("#calls_a", "vmtest"));

        VeinminerTest.chat();
        VeinminerTest.cmd("execute as VeinTester run function veinminer:player/welcome");
        hello = VeinminerTest.chat();
        VeinminerTest.check("join hint shows add-on requirement", hello.stream().anyMatch(m -> m.contains("whole vein. Test requirement. [Toggle]")), hello.toString());
        VeinminerTest.cmd("execute as VeinTester run function veinminer:settings");
        shown = VeinminerTest.chat();
        VeinminerTest.check("menu shows add-on requirement", shown.size() == 11 && shown.get(1).contains("Test requirement"), shown.size() + " lines: " + shown);

        VeinminerTest.cmd("datapack disable \"file/hookpack\"");
        VeinminerTest.ticks(5);
        arena();
        place("minecraft:iron_ore", IRON);
        tool("minecraft:iron_pickaxe");
        mine();
        VeinminerTest.check("removing the add-on clears its requirement and hooks", requiresCount() == -1 && remaining("minecraft:iron_ore", IRON) == 0,
            "requires=" + requiresCount() + " left=" + remaining("minecraft:iron_ore", IRON));
        cmd("tag VeinTester remove vmtest.veto_b");

        // 24. warm benchmark: default 64-block cap, repeated
        List<Long> runs = new ArrayList<>();
        for (int r = 0; r < 10; r++) {
            arena();
            cmd("fill " + OX + " " + OY + " " + OZ + " " + (OX + 4) + " " + (OY + 3) + " " + (OZ + 4) + " minecraft:coal_ore");
            tool("minecraft:diamond_pickaxe");
            VeinminerTest.ticks(3);
            long[] b0 = VeinminerTest.on(() -> VeinminerTest.server.getTickTimesNanos().clone());
            long idle = 0;
            for (long v : b0) idle = Math.max(idle, v);
            mine();
            long[] b1 = VeinminerTest.on(() -> VeinminerTest.server.getTickTimesNanos().clone());
            long w = 0;
            for (int i = 0; i < b1.length; i++) if (b1[i] != b0[i]) w = Math.max(w, b1[i]);
            runs.add(w);
        }
        java.util.Collections.sort(runs);
        VeinminerTest.info("64-block op tick times (ms, sorted): " + runs.stream().map(v -> String.format("%.1f", v / 1e6)).toList());

        // 23. uninstall removes objectives
        List<String> un = VeinminerTest.cmd("execute as VeinTester run function veinminer:uninstall");
        List<String> objs = VeinminerTest.cmd("scoreboard objectives list");
        VeinminerTest.check("uninstall removes all objectives", objs.stream().noneMatch(o -> o.contains("veinminer")), objs + " " + un);
        VeinminerTest.check("uninstall removes version_id", VeinminerTest.cmd("data get storage veinminer:meta version_id").toString().contains("Found no elements"), "");
    }
}
