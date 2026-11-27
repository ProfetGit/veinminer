package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.profetsettings.UiKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//?}

// What the ui scenes need from the game's input and window API, which differs by version. 26.3 is driven through the real
// MouseHandler (public there); older versions deliver the same events to the open screen.
final class UiInput {
    private UiInput() {}

    // a left click at GUI-scaled coordinates
    static void click(Minecraft mc, int guiX, int guiY) {
        //? if >=26.3 {
        var w = mc.getWindow();
        double px = guiX * (double) w.getScreenWidth() / w.getGuiScaledWidth(), py = guiY * (double) w.getScreenHeight() / w.getGuiScaledHeight();
        mc.mouseHandler.onMove(w.handle(), px, py, 0, 0);
        mc.mouseHandler.onButton(w.handle(), new MouseButtonInfo(UiKeys.LEFT_BUTTON, 0), 1);
        mc.mouseHandler.onButton(w.handle(), new MouseButtonInfo(UiKeys.LEFT_BUTTON, 0), 0);
        //?} else if >=1.21.9 {
        /*Screen screen = TestCompat.screen(mc);
        screen.mouseMoved(guiX, guiY);
        MouseButtonEvent e = new MouseButtonEvent(guiX, guiY, new MouseButtonInfo(UiKeys.LEFT_BUTTON, 0));
        screen.mouseClicked(e, false);
        screen.mouseReleased(e);
        *///?} else {
        /*Screen screen = TestCompat.screen(mc);
        screen.mouseMoved(guiX, guiY);
        screen.mouseClicked(guiX, guiY, UiKeys.LEFT_BUTTON);
        screen.mouseReleased(guiX, guiY, UiKeys.LEFT_BUTTON);
        *///?}
    }

    static void press(net.minecraft.client.gui.components.AbstractButton b) {
        //? if >=1.21.9 {
        b.onPress(new KeyEvent(257, 0, 0));
        //?} else {
        /*b.onPress();
        *///?}
    }

    static void escape(Screen screen) {
        //? if >=1.21.9 {
        screen.keyPressed(new KeyEvent(UiKeys.ESC, 0, 0));
        //?} else {
        /*screen.keyPressed(UiKeys.ESC, 0, 0);
        *///?}
    }

    static void resize(Minecraft mc) {
        //? if >=26.1 {
        mc.resizeGui();
        //?} else {
        /*mc.resizeDisplay();
        *///?}
    }
}
