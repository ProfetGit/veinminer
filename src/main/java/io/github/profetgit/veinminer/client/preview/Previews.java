package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.profetsettings.ModDef;
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.veinminer.client.config.VeinminerScreen;
import io.github.profetgit.veinminer.server.Config;
import java.util.List;
import java.util.Map;

/** Veinminer's live preview scenes for the shared Profet screen. Kinds: sneak, size, diagonal, wear, chain, loot, messages, group:<name>. */
public final class Previews {
    private Previews() {}

    // the values a preset preview plays with, or null: the live config then
    static Config.Values override;

    static Config.Values config() {
        return override != null ? override : Config.get();
    }

    public static String group(String name) {
        return "group:" + name;
    }

    /** Registers every scene kind on {@code m}; {@code groups} are the group names listed on the screen (earth, ores, modded, custom entries). */
    public static void register(ModDef m, List<String> groups) {
        m.scene("chain", d -> new Scenes.Chain()).scene("sneak", d -> new Scenes.Sneak()).scene("size", d -> new Scenes.Size())
            .scene("diagonal", d -> new Scenes.Diagonal()).scene("wear", d -> new Scenes.Wear()).scene("loot", d -> new Scenes.Loot())
            .scene("messages", d -> new Scenes.Messages());
        for (String name : groups) m.scene(group(name), d -> new Scenes.Group(name));
    }

    /** The values of a Quick setup (keys of the screen's rows; max_blocks is an index into the steps) on top of the defaults. */
    public static Config.Values values(Map<String, Object> preset) {
        Config.Values v = Config.Values.DEFAULTS;
        for (Map.Entry<String, Object> e : preset.entrySet()) {
            Object x = e.getValue();
            switch (e.getKey()) {
                case "require_sneak" -> v = v.requireSneak((Boolean) x);
                case "max_blocks" -> v = v.maxBlocks(VeinminerScreen.STEPS[((Number) x).intValue()]);
                case "diagonal" -> v = v.diagonal((Boolean) x);
                case "durability" -> v = v.durability((Boolean) x);
                case "animation" -> v = v.animation((Boolean) x);
                case "drops" -> v = v.drops(((Number) x).intValue());
                case "feedback" -> v = v.feedback((Boolean) x);
                default -> v = v.group(e.getKey(), (Boolean) x);
            }
        }
        return v;
    }

    /** One live preview per Quick setup, in the order the presets were added: Default, Small veins, Everything, Instant. */
    public static void registerPresets(ModDef m) {
        String[] ids = {"default", "small", "everything", "instant"};
        for (int i = 0; i < ids.length && i < m.presets.size(); i++) {
            ModDef.Preset p = m.presets.get(i);
            Config.Values values = values(p.values());
            String kind = "preset." + ids[i];
            m.presetPreview(p.name(), kind, net.minecraft.client.resources.language.I18n.get("veinminer.preset." + ids[i] + ".note"));
            m.scene(kind, d -> switch (kind) {
                case "preset.small" -> new PresetScene(values, new Scenes.Size(), null);
                case "preset.everything" -> new PresetScene(values, new Scenes.Group("dirt"), new Scenes.Chain());
                case "preset.instant" -> new PresetScene(values, new Scenes.Chain(), null);
                default -> new PresetScene(values, new Scenes.Chain(), null);
            });
        }
    }
}
