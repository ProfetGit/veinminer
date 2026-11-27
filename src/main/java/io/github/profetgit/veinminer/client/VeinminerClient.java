package io.github.profetgit.veinminer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-side logger; the loaders' client entries live in {@code platform/<loader>/}. */
public final class VeinminerClient {
    public static final Logger LOG = LoggerFactory.getLogger("Veinminer");

    private VeinminerClient() {}

    /** Client init on every loader: puts Veinminer into the shared Profet settings screen. */
    public static void register() {
        io.github.profetgit.profetsettings.ProfetSettings.register(io.github.profetgit.veinminer.Veinminer.ID, io.github.profetgit.veinminer.client.config.VeinminerSettings::create);
    }
}
