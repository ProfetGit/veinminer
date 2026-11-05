package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Fx;
import io.github.profetgit.veinminer.server.Tune;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.phys.Vec3;

/**
 * {@code chainview}: the chain animation in front of a first-person camera, one frame per client frame in
 * {@code frames/<subject>/}, for the contact sheet and the critic. Not part of {@code all}; select with {@code scene=chainview}
 * (or {@code chainview,cave} for one subject).
 */
final class ChainView {
    static final String[] SUBJECTS = {"small", "cave", "modded"};
    private static final int VEIN = 30;

    private static Vec3 home;
    private static volatile boolean started;
    private static List<BlockPos> cells;
    private static Block ore;
    private static Item drop;
    private static Vec3 eye;
    private static float yaw, pitch;

    private ChainView() {}

    static void build(MinecraftServer server, String subject) {
        Rig.begin(server);
        if (home == null) home = Rig.player.position();
        Config.use(Config.Values.DEFAULTS);
        Tune.reset();
        started = false;
        command("time set noon");
        command("weather clear");
        command("gamerule advance_time false");
        command("gamerule advance_weather false");
        Vec3 cam;
        Vec3 aim;
        switch (subject) {
            case "cave" -> {
                Rig.cube(0, 0, 0, 12, 12, 12, Blocks.STONE);
                Rig.cube(1, 1, 5, 10, 5, 6, Blocks.AIR);
                Rig.set(Rig.at(2, 3, 5), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
                Rig.set(Rig.at(9, 3, 5), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
                ore = Blocks.COAL_ORE;
                drop = Items.COAL;
                cells = blob(5.5, 3.5, 3.0);
                cam = new Vec3(8.0, 1, 8.5);
                aim = new Vec3(5.5, 3.6, 5.0);
            }
            case "modded" -> {
                backdrop(Blocks.GRASS_BLOCK);
                cells = blob(7.0, 3.5, 3.0);
                ore = TestBlocks.tin;
                drop = TestBlocks.tin.asItem();
                for (BlockPos p : cells) Rig.set(p, TestBlocks.tin.defaultBlockState());
                for (BlockPos p : blob(2.5, 3.0, 3.0, 14, cells)) Rig.set(p, Blocks.COAL_ORE.defaultBlockState());
                cam = new Vec3(8.5, 1, 9.5);
                aim = new Vec3(5.0, 3.4, 5.0);
            }
            default -> {
                backdrop(Blocks.GRASS_BLOCK);
                ore = Blocks.COAL_ORE;
                drop = Items.COAL;
                cells = blob(5.5, 3.5, 3.0);
                cam = new Vec3(8.0, 1, 8.0);
                aim = new Vec3(5.5, 3.6, 5.0);
            }
        }
        if (!subject.equals("modded")) for (BlockPos p : cells) Rig.set(p, ore.defaultBlockState());
        Rig.hold(Rig.pick(0, 0));
        Vec3 b = Vec3.atLowerCornerOf(Rig.base);
        Vec3 feet = b.add(cam);
        Rig.player.teleportTo(feet.x, feet.y, feet.z);
        eye = feet.add(0, Rig.player.getEyeHeight(), 0);
        Vec3 d = b.add(aim).subtract(eye);
        yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
    }

    /** A stone wall (z 0-4) with a floor of {@code floor} in front of it and nothing above: daylight on the vein. */
    private static void backdrop(Block floor) {
        Rig.cube(0, 0, 0, 12, 12, 5, Blocks.STONE);
        Rig.cube(0, 0, 5, 12, 1, 7, floor);
    }

    private static List<BlockPos> blob(double cx, double cy, double cz) {
        return blob(cx, cy, cz, VEIN, List.of());
    }

    /** The {@code n} wall cells nearest the centre, jittered so the outline is not a sphere; the front layer (z 4) is always among them. */
    private static List<BlockPos> blob(double cx, double cy, double cz, int n, List<BlockPos> avoid) {
        List<BlockPos> all = new ArrayList<>();
        for (int x = 0; x < 12; x++) {
            for (int y = 1; y < 6; y++) {
                for (int z = 2; z <= 4; z++) {
                    BlockPos p = Rig.at(x, y, z);
                    if (!avoid.contains(p)) all.add(p);
                }
            }
        }
        Comparator<BlockPos> byDist = Comparator.comparingDouble(p -> {
            int x = p.getX() - Rig.base.getX(), y = p.getY() - Rig.base.getY(), z = p.getZ() - Rig.base.getZ();
            double jitter = ((x * 73856093 ^ y * 19349663 ^ z * 83492791) & 255) / 255.0 * 0.9;
            double dz = (z - cz) * 1.6;
            return Math.sqrt((x + 0.5 - cx) * (x + 0.5 - cx) + (y + 0.5 - cy) * (y + 0.5 - cy) * 1.4 + dz * dz) + jitter;
        });
        all.sort(byDist);
        return new ArrayList<>(all.subList(0, n));
    }

    private static void command(String cmd) {
        Rig.server.getCommands().performPrefixedCommand(Rig.player.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    static void aim(Minecraft mc) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        mc.player.setYHeadRot(yaw);
    }

    static void strike(MinecraftServer server) {
        BlockPos hit = cells.stream().filter(p -> p.getZ() == Rig.base.getZ() + 4)
            .min(Comparator.comparingDouble(p -> p.distToCenterSqr(eye))).orElse(cells.get(0));
        Rig.strike(hit, true);
        Rig.check("chainview: chain started", Chain.count() == 1, "chains " + Chain.count() + " struck " + hit);
        started = true;
    }

    static boolean done() {
        return started && Chain.count() == 0;
    }

    static void finish(MinecraftServer server, String subject) {
        Rig.eq("chainview " + subject + ": vein of " + VEIN + " all broke", VEIN, Rig.air(cells));
        int items = 0;
        double near = 1e9, far = 0, lowest = 1e9;
        for (ItemEntity e : Rig.level.getEntitiesOfClass(ItemEntity.class, Rig.box())) {
            if (e.getItem().getItem() != drop) continue;
            items += e.getItem().getCount();
            double dist = e.position().distanceTo(eye);
            near = Math.min(near, dist);
            far = Math.max(far, dist);
            lowest = Math.min(lowest, e.getY() - Rig.base.getY());
        }
        Rig.check("chainview " + subject + ": loot is " + VEIN + " items", items == VEIN, "items " + items + " from the eyes " + String.format("%.1f-%.1f", near, far) + " lowest y " + String.format("%.2f", lowest));
        Rig.eq("chainview " + subject + ": no tracked fx left", 0, Fx.liveCount());
        Rig.grab();
        Rig.clear();
        Rig.player.teleportTo(home.x, home.y, home.z);
    }
}
