package io.github.profetgit.veinminer.client.render;

//? if >=26.2 {
import io.github.profetgit.veinminer.core.StandKeys;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The light of the chain's stand-ins, for the particles that spring from them. A particle takes the light of the cell it
 * is in, and a block deep in a vein sits in the dark, so its crumbs came out black. Near a stand-in a particle gets at
 * least the stand-in's own light.
 */
public final class ChainLight {
    private static final double RANGE_SQ = 2.25 * 2.25;
    private static final List<Vec3> AT = new ArrayList<>();
    private static final List<Integer> LIGHT = new ArrayList<>();
    private static long stamp = Long.MIN_VALUE;
    private static ClientLevel stampLevel;

    private ChainLight() {}

    /** @return the light to show at the point, never darker than {@code light} */
    public static int floor(double x, double y, double z, int light) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return light;
        long now = level.getGameTime();
        if (now != stamp || level != stampLevel) {
            stamp = now;
            stampLevel = level;
            AT.clear();
            LIGHT.clear();
            for (Entity e : level.entitiesForRendering()) {
                if (!(e instanceof Display.BlockDisplay d)) continue;
                Display.RenderState rs = d.renderState();
                if (rs == null || rs.glowColorOverride() != StandKeys.MARK || rs.brightnessOverride() == -1) continue;
                AT.add(d.position());
                LIGHT.add(rs.brightnessOverride());
            }
        }
        int best = light;
        for (int i = 0; i < AT.size(); i++) {
            Vec3 p = AT.get(i);
            double dx = p.x - x, dy = p.y + 0.5 - y, dz = p.z - z;
            if (dx * dx + dy * dy + dz * dz < RANGE_SQ) best = LightCoordsUtil.max(best, LIGHT.get(i));
        }
        return best;
    }
}
//?} else {
/*public final class ChainLight {
    private ChainLight() {}
}
*///?}
