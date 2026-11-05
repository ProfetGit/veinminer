package io.github.profetgit.veinminer.selftest;

import com.google.gson.JsonParser;
import io.github.profetgit.veinminer.server.Api;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Hooks;
import io.github.profetgit.veinminer.server.Migrate;
import io.github.profetgit.veinminer.server.Players;
import io.github.profetgit.veinminer.server.Say;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

/** PRD 13 M4 scenes {@code players}, {@code config_reload} and {@code migrate}: what survives a restart, a reload and the 1.3.x pack. */
final class Persist {
    private static final String PACK = "Veinminer-1.3.3";
    private static int failures;

    private Persist() {}

    // ---- players: the opt-out is saved data ----

    static void players(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        var player = Rig.player;
        try {
            Rig.check("players: on by default", Players.enabled(player), "off");
            M4.arena();
            Rig.hold(Rig.pick(1, 0));
            Rig.check("players: /veinminer turns it off", M4.run(s, player.createCommandSourceStack(), "veinminer") && !Players.enabled(player), "still on");
            List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
            Rig.strike(Rig.at(2, 2, 2), true);
            Rig.eq("players: off means no vein", 1, Rig.air(cells));
            Rig.grab();

            reopened(s, "off", true);

            Rig.check("players: /veinminer turns it on again", M4.run(s, player.createCommandSourceStack(), "veinminer") && Players.enabled(player), "still off");
            M4.arena();
            cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
            Rig.strike(Rig.at(2, 2, 2), true);
            Rig.eq("players: on means a vein", 27, Rig.air(cells));
            Rig.grab();
            reopened(s, "on again", false);
        } finally {
            Players.setEnabled(s, player.getUUID(), true);
            Rig.clear();
        }
    }

    /** Saves, then reads the file with a second storage over the same folder, as a restart would. */
    private static void reopened(MinecraftServer s, String name, boolean expectOff) {
        s.overworld().getDataStorage().saveAndJoin();
        Path file = null;
        try (var walk = Files.walk(s.getWorldPath(LevelResource.ROOT))) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (p.endsWith(Path.of("veinminer", "players.dat"))) file = p;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        Rig.check("players (" + name + "): data/veinminer/players.dat exists", file != null, "no file");
        if (file == null) return;
        try (SavedDataStorage fresh = new SavedDataStorage(file.getParent().getParent(), s.getFixerUpper(), s.registryAccess())) {
            Players read = fresh.computeIfAbsent(Players.type());
            Rig.check("players (" + name + "): a fresh storage reads the opt-out " + (expectOff ? "in" : "out"), read.contains(Rig.player.getUUID()) == expectOff,
                "contains " + read.contains(Rig.player.getUUID()));
        }
    }

    // ---- config_reload: the file is polled, a broken file keeps the last good values, /veinminer set round-trips ----

