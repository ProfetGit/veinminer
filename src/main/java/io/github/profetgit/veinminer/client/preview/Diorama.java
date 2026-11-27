package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.profetsettings.Gfx;
import java.util.Random;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

// The preview stage: the subject in a soft pool of light on the box's own dark, and its tool: the player's own pickaxe
// or shovel swung along a Blockbench animation (Tool). Blocks, chips and items are drawn live from the player's own
// textures; the ground is not drawn, only its shadows. Block space: 1 = a block, the spot's floor centre is the origin,
// +x runs to the lower right, +z to the lower left. A scene draws it at half size (one texture pixel per GUI pixel).
final class Diorama {
    private Diorama() {}

    static final int W = 198, H = 150, AX = 99, AY = 71;
    static final float HX = 32, HZ = 16, TALL = 40;
    static final float UNIT = 45.25f;

    private static final Identifier GLOW = art("glow"), SHADOW = art("shadow_block"), ITEM_SHADOW = art("shadow_item"),
        SHADE_TOP = art("shade_top"), SHADE_LEFT = art("shade_left"), SHADE_RIGHT = art("shade_right");
    private static final int LIT_TOP = 0xFFFFF5E3, LIT_LEFT = 0xFFD2C2BC, LIT_RIGHT = 0xFF918AA3, LIT_CHIP = 0xFFBBB0A8;

    static final int SWING = 0, SWING_FRAMES = 18, FOLLOW = 18, FOLLOW_FRAMES = 31, RAISE = 49, RAISE_FRAMES = 16, MINE = 65, MINE_FRAMES = 12;

    record Look(Identifier top, Identifier left, Identifier right) {
        static Look all(Identifier tex) {
            return new Look(tex, tex, tex);
        }
    }

    private static Identifier art(String name) {
        return Identifier.tryParse("veinminer:textures/gui/preview/" + name + ".png");
    }

    static void backdrop(Gfx g, int x, int y) {
        g.pushTranslate(x, y);
        g.scale(0.5f, 0.5f);
        g.blit(GLOW, 0, 0, 0, 0, W * 2, H * 2, W * 2, H * 2, 0xFFFFFFFF);
        g.pop();
    }

    static float sx(float cx, double x, double z) {
        return (float) (cx + (x - z) * HX);
    }

    static float sy(float floor, double x, double y, double z) {
        return (float) (floor + (x + z) * HZ - y * TALL);
    }

    // ---- blocks ----

    // a box of {@code wide} blocks across standing from {@code lo} to {@code hi} blocks above the floor, centred on (cx, floor)
    static void box(Gfx g, float cx, float floor, Look look, double wide, double lo, double hi, int crack) {
        if (wide <= 0.001 || hi - lo <= 0.001) return;
        double h = 0.5 * wide;
        float backX = sx(cx, -h, -h), backY = sy(floor, -h, hi, -h);
        float rightX = sx(cx, h, -h), rightY = sy(floor, h, hi, -h);
        float leftX = sx(cx, -h, h), leftY = sy(floor, -h, hi, h);
        float frontX = sx(cx, h, h), frontY = sy(floor, h, hi, h);
        float drop = (float) ((hi - lo) * TALL);
        Identifier cracks = crack < 0 ? null : VeinScene.tex("destroy_stage_" + Math.min(9, crack));
        face(g, look.top(), cracks, SHADE_TOP, 64, 64, LIT_TOP, backX, backY, rightX - backX, rightY - backY, leftX - backX, leftY - backY);
        face(g, look.left(), cracks, SHADE_LEFT, 64, 80, LIT_LEFT, leftX, leftY, frontX - leftX, frontY - leftY, 0, drop);
        face(g, look.right(), cracks, SHADE_RIGHT, 64, 80, LIT_RIGHT, frontX, frontY, rightX - frontX, rightY - frontY, 0, drop);
    }

