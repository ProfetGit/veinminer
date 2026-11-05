package io.github.profetgit.veinminer.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code config/veinminer.json} (PRD 5.7). Re-read when the file's modification time changes (polled every 40 server
 * ticks); a file that does not parse keeps the last good values. A commented default file is written on the first run
 * only, so an operator's edits are never rewritten.
 */
public final class Config {
    public record Values(int maxBlocks, boolean requireSneak, int drops, boolean durability, boolean diagonal, boolean animation,
                         boolean feedback, int visitCap, Map<String, Boolean> groups) {
        public static final Values DEFAULTS = new Values(64, true, 0, true, true, true, true, 4096, Map.of());

        public boolean groupOn(String group) {
            return groups.getOrDefault(group, true);
        }

        public Values maxBlocks(int v) {
            return new Values(v, requireSneak, drops, durability, diagonal, animation, feedback, visitCap, groups);
        }

        public Values requireSneak(boolean v) {
            return new Values(maxBlocks, v, drops, durability, diagonal, animation, feedback, visitCap, groups);
        }

        public Values durability(boolean v) {
            return new Values(maxBlocks, requireSneak, drops, v, diagonal, animation, feedback, visitCap, groups);
        }

        public Values diagonal(boolean v) {
            return new Values(maxBlocks, requireSneak, drops, durability, v, animation, feedback, visitCap, groups);
        }

        public Values feedback(boolean v) {
            return new Values(maxBlocks, requireSneak, drops, durability, diagonal, animation, v, visitCap, groups);
        }

        public Values visitCap(int v) {
            return new Values(maxBlocks, requireSneak, drops, durability, diagonal, animation, feedback, v, groups);
        }

        public Values drops(int v) {
            return new Values(maxBlocks, requireSneak, v, durability, diagonal, animation, feedback, visitCap, groups);
        }

        public Values animation(boolean v) {
            return new Values(maxBlocks, requireSneak, drops, durability, diagonal, v, feedback, visitCap, groups);
        }

        public Values group(String name, boolean on) {
            Map<String, Boolean> g = new HashMap<>(groups);
            g.put(name, on);
            return new Values(maxBlocks, requireSneak, drops, durability, diagonal, animation, feedback, visitCap, Map.copyOf(g));
        }
    }

    private static final String TEMPLATE = """
        {
          // Most blocks one vein mines, the struck block included (1..1024)
          "max_blocks": 64,
          // true: only veins while sneaking
          "require_sneak": true,
          // 0: loot lands in front of the player's feet; 1: at the player's feet
          "drops": 0,
          // true: every extra block costs pickaxe durability (Unbreaking applies); a vein never breaks the pickaxe
          "durability": true,
          // true: veins also run through the 20 diagonal neighbours of a block
          "diagonal": true,
          // true: the blocks pop one after another in a chain; false: the whole vein breaks at once
          "animation": true,
          // true: action bar message when a vein ends
          "feedback": true,
          // Most cells one scan looks at (64..65536)
          "visit_cap": 4096,
          // Switch one group off with "tin": false; groups missing here are on
          "groups": {}
        }
        """;

    private static volatile Values values = Values.DEFAULTS;
    private static long stamp = -1;
    private static boolean broken;
    private static int poll;

    private Config() {}

    public static Path file() {
        return Platform.get().configDir().resolve("veinminer.json");
    }

    public static Values get() {
        return values;
    }

    /** Tests: use these values until the next {@link #load}. */
    public static void use(Values v) {
        values = v;
    }

    public static boolean broken() {
        return broken;
    }

    public static void load() {
        Path file = file();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, TEMPLATE);
            }
            JsonElement e = JsonParser.parseString(Files.readString(file));
            if (!e.isJsonObject()) throw new IllegalStateException("not a JSON object");
            values = parse(e.getAsJsonObject());
            stamp = Files.getLastModifiedTime(file).toMillis();
            broken = false;
        } catch (IOException | RuntimeException e) {
            Veinminer.LOG.warn("Veinminer: config unreadable, keeping the current values ({})", e.toString());
            try {
                stamp = Files.getLastModifiedTime(file).toMillis();
            } catch (IOException ignored) {
            }
            broken = true;
        }
    }

    private static Values parse(JsonObject o) {
        Values d = Values.DEFAULTS;
        Map<String, Boolean> groups = new TreeMap<>();
        if (o.has("groups") && o.get("groups").isJsonObject()) {
            for (var en : o.getAsJsonObject("groups").entrySet()) {
                try {
                    groups.put(en.getKey(), en.getValue().getAsBoolean());
                } catch (RuntimeException ex) {
                    Veinminer.LOG.warn("Veinminer: config groups.{} is not true or false, ignored", en.getKey());
                }
            }
        }
        return new Values(
            number(o, "max_blocks", d.maxBlocks(), 1, 1024),
            bool(o, "require_sneak", d.requireSneak()),
            number(o, "drops", d.drops(), 0, 1),
            bool(o, "durability", d.durability()),
            bool(o, "diagonal", d.diagonal()),
            bool(o, "animation", d.animation()),
            bool(o, "feedback", d.feedback()),
            number(o, "visit_cap", d.visitCap(), 64, 65536),
            Map.copyOf(groups));
    }

    private static int number(JsonObject o, String key, int def, int min, int max) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        try {
            return Math.clamp(e.getAsInt(), min, max);
        } catch (RuntimeException ex) {
            Veinminer.LOG.warn("Veinminer: config {} is not a number, using {}", key, def);
            return def;
        }
    }

    private static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o.get(key);
        if (e == null) return def;
        try {
            return e.getAsBoolean();
        } catch (RuntimeException ex) {
            Veinminer.LOG.warn("Veinminer: config {} is not true or false, using {}", key, def);
            return def;
        }
    }

    /** Every 40 server ticks: reload when the file's modification time changed. */
    static void pollReload() {
        if (++poll < 40) return;
        poll = 0;
        try {
            Path file = file();
            if (Files.exists(file) && Files.getLastModifiedTime(file).toMillis() != stamp) load();
        } catch (IOException ignored) {
        }
    }
}
