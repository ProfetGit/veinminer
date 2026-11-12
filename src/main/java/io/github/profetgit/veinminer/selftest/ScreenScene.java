package io.github.profetgit.veinminer.selftest;

import com.google.gson.JsonParser;
import io.github.profetgit.veinminer.client.config.BlockPickerScreen;
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

    static boolean pickerWanted(String only) {
        return only.equals("all") || only.contains("picker");
    }

    private static BlockPickerScreen picker() {
        return Minecraft.getInstance().gui.screen() instanceof BlockPickerScreen p ? p : null;
    }

    private static void openPicker(Minecraft mc) {
        AbstractWidget open = widget("Add or remove blocks...");
        if (open == null) {
            for (GuiEventListener c : screen.children()) {
                if (c instanceof AbstractButton b && b.getMessage().getString().startsWith("Add or remove blocks")) open = b;
            }
        }
        press(open);
    }

    private static void search(String text) {
        picker().search().setValue(text);
    }

    private static void addRow(String id) {
        AbstractWidget b = picker().button(id);
        if (b == null) throw new IllegalStateException("no row for " + id);
        press(b);
    }

    private static void still(Script s, String name) {
        s.capture(name, 1).until("the " + name + " still written", 200, mc -> !SelfTest.capturing());
    }

    /**
     * {@code picker}: the "Your blocks" picker driven through its widgets: search netherrack, Add, Done, the main screen's
     * Netherrack row, the saved file and the server's {@link Config}, then Remove and the file again. Stills
     * {@code picker-search}, {@code picker-blocked}, {@code picker-entries}, {@code picker-main}.
     */
    static void picker(Script s) {
        s.server(server -> Config.save(Config.Values.DEFAULTS))
            .run(mc -> {
                screen = new VeinminerScreen(null);
                mc.gui.setScreen(screen);
            })
            .waitTicks(40)
            .check("picker: no Netherrack row yet", mc -> widget("Netherrack") == null ? null : "row already there")
            .run(ScreenScene::openPicker)
            .waitTicks(10)
            .check("picker: the button opens the picker", mc -> picker() != null ? null : "screen " + mc.gui.screen())
            .check("picker: empty list has no rows", mc -> picker().entries().isEmpty() ? null : "entries " + picker().entries())
            .run(mc -> search("nether"))
            .waitTicks(5);
        still(s, "picker-search");
        s.run(mc -> search("chest"))
            .waitTicks(5)
            .check("picker: chests show greyed", mc -> {
                AbstractWidget b = picker().button("minecraft:chest");
                return b != null && !b.active ? null : "chest row " + b + (b == null ? "" : " active " + b.active);
            });
        still(s, "picker-blocked");
        s.run(mc -> search("netherrack"))
            .waitTicks(5)
            .check("picker: netherrack is offered", mc -> picker().button("minecraft:netherrack") != null ? null : "no row")
            .run(mc -> addRow("minecraft:netherrack"))
            .waitTicks(3)
            .check("picker: Add puts it in the list", mc -> picker().entries().equals(List.of("minecraft:netherrack")) ? null : "entries " + picker().entries())
            .check("picker: the row now says Added", mc -> {
                AbstractWidget b = picker().button("minecraft:netherrack");
                return b != null && b.getMessage().getString().equals("Added") ? null : "button " + b;
            })
            .run(mc -> search(""))
            .waitTicks(5)
            .check("picker: empty search lists the entry with Remove", mc -> {
                AbstractWidget b = picker().button("minecraft:netherrack");
                return b != null && b.getMessage().getString().equals("Remove") ? null : "button " + b;
            });
        still(s, "picker-entries");
        s.run(mc -> search("#logs"))
            .waitTicks(5)
            .check("picker: a tag in a world is offered", mc -> picker().button("#minecraft:logs") != null ? null : "no #minecraft:logs row")
            .run(mc -> search(""))
            .run(mc -> picker().onClose())
            .waitTicks(40)
            .check("picker: Done returns to the settings screen", mc -> mc.gui.screen() instanceof VeinminerScreen ? null : "screen " + mc.gui.screen())
            .run(mc -> screen = (VeinminerScreen) mc.gui.screen())
            .check("picker: the main screen has a Netherrack row", mc -> widget("Netherrack") != null ? null : "no row")
            .check("picker: still one options list", mc -> {
                long lists = screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.OptionsList).count();
                return lists == 1 ? null : lists + " option lists";
            })
            .run(mc -> scrollTo("Netherrack"))
            .waitTicks(5);
        still(s, "picker-main");
        s.run(mc -> screen.onClose())
            .waitTicks(20)
            .server(server -> verifyBlocks(List.of("minecraft:netherrack")))
            .run(mc -> {
                screen = new VeinminerScreen(null);
                mc.gui.setScreen(screen);
            })
            .waitTicks(40)
            .check("picker: reopened with the Netherrack row", mc -> widget("Netherrack") != null ? null : "no row")
            .run(ScreenScene::openPicker)
            .waitTicks(10)
            .check("picker: reopened with the entry listed", mc -> picker().button("minecraft:netherrack") != null ? null : "entries " + picker().entries())
            .run(mc -> addRow("minecraft:netherrack"))
            .waitTicks(3)
            .run(mc -> picker().onClose())
            .waitTicks(40)
            .run(mc -> screen = (VeinminerScreen) mc.gui.screen())
            .check("picker: the Netherrack row is gone", mc -> widget("Netherrack") == null ? null : "row still there")
            .run(mc -> screen.onClose())
            .waitTicks(20)
            .server(server -> verifyBlocks(List.of()));
    }

    private static void scrollTo(String prefix) {
        List<ContainerEventHandler> children = entries();
        for (int i = 0; i < children.size(); i++) {
            for (GuiEventListener c : children.get(i).children()) {
                if (c instanceof AbstractWidget w && w.getMessage().getString().startsWith(prefix)) {
                    screen.options().setScrollAmount(Math.max(0, i - 3) * 25.0);
                    return;
                }
            }
        }
        throw new IllegalStateException("no " + prefix + " row to scroll to");
    }

    private static void verifyBlocks(List<String> want) {
        Config.Values disk;
        String text;
        try {
            text = Files.readString(Config.file());
            disk = Config.parse(JsonParser.parseString(text).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            Rig.check("picker: config file readable", false, e.toString());
            return;
        }
        Rig.check("picker: file custom_blocks " + want, disk.customBlocks().equals(want), "file " + disk.customBlocks());
        Rig.check("picker: file has the custom_blocks key", text.contains("custom_blocks"), text);
        Rig.check("picker: server Config agrees " + want, Config.get().customBlocks().equals(want), "server " + Config.get().customBlocks());
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
