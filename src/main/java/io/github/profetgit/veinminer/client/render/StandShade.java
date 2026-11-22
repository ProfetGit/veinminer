package io.github.profetgit.veinminer.client.render;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.CardinalLighting;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws a chain stand-in (a block display) with the same face shading terrain gives the real block. Terrain bakes a fixed
 * shade per face (down 0.5, up 1, north/south 0.8, east/west 0.6); entities get it from two directional lights in the vertex
 * shader, which come out darker on the sides and the underside. The shader reads the vertex normal, so each face's normal is
 * tilted just far enough for the entity light to land on terrain's number. Nothing else about the quad changes.
 */
public final class StandShade {
    private static CardinalLighting.Type cachedType;
    private static final Matrix3f[] TILT = new Matrix3f[6];

    private StandShade() {}

    /** @return false when the vanilla path should draw it (no model, tinted, translucent) */
    public static boolean submit(BlockState block, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (block == null) return false;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return false;
        BlockStateModel model = mc.getModelManager().getBlockStateModelSet().get(block);
        if (model.hasMaterialFlag(1)) return false;
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42L), parts);
        for (BlockStateModelPart part : parts) {
            for (int d = -1; d < 6; d++) {
                for (BakedQuad quad : part.getQuads(d < 0 ? null : Direction.values()[d])) {
                    if (quad.materialInfo().tintIndex() != -1) return false;
                }
            }
        }
        Matrix3f[] tilt = tilts(level);
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockItemSheet(), (pose, buffer) -> {
            QuadInstance instance = new QuadInstance();
            instance.setLightCoords(light);
            instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
            PoseStack.Pose[] shaded = new PoseStack.Pose[6];
            for (BlockStateModelPart part : parts) {
                for (int d = -1; d < 6; d++) {
                    for (BakedQuad quad : part.getQuads(d < 0 ? null : Direction.values()[d])) {
                        int f = quad.direction().ordinal();
                        if (shaded[f] == null) {
                            shaded[f] = pose.copy();
                            shaded[f].normal().mul(tilt[f]);
                        }
                        buffer.putBakedQuad(shaded[f], quad, instance);
                    }
                }
            }
        });
        return true;
    }

    private static Matrix3f[] tilts(ClientLevel level) {
        CardinalLighting.Type type = level.dimensionType().cardinalLightType();
        if (type != cachedType) {
            CardinalLighting target = type.get();
            for (Direction dir : Direction.values()) {
                TILT[dir.ordinal()] = solve(type, dir, target.byFace(dir));
            }
            cachedType = type;
        }
        return TILT;
    }

    /** The rotation of {@code face} to the nearest normal whose entity light equals {@code shade}. */
    private static Matrix3f solve(CardinalLighting.Type type, Direction face, float shade) {
        Vector3f f = new Vector3f(face.getUnitVec3f());
        if (Math.abs(entityLight(type, f) - shade) < 0.004f) return new Matrix3f();
        Vector3f best = null;
        double bestAngle = 1e9;
        Vector3f[] axes = {new Vector3f(0, 1, 0), new Vector3f(0, -1, 0), new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};
        for (Vector3f axis : axes) {
            if (Math.abs(axis.dot(f)) > 0.99f) continue;
            Vector3f perp = axis.sub(new Vector3f(f).mul(axis.dot(f)), new Vector3f()).normalize();
            for (int i = 1; i <= 360; i++) {
                double t = Math.toRadians(i * 0.25);
                if (t >= bestAngle) break;
                Vector3f n = new Vector3f(f).mul((float) Math.cos(t)).add(new Vector3f(perp).mul((float) Math.sin(t)));
                if (Math.abs(entityLight(type, n) - shade) < 0.004f) {
                    best = n;
                    bestAngle = t;
                    break;
                }
            }
        }
        if (best == null) return new Matrix3f();
        return new Matrix3f().rotation(new Quaternionf().rotationTo(f, best));
    }

    /** The vertex shader's light for a normal (light.glsl: two directions, power 0.6, ambient 0.4). */
    private static float entityLight(CardinalLighting.Type type, Vector3fc n) {
        Vector3f l0 = new Vector3f(0.2f, 1.0f, -0.7f).normalize();
        Vector3f l1 = (type == CardinalLighting.Type.NETHER ? new Vector3f(-0.2f, -1.0f, 0.7f) : new Vector3f(-0.2f, 1.0f, 0.7f)).normalize();
        float sum = Math.max(0f, l0.dot(n)) + Math.max(0f, l1.dot(n));
        return Math.min(1f, sum * 0.6f + 0.4f);
    }
}
//?} else {
/*public final class StandShade {
    private StandShade() {}
}
*///?}
