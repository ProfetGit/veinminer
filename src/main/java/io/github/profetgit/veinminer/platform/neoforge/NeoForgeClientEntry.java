package io.github.profetgit.veinminer.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.client.config.VeinminerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/^* The client half: the settings screen behind the Config button of NeoForge's mod list. *^/
@Mod(value = Veinminer.ID, dist = Dist.CLIENT)
public final class NeoForgeClientEntry {
    public NeoForgeClientEntry(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new VeinminerScreen(parent));
    }
}
*///?}
