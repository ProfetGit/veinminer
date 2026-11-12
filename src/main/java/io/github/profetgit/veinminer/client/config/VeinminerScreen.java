package io.github.profetgit.veinminer.client.config;

import io.github.profetgit.veinminer.client.VeinminerClient;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
//? if >=1.21 {
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
//?} else {
/*import net.minecraft.client.gui.screens.OptionsSubScreen;
*///?}

/**
 * Veinminer's settings on one page (Mod Menu on Fabric, the mod list's Config button on NeoForge and Forge). Labels are
 * plain English: without Fabric API a Fabric mod's language files are not loaded. It edits {@code config/veinminer.json}
 * through {@link Config}; with an integrated server the save runs on the server thread. On a remote server an operator
 * uses {@code /veinminer set}; these are the single-player settings.
 */
public final class VeinminerScreen extends OptionsSubScreen {
    public static final int[] STEPS = {8, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024};
    private static final Map<String, String> EARTH = Map.of("dirt", "Dirt and grass", "sand", "Sand", "gravel", "Gravel", "clay", "Clay");
    private static final List<String> EARTH_ORDER = List.of("dirt", "sand", "gravel", "clay");
    private static final List<String> ORES = List.of("coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald", "quartz", "netherite_scrap");

    private Config.Values original;
    private Config.Values pending;
    private final Set<String> live = new TreeSet<>();

    public VeinminerScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.literal("Veinminer"));
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) Config.load();
        original = pending = Config.get();
        if (server != null) {
            try {
                server.submit(() -> new ArrayList<>(Groups.sizes().keySet())).thenAccept(names -> mc.execute(() -> {
                    live.addAll(names);
                    if (mc.gui.screen() == this) rebuildWidgets();
                }));
            } catch (RuntimeException e) {
                VeinminerClient.LOG.warn("Veinminer: could not ask the server for its groups ({})", e.toString());
            }
        }
    }

//? if <1.21 {
    /*private OptionsList list;

    @Override
    protected void init() {
        list = new OptionsList(minecraft, width, height, 32, height - 32, 25);
        addOptions();
        addWidget(list);
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.CommonComponents.GUI_DONE, b -> onClose())
            .bounds(width / 2 - 100, height - 27, 200, 20).build());
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        basicListRender(g, list, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
    }

*///?} else {
    @Override
//?}
    protected void addOptions() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() == null && mc.level != null) {
            Compat.header(list, "On a server, its operators set these with /veinminer set. These are your single-player settings.");
        }
        Compat.header(list, "Veining");
        List<OptionInstance<?>> veining = new ArrayList<>();
        veining.add(bool("Sneak to vein", "Veins only start while you sneak. Off: every ore you mine starts one.", pending.requireSneak(), v -> pending = pending.requireSneak(v)));
        veining.add(new OptionInstance<>("Most blocks", tip("The most blocks one vein mines, the one you struck included."),
            (caption, v) -> Component.literal("Most blocks: " + STEPS[v]), new OptionInstance.IntRange(0, STEPS.length - 1), step(pending.maxBlocks()),
            v -> pending = pending.maxBlocks(STEPS[v])));
        veining.add(bool("Corners count", "Veins also run through blocks that only touch at an edge or corner.", pending.diagonal(), v -> pending = pending.diagonal(v)));
        veining.add(bool("Tool wear", "Every extra block wears your tool. A vein never breaks it. Off: veins are free.", pending.durability(), v -> pending = pending.durability(v)));
        veining.add(bool("Chain animation", "The vein pops block by block. Off: it all breaks at once.", pending.animation(), v -> pending = pending.animation(v)));
        veining.add(new OptionInstance<>("Loot lands", tip("In front: the loot drops where the ore was. At your feet: all of it drops by you."),
            (caption, v) -> Component.literal(v ? "At your feet" : "In front"), OptionInstance.BOOLEAN_VALUES, pending.drops() == 1,
            v -> pending = pending.drops(v ? 1 : 0)));
        veining.add(bool("Messages", "A short message above the hotbar when a vein ends.", pending.feedback(), v -> pending = pending.feedback(v)));
        row(veining);

        Compat.header(list, "Dirt, sand, gravel, clay (any shovel)");
        List<OptionInstance<?>> earth = new ArrayList<>();
        for (String g : EARTH_ORDER) earth.add(group(g));
        row(earth);

        Compat.header(list, "Ores");
        List<OptionInstance<?>> ores = new ArrayList<>();
        for (String g : oreNames()) ores.add(group(g));
        row(ores);
    }

    private void row(List<OptionInstance<?>> options) {
        list.addSmall(options.toArray(new OptionInstance<?>[0]));
    }

    private OptionInstance<Boolean> group(String name) {
        String label = label(name);
        return bool(label, "Veins of " + label.toLowerCase(Locale.ROOT) + " mine together.", pending.groupOn(name), v -> pending = pending.group(name, v));
    }

    private List<String> oreNames() {
        Set<String> extra = new TreeSet<>(pending.groups().keySet());
        extra.addAll(live);
        extra.removeAll(ORES);
        extra.removeAll(EARTH.keySet());
        List<String> all = new ArrayList<>(ORES);
        all.addAll(extra);
        return all;
    }

    static String label(String group) {
        String known = EARTH.get(group);
        if (known != null) return known;
        return switch (group) {
            case "quartz" -> "Nether quartz ore";
            case "netherite_scrap" -> "Ancient debris";
            case "coal", "iron", "copper", "gold", "redstone", "lapis", "diamond", "emerald" -> pretty(group) + " ore";
            default -> {
                int colon = group.indexOf(':');
                yield colon < 0 ? pretty(group) : pretty(group.substring(colon + 1)) + " (" + group.substring(0, colon) + ")";
            }
        };
    }

    private static String pretty(String path) {
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static int step(int blocks) {
        int best = 0;
        for (int i = 1; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - blocks) < Math.abs(STEPS[best] - blocks)) best = i;
        }
        return best;
    }

    private static OptionInstance<Boolean> bool(String name, String tooltip, boolean value, Compat.Listener<Boolean> set) {
        return OptionInstance.createBoolean(name, tip(tooltip), value, set);
    }

    private static <T> OptionInstance.TooltipSupplier<T> tip(String text) {
        return OptionInstance.cachedConstantTooltip(Component.literal(text));
    }

    /** The option list, for the self-test. */
    public OptionsList options() {
        return list;
    }

    @Override
    public void removed() {
        if (!pending.equals(original)) {
            Config.Values next = pending;
            original = next;
            save(Minecraft.getInstance(), next);
        }
        super.removed();
    }

    private static void save(Minecraft mc, Config.Values next) {
        Runnable job = () -> {
            String error = Config.save(next);
            if (error != null) {
                VeinminerClient.LOG.warn("Veinminer: settings not saved ({})", error);
                mc.execute(() -> Compat.notice(mc, Component.literal("Veinminer: settings not saved"), Component.literal(error)));
            }
        };
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) server.execute(job);
        else job.run();
    }
}
