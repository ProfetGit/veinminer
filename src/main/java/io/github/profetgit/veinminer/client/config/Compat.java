package io.github.profetgit.veinminer.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

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
}
