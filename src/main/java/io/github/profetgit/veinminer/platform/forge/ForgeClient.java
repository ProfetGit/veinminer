package io.github.profetgit.veinminer.platform.forge;

//? forge {
/*import io.github.profetgit.veinminer.client.config.VeinminerScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/^* Client only (ForgeEntry calls it on the client): the settings screen behind the Config button of Forge's mod list. *^/
final class ForgeClient {
    private ForgeClient() {
    }

    static void register(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new VeinminerScreen(parent)));
    }
}
*///?}
