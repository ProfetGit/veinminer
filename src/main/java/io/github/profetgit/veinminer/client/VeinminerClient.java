package io.github.profetgit.veinminer.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entry. */
public final class VeinminerClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("Veinminer");

    @Override
    public void onInitializeClient() {
        LOG.info("Veinminer client loaded");
    }
}
