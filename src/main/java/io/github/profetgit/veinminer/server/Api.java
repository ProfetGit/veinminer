package io.github.profetgit.veinminer.server;

import io.github.profetgit.veinminer.api.VeinminerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.11 {
import net.minecraft.server.permissions.LevelBasedPermissionSet;
//?}
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * The add-on API of the 1.3.3 pack, kept for Enchanted Veinminer: {@code storage veinminer:meta} and
 * {@code #veinminer:api/loaded} on load and after every reload, {@code #veinminer:api/cancel} (and the Java
 * {@link VeinminerEvents#BEFORE_VEIN}) before each vein. A function of the cancel tag vetoes with {@code return 1}.
 * Nothing touches the function library when the tags are empty.
 */
public final class Api {
    static final Identifier META = Identifier.fromNamespaceAndPath("veinminer", "meta");
    static final Identifier CANCEL = Identifier.fromNamespaceAndPath("veinminer", "api/cancel");
    static final Identifier LOADED = Identifier.fromNamespaceAndPath("veinminer", "api/loaded");
    public static final String VERSION = "2.0.0";
    public static final int VERSION_ID = 20000;
    private static boolean reloadPending;
    private static volatile int loadedRuns, cancelRuns;

    private Api() {}

    public static Identifier metaId() {
        return META;
    }

    /** Times {@code #veinminer:api/loaded} ran since the JVM started (tests). */
    public static int loadedRuns() {
        return loadedRuns;
    }

    /** Times {@code #veinminer:api/cancel} ran since the JVM started (tests). */
    public static int cancelRuns() {
        return cancelRuns;
    }

    /** Sets the meta (the pack set only these keys, so other keys an add-on keeps there survive) and runs the loaded tag. */
    static void load(MinecraftServer server) {
        CompoundTag meta = server.getCommandStorage().get(META).copy();
        meta.putString("version", VERSION);
        meta.putInt("version_id", VERSION_ID);
        meta.put("requires", new ListTag());
        server.getCommandStorage().set(META, meta);
        if (server.getFunctions().getTag(LOADED).isEmpty()) return;
        loadedRuns++;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "function #" + LOADED);
    }

    /**
     * NeoForge and Forge announce a reload before the server swaps in the new function library, so
     * {@code #veinminer:api/loaded} would run the old functions: it runs at the end of that tick instead.
     */
    static void reloadSoon() {
        reloadPending = true;
    }

    static void tick(MinecraftServer server) {
        if (reloadPending) {
            reloadPending = false;
            load(server);
        }
    }

    /** True when a listener or a function of the cancel tag vetoes the vein. */
    static boolean cancelled(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, String group) {
        for (VeinminerEvents.BeforeVein v : VeinminerEvents.BEFORE_VEIN.listeners()) {
            if (v.veto(level, player, pos, state, group)) return true;
        }
//? if >=1.20.3 {
        MinecraftServer server = level.getServer();
        if (server.getFunctions().getTag(CANCEL).isEmpty()) return false;
        cancelRuns++;
        boolean[] cancelled = {false};
        CommandSourceStack source = player.createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(pos))
            .withRotation(new Vec2(player.getXRot(), player.getYRot()))
//? if >=1.21.11 {
            .withPermission(LevelBasedPermissionSet.GAMEMASTER)
//?} else {
/*            .withPermission(2)
*///?}
            .withSuppressedOutput().withCallback((ok, result) -> {
                if (ok) cancelled[0] = true;
            });
        server.getCommands().performPrefixedCommand(source, "execute if function #" + CANCEL + " run return 1");
//?} else {
/*        // 1.20.1 has neither /return nor /execute if function: a function of the cancel tag cannot veto here, only BEFORE_VEIN listeners
        boolean[] cancelled = {false};
*///?}
        return cancelled[0];
    }
}
