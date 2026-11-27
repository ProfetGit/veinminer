package io.github.profetgit.veinminer.client.config;

import io.github.profetgit.profetsettings.ModDef;
import io.github.profetgit.profetsettings.ProfetSettings;
import io.github.profetgit.profetsettings.Setting;
import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.client.VeinminerClient;
import io.github.profetgit.veinminer.client.preview.Previews;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.GroupData;
import io.github.profetgit.veinminer.server.Groups;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;

// Veinminer's options for the shared Profet screen, bound to config/veinminer.json through Config.Values (immutable, so
// every row reads and writes the one pending value). The old VeinminerScreen stays in the tree, unused.
public final class VeinminerSettings {
    private static final int[] STEPS = VeinminerScreen.STEPS;
    private static final List<String> EARTH = List.of("dirt", "sand", "gravel", "clay");
    private static final List<String> ORES = List.of("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald", "quartz", "netherite_scrap");

    private static Config.Values pending = Config.Values.DEFAULTS;
    private static List<String> liveGroups = List.of();

    private VeinminerSettings() {}

    private static String t(String key) {
        return I18n.get("veinminer." + key);
    }

    /** A server other than the player's own world: its settings apply, nothing here edits anything. */
    public static boolean remote() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.getSingleplayerServer() == null;
    }

    private static void set(Config.Values next) {
        if (!remote()) pending = next;
    }

    private static void save(Config.Values next) {
        Minecraft mc = Minecraft.getInstance();
        Runnable job = () -> {
            String error = Config.save(next);
            if (error != null) VeinminerClient.LOG.warn("Veinminer: settings not saved ({})", error);
        };
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) server.execute(job);
        else job.run();
    }

    private static void openPicker(Screen parent) {
        if (remote()) return;
        Minecraft mc = Minecraft.getInstance();
        Compat.setScreen(mc, new BlockPickerScreen(parent, pending.customBlocks(), entries -> {
            Map<String, Boolean> groups = new java.util.HashMap<>(pending.groups());
            for (String gone : pending.customBlocks()) if (!entries.contains(gone)) groups.remove(gone);
            Config.Values c = pending.customBlocks(entries);
            pending = new Config.Values(c.maxBlocks(), c.requireSneak(), c.drops(), c.durability(), c.diagonal(), c.animation(), c.feedback(), c.visitCap(), Map.copyOf(groups), c.customBlocks());
            save(pending);
            Compat.setScreen(mc, parent);
        }));
    }

    private static int step(int blocks) {
        int best = 0;
        for (int i = 1; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - blocks) < Math.abs(STEPS[best] - blocks)) best = i;
        }
        return best;
    }

    private static Setting bool(String key, boolean def, java.util.function.Function<Config.Values, Boolean> get, java.util.function.BiFunction<Config.Values, Boolean, Config.Values> with) {
        return Setting.bool(key, t("setting." + key), t("setting." + key + ".desc"), () -> get.apply(pending), v -> set(with.apply(pending, v)), def);
    }

    // Ancient debris is group "ancient_debris" where #c:ores/ancient_debris exists (Groups.build) and "netherite_scrap"
    // where it doesn't: one row switches both names.
    private static final String DEBRIS = "netherite_scrap", DEBRIS_TAG = "ancient_debris";

    private static Setting group(String name, String label, String desc, String preview) {
        boolean def = GroupData.defaultOn(name);
        if (name.equals(DEBRIS)) {
            return Setting.bool(name, label, desc, () -> pending.groupOn(DEBRIS) && pending.groupOn(DEBRIS_TAG),
                v -> set(pending.group(DEBRIS, v).group(DEBRIS_TAG, v)), def).preview(preview);
        }
        return Setting.bool(name, label, desc, () -> pending.groupOn(name), v -> set(pending.group(name, v)), def).preview(preview);
    }

    private static List<String> liveNames() {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return liveGroups;
        try {
            liveGroups = server.submit(() -> new ArrayList<>(Groups.sizes().keySet())).get(300, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Veinminer.LOG.debug("Veinminer: using the last known group list ({})", e.toString());
        }
        return liveGroups;
    }

    private static List<String> oreNames(Set<String> customSeen) {
        Set<String> extra = new TreeSet<>(pending.groups().keySet());
        extra.addAll(liveNames());
        extra.removeAll(ORES);
        extra.removeAll(EARTH);
        extra.remove(DEBRIS_TAG);
        extra.removeAll(customSeen);
        extra.removeIf(n -> n.startsWith("#") || customSeen.contains("minecraft:" + n));
        List<String> all = new ArrayList<>(ORES);
        all.addAll(extra);
        return all;
    }

    public static ModDef create() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null) Config.load();
        if (!ProfetSettings.isScreen(Compat.screen(mc))) pending = Config.get();
        Config.Values d = Config.Values.DEFAULTS;
        ModDef m = new ModDef(Veinminer.ID, "Veinminer", "Veinminer", ProfetSettings.modVersion(Veinminer.ID), t("tagline"), "veinminer:icon.png")
            .issues("ProfetGit/veinminer");
        m.locked(() -> remote() ? t("note.server") : null);
        m.onChange = () -> {
            if (!remote()) save(pending);
        };
        m.snapshot = () -> {
            Config.Values original = pending;
            return () -> {
                if (remote()) return;
                pending = original;
                save(original);
            };
        };

        m.group(t("group.veining"),
            bool("require_sneak", d.requireSneak(), Config.Values::requireSneak, Config.Values::requireSneak).preview("sneak"),
            Setting.range("max_blocks", t("setting.max_blocks"), t("setting.max_blocks.desc"), 0, STEPS.length - 1,
                () -> step(pending.maxBlocks()), i -> set(pending.maxBlocks(STEPS[i])), step(d.maxBlocks()))
                .format(i -> I18n.get("veinminer.setting.max_blocks.value", STEPS[i])).preview("size"),
            bool("diagonal", d.diagonal(), Config.Values::diagonal, Config.Values::diagonal).preview("diagonal"),
            bool("durability", d.durability(), Config.Values::durability, Config.Values::durability).preview("wear"),
            bool("animation", d.animation(), Config.Values::animation, Config.Values::animation).preview("chain"),
            Setting.choice("drops", t("setting.drops"), t("setting.drops.desc"), new String[] {t("drops.front"), t("drops.feet")},
                () -> pending.drops() == 1 ? 1 : 0, i -> set(pending.drops(i)), d.drops()).preview("loot"),
            bool("feedback", d.feedback(), Config.Values::feedback, Config.Values::feedback).preview("messages"));

        List<String> groupNames = new ArrayList<>();
        List<Setting> earth = new ArrayList<>();
        for (String g : EARTH) {
            String lower = VeinminerScreen.label(g).toLowerCase(Locale.ROOT);
            earth.add(group(g, VeinminerScreen.label(g), I18n.get("veinminer.desc.earth", lower), Previews.group(g)));
            groupNames.add(g);
        }
        m.group(t("group.earth"), earth.toArray(new Setting[0]));

        Set<String> customSeen = new java.util.HashSet<>(pending.customBlocks());
        List<Setting> custom = new ArrayList<>();
        custom.add(Setting.action("pick_blocks", t("setting.pick_blocks"), t("setting.pick_blocks.desc"), t("setting.pick_blocks.button"), VeinminerSettings::openPicker));
        for (String entry : pending.customBlocks()) {
            String label = BlockIndex.label(entry);
            String desc = entry.startsWith("#") ? t("desc.custom.tag") : I18n.get("veinminer.desc.custom.block", label.toLowerCase(Locale.ROOT));
            custom.add(group(entry, label, desc, Previews.group(entry)));
            groupNames.add(entry);
        }
        m.group(t("group.custom"), custom.toArray(new Setting[0]));

        List<Setting> ores = new ArrayList<>();
        for (String g : oreNames(customSeen)) {
            String lower = VeinminerScreen.label(g).toLowerCase(Locale.ROOT);
            ores.add(group(g, VeinminerScreen.label(g), I18n.get("veinminer.desc.ore", lower), Previews.group(g)));
            groupNames.add(g);
        }
        m.group(t("group.ores"), ores.toArray(new Setting[0]));

        Previews.register(m, groupNames);
        m.diagnostics(() -> "Groups listed: " + groupNames.size());
        m.preset(t("preset.default"));
        m.preset(t("preset.small"), "max_blocks", step(16));
        m.preset(t("preset.everything"), "dirt", true, "sand", true, "gravel", true, "clay", true, "max_blocks", step(128));
        m.preset(t("preset.instant"), "animation", false);
        Previews.registerPresets(m);
        return m;
    }
}
