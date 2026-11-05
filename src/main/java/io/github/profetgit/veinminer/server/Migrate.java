package io.github.profetgit.veinminer.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.profetgit.veinminer.Veinminer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.ChatFormatting;
//? if <1.21.5 {
/*import net.minecraft.core.HolderLookup;
*///?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
//? if >=1.21.9
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.21.5
import net.minecraft.world.level.saveddata.SavedDataType;
//? if >=1.20.3
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

/**
 * PRD 5.8: a world played with the 1.3.x data pack. On the server start, before {@link Api} writes the meta, the pack's
 * {@code veinminer.config} scores go into {@code config/veinminer.json} (once) and its {@code veinminer.off} scores into
 * the opt-out data, then the pack's own {@code veinminer:uninstall} runs (it flushes a chain mid-flight through
 * {@code anim/flush} and removes the objectives and storages) and the pack is disabled if it is still selected, so it
 * does not install again on the next /reload. The zip is never deleted. A world that never had the pack skips all of it.
 */
public final class Migrate extends SavedData {
    /** Migrations that ran since the JVM started (tests). */
    public static volatile int runs;
    /** The last summary line (tests). */
    public static volatile String summary;

    private static final Identifier UNINSTALL = Identifier.fromNamespaceAndPath("veinminer", "uninstall");
    private static final Identifier PACK_UNINSTALL = Identifier.fromNamespaceAndPath("veinminer", "function/uninstall.mcfunction");
    private static final List<Identifier> STORAGES = List.of(id("groups"), id("menu"), id("op"), id("anim"));
    private static final Codec<Migrate> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.optionalFieldOf("notice", "").forGetter(d -> d.notice),
        Codec.BOOL.optionalFieldOf("imported", false).forGetter(d -> d.imported)
    ).apply(i, Migrate::new));
//? if >=1.21.5 {
    private static final SavedDataType<Migrate> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Veinminer.ID, "migrate"),
        () -> new Migrate("", false), CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
