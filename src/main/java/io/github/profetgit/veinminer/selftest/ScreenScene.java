package io.github.profetgit.veinminer.selftest;

import com.google.gson.JsonParser;
import io.github.profetgit.veinminer.client.config.VeinminerScreen;
import io.github.profetgit.veinminer.server.Config;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.MinecraftServer;

/**
 * {@code screen}: the settings screen opened from the title screen and again in a singleplayer world, stills of its top and
 * its groups ({@code frames/screen-top}, {@code frames/screen-groups}), a toggle and the slider changed through their
 * widgets, then the closed screen's save checked in the file and in the server's {@link Config}. Not part of {@code all};
 * select with {@code scene=screen}.
 */
final class ScreenScene {
    private static final int BLOCKS_STEP = 7;
    private static final int BLOCKS = 128;

    private static VeinminerScreen screen;

    private ScreenScene() {}

    static boolean wanted(String only) {
        return only.equals("all") || only.contains("screen");
    }

    /** Before the world is created: open and close the screen on the title screen. */
    static void title(Script s) {
        s.run(mc -> {
                screen = new VeinminerScreen(mc.gui.screen());
                mc.gui.setScreen(screen);
            })
            .waitTicks(10)
            .check("screen: opens from the title screen", mc -> mc.gui.screen() == screen && widget("Sneak to vein") != null ? null : "screen " + mc.gui.screen())
            .check("screen: dirt shows its default off before any world", ScreenScene::dirtOff)
            .run(mc -> screen.onClose())
            .waitTicks(5)
            .check("screen: closes back to the title screen", mc -> mc.gui.screen() instanceof TitleScreen ? null : "screen " + mc.gui.screen());
    }

    static void world(Script s) {
        s.server(server -> Config.save(Config.Values.DEFAULTS))
            .run(mc -> {
                screen = new VeinminerScreen(null);
                mc.gui.setScreen(screen);
            })
            .waitTicks(40)
            .check("screen: opens in the world with every row", ScreenScene::rows)
            .check("screen: dirt shows its default off in the world", ScreenScene::dirtOff)
            .check("screen: one options list after the server's groups arrived", mc -> {
                long lists = screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.OptionsList).count();
                return lists == 1 ? null : lists + " option lists";
            })
            .capture("screen-top", 1)
            .until("the top still written", 200, mc -> !SelfTest.capturing())
            .run(ScreenScene::scrollToGroups)
            .waitTicks(40)
            .capture("screen-groups", 1)
            .until("the groups still written", 200, mc -> !SelfTest.capturing())
            .run(ScreenScene::flip)
            .waitTicks(5)
            .check("screen: the rows show the new values", ScreenScene::shown)
            .run(mc -> screen.onClose())
            .waitTicks(10)
            .check("screen: closes into the world", mc -> mc.gui.screen() == null ? null : "screen " + mc.gui.screen())
            .server(ScreenScene::verify);
    }

    private static String rows(Minecraft mc) {
        if (mc.gui.screen() != screen) return "screen " + mc.gui.screen();
        for (String row : new String[] {"Sneak to vein", "Most blocks", "Corners count", "Tool wear", "Chain animation", "Loot lands: In front", "Messages",
            "Dirt and grass", "Sand", "Gravel", "Clay", "Coal ore", "Iron ore", "Copper ore", "Gold ore", "Redstone ore", "Lapis ore", "Diamond ore", "Emerald ore",
            "Nether quartz ore", "Ancient debris"}) {
            if (widget(row) == null) return "no row " + row;
        }
        return null;
    }

    private static String dirtOff(Minecraft mc) {
        AbstractWidget dirt = widget("Dirt and grass");
        if (dirt == null) return "no Dirt and grass row";
        String d = dirt.getMessage().getString();
        return d.contains("OFF") ? null : "dirt '" + d + "'";
    }

    private static void scrollToGroups(Minecraft mc) {
        List<ContainerEventHandler> children = entries();
        for (int i = 0; i < children.size(); i++) {
            for (GuiEventListener c : children.get(i).children()) {
                if (c instanceof AbstractWidget w && w.getMessage().getString().startsWith("Dirt and grass")) {
                    screen.options().setScrollAmount(Math.max(0, i - 1) * 25.0);
                    return;
                }
            }
        }
        throw new IllegalStateException("no Dirt and grass row to scroll to");
    }

    private static void flip(Minecraft mc) {
        AbstractWidget dirt = widget("Dirt and grass");
        AbstractWidget slider = widget("Most blocks");
        if (dirt == null || slider == null) throw new IllegalStateException("rows missing: dirt " + dirt + " slider " + slider);
        press(dirt);
        click(slider, slider.getX() + 4 + (slider.getWidth() - 8) * (double) BLOCKS_STEP / (VeinminerScreen.STEPS.length - 1));
    }

    private static String shown(Minecraft mc) {
        AbstractWidget dirt = widget("Dirt and grass");
        AbstractWidget slider = widget("Most blocks");
        if (dirt == null || slider == null) return "rows missing";
        String d = dirt.getMessage().getString(), b = slider.getMessage().getString();
        return d.contains("ON") && b.endsWith(": " + BLOCKS) ? null : "dirt '" + d + "' blocks '" + b + "'";
    }

    private static void verify(MinecraftServer server) {
        String text;
        Config.Values disk;
        try {
            text = Files.readString(Config.file());
            disk = Config.parse(JsonParser.parseString(text).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            Rig.check("screen: config file readable", false, e.toString());
            return;
        }
        Rig.check("screen: file has dirt on", text.contains("\"dirt\": true") && Boolean.TRUE.equals(disk.groups().get("dirt")), text.replaceAll("(?m)^\\s*//.*\\n", ""));
        Rig.eq("screen: file max_blocks", BLOCKS, disk.maxBlocks());
        Config.Values live = Config.get();
        Rig.check("screen: server Config agrees", live.equals(disk), "server " + live + " file " + disk);
        Rig.check("screen: the other settings stayed", disk.requireSneak() && disk.diagonal() && disk.durability() && disk.animation() && disk.feedback() && disk.drops() == 0,
            String.valueOf(disk));
    }

    private static List<ContainerEventHandler> entries() {
        return new ArrayList<ContainerEventHandler>(screen.options().children());
    }

    private static AbstractWidget widget(String prefix) {
        for (ContainerEventHandler entry : entries()) {
            for (GuiEventListener c : entry.children()) {
                if (c instanceof AbstractWidget w && w.getMessage().getString().startsWith(prefix)) return w;
            }
        }
        return null;
    }

    private static void press(AbstractWidget w) {
//? if >=1.21.9 {
        ((AbstractButton) w).onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0));
//?} else {
/*        ((AbstractButton) w).onPress();
*///?}
    }

    private static void click(AbstractWidget w, double x) {
//? if >=1.21.9 {
        w.onClick(new net.minecraft.client.input.MouseButtonEvent(x, w.getY() + w.getHeight() / 2.0, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
//?} else {
/*        w.onClick(x, w.getY() + w.getHeight() / 2.0);
*///?}
    }
}
