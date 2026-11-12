package io.github.profetgit.veinminer.platform.forge;

import io.github.profetgit.veinminer.client.config.VeinminerScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

// Client only (ForgeEntry calls it on the client): the settings screen behind the Config button of Forge's mod list
final class ForgeClient {
    private ForgeClient() {
    }

    static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new VeinminerScreen(parent)));
    }
}
