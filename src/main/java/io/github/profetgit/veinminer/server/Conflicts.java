package io.github.profetgit.veinminer.server;

import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** PRD 9: other vein-mining mods. Both would break the same vein: one warning each at server start. */
public final class Conflicts {
    static final Map<String, String> KNOWN = new TreeMap<>(Map.of(
        "ftbultimine", "FTB Ultimine",
        "liteminer", "Liteminer",
        "orevein", "OreVein",
        "oreexcavation", "Ore Excavation",
        "veinmining", "Vein Mining"));
    private static List<String> present = List.of();
    private static volatile int warnings;

    private Conflicts() {}

    static void started() {
        use(KNOWN.keySet().stream().filter(Platform.get()::isModLoaded).toList());
    }

    static void stopped() {
        present = List.of();
    }

    /** Warnings logged since the JVM started (tests). */
    public static int warnings() {
        return warnings;
    }

    public static List<String> present() {
        return present;
    }

    /** The conflicting mod ids found (the self-test passes its own list); logs one warning each. */
    public static List<String> use(List<String> ids) {
        present = ids.stream().filter(KNOWN::containsKey).distinct().sorted().toList();
        for (String id : present) {
            warnings++;
            Veinminer.LOG.warn("Veinminer: {} is also installed and mines veins too. Expect double breaks; remove one of them.", KNOWN.get(id));
        }
        return present;
    }
}
