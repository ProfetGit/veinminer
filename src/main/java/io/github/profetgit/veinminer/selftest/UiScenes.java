package io.github.profetgit.veinminer.selftest;

import com.google.gson.JsonParser;
import io.github.profetgit.profetsettings.ModDef;
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.profetsettings.ProfetSettings;
import io.github.profetgit.profetsettings.ProfetSettingsScreen;
import io.github.profetgit.profetsettings.Setting;
import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.client.config.BlockPickerScreen;
import io.github.profetgit.veinminer.client.config.ConfigScreens;
import io.github.profetgit.veinminer.client.config.VeinminerSettings;
import io.github.profetgit.veinminer.server.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

// Scene "ui" (only when named; run at 1920x1080): Veinminer in the shared Profet settings screen, every option type
// through the real mouse with the config file read back, a preset, Cancel, the previews and the bug report modal.
final class UiScenes {
    private static final String SWITCHES = "require_sneak diagonal durability animation feedback";
    private static final List<String> KINDS = List.of("require_sneak:sneak", "max_blocks:size", "diagonal:diagonal", "durability:wear", "animation:chain", "drops:loot", "feedback:messages");
    private static Config.Values opening;
    private static String lastClick = "";

    private UiScenes() {}

    static boolean wanted(String only) {
        return List.of(only.split(",")).contains("ui");
    }

    /** Before the world exists: the config button's screen from the title screen. */
    static void title(Script s) {
        s.run(mc -> {
                Config.save(Config.Values.DEFAULTS);
                gui(mc, 2);
                TestCompat.setScreen(mc, ConfigScreens.create(TestCompat.screen(mc)));
            })
            .waitTicks(30)
            .check("ui: fonts, sprites, icon and language load", mc -> {
                for (String path : new String[] {"profet_settings:font/tiny5.ttf", "profet_settings:textures/gui/sprites/golem.png", "veinminer:icon.png"}) {
                    if (mc.getResourceManager().getResource(Identifier.tryParse(path)).isEmpty()) return "missing " + path;
                }
                for (String key : new String[] {"profet_settings.ui.done", "veinminer.setting.max_blocks", "veinminer.preset.everything"}) {
                    if (net.minecraft.client.resources.language.I18n.get(key).equals(key)) return "untranslated " + key;
                }
                return null;
            })
            .check("ui: the config button opens the hub on Veinminer", mc -> hubOpen(mc) && hub(mc).debugSelected().id.equals(Veinminer.ID) ? null : "screen " + TestCompat.screen(mc))
            .check("ui: the title screen shows the options, no remote note", mc -> {
                List<String> keys = keys(mc);
                String want = "require_sneak max_blocks diagonal durability animation drops feedback dirt sand gravel clay coal";
                if (!String.join(" ", keys).startsWith(want)) return "keys " + keys;
                return VeinminerSettings.remote() ? "remote on the title screen" : null;
            });
        capture(s, "ui-title");
        s.run(mc -> UiInput.escape(hub(mc))).waitTicks(10);
    }

