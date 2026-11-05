package io.github.profetgit.veinminer.server;

import com.mojang.math.Transformation;
import io.github.profetgit.veinminer.core.StandKeys;
import io.github.profetgit.veinminer.mixin.BlockDisplayAccess;
import io.github.profetgit.veinminer.mixin.DisplayAccess;
import io.github.profetgit.veinminer.mixin.ItemDisplayAccess;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The chain's displays, posed straight from Java: block_display stand-ins and item_display loot ghosts. They are never
 * saved ({@code EntityMixin}), tagged {@link #TAG}, and each one belongs to a {@link Chain}; {@link #sweep} kills any whose
 * chain is gone.
 */
public final class Fx {
    public static final String TAG = "veinminer.fx";
    public static final String GHOST_TAG = "veinminer.ghost";
    private static final Map<Display, Chain> LIVE = new IdentityHashMap<>();
    private static final Quaternionf NONE = new Quaternionf();

    private Fx() {}

    public static boolean ours(Entity e) {
        return e instanceof Display && e.entityTags().contains(TAG);
    }

    public static Collection<Display> live() {
        return List.copyOf(LIVE.keySet());
    }

    public static int liveCount() {
        return LIVE.size();
    }

    /** A stand-in for the block at {@code block}, standing in the cell {@code block + n}, pose = the block's own place, a hair larger. */
    static Display.BlockDisplay stand(Chain owner, ServerLevel level, BlockState state, BlockPos block, int[] n) {
        Display.BlockDisplay d = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level);
        d.addTag(TAG);
        d.snapTo(block.getX() + n[0], block.getY() + n[1], block.getZ() + n[2]);
        ((BlockDisplayAccess) d).veinminer$setBlockState(state);
        double s = 1 + 2 * StandKeys.PAD;
        ((DisplayAccess) d).veinminer$setTransformation(of(-n[0] - StandKeys.PAD, -n[1] - StandKeys.PAD, -n[2] - StandKeys.PAD, NONE, s, s, s));
        level.addFreshEntity(d);
        LIVE.put(d, owner);
        return d;
    }

    /** One squash, stretch or vanish key: about the block centre, back by the cell offset. */
    static void key(Display d, int[] n, StandKeys.Key k, double squash) {
        double sxz = StandKeys.tuned(k.scaleXz(), squash), sy = StandKeys.tuned(k.scaleY(), squash);
        pose(d, new Vector3f(StandKeys.translation((float) sxz, n[0]), StandKeys.translation((float) sy, n[1]), StandKeys.translation((float) sxz, n[2])),
            NONE, new Vector3f((float) sxz, (float) sy, (float) sxz), k.duration());
    }

    /** A ghost parked at {@code at}, invisible (scale 0), translated {@code from} away so a pop starts where the item spawned. */
    static Display.ItemDisplay ghost(Chain owner, ServerLevel level, ItemStack stack, Vec3 at, Vec3 from) {
        Display.ItemDisplay d = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
        d.addTag(TAG);
        d.addTag(GHOST_TAG);
        d.snapTo(at.x, at.y, at.z);
        ((ItemDisplayAccess) d).veinminer$setItemStack(stack);
        ((ItemDisplayAccess) d).veinminer$setItemTransform(ItemDisplayContext.GROUND);
        DisplayAccess a = (DisplayAccess) d;
//? if >=1.20.2
        a.veinminer$setTeleportDuration(1);
        a.veinminer$setTransformation(of(from.x, from.y, from.z, NONE, 0, 0, 0));
        level.addFreshEntity(d);
        LIVE.put(d, owner);
        return d;
    }

    /** A new pose, reached over {@code ticks} from when it arrives. */
    static void pose(Display d, Vector3f translation, Quaternionf left, Vector3f scale, int ticks) {
        DisplayAccess a = (DisplayAccess) d;
        a.veinminer$setTransformation(new Transformation(translation, left, scale, NONE));
        a.veinminer$setInterpolationDuration(ticks);
        a.veinminer$setInterpolationDelay(0);
    }

    static void pose(Display d, double tx, double ty, double tz, Quaternionf left, double sx, double sy, double sz, int ticks) {
        pose(d, new Vector3f((float) tx, (float) ty, (float) tz), left, new Vector3f((float) sx, (float) sy, (float) sz), ticks);
    }

    static Transformation of(double x, double y, double z, Quaternionf left, double sx, double sy, double sz) {
        return new Transformation(new Vector3f((float) x, (float) y, (float) z), left, new Vector3f((float) sx, (float) sy, (float) sz), NONE);
    }

    static void kill(Display d) {
        LIVE.remove(d);
        if (!d.isRemoved()) d.discard();
    }

    /** Kills the displays whose chain has ended (a bug, a chunk unload, a hot swap) and forgets the ones already removed. */
    public static void sweep() {
        if (LIVE.isEmpty()) return;
        List<Display> kill = new ArrayList<>();
        for (Iterator<Map.Entry<Display, Chain>> it = LIVE.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Display, Chain> e = it.next();
            if (e.getKey().isRemoved()) it.remove();
            else if (e.getValue().finished()) kill.add(e.getKey());
        }
        for (Display d : kill) kill(d);
    }

    public static void clear() {
        for (Display d : List.copyOf(LIVE.keySet())) kill(d);
    }
}
