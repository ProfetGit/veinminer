package io.github.profetgit.veinminer.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.profetgit.veinminer.Veinminer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
//? if <1.21.5 {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
*///?}
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21.5
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The per-player vein toggle (PRD 8): the UUIDs that turned veining off, saved data {@code veinminer_players} in the
 * overworld's storage ({@code data/veinminer/players.dat}). It survives an uninstall as a tiny orphan file.
 */
public final class Players extends SavedData {
    private static final Codec<Players> CODEC = RecordCodecBuilder.create(i -> i.group(
        UUIDUtil.CODEC.listOf().optionalFieldOf("off", List.of()).forGetter(d -> List.copyOf(d.off))
    ).apply(i, Players::new));
//? if >=1.21.5 {
    private static final SavedDataType<Players> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Veinminer.ID, "players"),
        () -> new Players(List.of()), CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
//?} else {
/*    private static final SavedData.Factory<Players> TYPE = new SavedData.Factory<>(() -> new Players(List.of()),
        (tag, lookup) -> SavedCodec.load(CODEC, tag, lookup, () -> new Players(List.of())), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        return SavedCodec.save(CODEC, this, tag, lookup);
    }
*///?}

    private final Set<UUID> off = new HashSet<>();

    private Players(List<UUID> off) {
        this.off.addAll(off);
    }

//? if >=1.21.5 {
    /** The saved-data type, for tests that open the file with a second storage. */
    public static SavedDataType<Players> type() {
        return TYPE;
    }

    private static Players get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
//?} else {
/*    private static Players get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE, "veinminer_players");
    }
*///?}

    public boolean contains(UUID id) {
        return off.contains(id);
    }

    public static boolean enabled(ServerPlayer player) {
        return enabled(player.level().getServer(), player.getUUID());
    }

    public static boolean enabled(MinecraftServer server, UUID id) {
        return !get(server).off.contains(id);
    }

    public static void setEnabled(MinecraftServer server, UUID id, boolean enabled) {
        Players data = get(server);
        if (enabled ? data.off.remove(id) : data.off.add(id)) data.setDirty();
    }

    /** Flips the player's toggle; returns the new state. */
    public static boolean toggle(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        boolean on = !enabled(server, player.getUUID());
        setEnabled(server, player.getUUID(), on);
        return on;
    }
}