    static void world(Script s) {
        List<java.net.URI> opened = new ArrayList<>();
        java.util.function.Consumer<java.net.URI>[] prevOpener = new java.util.function.Consumer[1];
        s.server(srv -> Config.save(Config.Values.DEFAULTS))
            .run(mc -> {
                gui(mc, 2);
                TestCompat.setScreen(mc, ConfigScreens.create(null));
            })
            .waitTicks(30)
            .check("ui: in a world the hub lists Veinminer, every row has a label and a description", mc -> {
                if (!hub(mc).debugSelected().id.equals(Veinminer.ID)) return "selected " + hub(mc).debugSelected().id;
                for (Setting o : hub(mc).debugSelected().all()) {
                    if (o.label.startsWith("veinminer.") || o.desc.startsWith("veinminer.")) return "untranslated " + o.key;
                }
                return keys(mc).containsAll(List.of("netherite_scrap", "emerald", "clay")) ? null : "keys " + keys(mc);
            })
            .check("ui: not remote in a singleplayer world", mc -> VeinminerSettings.remote() ? "remote" : null);
        capture(s, "ui-0-top");
        s.run(mc -> hub(mc).debugScroll(100000)).waitTicks(20);
        capture(s, "ui-1-end");
        s.run(mc -> hub(mc).debugScroll(0)).waitTicks(10);

        previews(s);
        presetPreviews(s);

        for (String key : SWITCHES.split(" ")) {
            boolean[] was = new boolean[1];
            s.run(mc -> was[0] = setting(mc, key).getBool());
            click(s, key, -1f);
            s.check("ui: the " + key + " switch changes the option and the file", mc -> setting(mc, key).getBool() != was[0] && disk().equals(Config.get()) && flag(disk(), key) != was[0]
                ? null : "value " + setting(mc, key).getBool() + " file " + disk());
            click(s, key, -1f);
            s.check("ui: the " + key + " switch changes it back", mc -> setting(mc, key).getBool() == was[0] && flag(disk(), key) == was[0] ? null : "stuck");
        }
        click(s, "max_blocks", 1f);
        s.check("ui: the slider reaches 1024 blocks and the file has it", mc -> setting(mc, "max_blocks").getInt() == 13 && disk().maxBlocks() == 1024 ? null : "file " + disk().maxBlocks());
        click(s, "max_blocks", 0f);
        s.check("ui: and 8 blocks", mc -> disk().maxBlocks() == 8 ? null : "file " + disk().maxBlocks());
        s.check("ui: the slider shows the block count", mc -> setting(mc, "max_blocks").format.apply(5).equals("64 blocks") ? null : setting(mc, "max_blocks").format.apply(5));
        s.run(mc -> press(mc, hub(mc).debugReset("max_blocks"))).waitTicks(15)
            .check("ui: the reset arrow restores 64 blocks", mc -> disk().maxBlocks() == 64 ? null : "file " + disk().maxBlocks());
        click(s, "drops", 1f);
        s.check("ui: the choice picks At your feet and the file has drops 1", mc -> disk().drops() == 1 ? null : "drops " + disk().drops());
        click(s, "drops", 0f);
        s.check("ui: and In front", mc -> disk().drops() == 0 ? null : "drops " + disk().drops());
        click(s, "dirt", -1f);
        s.check("ui: a group switch (dirt) turns on and is saved", mc -> Boolean.TRUE.equals(disk().groups().get("dirt")) && disk().groupOn("dirt") ? null : "groups " + disk().groups() + " " + lastClick + " ui " + setting(mc, "dirt").getBool() + " cfg " + Config.get() + " changed " + hub(mc).debugSelected().all().stream().filter(Setting::changed).map(o -> o.key).toList());
        click(s, "coal", -1f);
        s.check("ui: an ore switch (coal) turns off and is saved", mc -> !disk().groupOn("coal") ? null : "groups " + disk().groups());
        capture(s, "ui-2-changed");
        s.run(mc -> hub(mc).debugScroll(0)).waitTicks(6);

        preset(s, 2, "Everything", v -> v.groupOn("dirt") && v.groupOn("sand") && v.groupOn("gravel") && v.groupOn("clay") && v.maxBlocks() == 128);
        capture(s, "ui-3-preset-everything");
        preset(s, 3, "Instant", v -> !v.animation() && !v.groupOn("dirt") && v.maxBlocks() == 64);
        preset(s, 1, "Small veins", v -> v.maxBlocks() == 16 && v.animation() && v.drops() == 0);
        preset(s, 0, "Default", v -> v.maxBlocks() == 64 && v.animation() && v.drops() == 0 && !v.groupOn("dirt") && v.requireSneak());
        s.run(mc -> press(mc, hub(mc).debugDone())).waitTicks(10)
            .check("ui: Done closes the screen", mc -> TestCompat.screen(mc) == null ? null : "screen " + TestCompat.screen(mc));

        cancel(s);
        picker(s);
        locked(s);

        s.run(mc -> {
                gui(mc, 2);
                TestCompat.setScreen(mc, ConfigScreens.create(null));
                prevOpener[0] = ProfetSettings.uriOpener;
                ProfetSettings.reportEndpoint = "";
                ProfetSettings.uriOpener = opened::add;
                opened.clear();
            })
            .waitTicks(20)
            .run(mc -> press(mc, hub(mc).debugBugButton()))
            .waitTicks(20)
            .check("bug: Report bug opens the modal", mc -> hub(mc).debugBugModalOpen() ? null : "modal did not open")
            .run(mc -> hub(mc).debugSetBugDraft("Vein glitch", "It stops early.", "1. Sneak\n2. Mine", "Whole vein"))
            .waitTicks(5)
            .check("bug: the report names Veinminer and lists the settings", mc -> {
                String b = hub(mc).debugBugBody();
                return b.contains("Veinminer") && b.contains("It stops early.") && b.contains("## Settings") ? null : "body " + b.substring(0, Math.min(300, b.length()));
            });
        capture(s, "ui-4-bug");
        s.run(mc -> press(mc, hub(mc).debugBugSend()))
            .waitTicks(10)
            .check("bug: Open on GitHub (no endpoint) emits a ProfetGit/veinminer issue URL", mc -> {
                ProfetSettings.uriOpener = prevOpener[0];
                if (opened.size() != 1) return "opened=" + opened.size();
                String u = opened.get(0).toString();
                return u.startsWith("https://github.com/ProfetGit/veinminer/issues/new?") ? null : "url=" + u;
            })
            .run(mc -> {
                UiInput.escape(hub(mc));
                UiInput.escape(hub(mc));
                ProfetSettings.debugResetBugState();
            })
            .waitTicks(10)
            .server(srv -> Config.save(Config.Values.DEFAULTS))
            .run(mc -> TestCompat.setScreen(mc, null));
    }