    private static void write(String text, int secondsAhead) {
        try {
            Path f = Config.file();
            Files.writeString(f, text);
            Files.setLastModifiedTime(f, FileTime.fromMillis(System.currentTimeMillis() + secondsAhead * 1000L));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void configEdit(MinecraftServer s) {
        Rig.begin(s);
        Config.save(Config.Values.DEFAULTS);
        failures = Config.failures();
        write("{\n  // edited by hand\n  \"max_blocks\": 7,\n  \"animation\": false,\n  \"groups\": {\"coal\": false}\n}\n", 20);
        M4.park();
    }

    static void configCheckEdit(MinecraftServer s) {
        Config.Values v = Config.get();
        Rig.check("config_reload: an edit is picked up by the mtime poll", v.maxBlocks() == 7 && !v.animation() && !v.groupOn("coal") && v.groupOn("tin"), v.toString());
        Rig.eq("config_reload: a good file logs nothing", failures, Config.failures());
        write("{ this is not json", 40);
    }

    static void configCheckBroken(MinecraftServer s) {
        Config.Values v = Config.get();
        Rig.check("config_reload: a broken file keeps the last good values", v.maxBlocks() == 7 && !v.animation() && Config.broken(), v.toString() + ", broken " + Config.broken());
        Rig.eq("config_reload: one log line for the broken file, not one per poll", failures + 1, Config.failures());
        write("{ \"max_blocks\": 9 }", 60);
    }

    static void configCheckFixed(MinecraftServer s) {
        Config.Values v = Config.get();
        Rig.check("config_reload: fixing the file recovers, missing keys are the defaults", v.maxBlocks() == 9 && v.animation() && v.groups().isEmpty() && !Config.broken(), v.toString());
        Rig.check("config_reload: set max_blocks 33", M4.run(s, "veinminer set max_blocks 33") && Config.get().maxBlocks() == 33, Config.get().toString());
        String text = read();
        Rig.check("config_reload: set wrote the file with its comments", text.contains("\"max_blocks\": 33") && text.contains("// Most blocks one vein mines"), text);
        Rig.check("config_reload: the file reads back to the live values", Config.parse(JsonParser.parseString(text).getAsJsonObject()).equals(Config.get()), text);
        Rig.check("config_reload: set groups.tin false", M4.run(s, "veinminer set groups.tin false") && !Config.get().groupOn("tin"), Config.get().toString());
        Rig.check("config_reload: set require_sneak false, drops 1, visit_cap 100, animation false",
            M4.run(s, "veinminer set require_sneak false") && M4.run(s, "veinminer set drops 1") && M4.run(s, "veinminer set visit_cap 100") && M4.run(s, "veinminer set animation false")
                && !Config.get().requireSneak() && Config.get().drops() == 1 && Config.get().visitCap() == 100 && !Config.get().animation(), Config.get().toString());
        Config.Values before = Config.get();
        String fileBefore = read();
        for (String bad : new String[]{"max_blocks 5000", "max_blocks abc", "max_blocks 0", "require_sneak maybe", "drops 2", "visit_cap 10", "bogus 1", "groups. true", "groups.tin 1"}) {
            Rig.check("config_reload: set " + bad + " is refused", !M4.run(s, "veinminer set " + bad) && Config.get().equals(before), Config.get().toString());
        }
        Rig.check("config_reload: a refused set leaves the file as it was", read().equals(fileBefore), "file changed");
        var dispatcher = s.getCommands().getDispatcher();
        var low = Rig.player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.ALL);
        var high = Rig.player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER);
        Rig.check("config_reload: set needs level 2", !dispatcher.findNode(List.of("veinminer", "set")).canUse(low) && dispatcher.findNode(List.of("veinminer", "set")).canUse(high), "permission");
        Rig.check("config_reload: the toggle is for everyone", dispatcher.getRoot().getChild("veinminer").canUse(low), "permission");
        Config.save(Config.Values.DEFAULTS);
        Config.use(Rig.PLAIN);
    }

