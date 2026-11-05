package io.github.profetgit.veinminer.server;

import com.mojang.brigadier.CommandDispatcher;
import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Every server event Veinminer listens to, in one place and one order. Each loader's entry ({@code platform/<loader>/})
 * calls these from its own events; nothing else in {@code server/} names a loader.
 */
public final class Hooks {
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("veinminer", "tools"));

    private record Start(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, String group, ItemStack tool, int mark) {}

    private static final List<Start> PENDING = new ArrayList<>();
    private static int sweep;

    private Hooks() {}

    public static void serverStarted(MinecraftServer server) {
        Groups.invalidate();
        Groups.logReport();
        Migrate.start(server);
        Api.load(server);
        Conflicts.started();
    }

    /** Before the worlds save: a chain in flight lands its loot and removes its displays, the rest of its vein stays ore. */
    public static void serverStopping(MinecraftServer server) {
        PENDING.clear();
        Chain.flushAll();
        Fx.clear();
    }

    public static void serverStopped(MinecraftServer server) {
        PENDING.clear();
        Chain.reset();
        Conflicts.stopped();
        Lag.reset();
    }

    public static void dataReloaded(MinecraftServer server, boolean success) {
        if (success) Api.load(server);
    }

    /** NeoForge and Forge announce a reload before the new function library is in: see {@link Api#reloadSoon}. */
    public static void dataReloadedSoon() {
        Api.reloadSoon();
    }

    public static void join(MinecraftServer server, ServerPlayer player) {
        Migrate.join(server, player);
    }

    public static void tagsReloaded() {
        Groups.invalidate();
    }

    public static void leave(ServerPlayer player) {
        PENDING.removeIf(s -> s.player == player);
        Chain.flushFor(player);
    }

    public static void serverTickEnd(MinecraftServer server) {
        Lag.tick(server);
        Api.tick(server);
        Config.pollReload();
        if (server.tickRateManager().runsNormally()) Chain.tickAll();
        if (++sweep >= 100) {
            sweep = 0;
            Fx.sweep();
        }
        drain();
    }

    /**
     * After a player broke a block. The struck block is gone, but vanilla has not yet dropped its loot or worn the tool
     * (Fabric's AFTER fires inside {@code destroyBlock}), so the vein is only queued here and runs in {@link #drain}
     * once that break is finished. Gates are in PRD 5.1 order, cheapest first.
     */
    public static void blockBroken(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity be) {
        blockBroken(level, player, pos, state, be, -1);
    }

    /**
     * {@code mark} is the entity-id mark taken before the struck block's loot exists: the loader entries whose event fires
     * after the break (NeoForge and Forge, through a mixin at the return of {@code destroyBlock}) take it at the head and pass
     * it here; -1 takes it now, for Fabric's event that fires inside the break before the loot.
     */
    public static void blockBroken(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity be, int mark) {
        if (Vein.running()) return;
        String group = Groups.of(state.getBlock());
        if (group == null) return;
        if (Chain.claimed(level, pos)) return;
        if (player.getClass() != ServerPlayer.class || Platform.get().isFakePlayer(player)) return;
        if (player.gameMode() != GameType.SURVIVAL) return;
        if (!Players.enabled(player)) return;
        Config.Values cfg = Config.get();
        if (!cfg.groupOn(group)) return;
        if (cfg.requireSneak() && !player.isShiftKeyDown()) return;
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(TOOLS)) return;
        if (be != null || !player.hasCorrectToolForDrops(state)) return;
        PENDING.add(new Start(level, player, pos.immutable(), state, group, tool, mark >= 0 ? mark : Chain.mark(level)));
    }

    /** Runs the queued veins; the end of every server tick, and the self-test after its own break. */
    public static void drain() {
        if (PENDING.isEmpty()) return;
        List<Start> todo = new ArrayList<>(PENDING);
        PENDING.clear();
        for (Start s : todo) {
            if (s.player.isRemoved() || !s.player.isAlive() || s.player.level() != s.level || s.player.getMainHandItem() != s.tool || s.tool.isEmpty()) continue;
            try {
                if (Api.cancelled(s.level, s.player, s.pos, s.state, s.group)) continue;
                if (Config.get().animation()) Chain.start(s.level, s.player, s.pos, s.group, s.tool, s.mark);
                else Vein.run(s.level, s.player, s.pos, s.group, s.tool);
            } catch (RuntimeException e) {
                Veinminer.LOG.error("Veinminer: a vein failed, the rest stays ore", e);
            }
        }
    }

    public static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        VeinminerCommand.register(dispatcher);
    }
}
