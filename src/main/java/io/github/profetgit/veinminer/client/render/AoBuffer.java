package io.github.profetgit.veinminer.client.render;

//? if >=26.2 {
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * A stand-in's buffer under a shader pack that asked for separate AO. Sodium's chunk meshes then keep the vertex colour as the
 * tint and put the occlusion in alpha; the moving-block mesher bakes it into grey rgb, which the pack multiplies into its glow
 * test and glow strength (side faces of a glowing ore turn olive). Untinted (grey) vertex colours move the grey into alpha.
 */
final class AoBuffer implements VertexConsumer {
    private final VertexConsumer in;

    AoBuffer(VertexConsumer in) {
        this.in = in;
    }

    private static boolean grey(int c) {
        int r = c >> 16 & 255;
        return r == (c >> 8 & 255) && r == (c & 255);
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        in.addVertex(x, y, z);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        if (grey(color)) color = (color >> 16 & 255) << 24 | 0xFFFFFF;
        in.addVertex(x, y, z, color, u, v, overlay, light, nx, ny, nz);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        if (r == g && r == b) {
            a = r;
            r = g = b = 255;
        }
        in.setColor(r, g, b, a);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        if (grey(color)) color = (color >> 16 & 255) << 24 | 0xFFFFFF;
        in.setColor(color);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        in.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        in.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        in.setUv2(u, v);
        return this;
    }

//? if >=26.3 {
    @Override
    public VertexConsumer setUv3(float u, float v) {
        in.setUv3(u, v);
        return this;
    }

//?}
    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        in.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        in.setLineWidth(width);
        return this;
    }
}
//?} else {
/*final class AoBuffer {
    private AoBuffer() {}
}
*///?}
