package io.github.profetgit.veinminer.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.selftest.BreakGuard;
import io.github.profetgit.veinminer.selftest.TestBlocks;
import io.github.profetgit.veinminer.server.GroupData;
import io.github.profetgit.veinminer.server.Hooks;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

// NeoForge: its events into Hooks; the vein itself starts from ServerPlayerGameModeMixin (no after-break event here)
@Mod(Veinminer.ID)
public final class NeoForgeEntry implements Platform {
    public NeoForgeEntry(IEventBus modBus) {
        Veinminer.init(this);
        modBus.addListener((RegisterEvent e) -> {
            if (e.getRegistryKey().equals(Registries.BLOCK) && TestBlocks.wanted()) TestBlocks.register();
        });
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener((AddServerReloadListenersEvent e) -> e.addListener(GroupData.ID, GroupData.listener()));
        bus.addListener((ServerStartedEvent e) -> Hooks.serverStarted(e.getServer()));
        bus.addListener((ServerStoppingEvent e) -> Hooks.serverStopping(e.getServer()));
        bus.addListener((ServerStoppedEvent e) -> Hooks.serverStopped(e.getServer()));
        // a null player is the sync after /reload (a join syncs to that player only); it comes before the new functions
        bus.addListener((OnDatapackSyncEvent e) -> {
            if (e.getPlayer() == null) Hooks.dataReloadedSoon();
        });
        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) Hooks.join(p.level().getServer(), p);
        });
        if (BreakGuard.wanted()) {
            bus.addListener((BreakBlockEvent e) -> {
                if (e.getLevel() instanceof ServerLevel l && !BreakGuard.allows(l, e.getPos())) e.setCanceled(true);
            });
        }
        bus.addListener((ServerTickEvent.Post e) -> Hooks.serverTickEnd(e.getServer()));
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) Hooks.leave(p);
        });
        bus.addListener((TagsUpdatedEvent e) -> Hooks.tagsReloaded());
        bus.addListener((RegisterCommandsEvent e) -> Hooks.commands(e.getDispatcher()));
    }

    @Override
    public String loader() {
        return "neoforge";
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public String modVersion(String modId) {
        return ModList.get().getModContainerById(modId).map(m -> m.getModInfo().getVersion().toString()).orElse(null);
    }

    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    @Override
    public ServerPlayer testFakePlayer(ServerLevel level) {
        return FakePlayerFactory.getMinecraft(level);
    }

    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, player)).isCanceled();
    }
}
*///?}
