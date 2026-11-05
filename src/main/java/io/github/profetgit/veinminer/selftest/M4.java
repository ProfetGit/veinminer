package io.github.profetgit.veinminer.selftest;

import io.github.profetgit.veinminer.server.Chain;
import io.github.profetgit.veinminer.server.Config;
import io.github.profetgit.veinminer.server.Fx;
import io.github.profetgit.veinminer.server.Tune;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;

/** Helpers the M4 server-grade scenes share. */
final class M4 {
    private M4() {}

    static boolean want(String scenes, String name) {
        if (scenes.equals("all")) return true;
        for (String s : scenes.split(",")) if (s.trim().equals(name)) return true;
        return false;
    }

    /** A stone floor under the work volume and the player standing on it in front (south) of the vein area. */
    static void arena() {
        Rig.clear();
        Rig.cube(0, 0, 0, Rig.SIZE, 1, Rig.SIZE, Blocks.STONE);
        Rig.player.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
        Config.use(Rig.PLAIN);
        Tune.reset();
    }

    /** The arena floor and the player on it again: a step that ends before real server ticks must leave the player something to stand on. */
    static void park() {
        Rig.cube(0, 0, 0, Rig.SIZE, 1, Rig.SIZE, Blocks.STONE);
        Rig.player.teleportTo(Rig.base.getX() + 4.5, Rig.base.getY() + 1, Rig.base.getZ() + 9.5);
        Rig.player.setDeltaMovement(0, 0, 0);
    }

    /** Runs a command as the server (level 4, quiet); true when it succeeded with a result above 0. */
    static boolean run(MinecraftServer server, String command) {
        return run(server, server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    static boolean run(MinecraftServer server, CommandSourceStack source, String command) {
        boolean[] ok = {false};
        server.getCommands().performPrefixedCommand(source.withCallback((success, result) -> {
            if (success && result > 0) ok[0] = true;
        }), command);
        return ok[0];
    }

    /** Runs a command that does not fail (reload, datapack): true when the command ran without an error. */
    static boolean ran(MinecraftServer server, String command) {
        boolean[] ok = {false};
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withCallback((success, result) -> ok[0] |= success), command);
        return ok[0];
    }

    static int count(Class<? extends Entity> type) {
        return Rig.level.getEntitiesOfClass(type, Rig.box()).size();
    }

    static void noLeak(String name) {
        Rig.eq(name + ": no chains left", 0, Chain.count());
        Rig.eq(name + ": no tracked displays", 0, Fx.liveCount());
        Rig.eq(name + ": no displays in the world", 0, count(Display.class));
        int other = 0;
        for (Entity e : Rig.level.getEntitiesOfClass(Entity.class, Rig.box())) {
            if (!(e instanceof ItemEntity) && !(e instanceof ExperienceOrb) && e != Rig.player && !(e instanceof net.minecraft.server.level.ServerPlayer)) other++;
        }
        Rig.eq(name + ": no other entities", 0, other);
    }

    static void deleteTree(Path dir) {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            for (Path q : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) Files.delete(q);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static void copyTree(Path from, Path to) {
        deleteTree(to);
        try (var walk = Files.walk(from)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                Path dst = to.resolve(from.relativize(src).toString());
                if (Files.isDirectory(src)) Files.createDirectories(dst);
                else {
                    Files.createDirectories(dst.getParent());
                    Files.copy(src, dst);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Path workspace() {
        return Path.of(System.getProperty("veinminer.workspace", "/home/emppu/Projects/Minecraft Datapacks"));
    }
}
