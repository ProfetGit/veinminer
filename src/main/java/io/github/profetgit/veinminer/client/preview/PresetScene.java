package io.github.profetgit.veinminer.client.preview;

import io.github.profetgit.profetsettings.Ctx;
import io.github.profetgit.profetsettings.Gfx;
import io.github.profetgit.profetsettings.Preview;
import io.github.profetgit.veinminer.server.Config;
import java.util.Objects;

// A Quick setup's preview: a scene (or two, one after the other) playing with that preset's values on top of the defaults
// instead of the live config, put back right after the frame. With two scenes the next one's blocks drop in over the last
// 500 ms of the one before, and each one's loot fades out, so the loop closes without a cut.
final class PresetScene extends Preview {
    private final Config.Values values;
    private final VeinScene first, second;

    PresetScene(Config.Values values, VeinScene first, VeinScene second) {
        this.values = values;
        this.first = first;
        this.second = second;
    }

    @Override
    public void render(Gfx g, Ctx c, int x, int y, int w, int h) {
        Previews.override = values;
        float was = debugTime;
        try {
            if (second == null) {
                first.render(g, c, x, y, w, h);
                return;
            }
            first.chained = true;
            second.chained = true;
            float pa = first.loopMs(), pb = second.loopMs(), total = pa + pb;
            float t = clock(c, Objects.hash(values, "two"), total);
            float pre = VeinScene.PRE;
            if (t < pa) {
                boolean next = t >= pa - pre;
                play(first, g, c, x, y, w, h, t, next);
                if (next) incoming(second, g, c, x, y, w, h, pb - pre + (t - (pa - pre)));
            } else {
                boolean next = t >= total - pre;
                play(second, g, c, x, y, w, h, t - pa, next);
                if (next) incoming(first, g, c, x, y, w, h, pa - pre + (t - (total - pre)));
            }
        } finally {
            Preview.debugTime = was;
            Previews.override = null;
        }
    }

    // the scene playing; while the next one drops in, its tool is the next one's
    private static void play(VeinScene s, Gfx g, Ctx c, int x, int y, int w, int h, float t, boolean handOver) {
        Preview.debugTime = t;
        s.blocksOnly = false;
        s.hideTool = handOver;
        s.render(g, c, x, y, w, h);
    }

    private static void incoming(VeinScene s, Gfx g, Ctx c, int x, int y, int w, int h, float t) {
        Preview.debugTime = t;
        s.blocksOnly = true;
        s.hideTool = false;
        s.render(g, c, x, y, w, h);
        s.blocksOnly = false;
    }
}
