package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Config;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code demo} (not in {@code all}): builds a showroom in the world the test creates (the world is then saved and copied to a
 * play client): a stone platform in the air, a wall with four veins (30 coal, 12 iron, an 80-coal blob against the cap, and
 * a modded vibranium vein when Immersive Ores is installed), pickaxes in the hotbar and the player standing in front.
 */
final class Demo {
    private Demo() {}

    static void build(MinecraftServer server) {
        Rig.begin(server);
        Config.use(Config.Values.DEFAULTS);
        Rig.player.setShiftKeyDown(false);
        BlockPos o = Rig.player.blockPosition().offset(-14, 16, -14);
        for (int x = 0; x < 30; x++) {
            for (int z = 0; z < 18; z++) {
                set(o.offset(x, 0, z), Blocks.STONE);
                for (int y = 1; y < 12; y++) set(o.offset(x, y, z), Blocks.AIR);
            }
        }
        for (int x = 0; x < 30; x++) {
            for (int y = 1; y < 9; y++) {
                for (int z = 0; z < 5; z++) set(o.offset(x, y, z), Blocks.STONE);
            }
        }
        Block vib = BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation("immersiveores", "vibranium_ore"));
        vein(o, 3.5, 4.5, Blocks.COAL_ORE, 30);
        vein(o, 10.5, 4.5, Blocks.IRON_ORE, 12);
        vein(o, 17.5, 4.5, Blocks.COAL_ORE, 80);
        if (vib != Blocks.AIR) vein(o, 25.5, 4.5, vib, 24);
        Rig.player.teleportTo(o.getX() + 10.5, o.getY() + 1, o.getZ() + 12.5);
        Rig.player.setYRot(180f);
        Rig.player.setXRot(5f);
        var inv = Rig.player.getInventory();
        inv.clearContent();
        inv.setItem(0, new ItemStack(Items.IRON_PICKAXE));
        inv.setItem(1, Rig.pick(0, 3));
        inv.setItem(2, Rig.pick(1, 0));
        inv.setItem(3, new ItemStack(Items.NETHERITE_PICKAXE));
        inv.setItem(4, new ItemStack(Items.TORCH, 64));
        inv.selected = 0;
        Rig.server.getCommands().performPrefixedCommand(Rig.player.createCommandSourceStack().withSuppressedOutput(),
            "setworldspawn " + (o.getX() + 10) + " " + (o.getY() + 1) + " " + (o.getZ() + 12));
        Rig.server.getCommands().performPrefixedCommand(Rig.player.createCommandSourceStack().withSuppressedOutput(), "time set noon");
        Rig.server.getCommands().performPrefixedCommand(Rig.player.createCommandSourceStack().withSuppressedOutput(), "gamerule doDaylightCycle false");
    }

    private static void set(BlockPos p, Block b) {
        Rig.level.setBlock(p, b.defaultBlockState(), 2 | 16);
    }

    /** The {@code n} wall cells nearest the centre (cx, cy), jittered so the outline is not a sphere. */
    private static void vein(BlockPos o, double cx, double cy, Block ore, int n) {
        List<BlockPos> all = new ArrayList<>();
        for (int x = 0; x < 30; x++) {
            for (int y = 1; y < 9; y++) {
                for (int z = 2; z <= 4; z++) all.add(o.offset(x, y, z));
            }
        }
        all.sort(Comparator.comparingDouble(p -> {
            int x = p.getX() - o.getX(), y = p.getY() - o.getY(), z = p.getZ() - o.getZ();
            double jitter = ((x * 73856093 ^ y * 19349663 ^ z * 83492791) & 255) / 255.0 * 0.9;
            double dz = (z - 3.5) * 1.6;
            return Math.sqrt((x + 0.5 - cx) * (x + 0.5 - cx) + (y - cy) * (y - cy) * 1.4 + dz * dz) + jitter;
        }));
        for (BlockPos p : all.subList(0, n)) set(p, ore);
    }
}