    private static void face(Gfx g, Identifier tex, Identifier cracks, Identifier shade, int sw, int sh, int tint,
        float ox, float oy, float ux, float uy, float vx, float vy) {
        g.push();
        g.mul(new Matrix3x2f(ux / 16, uy / 16, vx / 16, vy / 16, ox, oy));
        g.blit(tex, 0, 0, 0, 0, 16, 16, 16, 16, tint);
        if (cracks != null) g.blit(cracks, 0, 0, 0, 0, 16, 16, 16, 16, 0x70000000);
        g.pop();
        g.push();
        g.mul(new Matrix3x2f(ux / sw, uy / sw, vx / sh, vy / sh, ox, oy));
        g.blit(shade, 0, 0, 0, 0, sw, sh, sw, sh, 0xFFFFFFFF);
        g.pop();
    }

    // the soft shadow of a block footprint on the floor
    static void shadow(Gfx g, float cx, float floor, float size, float alpha) {
        if (size <= 0.01f) return;
        g.pushTranslate(cx, floor);
        g.scale(size / 2, size / 2);
        float a = g.alpha;
        g.alpha *= Math.min(1, size) * alpha;
        g.blit(SHADOW, -96, -56, 0, 0, 192, 112, 192, 112, 0xFFFFFFFF);
        g.alpha = a;
        g.pop();
    }

    // a soft contact shadow, fading as its owner rises {@code height} blocks
    static void groundShadow(Gfx g, float x, float y, float size, double height) {
        float a = g.alpha;
        g.alpha *= (float) Math.max(0, Math.min(1, 1 - height * 0.7));
        g.pushTranslate(x, y);
        g.scale(size / 2, size / 2);
        g.blit(ITEM_SHADOW, -32, -16, 0, 0, 64, 32, 64, 32, 0xFFFFFFFF);
        g.pop();
        g.alpha = a;
    }

    // ---- chips ----

    // one chip, flown with vanilla's particle physics for {@code age} ticks from (x, y, z) in blocks; {@code gone} is the
    // age by which it has to have faded
    static void chip(Gfx g, Random r, float cx, float floor, Identifier tex, float age, double x, double y, double z, double[] v,
        double scale, float gone) {
        float life = Math.min((int) (4 / (r.nextFloat() * 0.9f + 0.1f)), gone);
        double quad = 0.1 * (r.nextFloat() * 0.5 + 0.5) * scale;
        float u0 = r.nextFloat() * 12, v0 = r.nextFloat() * 12;
        if (age < 0 || age >= life) return;
        double xd = v[0], yd = v[1], zd = v[2], px = x, py = y, pz = z;
        for (int i = 0; i <= (int) age; i++) {
            px = x;
            py = y;
            pz = z;
            yd -= 0.04;
            x += xd;
            y += yd;
            z += zd;
            xd *= 0.98;
            yd *= 0.98;
            zd *= 0.98;
            if (y <= 0) {
                y = 0;
                yd = 0;
                xd *= 0.7;
                zd *= 0.7;
            }
        }
        double a = age - (int) age;
        double rx = px + (x - px) * a, ry = py + (y - py) * a, rz = pz + (z - pz) * a;
        float fade = Math.max(0, Math.min(1, (life - age) / 4));
        float s = (float) (quad * 2 * UNIT) * fade;
        if (s < 1) return;
        g.pushTranslate(sx(cx, rx, rz) - s / 2, sy(floor, rx, ry, rz) - s / 2);
        g.scale(s / 8, s / 8);
        g.blit(tex, 0, 0, u0, v0, 8, 8, 4, 4, 16, 16, LIT_CHIP);
        g.pop();
    }

    // ---- the note ----

    // why the scene shows vanilla, in the accent colour on one line along the bottom edge: the only text a preview has. A
    // line too wide for the box loses the end of the name before " is off".
    static void note(Gfx g, int x, int y, int w, int h, String line) {
        int avail = w - 8;
        String text = line;
        int end = line.indexOf(" is off");
        if (end < 0) end = line.indexOf(" are off");
        int cut = end;
        while (g.vanillaWidth(text) > avail && cut > 2) {
            cut--;
            text = line.substring(0, cut) + "\u2026" + line.substring(end);
        }
        while (g.vanillaWidth(text) > avail && text.length() > 4) text = text.substring(0, text.length() - 2) + "\u2026";
        g.vanilla(text, x + (w - g.vanillaWidth(text)) / 2, y + h - 12, 0xFFF0A566, true);
    }
}
