package io.github.profetgit.veinminer.client.config;

import io.github.profetgit.profetsettings.ProfetSettings;
import io.github.profetgit.veinminer.Veinminer;
import net.minecraft.client.gui.screens.Screen;

/** The one place that decides which settings screen a loader's config button opens. */
public final class ConfigScreens {
    private ConfigScreens() {}

    public static Screen create(Screen parent) {
        return ProfetSettings.screen(parent, Veinminer.ID);
    }
}
