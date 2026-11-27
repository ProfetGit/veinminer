package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Api;
import io.github.profetgit.veinminer.server.Config;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;

//? if >=26.1 {
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.ScoreHolder;

/** Scene {@code enchanted}: the real Enchanted Veinminer data pack on top of this mod (dynamic registry, so the world is reopened with the pack in). */
final class EnchantedAddon {
    private static String worldId;
    private static MinecraftServer old;

    private EnchantedAddon() {}

    static void build(Script s) {
        s.server(EnchantedAddon::install)
            .run(EnchantedAddon::close)
            .until("the world to close", 2400, mc -> mc.getSingleplayerServer() == null && mc.level == null && !old.isRunning())
            .waitTicks(100)
            .run(EnchantedAddon::open)
            .until("the world to reopen", 4800, EnchantedAddon::reopened)
            .waitTicks(100)
            .server(EnchantedAddon::check)
            .server(EnchantedAddon::gone);
    }

    private static void install(MinecraftServer s) {
        Path world = s.getWorldPath(LevelResource.DATAPACK_DIR);
        old = s;
        worldId = world.getParent().getFileName().toString();
        M4.copyTree(M4.workspace().resolve("packs/EnchantedVeinminer/pack"), world.resolve("enchanted_veinminer"));
        if (net.minecraft.SharedConstants.getCurrentVersion().name().startsWith("26.1")) {
            try {
                Path mc = world.resolve("enchanted_veinminer/pack.mcmeta");
                java.nio.file.Files.writeString(mc, java.nio.file.Files.readString(mc).replace("[107, 1]", "[101, 1]").replace("\"max_format\": 121", "\"max_format\": 101"));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static void close(Minecraft mc) {
        mc.disconnectFromWorld(Component.literal("self-test reopen"));
    }

    private static void open(Minecraft mc) {
        mc.createWorldOpenFlows().openWorld(worldId, () -> { });
    }

    private static boolean reopened(Minecraft mc) {
        if (TestCompat.screen(mc) instanceof net.minecraft.client.gui.screens.BackupConfirmScreen b) {
            try {
                var f = net.minecraft.client.gui.screens.BackupConfirmScreen.class.getDeclaredField("onProceed");
                f.setAccessible(true);
                ((net.minecraft.client.gui.screens.BackupConfirmScreen.Listener) f.get(b)).proceed(false, false);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        MinecraftServer fresh = mc.getSingleplayerServer();
        return fresh != null && fresh.isRunning() && mc.player != null && mc.level != null && TestCompat.screen(mc) == null && !fresh.getPlayerList().getPlayers().isEmpty();
    }

    private static int score(MinecraftServer s, String holder) {
        var o = s.getScoreboard().getObjective("enchanted_veinminer.data");
        if (o == null) return -1;
        var info = s.getScoreboard().getPlayerScoreInfo(ScoreHolder.forNameOnly(holder), o);
        return info == null ? 0 : info.value();
    }

    private static int requires(MinecraftServer s) {
        return s.getCommandStorage().get(Api.metaId()).get("requires") instanceof ListTag l ? l.size() : -1;
    }

    private static int mineBlock(net.minecraft.world.level.block.Block block, ItemStack tool) {
        Config.Values keep = Config.get();
        M4.arena();
        Config.use(keep);
        Rig.hold(tool);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, block);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.settle();
        int air = Rig.air(cells);
        Rig.grab();
        return air;
    }

    private static int mine(String name, ItemStack tool) {
        M4.arena();
        Rig.hold(tool);
        List<BlockPos> cells = Rig.cube(1, 1, 1, 3, 3, 3, Blocks.COAL_ORE);
        Rig.strike(Rig.at(2, 2, 2), true);
        Rig.settle();
        int air = Rig.air(cells);
        Rig.grab();
        return air;
    }

    private static void check(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        Rig.check("enchanted: pack enabled with the world", s.getPackRepository().getSelectedIds().contains("file/enchanted_veinminer"), s.getPackRepository().getSelectedIds().toString());
        Rig.eq("enchanted: #problem 0 with Veinminer present", 0, score(s, "#problem"));
        Rig.eq("enchanted: requirement registered with Veinminer once", 1, requires(s));
        var ench = s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("enchanted_veinminer", "veinminer")));
        Rig.check("enchanted: enchantment registered", ench.isPresent(), "missing");
        Rig.eq("enchanted: plain pickaxe breaks one block", 1, mine("plain", new ItemStack(Items.IRON_PICKAXE)));
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        ench.ifPresent(h -> tool.enchant(h, 1));
        Rig.eq("enchanted: enchanted pickaxe mines the whole vein", 27, mine("enchanted", tool));
        modded(s, ench.map(h -> {
            ItemStack t = new ItemStack(Items.IRON_PICKAXE);
            t.enchant(h, 1);
            return t;
        }).orElse(new ItemStack(Items.IRON_PICKAXE)), ench.get().value());
        M4.park();
        M4.ran(s, "datapack disable \"file/enchanted_veinminer\"");
        M4.ran(s, "reload");
    }

    private static void modded(MinecraftServer s, ItemStack tool, net.minecraft.world.item.enchantment.Enchantment ench) {
        io.github.profetgit.veinminer.server.Groups.invalidate();
        Rig.eq("modded tin (c:ores/tin), plain pickaxe: one block", 1, mineBlock(TestBlocks.tin, new ItemStack(Items.IRON_PICKAXE)));
        Rig.eq("modded tin (c:ores/tin), enchanted pickaxe: whole vein", 27, mineBlock(TestBlocks.tin, tool.copy()));
        Rig.eq("modded zinc (c:ores/zinc), enchanted pickaxe: whole vein", 27, mineBlock(TestBlocks.zinc, tool.copy()));
        Rig.eq("modded odd (only c:ores), enchanted pickaxe: whole vein", 27, mineBlock(TestBlocks.odd, tool.copy()));
        Config.use(Rig.PLAIN.customBlocks(List.of("minecraft:polished_andesite")));
        io.github.profetgit.veinminer.server.Groups.invalidate();
        Rig.eq("custom_blocks group, plain pickaxe: one block", 1, mineBlock(Blocks.POLISHED_ANDESITE, new ItemStack(Items.IRON_PICKAXE)));
        Rig.eq("custom_blocks group, enchanted pickaxe: whole vein", 27, mineBlock(Blocks.POLISHED_ANDESITE, tool.copy()));
        Config.use(Rig.PLAIN.group("dirt", true));
        io.github.profetgit.veinminer.server.Groups.invalidate();
        Rig.eq("dirt group on, enchanted pickaxe: one block (a pickaxe is no correct tool for dirt, Veinminer rule)", 1, mineBlock(Blocks.DIRT, tool.copy()));
        Rig.eq("dirt group on, plain shovel (cannot be enchanted): vein", 27, mineBlock(Blocks.DIRT, new ItemStack(Items.IRON_SHOVEL)));
        ItemStack forced = new ItemStack(Items.IRON_SHOVEL);
        forced.enchant(s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("enchanted_veinminer", "veinminer"))), 1);
        Rig.eq("dirt group on, shovel with the enchantment forced on (control): vein", 27, mineBlock(Blocks.DIRT, forced));
        Config.use(Rig.PLAIN);
        io.github.profetgit.veinminer.server.Groups.invalidate();
        StringBuilder unsupported = new StringBuilder();
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ItemStack st = new ItemStack(item);
            if (st.is(io.github.profetgit.veinminer.server.Hooks.TOOLS) && !ench.isSupportedItem(st)) unsupported.append(item).append(' ');
        }
        Rig.check("every item of #veinminer:tools can take the enchantment", unsupported.length() == 0, unsupported.toString());
        Rig.check("iron shovel / axe / hoe cannot take it", !ench.isSupportedItem(new ItemStack(Items.IRON_SHOVEL)) && !ench.isSupportedItem(new ItemStack(Items.IRON_AXE)), "");
    }

    private static void gone(MinecraftServer s) {
        Rig.begin(s);
        Config.use(Rig.PLAIN);
        Rig.eq("enchanted: disabling the add-on drops its requirement", 0, Math.max(0, requires(s)));
        Rig.hold(new ItemStack(Items.IRON_PICKAXE));
        M4.park();
        M4.deleteTree(s.getWorldPath(LevelResource.DATAPACK_DIR).resolve("enchanted_veinminer"));
    }
}
//?} else {
/*final class EnchantedAddon {
    private EnchantedAddon() {}

    static void build(Script s) {}
}
*///?}
