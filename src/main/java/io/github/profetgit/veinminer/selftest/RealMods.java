package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Groups;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code real} (not in {@code all}; {@code --var scene=real}): the ores of the mods installed next to Veinminer (an addon
 * in {@code dev/modtest.json}), found by asking {@link Groups} for every block outside minecraft/veinminer. Each gets a vein of 6,
 * struck with a netherite pickaxe, and the result is reported: group name, blocks mined, items dropped. A modded ore the
 * netherite pickaxe cannot harvest is reported and not counted as a failure.
 */
final class RealMods {
    private RealMods() {}

    static void run(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Config.Values.DEFAULTS);
        List<Block> ores = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            String ns = BuiltInRegistries.BLOCK.getKey(b).getNamespace();
            if (ns.equals("minecraft") || ns.equals("veinminer")) continue;
            if (Groups.of(b) != null) ores.add(b);
        }
        SelfTest.record("real: modded ores in a Veinminer group", true, ores.size() + " blocks: " + ores.stream().map(b -> BuiltInRegistries.BLOCK.getKey(b) + "=" + Groups.of(b)).toList());
        Rig.check("real: at least one modded ore is in a group", !ores.isEmpty(), "none: the mod's ores are not tagged c:ores");
        for (Block ore : ores) {
            String id = BuiltInRegistries.BLOCK.getKey(ore).toString();
            Rig.clear();
            Rig.cube(0, 0, 0, Rig.SIZE, 1, Rig.SIZE, Blocks.STONE);
            Rig.player.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
            Rig.hold(new ItemStack(Items.NETHERITE_PICKAXE));
            BlockPos origin = Rig.at(4, 2, 5);
            List<BlockPos> cells = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                BlockPos p = Rig.at(4 + (i % 3), 2 + i / 3, 5);
                Rig.set(p, ore.defaultBlockState());
                cells.add(p);
            }
            boolean harvest = Rig.player.hasCorrectToolForDrops(ore.defaultBlockState());
            Rig.strike(origin, true);
            Rig.settle();
            int air = Rig.air(cells);
            Rig.Loot loot = Rig.grab();
            String detail = id + ": group " + Groups.of(ore) + ", mined " + air + " of 6, items " + loot.items() + ", xp " + loot.xp() + ", chains left " + Chain.count();
            if (!harvest) SelfTest.record("real: " + id + " needs a better tool than netherite (not counted)", true, detail);
            else Rig.check("real: " + id + " veins completely", air == 6, detail);
            if (!harvest) ownTool(ore, id);
        }
        Rig.clear();
    }

    /** The mod's own pickaxe of that ore (<ns>:<name>_pickaxe), when it has one: is it a vein tool, and does it vein the ore? */
    private static void ownTool(Block ore, String id) {
        var key = BuiltInRegistries.BLOCK.getKey(ore);
        String path = key.getPath().replace("_ore", "") + "_pickaxe";
        var item = BuiltInRegistries.ITEM.get(new net.minecraft.resources.ResourceLocation(key.getNamespace(), path));
        if (item == Items.AIR) {
            SelfTest.record("real: " + id + " has no pickaxe named " + key.getNamespace() + ":" + path, true, "skipped");
            return;
        }
        Rig.clear();
        Rig.cube(0, 0, 0, Rig.SIZE, 1, Rig.SIZE, Blocks.STONE);
        Rig.player.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
        ItemStack tool = new ItemStack(item);
        Rig.hold(tool);
        List<BlockPos> cells = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            BlockPos p = Rig.at(4 + (i % 3), 2 + i / 3, 5);
            Rig.set(p, ore.defaultBlockState());
            cells.add(p);
        }
        boolean harvest = Rig.player.hasCorrectToolForDrops(ore.defaultBlockState());
        boolean veinTool = tool.is(io.github.profetgit.veinminer.server.Hooks.TOOLS);
        Rig.strike(Rig.at(4, 2, 5), true);
        Rig.settle();
        int air = Rig.air(cells);
        Rig.Loot loot = Rig.grab();
        SelfTest.record("real: " + id + " with " + key.getNamespace() + ":" + path + ": harvests " + harvest + ", in #veinminer:tools " + veinTool + ", mined " + air + " of 6", true,
            "items " + loot.items() + " xp " + loot.xp());
    }
}