//?} else {
/*    private static final SavedData.Factory<Migrate> TYPE = new SavedData.Factory<>(() -> new Migrate("", false),
        (tag, lookup) -> SavedCodec.load(CODEC, tag, lookup, () -> new Migrate("", false)), DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        return SavedCodec.save(CODEC, this, tag, lookup);
    }
*///?}

    /** The id of the old pack we disabled, until an operator has been told. */
    private String notice;
    /** The old config was copied into config/veinminer.json: it never happens twice. */
    private boolean imported;

    private Migrate(String notice, boolean imported) {
        this.notice = notice;
        this.imported = imported;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("veinminer", path);
    }

    private static Migrate get(MinecraftServer server) {
//? if >=1.21.5 {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
//?} else {
/*        return server.overworld().getDataStorage().computeIfAbsent(TYPE, "veinminer_migrate");
*///?}
    }

    private static Migrate peek(MinecraftServer server) {
//? if >=1.21.5 {
        return server.overworld().getDataStorage().get(TYPE);
//?} else {
/*        return server.overworld().getDataStorage().get(TYPE, "veinminer_migrate");
*///?}
    }

    /** Whether this world still carries the 1.3.x pack: its function, its objectives or an old meta version. */
    static boolean found(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        CompoundTag meta = server.getCommandStorage().get(Api.META);
//? if >=1.21.5 {
        int versionId = meta.getIntOr("version_id", -1);
//?} else {
/*        int versionId = meta.contains("version_id", 99) ? meta.getInt("version_id") : -1;
*///?}
        return versionId >= 0 && versionId < Api.VERSION_ID || sb.getObjective("veinminer.config") != null || sb.getObjective("veinminer.off") != null
            || server.getFunctions().get(UNINSTALL).isPresent();
    }

    /** Runs at the server start before the meta is written; returns the summary line, or null when the world never had the pack. */
    public static String start(MinecraftServer server) {
        if (!found(server)) return null;
        Scoreboard sb = server.getScoreboard();
        Migrate state = get(server);
        int config = 0;
        Objective scores = sb.getObjective("veinminer.config");
        if (scores != null && !state.imported) {
            Config.Values v = Config.get();
            for (var e : scoreMap(sb, scores).entrySet()) {
                Config.Values next = importKey(v, e.getKey(), e.getValue());
                if (next != null) {
                    v = next;
                    config++;
                }
            }
            String err = Config.save(v);
            if (err != null) Veinminer.LOG.warn("Veinminer: could not write the imported 1.3.x config: {}", err);
            state.imported = true;
            state.setDirty();
        }
        int toggles = 0, unknown = 0;
        Objective off = sb.getObjective("veinminer.off");
        if (off != null) {
            for (var e : scoreMap(sb, off).entrySet()) {
                if (e.getValue() != 1) continue;
                UUID id = uuid(server, e.getKey());
                if (id == null) {
                    Veinminer.LOG.warn("Veinminer: 1.3.x toggle of unknown player {} not imported", e.getKey());
                    unknown++;
                    continue;
                }
                Players.setEnabled(server, id, false);
                toggles++;
            }
        }
        boolean uninstalled = server.getFunctions().get(UNINSTALL).isPresent();
        if (uninstalled) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "function " + UNINSTALL);
        }
        for (Objective o : new ArrayList<>(sb.getObjectives())) {
            if (o.getName().equals("veinminer") || o.getName().startsWith("veinminer.")) sb.removeObjective(o);
        }
        for (Identifier storage : STORAGES) server.getCommandStorage().set(storage, new CompoundTag());
        CompoundTag meta = server.getCommandStorage().get(Api.META);
        meta.remove("version");
        meta.remove("version_id");
        server.getCommandStorage().set(Api.META, meta);
        String pack = oldPack(server);
        runs++;
        summary = "Veinminer: migrated a 1.3.x world: " + config + " config keys, " + toggles + " player opt-outs (" + unknown + " unknown players), "
            + (uninstalled ? "uninstall function ran" : "no uninstall function") + ", pack " + (pack == null ? "was not enabled" : pack + " disabled");
        Veinminer.LOG.info(summary);
        if (pack != null) {
            state.notice = pack;
            state.setDirty();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "datapack disable \"" + pack.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
            for (ServerPlayer p : server.getPlayerList().getPlayers()) join(server, p);
        }
        return summary;
    }

    /** The 1.3.x score {@code #key} as a new config, or null for a key that has no equivalent. */
    static Config.Values importKey(Config.Values v, String owner, int score) {
        String key = owner.startsWith("#") ? owner.substring(1) : owner;
        try {
            if (key.startsWith("ore.")) return Config.with(v, "groups." + key.substring("ore.".length()), score == 1 ? "true" : "false");
            return switch (key) {
                case "max_blocks" -> Config.with(v, key, Integer.toString(Math.clamp(score, 1, 1024)));
                case "drops" -> Config.with(v, key, score == 1 ? "1" : "0");
                case "require_sneak", "durability", "diagonal", "animation", "feedback" -> Config.with(v, key, score == 1 ? "true" : "false");
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TreeMap<String, Integer> scoreMap(Scoreboard sb, Objective o) {
        TreeMap<String, Integer> out = new TreeMap<>();
//? if >=1.20.3 {
        for (PlayerScoreEntry e : sb.listPlayerScores(o)) out.put(e.owner(), e.value());
//?} else {
/*        for (net.minecraft.world.scores.Score e : sb.getPlayerScores(o)) out.put(e.getOwner(), e.getScore());
*///?}
        return out;
    }

    private static UUID uuid(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getUUID();
//? if >=1.21.9 {
        Optional<NameAndId> cached = server.services().nameToIdCache().get(name);
        return cached.map(NameAndId::id).orElse(null);
//?} else {
/*        return server.getProfileCache().get(name).map(com.mojang.authlib.GameProfile::getId).orElse(null);
*///?}
    }

    /** The id of a selected world data pack that is the 1.3.x Veinminer (it has {@code veinminer:uninstall}), or null. */
    private static String oldPack(MinecraftServer server) {
        for (Pack p : server.getPackRepository().getSelectedPacks()) {
            if (!p.getId().startsWith("file/")) continue;
            //? if >=26.3 {
            List<PackResources> opened = p.open().toList();
            //?} else {
            /*List<PackResources> opened = List.of(p.open());
            *///?}
            try {
                for (PackResources r : opened) if (r.getResource(PackType.SERVER_DATA, PACK_UNINSTALL) != null) return p.getId();
            } finally {
                for (PackResources r : opened) r.close();
            }
        }
        return null;
    }

    /** Tells an operator once that the old pack was replaced. */
    static void join(MinecraftServer server, ServerPlayer player) {
        Migrate state = peek(server);
        if (state == null || state.notice.isEmpty()) return;
//? if >=1.21.9 {
        if (!server.getPlayerList().isOp(player.nameAndId())) return;
//?} else {
/*        if (!server.getPlayerList().isOp(player.getGameProfile())) return;
*///?}
        Say.chat(player, Component.literal("Veinminer 2.0 replaced the Veinminer 1.3 data pack ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("(" + state.notice + "): its settings and player toggles were carried over and the pack was disabled. "
                + "You can delete the pack from the world's datapacks folder.").withStyle(ChatFormatting.GRAY)));
        state.notice = "";
        state.setDirty();
    }
}
