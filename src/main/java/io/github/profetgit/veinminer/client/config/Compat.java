package io.github.profetgit.veinminer.client.config;

import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Vanilla API differences the settings screen meets between the 1.20.1, 1.21.x and 26.x targets. */
final class Compat {
    private Compat() {}

//? if >=1.21 {
    static final SystemToast.SystemToastId NOTICE = new SystemToast.SystemToastId(8_000L);
//?} else {
/*    static final SystemToast.SystemToastIds NOTICE = SystemToast.SystemToastIds.PERIODIC_NOTIFICATION;
*///?}

//? if >=26.2 {
    interface Listener<T> extends net.minecraft.client.OptionInstance.ValueUpdateListener<T> {}
//?} else {
/*    interface Listener<T> extends java.util.function.Consumer<T> {}
*///?}

    static void notice(Minecraft mc, Component title, Component message) {
//? if >=26.2 {
        SystemToast.add(mc.gui.toastManager(), NOTICE, title, message);
//?} else if >=1.21.2 {
/*        SystemToast.add(mc.getToastManager(), NOTICE, title, message);
*///?} else {
/*        SystemToast.add(mc.getToasts(), NOTICE, title, message);
*///?}
    }

    static void header(OptionsList list, String text) {
//? if >=1.21.11 {
        list.addHeader(Component.literal(text));
//?}
    }

    /** A button row in the option list; before 1.21 the list takes no plain widgets, so the 1.20.1 screen puts the button in its footer. */
    static void button(OptionsList list, AbstractWidget button) {
//? if >=1.21.11 {
        list.addBig(button);
//?} else if >=1.21 {
/*        list.addSmall(button, null);
*///?}
    }

    static void text(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
//? if >=26.2 {
        g.text(font, text, x, y, color);
//?} else {
/*        g.drawString(font, text, x, y, color);
*///?}
    }

    static void item(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
//? if >=26.2 {
        g.item(stack, x, y);
//?} else {
/*        g.renderItem(stack, x, y);
*///?}
    }

    static void tooltip(GuiGraphicsExtractor g, Font font, Component text, int x, int y) {
//? if >=26.2 {
        g.setTooltipForNextFrame(font, text, x, y);
//?} else {
/*        g.renderTooltip(font, text, x, y);
*///?}
    }

    /** 1.20.1 screens draw their own background; later versions draw it inside {@code Screen.render}. */
    static void background(Screen screen, GuiGraphicsExtractor g) {
//? if <1.21 {
/*        screen.renderBackground(g);
*///?}
    }

    static Stream<HolderSet.Named<Block>> blockTags() {
//? if >=26.2 {
        return BuiltInRegistries.BLOCK.getTags();
//?} else {
/*        return BuiltInRegistries.BLOCK.getTags().map(com.mojang.datafixers.util.Pair::getSecond);
*///?}
    }
}
