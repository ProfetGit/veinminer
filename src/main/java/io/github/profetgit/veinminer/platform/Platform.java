package io.github.profetgit.veinminer.platform;

import io.github.profetgit.veinminer.Veinminer;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * What Veinminer needs from the mod loader. Each loader's entry class in {@code platform/<loader>/} implements it, hands
 * it to {@link Veinminer#init} and calls {@link io.github.profetgit.veinminer.server.Hooks} from its events.
 */
public interface Platform {
    static Platform get() {
        return Veinminer.platform();
    }

    String loader();

    Path configDir();

    boolean isModLoaded(String modId);

    /** The installed version of a mod, or null when it isn't loaded. */
    @Nullable String modVersion(String modId);

    /** The loader's own fake-player check (machines, turtles); the exact-class check is done by the caller. */
    boolean isFakePlayer(ServerPlayer player);

    /**
     * Asks the loader's break event (claims) about one block without breaking it. {@code ServerPlayerGameMode.destroyBlock}
     * already asks it, so the vein path does not call this; it is for callers that break by another route.
     */
    boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state);

    /** Tests: the loader's own fake player for this level (a Fabric {@code FakePlayer}), or null where there is none. */
    default @Nullable ServerPlayer testFakePlayer(ServerLevel level) {
        return null;
    }
}
