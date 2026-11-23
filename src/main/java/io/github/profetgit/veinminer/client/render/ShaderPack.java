package io.github.profetgit.veinminer.client.render;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.profetgit.veinminer.Veinminer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Iris, reflective (no dependency): whether a shader pack is on, and the per-vertex block id its chunk meshes carry. */
public final class ShaderPack {
    private static MethodHandle inUse;
    private static boolean tried;

    private ShaderPack() {}

    public static boolean active() {
        if (!tried) {
            tried = true;
            // Oculus keeps Iris's classes under its own mod id
            if (Veinminer.platform().isModLoaded("iris") || Veinminer.platform().isModLoaded("oculus")) {
                try {
                    inUse = MethodHandles.publicLookup().findStatic(Class.forName("net.irisshaders.iris.Iris"), "isPackInUseQuick", MethodType.methodType(boolean.class));
                } catch (Throwable t) {
                    Veinminer.LOG.warn("Veinminer: can't ask Iris whether a shader pack is on ({})", t.toString());
                }
            }
        }
        if (inUse == null) return false;
        try {
            return (boolean) inUse.invokeExact();
        } catch (Throwable t) {
            inUse = null;
            return false;
        }
    }

    private static MethodHandle stateIds, idOf, beginBlock;
    private static Class<?> sensitive;
    private static boolean tagTried;
    private static BlockState tagState;
    private static BlockPos tagPos;

    /** The stand-in being tesselated (null when none), for {@link #tag}. */
    public static void tagging(BlockState state, BlockPos pos) {
        tagState = state;
        tagPos = pos;
    }

    /**
     * Gives the buffer of a stand-in the block's id in the shader pack (Iris's per-vertex mc_Entity, plus emission and position in
     * the section). Iris writes those from {@code BlockSensitiveBufferBuilder.beginBlock}, which only its own chunk meshing calls;
     * moving blocks arrive with the terrain program but no id, so id-driven looks such as an ore's glow would be missing.
     */
    public static void tag(VertexConsumer vc) {
        if (tagState == null || !active()) return;
        try {
            if (!tagTried) {
                tagTried = true;
                MethodHandles.Lookup l = MethodHandles.publicLookup();
                Class<?> w = Class.forName("net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings");
                stateIds = l.findVirtual(w, "getBlockStateIds", MethodType.methodType(Class.forName("it.unimi.dsi.fastutil.objects.Object2IntMap")))
                    .bindTo(w.getField("INSTANCE").get(null));
                idOf = l.findVirtual(Class.forName("it.unimi.dsi.fastutil.objects.Object2IntFunction"), "getOrDefault", MethodType.methodType(int.class, Object.class, int.class));
                sensitive = Class.forName("net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder");
                beginBlock = l.findVirtual(sensitive, "beginBlock", MethodType.methodType(void.class, int.class, byte.class, byte.class, int.class, int.class, int.class));
            }
            if (beginBlock == null || !sensitive.isInstance(vc)) return;
            Object ids = stateIds.invoke();
            if (ids == null) return;
            int id = (int) idOf.invoke(ids, tagState, -1);
            if (id < 0) return;
            beginBlock.invoke(vc, id, (byte) 0, (byte) tagState.getLightEmission(), tagPos.getX() & 15, tagPos.getY() & 15, tagPos.getZ() & 15);
        } catch (Throwable t) {
            beginBlock = null;
            Veinminer.LOG.warn("Veinminer: can't give stand-ins their shader-pack id ({})", t.toString());
        }
    }
}
//?} else {
/*public final class ShaderPack {
    private ShaderPack() {}
}
*///?}
