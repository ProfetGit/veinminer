package io.github.profetgit.veinminer.server;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** The per-player vein toggle. Held in memory only until M4 adds the saved opt-out data. */
public final class Players {
    private static final Set<UUID> OFF = new HashSet<>();

    private Players() {}

    public static boolean enabled(ServerPlayer player) {
        return OFF.isEmpty() || !OFF.contains(player.getUUID());
    }

    /** Flips the player's toggle; returns the new state. */
    public static boolean toggle(ServerPlayer player) {
        if (!OFF.remove(player.getUUID())) {
            OFF.add(player.getUUID());
            return false;
        }
        return true;
    }
}