    private static String read() {
        try {
            return Files.readString(Config.file());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- migrate: a world with the real 1.3.3 pack enabled, scores set, then the 2.0 start-up migration ----

    private static Path worldPack(MinecraftServer s) {
        return s.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK);
    }

    static void migrateInstall(MinecraftServer s) {
        Rig.begin(s);
        M4.park();
        Config.save(Config.Values.DEFAULTS);
        M4.copyTree(M4.workspace().resolve("packs/Veinminer/pack"), worldPack(s));
        M4.ran(s, "reload");
        Rig.check("migrate: /reload found and enabled the 1.3.3 pack", s.getPackRepository().getSelectedIds().contains("file/" + PACK), s.getPackRepository().getSelectedIds().toString());
    }

    static void migrateRun(MinecraftServer s) {
        Rig.begin(s);
        var sb = s.getScoreboard();
        boolean loaded = sb.getObjective("veinminer.config") != null && sb.getObjective("veinminer.off") != null
            && s.getFunctions().get(Identifier.fromNamespaceAndPath("veinminer", "uninstall")).isPresent();
        Rig.check("migrate: the 1.3.3 pack runs in the world (objectives and uninstall function exist)", loaded, s.getPackRepository().getSelectedIds().toString());
        if (!loaded) return;
        String me = Rig.player.getScoreboardName();
        for (String c : new String[]{
            "scoreboard players set #max_blocks veinminer.config 200", "scoreboard players set #animation veinminer.config 0",
            "scoreboard players set #diagonal veinminer.config 0", "scoreboard players set #feedback veinminer.config 0",
            "scoreboard players set #drops veinminer.config 1", "scoreboard players set #ore.coal veinminer.config 0",
            "scoreboard players set " + me + " veinminer.off 1", "scoreboard players set Ghost_Nobody veinminer.off 1",
            "scoreboard players set Someone_Else veinminer.off 0"}) {
            M4.run(s, c);
        }
        Config.save(Config.Values.DEFAULTS);
        Players.setEnabled(s, Rig.player.getUUID(), true);
        List<String> told = new ArrayList<>();
        Say.tap = (p, text) -> told.add(p.getScoreboardName() + ": " + text.getString());
        int runs = Migrate.runs;
        String summary;
        try {
            summary = Migrate.start(s);
        } finally {
            Say.tap = null;
        }
        Rig.check("migrate: start reports a migration", summary != null && Migrate.runs == runs + 1, String.valueOf(summary));
        Config.Values v = Config.get();
        Rig.check("migrate: scores imported into the config", v.maxBlocks() == 200 && !v.animation() && !v.diagonal() && !v.feedback() && v.drops() == 1 && !v.groupOn("coal") && v.groupOn("iron") && v.requireSneak(), v.toString());
        String file = read();
        Rig.check("migrate: config/veinminer.json has them", file.contains("\"max_blocks\": 200") && file.contains("\"coal\": false"), file);
        Rig.check("migrate: the player's veinminer.off score became the saved opt-out", !Players.enabled(Rig.player), "still on");
        Rig.check("migrate: both veinminer.off = 1 holders imported (an offline-mode cache resolves any name), the 0 one not", summary != null && summary.contains("2 player opt-outs"), String.valueOf(summary));
        boolean objectives = false;
        for (String o : sb.getObjectiveNames()) objectives |= o.equals("veinminer") || o.startsWith("veinminer.");
        Rig.check("migrate: no veinminer objective is left", !objectives, sb.getObjectiveNames().toString());
        boolean storages = true;
        for (String id : new String[]{"groups", "menu", "op", "anim"}) storages &= s.getCommandStorage().get(Identifier.fromNamespaceAndPath("veinminer", id)).isEmpty();
        Rig.check("migrate: the pack's storages are empty", storages, "not empty");
        Rig.check("migrate: the meta version was dropped for Api.load", s.getCommandStorage().get(Api.metaId()).getIntOr("version_id", -1) == -1, s.getCommandStorage().get(Api.metaId()).toString());
        Hooks.dataReloaded(s, true);
        Rig.eq("migrate: Api.load then wrote version_id 20000", 20000, s.getCommandStorage().get(Api.metaId()).getIntOr("version_id", -1));
        Rig.check("migrate: the op was told in chat", told.stream().anyMatch(t -> t.contains("replaced the Veinminer 1.3 data pack") && t.contains(PACK)), told.toString());
        Rig.check("migrate: the pack folder is still there", Files.isDirectory(worldPack(s)), "deleted");
        M4.park();
    }

    static void migrateAfter(MinecraftServer s) {
        Rig.begin(s);
        var repo = s.getPackRepository();
        Rig.check("migrate: the old pack is disabled", !repo.getSelectedIds().contains("file/" + PACK), repo.getSelectedIds().toString());
        Rig.check("migrate: the pack folder is still there", Files.isDirectory(worldPack(s)), "deleted");
        Rig.check("migrate: nothing to migrate any more", Migrate.start(s) == null, "found");
        M4.run(s, "scoreboard objectives add veinminer.config dummy");
        M4.run(s, "scoreboard players set #max_blocks veinminer.config 55");
        int max = Config.get().maxBlocks();
        Migrate.start(s);
        Rig.check("migrate: the config is imported once, a second run leaves it", Config.get().maxBlocks() == max && max == 200, "max_blocks " + Config.get().maxBlocks());
        Rig.check("migrate: and still removes the objective", s.getScoreboard().getObjective("veinminer.config") == null, "left");
        Players.setEnabled(s, Rig.player.getUUID(), true);
        Config.save(Config.Values.DEFAULTS);
        Config.use(Rig.PLAIN);
        M4.deleteTree(worldPack(s));
    }
}
