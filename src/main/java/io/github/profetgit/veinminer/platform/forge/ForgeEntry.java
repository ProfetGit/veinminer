package io.github.profetgit.veinminer.platform.forge;

//? forge {
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
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

// Forge: its events into Hooks; the vein itself starts from ServerPlayerGameModeMixin (no after-break event here)
@Mod(Veinminer.ID)
public final class ForgeEntry implements Platform {
    public ForgeEntry(FMLJavaModLoadingContext context) {
        Veinminer.init(this);
        // Forge's registries are locked outside their own events and take entries through the event's helper
        RegisterEvent.getBus(context.getModBusGroup()).addListener(e -> {
            if (!TestBlocks.wanted()) return;
            e.register(Registries.BLOCK, h -> TestBlocks.registerBlocks((key, block) -> h.register(key.identifier(), block)));
            e.register(Registries.ITEM, h -> TestBlocks.registerItems((key, item) -> h.register(key.identifier(), item)));
            e.register(Registries.BLOCK_ENTITY_TYPE, h -> TestBlocks.registerEntityTypes((key, type) -> h.register(key.identifier(), type)));
        });
        AddReloadListenerEvent.BUS.addListener(e -> e.addListener(GroupData.listener()));
        ServerStartedEvent.BUS.addListener(e -> Hooks.serverStarted(e.getServer()));
        ServerStoppingEvent.BUS.addListener(e -> Hooks.serverStopping(e.getServer()));
        ServerStoppedEvent.BUS.addListener(e -> Hooks.serverStopped(e.getServer()));
        // a null player is the sync after /reload (a join syncs to that player only); it comes before the new functions
        OnDatapackSyncEvent.BUS.addListener(e -> {
            if (e.getPlayer() == null) Hooks.dataReloadedSoon();
        });
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer p) Hooks.join(p.level().getServer(), p);
        });
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> Hooks.serverTickEnd(e.server()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer p) Hooks.leave(p);
        });
        // the claims stand-in of the self-test: a listener that returns true cancels the break
        if (BreakGuard.wanted()) {
            BlockEvent.BreakEvent.BUS.addListener(e -> {
                if (!(e.getLevel() instanceof ServerLevel l) || BreakGuard.allows(l, e.getPos())) return false;
                e.setResult(Result.DENY);
                return true;
            });
        }
        TagsUpdatedEvent.BUS.addListener(e -> Hooks.tagsReloaded());
        RegisterCommandsEvent.BUS.addListener(e -> Hooks.commands(e.getDispatcher()));
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
        return ModList.isLoaded(modId);
    }

    @Override
    public String modVersion(String modId) {
        return ModList.getModContainerById(modId).map(m -> m.getModInfo().getVersion().toString()).orElse(null);
    }

    // Forge 26.x has no FakePlayer class; the exact-class check in the caller covers vanilla's own fakes
    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return false;
    }

    // Forge's own hook only reads the result; a listener may also cancel
    @Override
    public boolean mayBreak(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        BlockEvent.BreakEvent e = new BlockEvent.BreakEvent(level, pos, state, player, Result.DEFAULT);
        return !(BlockEvent.BreakEvent.BUS.post(e) | e.getResult().isDenied());
    }
}
*///?}