    private static void previews(Script s) {
        for (String pair : KINDS) {
            String key = pair.substring(0, pair.indexOf(':')), kind = pair.substring(pair.indexOf(':') + 1);
            hoverCheck(s, key, kind, key.equals("max_blocks") ? 0.5f : -1f);
        }
        for (String g : new String[] {"dirt", "coal"}) hoverCheck(s, g, "group:" + g, -1f);
    }

    // every Quick setup button shows its own preview, titled with the preset's name, with a note on what it changes
    private static void presetPreviews(Script s) {
        String[] ids = {"default", "small", "everything", "instant"};
        String[] keys = {"default", "small", "everything", "instant"};
        s.run(mc -> {
                Preview.debugTime = 1900;
                hub(mc).debugScroll(0);
            })
            .waitTicks(8);
        for (int i = 0; i < ids.length; i++) {
            int idx = i;
            String name = net.minecraft.client.resources.language.I18n.get("veinminer.preset." + keys[i]);
            s.run(mc -> {
                    int[] p = hub(mc).debugPresetPoint(idx);
                    hub(mc).debugHover(p[0], p[1]);
                })
                .waitTicks(12)
                .check("ui: hovering Quick setup " + name + " shows its preview", mc -> ("preset." + ids[idx]).equals(hub(mc).debugPreviewKind()) ? null : "preview " + hub(mc).debugPreviewKind())
                .check("ui: the preview title is the preset name " + name, mc -> name.equals(hub(mc).debugPreviewTitle()) ? null : "title " + hub(mc).debugPreviewTitle())
                .check("ui: the preview note for " + name + " is written", mc -> {
                    String n = hub(mc).debugPreviewNote();
                    return n != null && !n.isBlank() ? null : "note '" + n + "'";
                });
            capture(s, "ui-pv-preset-" + ids[i]);
        }
        s.run(mc -> Preview.debugTime = -1);
    }

    // strict once the mod registers the kind (Previews.register); until then only the hover is exercised
    private static void hoverCheck(Script s, String key, String kind, float frac) {
        s.run(mc -> hub(mc).debugRevealRow(key))
            .waitTicks(4)
            .run(mc -> {
                int[] p = hub(mc).debugPoint(key, frac);
                hub(mc).debugHover(p[0], p[1]);
            })
            .waitTicks(15)
            .check("ui: hovering " + key + " shows the " + kind + " preview", mc -> {
                ModDef m = hub(mc).debugSelected();
                String shown = hub(mc).debugPreviewKind();
                if (!m.previews.containsKey(kind)) return null;
                return kind.equals(shown) ? null : "registered but pane shows " + shown;
            });
        capture(s, "ui-pv-" + kind.replace(':', '-'));
    }

    private static void preset(Script s, int index, String name, java.util.function.Predicate<Config.Values> want) {
        s.run(mc -> hub(mc).debugScroll(0)).waitTicks(6)
            .run(mc -> press(mc, hub(mc).debugPreset(index))).waitTicks(20)
            .check("ui: Quick setup " + name, mc -> want.test(disk()) && disk().equals(Config.get()) ? null : "file " + disk());
    }

    private static void cancel(Script s) {
        s.server(srv -> Config.save(Config.Values.DEFAULTS.maxBlocks(96).diagonal(false).group("dirt", true)))
            .run(mc -> {
                opening = Config.get();
                TestCompat.setScreen(mc, ConfigScreens.create(null));
            })
            .waitTicks(30);
        for (String key : SWITCHES.split(" ")) click(s, key, -1f);
        click(s, "max_blocks", 1f);
        click(s, "drops", 1f);
        click(s, "clay", -1f);
        s.check("ui: options changed before Cancel", mc -> !opening.equals(Config.get()) ? null : "nothing changed")
            .run(mc -> press(mc, hub(mc).debugCancel())).waitTicks(20)
            .check("ui: Cancel puts every option back", mc -> opening.equals(Config.get()) ? null : "config " + Config.get())
            .check("ui: Cancel writes the old values to the file", mc -> opening.equals(disk()) ? null : "file " + disk())
            .check("ui: Cancel closes the screen", mc -> TestCompat.screen(mc) == null ? null : "screen " + TestCompat.screen(mc))
            .server(srv -> Config.save(Config.Values.DEFAULTS));
    }

    private static BlockPickerScreen picker(Minecraft mc) {
        return TestCompat.screen(mc) instanceof BlockPickerScreen p ? p : null;
    }

