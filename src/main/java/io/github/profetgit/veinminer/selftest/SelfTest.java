package io.github.profetgit.veinminer.selftest;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.profetgit.veinminer.client.VeinminerClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * In-game test runner, inert unless -Dveinminer.selftest=&lt;out dir&gt; is set (ModTest sets it). Mode "boot" only
 * checks that the title screen is reached; "full" creates a world and runs {@link Scenarios}; "mp-host" joins a dedicated server and runs {@link MpHost}. Results go to
 * results.json, captured frames to frames/&lt;name&gt;/, then the game quits.
 */
public final class SelfTest {
    private static final String OUT = System.getProperty("veinminer.selftest");
    private static final String MODE = System.getProperty("veinminer.selftest.mode", "full");
    private static final List<String> RESULTS = new ArrayList<>();
    private static final ExecutorService WRITER = Executors.newFixedThreadPool(2);

    private static boolean finished;
    private static int titleTicks, ticks;
    private static List<Script.Step> steps;
    private static int index;
    private static String captureName;
    private static int captureLeft, captureFrame;
    private static final StringBuilder CAPTURE_TICKS = new StringBuilder();

    private SelfTest() {}

    public static boolean active() {
        return OUT != null;
    }

    public static void onClientTick(Minecraft mc) {
        if (finished) return;
        if (++ticks % 400 == 0) {
            VeinminerClient.LOG.info("[selftest] step {}/{} screen={}", index, steps == null ? 0 : steps.size(),
                TestCompat.screen(mc) == null ? null : TestCompat.screen(mc).getClass().getSimpleName());
        }
        if (steps == null && MODE.equals("mp-host")) {
            if (!mc.isGameLoadFinished()) return;
            ModTestHook.audit();
            record("boot", true, "game loaded, joining the server");
            steps = MpHost.build().steps;
        }
        if (steps == null) {
            if (!mc.isGameLoadFinished() || !(TestCompat.screen(mc) instanceof TitleScreen) || ++titleTicks < 20) return;
            ModTestHook.audit();
            record("boot", true, "title screen reached");
            if (MODE.equals("boot")) {
                finish(mc);
                return;
            }
            steps = Scenarios.build().steps;
        }
        try {
            while (index < steps.size() && steps.get(index).tick(mc)) index++;
        } catch (Throwable t) {
            VeinminerClient.LOG.error("[selftest] step {} failed", index, t);
            record("step " + index, false, t.toString());
            index = steps.size();
        }
        if (index >= steps.size()) finish(mc);
    }

    private static boolean timing;
    private static long lastFrameNs;
    private static final java.util.ArrayList<Long> FRAME_NS = new java.util.ArrayList<>();

    /** Starts (or restarts) recording the time between rendered frames. */
    static void frameTimes(boolean on) {
        timing = on;
        lastFrameNs = 0;
        if (on) FRAME_NS.clear();
    }

    /** The recorded frame times in ms, sorted. */
    static double[] frameMs() {
        double[] a = new double[FRAME_NS.size()];
        for (int i = 0; i < a.length; i++) a[i] = FRAME_NS.get(i) / 1e6;
        java.util.Arrays.sort(a);
        return a;
    }

    public static void onFrame(Minecraft mc) {
        if (timing) {
            long now = System.nanoTime();
            if (lastFrameNs != 0) FRAME_NS.add(now - lastFrameNs);
            lastFrameNs = now;
        }
        if (captureLeft <= 0) return;
        Path out = Path.of(OUT, "frames", captureName, String.format("f%03d.png", captureFrame++));
        captureLeft--;
        CAPTURE_TICKS.append(captureFrame - 1).append(' ').append(ticks).append('\n');
        if (captureLeft == 0) writeTicks();
        TestCompat.screenshot(TestCompat.mainRenderTarget(mc), (NativeImage img) -> WRITER.execute(() -> {
            try (img) {
                Files.createDirectories(out.getParent());
                img.writeToFile(out);
            } catch (IOException e) {
                VeinminerClient.LOG.warn("[selftest] frame write failed: {}", e.toString());
            }
        }));
    }

    static boolean capturing() {
        return captureLeft > 0;
    }

    static void startCapture(String name, int frames) {
        captureName = name;
        captureLeft = frames;
        captureFrame = 0;
        CAPTURE_TICKS.setLength(0);
    }

    static void stopCapture() {
        if (captureLeft > 0) {
            captureLeft = 0;
            writeTicks();
        }
    }

    /** {@code ticks.txt} beside the frames: "frame client-tick" per line, to read the real frame rate off the sheet. */
    private static void writeTicks() {
        Path file = Path.of(OUT, "frames", captureName, "ticks.txt");
        String text = CAPTURE_TICKS.toString();
        WRITER.execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, text);
            } catch (IOException e) {
                VeinminerClient.LOG.warn("[selftest] ticks write failed: {}", e.toString());
            }
        });
    }

    public static void record(String name, boolean pass, String detail) {
        RESULTS.add(String.format("{\"name\":\"%s\",\"pass\":%s,\"detail\":\"%s\"}",
            name, pass, detail.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")));
        VeinminerClient.LOG.info("[selftest] {} {} {}", pass ? "PASS" : "FAIL", name, detail);
    }

    private static void finish(Minecraft mc) {
        finished = true;
        WRITER.shutdown();
        try {
            WRITER.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        String json = String.format("{\"loader\":\"%s\",\"mc\":\"%s\",\"results\":[%s]}%n",
            "fabric", TestCompat.versionName(), String.join(",", RESULTS));
        try {
            Files.createDirectories(Path.of(OUT));
            Files.writeString(Path.of(OUT, "results.json"), json);
        } catch (IOException e) {
            VeinminerClient.LOG.error("[selftest] could not write results", e);
        }
        mc.stop();
    }
}
