package io.github.profetgit.veinminer;

import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.server.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry: each loader's entry ({@code platform/<loader>/}) calls {@link #init} first, then wires its events. */
public final class Veinminer {
    public static final String ID = "veinminer";
    public static final Logger LOG = LoggerFactory.getLogger("Veinminer");
    private static Platform platform;

    private Veinminer() {}

    public static Platform platform() {
        return platform;
    }

    public static void init(Platform p) {
        platform = p;
        Config.load();
        LOG.info("Veinminer loaded ({})", p.loader());
    }
}