    // "Your blocks": the action row opens the picker, Done adds the entry, the new row and the file have it, then back out
    private static void picker(Script s) {
        s.server(srv -> Config.save(Config.Values.DEFAULTS))
            .run(mc -> TestCompat.setScreen(mc, ConfigScreens.create(null)))
            .waitTicks(30)
            .check("ui: Your blocks has the picker action and no block rows yet", mc -> hub(mc).debugPoint("pick_blocks", -1f) != null && hub(mc).debugPoint("minecraft:netherrack", -1f) == null ? null : "rows");
        click(s, "pick_blocks", -1f);
        s.check("ui: the action opens the block picker", mc -> picker(mc) != null ? null : "screen " + TestCompat.screen(mc))
            .run(mc -> picker(mc).search().setValue("netherrack"))
            .waitTicks(5)
            .run(mc -> UiInput.press(picker(mc).button("minecraft:netherrack")))
            .waitTicks(3);
        capture(s, "ui-6-picker");
        s.run(mc -> picker(mc).onClose())
            .waitTicks(30)
            .check("ui: Done returns to the Profet screen", mc -> hubOpen(mc) ? null : "screen " + TestCompat.screen(mc))
            .check("ui: the new block has a row and the file lists it", mc -> hub(mc).debugSelected().find("minecraft:netherrack") != null
                && disk().customBlocks().equals(List.of("minecraft:netherrack")) ? null : "file " + disk().customBlocks() + " row " + hub(mc).debugSelected().find("minecraft:netherrack"));
        capture(s, "ui-7-picker-back");
        click(s, "pick_blocks", -1f);
        s.run(mc -> UiInput.press(picker(mc).button("minecraft:netherrack")))
            .waitTicks(3)
            .run(mc -> picker(mc).onClose())
            .waitTicks(30)
            .check("ui: removing it drops the row and the entry", mc -> hub(mc).debugSelected().find("minecraft:netherrack") == null && disk().customBlocks().isEmpty() ? null : "file " + disk().customBlocks())
            .run(mc -> TestCompat.setScreen(mc, null));
    }

    // the remote-server lock is the library's lockAll here: the rows must not edit
    private static void locked(Script s) {
        s.run(mc -> {
                ProfetSettings.debugLockAll("On a server, the server's own settings apply.");
                TestCompat.setScreen(mc, ConfigScreens.create(null));
            })
            .waitTicks(30)
            .check("ui: a locked screen shows the server note", mc -> hub(mc).debugHasLockNote() ? null : "no note");
        capture(s, "ui-5-locked");
        click(s, "require_sneak", -1f);
        s.check("ui: a locked screen does not edit", mc -> setting(mc, "require_sneak").getBool() && disk().requireSneak() ? null : "changed while locked")
            .run(mc -> {
                ProfetSettings.debugLockAll(null);
                UiInput.escape(hub(mc));
            })
            .waitTicks(10);
    }

    // ---- helpers ----

    private static boolean flag(Config.Values v, String key) {
        return switch (key) {
            case "require_sneak" -> v.requireSneak();
            case "diagonal" -> v.diagonal();
            case "durability" -> v.durability();
            case "animation" -> v.animation();
            case "feedback" -> v.feedback();
            default -> throw new IllegalArgumentException(key);
        };
    }

    private static Config.Values disk() {
        try {
            return Config.parse(JsonParser.parseString(Files.readString(Config.file())).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("config unreadable: " + e);
        }
    }

    private static List<String> keys(Minecraft mc) {
        List<String> keys = new ArrayList<>();
        for (Setting o : hub(mc).debugSelected().all()) keys.add(o.key);
        return keys;
    }

    private static void gui(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        UiInput.resize(mc);
    }

    private static void capture(Script s, String name) {
        s.run(mc -> SelfTest.startCapture(name, 1)).waitTicks(3);
    }

    private static boolean hubOpen(Minecraft mc) {
        return ProfetSettings.isScreen(TestCompat.screen(mc));
    }

    private static ProfetSettingsScreen hub(Minecraft mc) {
        return (ProfetSettingsScreen) TestCompat.screen(mc);
    }

    private static Setting setting(Minecraft mc, String key) {
        return hub(mc).debugSelected().find(key);
    }

    private static void click(Script s, String key, float frac) {
        s.run(mc -> hub(mc).debugRevealRow(key))
            .waitTicks(25)
            .run(mc -> {
                int[] p = hub(mc).debugPoint(key, frac);
                if (p == null) throw new IllegalStateException("no control for " + key);
                lastClick = key + "@" + p[0] + "," + p[1];
                UiInput.click(mc, p[0], p[1]);
            })
            .waitTicks(15);
    }

    private static void press(Minecraft mc, int[] p) {
        if (p == null) throw new IllegalStateException("nothing to press");
        hub(mc).debugPress(p[0], p[1]);
        hub(mc).debugRelease(p[0], p[1]);
    }
}
