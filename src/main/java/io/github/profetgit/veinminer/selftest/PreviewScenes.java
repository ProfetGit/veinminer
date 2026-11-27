package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.profetsettings.ProfetSettingsScreen;
import io.github.profetgit.veinminer.client.config.ConfigScreens;
import io.github.profetgit.veinminer.server.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;

// Scene "preview" (only when named; run at 1920x1080): every live preview of the Veinminer screen at fixed moments of its
// loop, in every state of the options it reads. Frames: ui-pv-<kind>-<variant>-<ms>. Tokens in the scene list:
// preview:k=chain+sneak:step=250:len=3000:dense in one token limits the kinds, sets the frame distance and the loop length (ms), captures every 60 fps frame of the loop (ui-dense-<kind>-<variant>-<ms>).
final class PreviewScenes {
    private record Variant(String name, UnaryOperator<Config.Values> config) {}

    private record Kind(String kind, String key, float frac, List<Variant> variants) {}

    private PreviewScenes() {}

    static boolean wanted(String only) {
        return spec(only) != null;
    }

    // one comma-free token: preview[:kinds a+b][:step250][:len3000][:dense]
    private static String spec(String only) {
        for (String t : only.split(",")) if (t.equals("preview") || t.startsWith("preview:")) return t;
        return null;
    }

    private static String token(String only, String prefix) {
        for (String t : spec(only).split(":")) if (t.startsWith(prefix)) return t.substring(prefix.length());
        return null;
    }

    private static Variant on(String n) {
        return new Variant(n, v -> v);
    }

    private static List<Kind> kinds() {
        List<Kind> out = new ArrayList<>();
        out.add(new Kind("chain", "animation", -1f, List.of(on("on"), new Variant("off", v -> v.animation(false)))));
        out.add(new Kind("sneak", "require_sneak", -1f, List.of(on("on"), new Variant("off", v -> v.requireSneak(false)))));
        List<Variant> sizes = new ArrayList<>();
        for (int n : new int[] {8, 16, 24, 64}) sizes.add(new Variant("n" + n, v -> v.maxBlocks(n)));
        out.add(new Kind("size", "max_blocks", 0.5f, sizes));
        out.add(new Kind("diagonal", "diagonal", -1f, List.of(on("on"), new Variant("off", v -> v.diagonal(false)))));
        out.add(new Kind("wear", "durability", -1f, List.of(on("on"), new Variant("off", v -> v.durability(false)))));
        out.add(new Kind("loot", "drops", -1f, List.of(on("front"), new Variant("feet", v -> v.drops(1)), new Variant("noanim", v -> v.animation(false)), new Variant("noanim-feet", v -> v.animation(false).drops(1)))));
        out.add(new Kind("messages", "feedback", -1f, List.of(on("on"), new Variant("off", v -> v.feedback(false)))));
        for (String g : new String[] {"dirt", "sand", "gravel", "clay", "coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald", "quartz", "netherite_scrap"}) {
            out.add(new Kind("group:" + g, g, -1f, List.of(new Variant("on", v -> v.group(g, true)), new Variant("off", v -> v.group(g, false)))));
        }
        return out;
    }

    static void world(Script s) {
        String only = System.getProperty("veinminer.selftest.scene", "all");
        String limit = token(only, "k=");
        int step = Integer.parseInt(token(only, "step=") == null ? "250" : token(only, "step="));
        int len = Integer.parseInt(token(only, "len=") == null ? "4500" : token(only, "len="));
        boolean dense = spec(only).contains(":dense");
        s.server(srv -> Config.save(Config.Values.DEFAULTS))
            .run(mc -> {
                mc.options.guiScale().set(2);
                UiInput.resize(mc);
                TestCompat.setScreen(mc, ConfigScreens.create(null));
            })
            .waitTicks(30);
        for (Kind k : kinds()) {
            if (limit != null && !List.of(limit.split("\\+")).contains(k.kind().replace("group:", ""))) continue;
            for (Variant v : k.variants()) {
                String tag = k.kind().replace(':', '-') + "-" + v.name();
                s.server(srv -> Config.save(v.config().apply(Config.Values.DEFAULTS)))
                    .run(mc -> hub(mc).debugRevealRow(k.key()))
                    .waitTicks(4)
                    .run(mc -> {
                        int[] p = hub(mc).debugPoint(k.key(), k.frac());
                        hub(mc).debugHover(p[0], p[1]);
                        Preview.debugTime = 0;
                    })
                    .waitTicks(15)
                    .check("preview: " + k.key() + " shows " + k.kind(), mc -> k.kind().equals(hub(mc).debugPreviewKind()) ? null : "shows " + hub(mc).debugPreviewKind());
                if (dense) {
                    for (int i = 0; i < len * 60 / 1000; i++) {
                        int t = i * 1000 / 60;
                        s.run(mc -> {
                                Preview.debugTime = t;
                                SelfTest.startCapture(String.format("ui-dense-%s-%04d", tag, t), 1);
                            })
                            .until("dense frame written", 40, mc -> !SelfTest.capturing());
                    }
                } else {
                    for (int t = 0; t < len; t += step) {
                        int at = t;
                        s.run(mc -> Preview.debugTime = at).waitTicks(3).run(mc -> SelfTest.startCapture("ui-pv-" + tag + "-" + at, 1)).waitTicks(3);
                    }
                }
            }
        }
        if (limit == null || limit.contains("presets")) presets(s, 9000, 500, spec(only).contains(":trans"));
        s.run(mc -> {
                Preview.debugTime = -1;
                TestCompat.setScreen(mc, null);
            })
            .server(srv -> Config.save(Config.Values.DEFAULTS));
    }

    // each Quick setup button hovered, its preview at fixed moments (frames ui-pv-preset-<id>-<ms>)
    private static void presets(Script s, int len, int step, boolean trans) {
        String[] ids = {"default", "small", "everything", "instant"};
        s.server(srv -> Config.save(Config.Values.DEFAULTS)).run(mc -> hub(mc).debugScroll(0)).waitTicks(8);
        for (int i = 0; i < ids.length; i++) {
            int idx = i;
            s.run(mc -> {
                    int[] p = hub(mc).debugPresetPoint(idx);
                    hub(mc).debugHover(p[0], p[1]);
                    Preview.debugTime = 0;
                })
                .waitTicks(15)
                .check("preview: preset " + ids[i] + " shows its scene", mc -> ("preset." + ids[idx]).equals(hub(mc).debugPreviewKind()) ? null : "shows " + hub(mc).debugPreviewKind());
            if (trans) {
                if (!ids[i].equals("everything")) continue;
                for (int t = 2200; t < 7600; t += 50) {
                    if (t >= 4200 && t < 5200) continue;
                    int at = t;
                    s.run(mc -> Preview.debugTime = at).waitTicks(2).run(mc -> SelfTest.startCapture("ui-pv-preset-everything-" + at, 1)).waitTicks(3);
                }
                continue;
            }
            for (int t = 0; t < len; t += step) {
                int at = t;
                s.run(mc -> Preview.debugTime = at).waitTicks(3).run(mc -> SelfTest.startCapture("ui-pv-preset-" + ids[idx] + "-" + at, 1)).waitTicks(3);
            }
        }
    }

    private static ProfetSettingsScreen hub(Minecraft mc) {
        return (ProfetSettingsScreen) TestCompat.screen(mc);
    }
}
