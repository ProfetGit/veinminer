package io.github.profetgit.veinminer.platform.fabric;

//? fabric {
import io.github.profetgit.veinminer.client.VeinminerClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric and Quilt client entry. */
public final class FabricClientEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        VeinminerClient.LOG.info("Veinminer client loaded");
    }
}
//?}
