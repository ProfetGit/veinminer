package io.github.profetgit.veinminer.platform.fabric;

//? fabric {
import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.selftest.BreakGuard;
import io.github.profetgit.veinminer.selftest.TestBlocks;
import io.github.profetgit.veinminer.server.Hooks;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric and Quilt (Quilt loads the Fabric jar): Fabric API events into {@link Hooks}. */
public final class FabricEntry implements ModInitializer, Platform {
    @Override
    public void onInitialize() {
        Veinminer.init(this);
        if (TestBlocks.wanted()) TestBlocks.register();
        ServerLifecycleEvents.SERVER_STARTED.register(Hooks::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(Hooks::serverStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(Hooks::serverStopped);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> Hooks.dataReloaded(server, success));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Hooks.join(server, handler.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(Hooks::serverTickEnd);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> Hooks.leave(handler.getPlayer()));
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> Hooks.tagsReloaded());
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) Hooks.blockBroken(sl, sp, pos, state, be);
        });
        if (BreakGuard.wanted()) PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> BreakGuard.allows(level, pos));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> Hooks.commands(dispatcher));
    }

    @Override
    public String loader() {
        return FabricLoader.getInstance().isModLoaded("quilt_loader") ? "quilt" : "fabric";
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse(null);
    }

    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    @Override
    public ServerPlayer testFakePlayer(ServerLevel level) {
        return FakePlayer.get(level);
    }

    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }
}
//?}
