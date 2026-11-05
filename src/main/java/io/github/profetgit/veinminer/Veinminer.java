package io.github.profetgit.veinminer;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entry. */
public final class Veinminer implements ModInitializer {
    public static final String ID = "veinminer";
    public static final Logger LOG = LoggerFactory.getLogger("Veinminer");

    @Override
    public void onInitialize() {
        LOG.info("Veinminer loaded");
    }
}
