package io.github.profetgit.veinminer.platform.forge;

import io.github.profetgit.veinminer.Veinminer;
import io.github.profetgit.veinminer.platform.Platform;
import io.github.profetgit.veinminer.selftest.TestBlocks;
import io.github.profetgit.veinminer.server.Hooks;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

// Forge 52 (1.21.1): its events into Hooks; the vein itself starts from the forge ForgeChopMixin (no after-break event here)
@Mod(Veinminer.ID)
public final class ForgeEntry implements Platform {
    public ForgeEntry() {
        Veinminer.init(this);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterEvent e) -> {
            if (e.getRegistryKey().equals(Registries.BLOCK) && TestBlocks.wanted()) TestBlocks.register();
        });
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener((ServerStartedEvent e) -> Hooks.serverStarted(e.getServer()));
        bus.addListener((ServerStoppedEvent e) -> Hooks.serverStopped(e.getServer()));
        bus.addListener((TickEvent.ServerTickEvent.Post e) -> Hooks.serverTickEnd(e.getServer()));
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) Hooks.leave(p);
        });
        bus.addListener((TagsUpdatedEvent e) -> Hooks.tagsReloaded());
        bus.addListener((RegisterCommandsEvent e) -> Hooks.commands(e.getDispatcher()));
    }

    @Override
    public String loader() {
        return "forge";
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

    // Forge has no FakePlayer class; the exact-class check in the caller covers vanilla's own fakes
    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return false;
    }

    // Forge's own hook checks cancellation; post returns true when a listener cancelled
    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        return !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player));
    }
}
