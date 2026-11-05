package io.github.profetgit.veinminer.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Add-on hooks for mods (data packs use the {@code #veinminer:api/*} function tags). Loader-neutral on purpose. */
public final class VeinminerEvents {
    private VeinminerEvents() {}

    /** Listeners run in registration order, on the server thread. */
    public static final class Registry<T> {
        private final List<T> listeners = new CopyOnWriteArrayList<>();

        public void register(T listener) {
            listeners.add(listener);
        }

        public void unregister(T listener) {
            listeners.remove(listener);
        }

        public List<T> listeners() {
            return listeners;
        }
    }

    /**
     * Asked once per vein that passed Veinminer's own gates (toggle, game mode, sneak, tool, group), before the vein is
     * scanned. Return true to veto: the struck block stays broken and nothing else happens. The first listener that
     * vetoes decides; they run before the {@code #veinminer:api/cancel} function tag.
     */
    @FunctionalInterface
    public interface BeforeVein {
        boolean veto(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, String group);
    }

    /** Told after a vein ended (the whole vein for {@code animation} off, the end of the chain otherwise). */
    @FunctionalInterface
    public interface AfterVein {
        void after(ServerLevel level, ServerPlayer player, BlockPos pos, String group, int mined);
    }

    public static final Registry<BeforeVein> BEFORE_VEIN = new Registry<>();
    public static final Registry<AfterVein> AFTER_VEIN = new Registry<>();
}
