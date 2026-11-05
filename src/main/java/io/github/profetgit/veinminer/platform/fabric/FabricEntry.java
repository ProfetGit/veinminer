package io.github.profetgit.veinminer.platform.fabric;

//? fabric {
import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.selftest.BreakGuard;
import io.github.profetgit.veinminer.selftest.TestBlocks;
import io.github.profetgit.veinminer.server.GroupData;
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
//? if >=1.21.11 {
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
//?} else {
/*import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
*///?}
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric and Quilt (Quilt loads the Fabric jar): Fabric API events into {@link Hooks}. */
public final class FabricEntry implements ModInitializer, Platform {
    @Override
    public void onInitialize() {
        Veinminer.init(this);
        if (TestBlocks.wanted()) TestBlocks.register();
//? if >=1.21.11 {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(GroupData.ID, GroupData.listener());
//?} else if >=1.21.2 {
/*        PreparableReloadListener groups = GroupData.listener();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return GroupData.ID;
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
                return groups.reload(barrier, manager, backgroundExecutor, gameExecutor);
            }
        });
*///?} else {
/*        PreparableReloadListener groups = GroupData.listener();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return GroupData.ID;
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller prepare,
                                                  ProfilerFiller apply, Executor backgroundExecutor, Executor gameExecutor) {
                return groups.reload(barrier, manager, prepare, apply, backgroundExecutor, gameExecutor);
            }
        });
*///?}
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
